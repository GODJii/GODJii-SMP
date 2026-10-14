package com.godjiismp;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
import net.minecraft.world.level.GameRules;

import java.util.*;

/** Server-side arena sessions. Player inventory/state is captured before any arena kit is issued. */
public final class ArenaManager {
	private ArenaManager() {}
	public enum Mode { DUEL, EVENT }
	private static final int X_SIZE=60, Z_SIZE=30, Y_SIZE=15;
	private static final int OFFSET=10_000;
	private static int arenaX=OFFSET, arenaZ=0;
	private static final long EVENT_INTERVAL=72_000L; // one real hour in game ticks
	private static final Map<UUID, Snapshot> SNAPSHOTS=new HashMap<>();
	private static final Map<UUID, UUID> CHALLENGES=new HashMap<>(); // challenged -> challenger
	private static final Map<UUID, Long> STAKES=new HashMap<>();
	private static final Map<UUID, UUID> TEAMS=new HashMap<>();
	private static final Set<UUID> SPECTATORS=new HashSet<>();
	private static final Set<UUID> ARENA_PLAYERS=new HashSet<>();
	private static Mode mode;
	private static UUID duelA, duelB;
	private static long nextEventTick;
	private static boolean prizeClaimed;
	private static BlockPos prizePos;
	private static long prizeValue;
	private static boolean oldReducedDebug;
	private static MinecraftServer activeServer;
	private static boolean arenaBuilt;

