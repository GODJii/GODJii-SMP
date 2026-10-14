package com.godjiismp;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

import java.util.*;

/** Arena sessions. Player state is snapshotted before issuing the temporary arena kit. */
public final class ArenaManager {
	private ArenaManager() {}
	public enum Mode { DUEL, EVENT }
	private static final int X_SIZE = 60, Z_SIZE = 30, Y_SIZE = 15;
	private static final int OFFSET = 10_000;
	private static final long EVENT_INTERVAL = 72_000L; // one real hour at 20 TPS
	private static int arenaX = OFFSET, arenaZ;
	private static int arenaMinY;
	private static final Map<UUID, Snapshot> SNAPSHOTS = new HashMap<>();
	private static final Map<UUID, UUID> CHALLENGES = new HashMap<>(); // challenged -> challenger
	private static final Map<UUID, Long> STAKES = new HashMap<>();
	private static final Set<UUID> ARENA_PLAYERS = new HashSet<>();
	private static final Set<UUID> EVENT_ENTRANTS = new HashSet<>();
	private static final Set<UUID> SPECTATORS = new HashSet<>();
	private static final Set<UUID> PENDING_RESPAWN = new HashSet<>();
	private static Mode mode;
	private static UUID duelA, duelB;
	private static long currentStake, eventPrize;
	private static long nextEventTick;
	private static boolean eventHadMultiplePlayers;
	private static boolean oldReducedDebug;
	private static MinecraftServer activeServer;
	private static boolean arenaBuilt;