	public static void registerHooks() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (entity instanceof ServerPlayer player && ARENA_PLAYERS.contains(player.getUUID())) {
				eliminate(player);
				return false;
			}
			return true;
		});
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> world.dimension()!=Level.OVERWORLD || !inside(pos));
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer p=handler.player;
			if(SNAPSHOTS.containsKey(p.getUUID()) && !ARENA_PLAYERS.contains(p.getUUID())) restore(p);
		});
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer p=handler.player;
			if(mode==Mode.DUEL && (p.getUUID().equals(duelA)||p.getUUID().equals(duelB))) {
				UUID winnerId=p.getUUID().equals(duelA)?duelB:duelA;
				ServerPlayer winner=server.getPlayerList().getPlayer(winnerId);
				if(winner!=null) { Money.give(server,winnerId,currentStake*2);restore(winner);tell(winner,"คู่ต่อสู้ออกจากเกม คุณชนะ",ChatFormatting.GOLD); }
				ARENA_PLAYERS.remove(p.getUUID());clearMatch();
			} else if(mode==Mode.EVENT && ARENA_PLAYERS.contains(p.getUUID())) SPECTATORS.add(p.getUUID());
		});
	}

	private static boolean inside(BlockPos p) {
		return p.getY() >= 1 && p.getY() <= Y_SIZE && Math.abs(p.getX()-arenaX) <= X_SIZE/2 && Math.abs(p.getZ()-arenaZ) <= Z_SIZE/2;
	}
	public static boolean inArena(UUID id) { return ARENA_PLAYERS.contains(id); }
	public static boolean isParticipant(UUID id) { return ARENA_PLAYERS.contains(id) && !SPECTATORS.contains(id); }
	public static void tick(MinecraftServer server) {
		if (nextEventTick == 0) nextEventTick=server.overworld().getGameTime()+EVENT_INTERVAL;
		if (arenaBuilt && server.overworld().getGameTime()%10==0) repairShell(server.overworld());
		if(mode!=null) clampPlayers(server);
		if (mode == null && server.overworld().getGameTime() >= nextEventTick && nextEventTick != 0) {
			startEvent(server, null);
			nextEventTick=server.overworld().getGameTime()+EVENT_INTERVAL;
		}
	}
	public static void schedule(MinecraftServer server) { nextEventTick=server.overworld().getGameTime()+EVENT_INTERVAL; }

	public static BlockPos center(MinecraftServer server) { return new BlockPos(arenaX, 8, arenaZ); }
	private static void buildArena(ServerLevel level) {
		arenaBuilt=true;
		arenaX=level.getSharedSpawnPos().getX()+OFFSET;arenaZ=level.getSharedSpawnPos().getZ();
		activeServer=level.getServer();
		oldReducedDebug=level.getGameRules().getBoolean(GameRules.RULE_REDUCEDDEBUGINFO);
		level.getGameRules().getRule(GameRules.RULE_REDUCEDDEBUGINFO).set(true,level.getServer());
		BlockPos c=center(level.getServer());
		int minX=c.getX()-X_SIZE/2, maxX=minX+X_SIZE-1;
		int minZ=c.getZ()-Z_SIZE/2, maxZ=minZ+Z_SIZE-1;
		for(int cx=minX>>4;cx<=maxX>>4;cx++) for(int cz=minZ>>4;cz<=maxZ>>4;cz++) level.setChunkForced(cx,cz,true);
		int minY=1, maxY=Y_SIZE;
		for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) for(int y=minY;y<=maxY;y++) {
			boolean shell=x==minX||x==maxX||z==minZ||z==maxZ||y==minY||y==maxY;
			level.setBlockAndUpdate(new BlockPos(x,y,z), shell?Blocks.GLOWSTONE.defaultBlockState():Blocks.AIR.defaultBlockState());
		}
	}
	private static void repairShell(ServerLevel level) {
		int minX=arenaX-X_SIZE/2, maxX=minX+X_SIZE-1, minZ=arenaZ-Z_SIZE/2, maxZ=arenaZ+Z_SIZE/2-1;
		for(int x=minX;x<=maxX;x++) for(int z=minZ;z<=maxZ;z++) {
			for(int y=1;y<=Y_SIZE;y++) {
				if(x==minX||x==maxX||z==minZ||z==maxZ||y==1||y==Y_SIZE) {
					BlockPos pos=new BlockPos(x,y,z);
					if(!level.getBlockState(pos).is(Blocks.GLOWSTONE)) level.setBlockAndUpdate(pos,Blocks.GLOWSTONE.defaultBlockState());
				}
			}
		}
	}
	private static void clampPlayers(MinecraftServer server) {
		for(UUID id:ARENA_PLAYERS) {
			ServerPlayer p=server.getPlayerList().getPlayer(id);
			if(p==null) continue;
			if(p.serverLevel()!=server.overworld()) {p.teleportTo(server.overworld(),arenaX,2,arenaZ,p.getYRot(),p.getXRot());continue;}
			double x=Math.max(arenaX-X_SIZE/2+1,Math.min(arenaX+X_SIZE/2-2,p.getX()));
			double z=Math.max(arenaZ-Z_SIZE/2+1,Math.min(arenaZ+Z_SIZE/2-2,p.getZ()));
			double y=Math.max(2,Math.min(Y_SIZE-1,p.getY()));
			if(x!=p.getX()||y!=p.getY()||z!=p.getZ())p.teleportTo(server.overworld(),x,y,z,p.getYRot(),p.getXRot());
		}
	}
	public static void challenge(ServerPlayer from, ServerPlayer to, long stake) {
		if (from==to || stake<10_000 || mode!=null || ARENA_PLAYERS.contains(from.getUUID()) || ARENA_PLAYERS.contains(to.getUUID())) { tell(from,"คำท้าหรือเงินเดิมพันไม่ถูกต้อง",ChatFormatting.RED); return; }
		if (Money.balance(from.server,from.getUUID())<stake || Money.balance(to.server,to.getUUID())<stake) { tell(from,"ทั้งสองฝ่ายต้องมีเงินอย่างน้อยเท่าเงินเดิมพัน",ChatFormatting.RED); return; }
		CHALLENGES.put(to.getUUID(),from.getUUID()); STAKES.put(to.getUUID(),stake);
		to.sendSystemMessage(Component.literal(from.getName().getString()+" ท้าดวลเดิมพัน $"+Money.fmt(stake)+" พิมพ์ /arena accept เพื่อรับ").withStyle(ChatFormatting.GOLD));
		tell(from,"ส่งคำท้าแล้ว",ChatFormatting.GREEN);
	}
	public static boolean accept(ServerPlayer to) {
		UUID fromId=CHALLENGES.remove(to.getUUID()); Long stake=STAKES.remove(to.getUUID());
		ServerPlayer from=fromId==null?null:to.server.getPlayerList().getPlayer(fromId);
		if(from==null||stake==null||mode!=null||Money.balance(to.server,to.getUUID())<stake||Money.balance(to.server,fromId)<stake) { tell(to,"คำท้าหมดอายุหรือเงินไม่พอ",ChatFormatting.RED); return false; }
		if(!Money.take(to.server,fromId,stake)) return false;
		if(!Money.take(to.server,to.getUUID(),stake)) { Money.give(to.server,fromId,stake); return false; }
		mode=Mode.DUEL; duelA=fromId;duelB=to.getUUID();
		currentStake=stake;
		ServerLevel level=to.server.overworld(); buildArena(level);
		enter(from,level,-10,0);enter(to,level,10,0);
		broadcast(to.server,"ดวลเริ่มแล้ว! เงินรางวัล $"+Money.fmt(stake*2),ChatFormatting.GOLD); return true;
	}
	public static void startEvent(MinecraftServer server, ServerPlayer solo) {
		if(mode!=null) { if(solo!=null) tell(solo,"มีอารีน่ากำลังใช้งาน",ChatFormatting.RED);return; }
		mode=Mode.EVENT; prizeClaimed=false; prizeValue=1_000_000L+new Random().nextInt(2_000_001);
		ServerLevel level=server.overworld();buildArena(level);
		if(solo!=null) enter(solo,level,0,0);
		else broadcast(server,"อีเวนต์อารีน่าเปิดแล้ว ใช้ /arena join เพื่อเข้าร่วม",ChatFormatting.LIGHT_PURPLE);
		prizePos=new BlockPos(arenaX,2,arenaZ);
		level.setBlockAndUpdate(prizePos,Blocks.CHEST.defaultBlockState());
		if(level.getBlockEntity(prizePos) instanceof net.minecraft.world.Container container) container.setItem(0, prizeItem());
	}
	private static ItemStack prizeItem() {
		// Prize is redeemed as My Market currency, with value randomized in the requested 1-3M range.
		ItemStack token=new ItemStack(Items.PAPER);token.setHoverName(Component.literal("รางวัลอารีน่า $"+Money.fmt(prizeValue)).withStyle(ChatFormatting.GOLD));return token;
	}
	public static void join(ServerPlayer p) {
		if(mode!=Mode.EVENT) { tell(p,"ยังไม่มีอีเวนต์เปิดอยู่",ChatFormatting.RED);return; }
		enter(p,p.server.overworld(),0,0);
	}
	private static void enter(ServerPlayer p, ServerLevel level, int dx, int dz) {
		SNAPSHOTS.put(p.getUUID(),new Snapshot(p));ARENA_PLAYERS.add(p.getUUID());
		p.getInventory().clearContent();p.setGameMode(GameType.SURVIVAL);p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(0);
		p.teleportTo(level,arenaX+dx,2,arenaZ+dz,p.getYRot(),p.getXRot());giveKit(p);
		p.sendSystemMessage(Component.literal("เข้าสู่อารีน่าแล้ว: ของเดิมจะคืนให้เมื่อออกจากกิจกรรม").withStyle(ChatFormatting.AQUA));
	}
	private static void giveKit(ServerPlayer p) {
		Inventory inv=p.getInventory();
		inv.armor.set(0,new ItemStack(Items.NETHERITE_BOOTS));inv.armor.set(1,new ItemStack(Items.NETHERITE_LEGGINGS));inv.armor.set(2,new ItemStack(Items.NETHERITE_CHESTPLATE));inv.armor.set(3,new ItemStack(Items.NETHERITE_HELMET));
		inv.add(new ItemStack(Items.NETHERITE_SWORD));inv.add(new ItemStack(Items.NETHERITE_PICKAXE));inv.add(new ItemStack(Items.BOW));inv.add(new ItemStack(Items.ARROW,32));inv.add(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE,10));inv.add(new ItemStack(Items.OBSIDIAN,15));inv.add(new ItemStack(Items.END_CRYSTAL,15));inv.add(new ItemStack(Items.TOTEM_OF_UNDYING,5));inv.add(new ItemStack(Items.ENDER_PEARL,16));
	}
	private static void eliminate(ServerPlayer p) {
		UUID id=p.getUUID();
		if(mode==Mode.EVENT) { SPECTATORS.add(id);p.setGameMode(GameType.SPECTATOR);p.setHealth(p.getMaxHealth());p.teleportTo(p.server.overworld(),arenaX,2,arenaZ,0,0);tell(p,"ตกรอบแล้ว รับชมได้จนจบอีเวนต์",ChatFormatting.GRAY);if(ARENA_PLAYERS.stream().noneMatch(id2 -> !SPECTATORS.contains(id2)))endEvent(p.server);return; }
		UUID winner=id.equals(duelA)?duelB:duelA;finishDuel(p.server.getPlayerList().getPlayer(winner),p);
	}
	private static void finishDuel(ServerPlayer winner,ServerPlayer loser) {
		MinecraftServer server=(winner!=null?winner:loser).server;long payout=0;
		if(winner!=null){Money.give(server,winner.getUUID(),currentStake*2);tell(winner,"ชนะดวล! รับเงินเดิมพัน",ChatFormatting.GOLD);}
		if(winner!=null) restore(winner);if(loser!=null) restore(loser);clearMatch();
	}
	private static long currentStake;
	public static void leave(ServerPlayer p) { if(!ARENA_PLAYERS.contains(p.getUUID())) return; if(mode==Mode.EVENT) { restore(p);SPECTATORS.remove(p.getUUID()); if(ARENA_PLAYERS.isEmpty()) endEvent(p.server); } }
	public static void claim(ServerPlayer p) {
		if(mode!=Mode.EVENT||!isParticipant(p.getUUID())||prizeClaimed||p.blockPosition().distSqr(prizePos)>25) { tell(p,"ยังรับรางวัลไม่ได้",ChatFormatting.RED);return; }
		prizeClaimed=true;Money.give(p.server,p.getUUID(),prizeValue);broadcast(p.server,p.getName().getString()+" คว้ารางวัล $"+Money.fmt(prizeValue),ChatFormatting.GOLD);endEvent(p.server);
	}
	public static void stop(MinecraftServer server) { if(mode==Mode.EVENT) endEvent(server);else if(mode==Mode.DUEL) { ServerPlayer a=server.getPlayerList().getPlayer(duelA),b=server.getPlayerList().getPlayer(duelB);Money.give(server,duelA,currentStake);Money.give(server,duelB,currentStake);if(a!=null)restore(a);if(b!=null)restore(b);clearMatch(); } }
	private static void endEvent(MinecraftServer server) { for(UUID id:new ArrayList<>(ARENA_PLAYERS)){ServerPlayer p=server.getPlayerList().getPlayer(id);if(p!=null)restore(p);}if(prizePos!=null)server.overworld().setBlockAndUpdate(prizePos,Blocks.AIR.defaultBlockState());clearMatch(); }
	private static void clearMatch(){if(mode!=null && activeServer!=null) {var rules=activeServer.overworld().getGameRules();rules.getRule(GameRules.RULE_REDUCEDDEBUGINFO).set(oldReducedDebug,activeServer);}mode=null;activeServer=null;duelA=duelB=null;ARENA_PLAYERS.clear();SPECTATORS.clear();currentStake=0;}
	private static void restore(ServerPlayer p) { Snapshot s=SNAPSHOTS.remove(p.getUUID());ARENA_PLAYERS.remove(p.getUUID());if(s==null)return;s.restore(p); }
	private static void tell(ServerPlayer p,String s,ChatFormatting color){p.sendSystemMessage(Component.literal(s).withStyle(color));}
	private static void broadcast(MinecraftServer s,String text,ChatFormatting color){s.getPlayerList().broadcastSystemMessage(Component.literal(text).withStyle(color),false);}
	private static final class Snapshot {
		final List<ItemStack> items=new ArrayList<>();final ResourceKey<Level> dimension;final double x,y,z;final float yaw,pitch,health;final int food,level,totalExperience;final float saturation,exhaustion,xp;final GameType gameType;
		Snapshot(ServerPlayer p){for(int i=0;i<p.getInventory().getContainerSize();i++)items.add(p.getInventory().getItem(i).copy());dimension=p.level().dimension();x=p.getX();y=p.getY();z=p.getZ();yaw=p.getYRot();pitch=p.getXRot();health=p.getHealth();food=p.getFoodData().getFoodLevel();saturation=p.getFoodData().getSaturationLevel();exhaustion=p.getFoodData().getExhaustionLevel();level=p.experienceLevel;totalExperience=p.totalExperience;xp=p.experienceProgress;gameType=p.gameMode.getGameModeForPlayer();}
		void restore(ServerPlayer p){p.getInventory().clearContent();for(int i=0;i<items.size();i++)p.getInventory().setItem(i,items.get(i));p.setGameMode(gameType);ServerLevel w=p.server.getLevel(dimension);if(w!=null)p.teleportTo(w,x,y,z,yaw,pitch);p.setHealth(Math.min(health,p.getMaxHealth()));p.getFoodData().setFoodLevel(food);p.getFoodData().setSaturation(saturation);p.getFoodData().setExhaustion(exhaustion);p.experienceLevel=level;p.totalExperience=totalExperience;p.experienceProgress=xp;p.inventoryMenu.broadcastChanges();}
	}
}