	public static void registerHooks() {
		// Let vanilla complete the real death. A Totem prevents this callback from running,
		// so keeping one in the offhand retains normal Totem behavior.
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && ARENA_PLAYERS.contains(player.getUUID())) {
				player.getInventory().clearContent(); // never drop the temporary arena kit
				onActualDeath(player);
			}
			return true;
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> afterRespawn(newPlayer));
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) ->
				world.dimension() != Level.OVERWORLD || !inside(pos) || state.is(Blocks.OBSIDIAN));
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p = handler.player;
			UUID id = p.getUUID();
			if (SNAPSHOTS.containsKey(id) && !ARENA_PLAYERS.contains(id)) restore(p);
			else if (mode == Mode.EVENT && SPECTATORS.contains(id)) putInSpectator(p);
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer p = handler.player;
			UUID id = p.getUUID();
			if (mode == Mode.DUEL && (id.equals(duelA) || id.equals(duelB))) {
				UUID winnerId = id.equals(duelA) ? duelB : duelA;
				Money.give(server, winnerId, currentStake * 2);
				ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
				if (winner != null) {
					restore(winner);
					tell(winner, "คู่ต่อสู้ออกจากเกม คุณชนะ รับเงินเดิมพันแล้ว", ChatFormatting.GOLD);
				}
				ARENA_PLAYERS.remove(id); // keep this player's snapshot for their next login
				clearMatch();
			} else if (mode == Mode.EVENT && ARENA_PLAYERS.contains(id)) {
				SPECTATORS.add(id);
				broadcast(server, p.getName().getString() + " ออกจากอีเวนต์และตกรอบ", ChatFormatting.GRAY);
				resolveEvent(server);
			}
		});
	}

	private static boolean inside(BlockPos p) {
		return p.getY() >= arenaMinY && p.getY() < arenaMinY + Y_SIZE
				&& p.getX() >= arenaX - X_SIZE / 2 && p.getX() < arenaX - X_SIZE / 2 + X_SIZE
				&& p.getZ() >= arenaZ - Z_SIZE / 2 && p.getZ() < arenaZ - Z_SIZE / 2 + Z_SIZE;
	}

	public static boolean inArena(UUID id) { return ARENA_PLAYERS.contains(id); }
	public static boolean isParticipant(UUID id) { return ARENA_PLAYERS.contains(id) && !SPECTATORS.contains(id); }

	public static void tick(MinecraftServer server) {
		if (nextEventTick == 0) nextEventTick = server.overworld().getGameTime() + EVENT_INTERVAL;
		processPendingRespawns(server);
		if (arenaBuilt && mode != null && server.overworld().getGameTime() % 10 == 0) repairShell(server.overworld());
		if (mode != null) clampPlayers(server);
		if (mode == null && server.overworld().getGameTime() >= nextEventTick) {
			startEvent(server, null);
			nextEventTick = server.overworld().getGameTime() + EVENT_INTERVAL;
		}
	}

	public static void schedule(MinecraftServer server) {
		nextEventTick = server.overworld().getGameTime() + EVENT_INTERVAL;
	}

	private static BlockPos center() { return new BlockPos(arenaX, arenaMinY + 3, arenaZ); }

	private static void buildArena(ServerLevel level) {
		arenaX = level.getSharedSpawnPos().getX() + OFFSET;
		arenaZ = level.getSharedSpawnPos().getZ();
		arenaMinY = level.getMinY() + 12; // deep below the surface in the Overworld
		activeServer = level.getServer();
		oldReducedDebug = level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_REDUCEDDEBUGINFO);
		level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_REDUCEDDEBUGINFO).set(true, level.getServer());

		int minX = arenaX - X_SIZE / 2, maxX = minX + X_SIZE - 1;
		int minZ = arenaZ - Z_SIZE / 2, maxZ = minZ + Z_SIZE - 1;
		int maxY = arenaMinY + Y_SIZE - 1;
		for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) for (int y = arenaMinY; y <= maxY; y++) {
			boolean shell = x == minX || x == maxX || z == minZ || z == maxZ || y == arenaMinY || y == maxY;
			level.setBlockAndUpdate(new BlockPos(x, y, z), shell ? Blocks.BEDROCK.defaultBlockState() : Blocks.AIR.defaultBlockState());
		}
		decorateArena(level, minX, maxX, minZ, maxZ, maxY);
		arenaBuilt = true;
	}

	/** Dark stone palette, framed soft lighting, and a bedrock shell for explosion-proof boundaries. */
	private static void decorateArena(ServerLevel level, int minX, int maxX, int minZ, int maxZ, int maxY) {
		int floor = arenaMinY + 1;
		for (int x = minX + 1; x < maxX; x++) for (int z = minZ + 1; z < maxZ; z++) {
			boolean stripe = ((x + z) & 7) == 0 || Math.abs(x - arenaX) <= 1 || Math.abs(z - arenaZ) <= 1;
			level.setBlockAndUpdate(new BlockPos(x, floor, z),
					(stripe ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.DEEPSLATE_TILES).defaultBlockState());
		}
		// Raised dark panels with recessed sea-lanterns keep the light even without glowstone glare.
		for (int z = minZ + 4; z < maxZ - 3; z += 8) {
			for (int y : new int[]{arenaMinY + 5, arenaMinY + 9}) {
				panel(level, minX + 1, z, y);
				panel(level, maxX - 1, z, y);
			}
		}
		for (int x = minX + 5; x < maxX - 4; x += 8) {
			panel(level, x, minZ + 1, arenaMinY + 7);
			panel(level, x, maxZ - 1, arenaMinY + 7);
		}
		// Ceiling lamps sit above player height and are surrounded by dark trim.
		for (int x = minX + 7; x < maxX - 5; x += 12) for (int z = minZ + 5; z < maxZ - 4; z += 8) {
			BlockPos lamp = new BlockPos(x, maxY - 1, z);
			level.setBlockAndUpdate(lamp, Blocks.SEA_LANTERN.defaultBlockState());
		}
	}

	private static void panel(ServerLevel level, int x, int z, int y) {
		for (int dy = -1; dy <= 1; dy++) {
			level.setBlockAndUpdate(new BlockPos(x, y + dy, z),
					dy == 0 ? Blocks.SEA_LANTERN.defaultBlockState() : Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState());
		}
	}

	private static void repairShell(ServerLevel level) {
		int minX = arenaX - X_SIZE / 2, maxX = minX + X_SIZE - 1;
		int minZ = arenaZ - Z_SIZE / 2, maxZ = minZ + Z_SIZE - 1;
		int maxY = arenaMinY + Y_SIZE - 1;
		for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
			for (int y = arenaMinY; y <= maxY; y++) {
				if (x == minX || x == maxX || z == minZ || z == maxZ || y == arenaMinY || y == maxY) {
					BlockPos pos = new BlockPos(x, y, z);
					if (!level.getBlockState(pos).is(Blocks.BEDROCK)) level.setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
				}
			}
		}
	}

	private static void clampPlayers(MinecraftServer server) {
		for (UUID id : ARENA_PLAYERS) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p == null || PENDING_RESPAWN.contains(id)) continue;
			if (p.serverLevel() != server.overworld()) {
				p.teleportTo(server.overworld(), arenaX, arenaMinY + 2, arenaZ, p.getYRot(), p.getXRot());
				continue;
			}
			double x = Math.max(arenaX - X_SIZE / 2 + 1, Math.min(arenaX + X_SIZE / 2 - 2, p.getX()));
			double z = Math.max(arenaZ - Z_SIZE / 2 + 1, Math.min(arenaZ + Z_SIZE / 2 - 2, p.getZ()));
			double y = Math.max(arenaMinY + 2, Math.min(arenaMinY + Y_SIZE - 3, p.getY()));
			if (x != p.getX() || y != p.getY() || z != p.getZ()) p.teleportTo(server.overworld(), x, y, z, p.getYRot(), p.getXRot());
		}
	}

	public static void challenge(ServerPlayer from, ServerPlayer to, long stake) {
		if (!Config.arenaEnabled) { tell(from, "อารีน่ายังปิดอยู่: ตั้ง arenaEnabled เป็น true แล้วรีสตาร์ตเซิร์ฟเวอร์ทดสอบ", ChatFormatting.RED); return; }
		if (from == to || stake < 10_000 || mode != null || ARENA_PLAYERS.contains(from.getUUID()) || ARENA_PLAYERS.contains(to.getUUID())) {
			tell(from, "คำท้าหรือเงินเดิมพันไม่ถูกต้อง", ChatFormatting.RED); return;
		}
		if (Money.balance(from.server, from.getUUID()) < stake || Money.balance(to.server, to.getUUID()) < stake) {
			tell(from, "ทั้งสองฝ่ายต้องมีเงินอย่างน้อยเท่าเงินเดิมพัน", ChatFormatting.RED); return;
		}
		CHALLENGES.put(to.getUUID(), from.getUUID()); STAKES.put(to.getUUID(), stake);
		to.sendSystemMessage(Component.literal(from.getName().getString() + " ท้าดวลเดิมพัน $" + Money.fmt(stake) + " พิมพ์ /arena accept เพื่อรับ").withStyle(ChatFormatting.GOLD));
		tell(from, "ส่งคำท้าแล้ว", ChatFormatting.GREEN);
	}

	public static boolean accept(ServerPlayer to) {
		UUID fromId = CHALLENGES.remove(to.getUUID());
		Long stake = STAKES.remove(to.getUUID());
		ServerPlayer from = fromId == null ? null : to.server.getPlayerList().getPlayer(fromId);
		if (from == null || stake == null || mode != null || Money.balance(to.server, to.getUUID()) < stake || Money.balance(to.server, fromId) < stake) {
			tell(to, "คำท้าหมดอายุหรือเงินไม่พอ", ChatFormatting.RED); return false;
		}
		if (!Money.take(to.server, fromId, stake)) return false;
		if (!Money.take(to.server, to.getUUID(), stake)) { Money.give(to.server, fromId, stake); return false; }
		mode = Mode.DUEL; duelA = fromId; duelB = to.getUUID(); currentStake = stake;
		ServerLevel level = to.server.overworld(); buildArena(level);
		enter(from, level, -10, 0); enter(to, level, 10, 0);
		broadcast(to.server, "ดวลเริ่มแล้ว! ผู้ชนะรับเงินเดิมพัน $" + Money.fmt(stake * 2), ChatFormatting.GOLD);
		return true;
	}

	public static void startEvent(MinecraftServer server, ServerPlayer starter) {
		if (!Config.arenaEnabled) { if (starter != null) tell(starter, "อารีน่ายังปิดอยู่: ตั้ง arenaEnabled เป็น true แล้วรีสตาร์ตเซิร์ฟเวอร์ทดสอบ", ChatFormatting.RED); return; }
		if (mode != null) { if (starter != null) tell(starter, "มีอารีน่ากำลังใช้งาน", ChatFormatting.RED); return; }
		mode = Mode.EVENT;
		eventPrize = 1_000_000L + new Random().nextInt(2_000_001);
		eventHadMultiplePlayers = false;
		EVENT_ENTRANTS.clear();
		ServerLevel level = server.overworld(); buildArena(level);
		if (starter != null) enter(starter, level, 0, 0);
		broadcast(server, "อีเวนต์อารีน่า: ต่อสู้จนเหลือคนสุดท้าย! ผู้ชนะได้เงิน $" + Money.fmt(eventPrize) + ". ใช้ /arena join เพื่อเข้าร่วม", ChatFormatting.LIGHT_PURPLE);
	}

	public static void join(ServerPlayer p) {
		if (mode != Mode.EVENT) { tell(p, "ยังไม่มีอีเวนต์เปิดอยู่", ChatFormatting.RED); return; }
		if (ARENA_PLAYERS.contains(p.getUUID())) { tell(p, "คุณอยู่ในอีเวนต์แล้ว", ChatFormatting.YELLOW); return; }
		enter(p, p.server.overworld(), 0, 0);
	}

	private static void enter(ServerPlayer p, ServerLevel level, int dx, int dz) {
		UUID id = p.getUUID();
		SNAPSHOTS.put(id, new Snapshot(p));
		ARENA_PLAYERS.add(id);
		if (mode == Mode.EVENT) {
			EVENT_ENTRANTS.add(id);
			if (EVENT_ENTRANTS.size() >= 2) eventHadMultiplePlayers = true;
		}
		SPECTATORS.remove(id); PENDING_RESPAWN.remove(id);
		p.getInventory().clearContent();
		p.setGameMode(GameType.SURVIVAL);
		p.setHealth(p.getMaxHealth()); p.setAbsorptionAmount(0);
		p.teleportTo(level, arenaX + dx, arenaMinY + 2, arenaZ + dz, p.getYRot(), p.getXRot());
		giveKit(p);
		p.sendSystemMessage(Component.literal("เข้าสู่อารีน่าแล้ว: ของเดิมจะคืนเมื่อจบกิจกรรม").withStyle(ChatFormatting.AQUA));
	}

	private static void giveKit(ServerPlayer p) {
		Inventory inv = p.getInventory();
		inv.armor.set(0, new ItemStack(Items.NETHERITE_BOOTS));
		inv.armor.set(1, new ItemStack(Items.NETHERITE_LEGGINGS));
		inv.armor.set(2, new ItemStack(Items.NETHERITE_CHESTPLATE));
		inv.armor.set(3, new ItemStack(Items.NETHERITE_HELMET));
		inv.add(new ItemStack(Items.NETHERITE_SWORD));
		inv.add(new ItemStack(Items.NETHERITE_PICKAXE));
		inv.add(new ItemStack(Items.BOW));
		inv.add(new ItemStack(Items.ARROW, 32));
		inv.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 10));
		inv.add(new ItemStack(Items.OBSIDIAN, 15));
		inv.add(new ItemStack(Items.END_CRYSTAL, 15));
		inv.offhand.set(0, new ItemStack(Items.TOTEM_OF_UNDYING));
		inv.add(new ItemStack(Items.TOTEM_OF_UNDYING, 4));
		inv.add(new ItemStack(Items.ENDER_PEARL, 16));
		p.inventoryMenu.broadcastChanges();
	}

	private static void onActualDeath(ServerPlayer loser) {
		UUID id = loser.getUUID();
		PENDING_RESPAWN.add(id);
		if (mode == Mode.EVENT) {
			SPECTATORS.add(id);
			tell(loser, "คุณตายในอีเวนต์จริงแล้ว กด Respawn เพื่อกลับมาเป็นผู้ชม", ChatFormatting.GRAY);
			resolveEvent(loser.server);
			return;
		}
		if (mode == Mode.DUEL) {
			UUID winnerId = id.equals(duelA) ? duelB : duelA;
			Money.give(loser.server, winnerId, currentStake * 2);
			ServerPlayer winner = loser.server.getPlayerList().getPlayer(winnerId);
			if (winner != null) {
				restore(winner);
				tell(winner, "ชนะดวล! เงินเดิมพัน $" + Money.fmt(currentStake * 2) + " เข้าบัญชีแล้ว", ChatFormatting.GOLD);
			}
			tell(loser, "คุณแพ้ดวล เมื่อเกิดใหม่ของเดิมจะคืนให้", ChatFormatting.GRAY);
			ARENA_PLAYERS.remove(id);
			clearMatch();
		}
	}

	private static void resolveEvent(MinecraftServer server) {
		if (mode != Mode.EVENT) return;
		List<UUID> contenders = EVENT_ENTRANTS.stream().filter(id -> !SPECTATORS.contains(id)).toList();
		if (contenders.size() == 1 && eventHadMultiplePlayers) {
			UUID winnerId = contenders.get(0);
			Money.give(server, winnerId, eventPrize);
			ServerPlayer winner = server.getPlayerList().getPlayer(winnerId);
			if (winner != null) tell(winner, "ชนะอีเวนต์! รับเงิน $" + Money.fmt(eventPrize) + " แล้ว", ChatFormatting.GOLD);
			broadcast(server, "อีเวนต์จบแล้ว ผู้ชนะรับเงิน $" + Money.fmt(eventPrize), ChatFormatting.GOLD);
			endEvent(server);
		} else if (contenders.isEmpty()) {
			broadcast(server, "อีเวนต์จบแล้ว ไม่มีผู้ชนะ", ChatFormatting.GRAY);
			endEvent(server);
		}
	}

	private static void afterRespawn(ServerPlayer p) {
		UUID id = p.getUUID();
		if (!PENDING_RESPAWN.remove(id)) return;
		if (mode == Mode.EVENT && ARENA_PLAYERS.contains(id) && SPECTATORS.contains(id)) {
			putInSpectator(p);
		} else {
			restore(p);
		}
	}

	private static void putInSpectator(ServerPlayer p) {
		p.getInventory().clearContent();
		p.setGameMode(GameType.SPECTATOR);
		p.setHealth(p.getMaxHealth());
		p.teleportTo(p.server.overworld(), arenaX, arenaMinY + 2, arenaZ, 0, 0);
	}

	private static void processPendingRespawns(MinecraftServer server) {
		for (UUID id : new ArrayList<>(PENDING_RESPAWN)) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (p != null && !p.isDeadOrDying()) afterRespawn(p);
		}
	}

	public static void leave(ServerPlayer p) {
		UUID id = p.getUUID();
		if (!ARENA_PLAYERS.contains(id)) return;
		if (mode == Mode.DUEL) {
			UUID winnerId = id.equals(duelA) ? duelB : duelA;
			Money.give(p.server, winnerId, currentStake * 2);
			ServerPlayer winner = p.server.getPlayerList().getPlayer(winnerId);
			if (winner != null) restore(winner);
			restore(p);
			clearMatch();
			if (winner != null) tell(winner, "คู่ต่อสู้ยอมแพ้ คุณชนะและรับเงินแล้ว", ChatFormatting.GOLD);
			return;
		}
		if (mode == Mode.EVENT) {
			restore(p); EVENT_ENTRANTS.remove(id); SPECTATORS.remove(id);
			resolveEvent(p.server);
		}
	}

	public static void stop(MinecraftServer server) {
		if (mode == Mode.EVENT) endEvent(server);
		else if (mode == Mode.DUEL) {
			Money.give(server, duelA, currentStake); Money.give(server, duelB, currentStake);
			ServerPlayer a = server.getPlayerList().getPlayer(duelA), b = server.getPlayerList().getPlayer(duelB);
			if (a != null) restore(a); if (b != null) restore(b);
			clearMatch();
		}
	}

	private static void endEvent(MinecraftServer server) {
		for (UUID id : new ArrayList<>(ARENA_PLAYERS)) {
			ServerPlayer p = server.getPlayerList().getPlayer(id);
			if (PENDING_RESPAWN.contains(id)) {
				ARENA_PLAYERS.remove(id); // restore after the real respawn
			} else if (p != null) {
				restore(p);
			}
		}
		clearMatch();
	}

	private static void clearMatch() {
		if (mode != null && activeServer != null) {
			var rules = activeServer.overworld().getGameRules();
			rules.getRule(net.minecraft.world.level.GameRules.RULE_REDUCEDDEBUGINFO).set(oldReducedDebug, activeServer);
		}
		mode = null; activeServer = null; duelA = duelB = null; currentStake = 0;
		ARENA_PLAYERS.clear(); EVENT_ENTRANTS.clear(); SPECTATORS.clear(); eventHadMultiplePlayers = false;
	}

	private static void restore(ServerPlayer p) {
		UUID id = p.getUUID();
		Snapshot s = SNAPSHOTS.remove(id);
		ARENA_PLAYERS.remove(id); PENDING_RESPAWN.remove(id); EVENT_ENTRANTS.remove(id); SPECTATORS.remove(id);
		if (s == null) return;
		s.restore(p);
	}

	private static void tell(ServerPlayer p, String s, ChatFormatting color) {
		p.sendSystemMessage(Component.literal(s).withStyle(color));
	}

	private static void broadcast(MinecraftServer s, String text, ChatFormatting color) {
		s.getPlayerList().broadcastSystemMessage(Component.literal(text).withStyle(color), false);
	}

	private static final class Snapshot {
		final List<ItemStack> items = new ArrayList<>();
		final ResourceKey<Level> dimension;
		final double x, y, z;
		final float yaw, pitch, health, absorption;
		final int food, level, totalExperience;
		final float saturation, exhaustion, xp;
		final GameType gameType;
		Snapshot(ServerPlayer p) {
			for (int i = 0; i < p.getInventory().getContainerSize(); i++) items.add(p.getInventory().getItem(i).copy());
			dimension = p.level().dimension(); x = p.getX(); y = p.getY(); z = p.getZ(); yaw = p.getYRot(); pitch = p.getXRot(); health = p.getHealth(); absorption = p.getAbsorptionAmount();
			food = p.getFoodData().getFoodLevel(); saturation = p.getFoodData().getSaturationLevel(); exhaustion = p.getFoodData().getExhaustionLevel();
			level = p.experienceLevel; totalExperience = p.totalExperience; xp = p.experienceProgress; gameType = p.gameMode.getGameModeForPlayer();
		}
		void restore(ServerPlayer p) {
			p.getInventory().clearContent();
			for (int i = 0; i < items.size(); i++) p.getInventory().setItem(i, items.get(i));
			p.setGameMode(gameType);
			ServerLevel world = p.server.getLevel(dimension);
			if (world != null) p.teleportTo(world, x, y, z, yaw, pitch);
			p.setHealth(Math.min(health, p.getMaxHealth())); p.setAbsorptionAmount(absorption);
			p.getFoodData().setFoodLevel(food); p.getFoodData().setSaturation(saturation); p.getFoodData().setExhaustion(exhaustion);
			p.experienceLevel = level; p.totalExperience = totalExperience; p.experienceProgress = xp;
			p.inventoryMenu.broadcastChanges();
		}
	}
}
