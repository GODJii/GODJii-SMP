package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.context.UseOnContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/** Summon Crystal, one-at-a-time Skeleton King, boss bar, drop roll, and owner test commands. */
public final class BossSystem {
	private BossSystem() {}
	private static final String CRYSTAL_KEY = "godjiismp_summon_crystal";
	private record Pending(long due, BlockPos pos) {}
	private static final Map<UUID, Pending> PENDING = new HashMap<>();
	private static ServerBossEvent bossBar;

	public static void init() {
		CommandRegistrationCallback.EVENT.register(BossSystem::register);
		ServerTickEvents.END_SERVER_TICK.register(BossSystem::tick);
		ServerLivingEntityEvents.AFTER_DEATH.register(BossSystem::onDeath);
		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof Skeleton && entity.getUUID().equals(BossData.get(world.getServer()).boss())) {
				if (bossBar == null) makeBossBar();
				for (ServerPlayer p : world.getServer().getPlayerList().getPlayers()) bossBar.addPlayer(p);
			}
		});
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			if (bossBar != null && BossData.get(server).boss() != null) bossBar.addPlayer(handler.player);
		});
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
							 Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("givecrystal").requires(s -> s.hasPermission(2))
				.then(Commands.argument("player", EntityArgument.player()).executes(c -> {
					ServerPlayer target = EntityArgument.getPlayer(c, "player");
					target.getInventory().add(makeCrystal());
					c.getSource().sendSuccess(() -> Component.literal("Summon Crystal given to " + target.getName().getString() + "."), true);
					return 1;
				})));
		dispatcher.register(Commands.literal("boss").requires(s -> s.hasPermission(2))
				.then(Commands.literal("kill").executes(c -> {
					UUID uuid = BossData.get(c.getSource().getServer()).boss();
					Entity entity = uuid == null ? null : c.getSource().getServer().overworld().getEntity(uuid);
					if (!(entity instanceof Skeleton)) {
						c.getSource().sendFailure(Component.literal("No loaded Skeleton King is active."));
						return 0;
					}
					entity.kill();
					return 1;
				})));
	}

	public static boolean isSummonCrystal(ItemStack stack) {
		if (stack.isEmpty() || !stack.is(Items.END_CRYSTAL)) return false;
		CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
		return custom != null && custom.copyTag().getBoolean(CRYSTAL_KEY);
	}

	public static ItemStack makeCrystal() {
		ItemStack stack = new ItemStack(Items.END_CRYSTAL);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Summon Crystal").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
		stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
		CompoundTag tag = new CompoundTag(); tag.putBoolean(CRYSTAL_KEY, true);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	/** Called by EndCrystalItem mixin before vanilla placement. Always consumes the click for tagged crystals. */
	public static InteractionResult useCrystal(UseOnContext context) {
		ItemStack stack = context.getItemInHand();
		if (!isSummonCrystal(stack)) return null;
		Level level = context.getLevel();
		if (level.isClientSide) return InteractionResult.SUCCESS;
		if (!(context.getPlayer() instanceof ServerPlayer player)) return InteractionResult.FAIL;
		if (level.dimension() != Level.OVERWORLD || !level.canSeeSky(context.getClickedPos().above())) {
			player.sendSystemMessage(Component.literal("Summon Crystals only work outdoors in the Overworld.").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		MinecraftServer server = player.getServer();
		if (BossData.get(server).boss() != null) {
			player.sendSystemMessage(Component.literal("A Skeleton King is already active on this server.").withStyle(ChatFormatting.RED));
			return InteractionResult.FAIL;
		}
		if (PENDING.containsKey(player.getUUID())) return InteractionResult.FAIL;
		PENDING.put(player.getUUID(), new Pending(server.overworld().getGameTime() + 60, context.getClickedPos().above()));
		player.sendSystemMessage(Component.literal("The crystal is charging. Hold position for 3 seconds...").withStyle(ChatFormatting.LIGHT_PURPLE));
		player.displayClientMessage(Component.literal("3").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), true);
		return InteractionResult.SUCCESS;
	}

	private static void tick(MinecraftServer server) {
		long now = server.overworld().getGameTime();
		for (var it = PENDING.entrySet().iterator(); it.hasNext();) {
			var entry = it.next();
			if (now < entry.getValue().due()) continue;
			it.remove();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) continue;
			if (BossData.get(server).boss() != null) {
				player.sendSystemMessage(Component.literal("Another player summoned the Skeleton King first.").withStyle(ChatFormatting.RED));
				continue;
			}
			if (!consumeCrystal(player)) {
				player.sendSystemMessage(Component.literal("Summoning cancelled: the Summon Crystal is no longer in your inventory.").withStyle(ChatFormatting.RED));
				continue;
			}
			spawn(server, player.serverLevel(), entry.getValue().pos());
		}
		UUID uuid = BossData.get(server).boss();
		if (uuid == null) return;
		Entity entity = server.overworld().getEntity(uuid);
		if (!(entity instanceof Skeleton skeleton)) return; // Unloaded boss stays reserved; it is persistent.
		if (bossBar == null) makeBossBar();
		bossBar.setProgress(Math.max(0, Math.min(1, skeleton.getHealth() / skeleton.getMaxHealth())));
		for (ServerPlayer p : server.getPlayerList().getPlayers()) if (!bossBar.getPlayers().contains(p)) bossBar.addPlayer(p);
	}

	private static boolean consumeCrystal(ServerPlayer player) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack stack = player.getInventory().getItem(i);
			if (isSummonCrystal(stack)) { stack.shrink(1); return true; }
		}
		if (isSummonCrystal(player.getOffhandItem())) { player.getOffhandItem().shrink(1); return true; }
		return false;
	}

	private static void spawn(MinecraftServer server, ServerLevel world, BlockPos pos) {
		BossData data = BossData.get(server);
		if (data.boss() != null) return;
		for (int y = 0; y < 4; y++) {
			if (!world.getBlockState(pos.above(y)).isAir()) {
				server.getPlayerList().broadcastSystemMessage(Component.literal("The Summon Crystal could not find enough open space; the crystal was consumed.").withStyle(ChatFormatting.RED), false);
				return;
			}
		}
		Skeleton boss = new Skeleton(world);
		boss.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
		boss.setCustomName(Component.literal("Skeleton King").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
		boss.setCustomNameVisible(true);
		boss.setPersistenceRequired();
		AttributeInstance health = boss.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) health.setBaseValue(60.0);
		AttributeInstance damage = boss.getAttribute(Attributes.ATTACK_DAMAGE);
		if (damage != null) damage.setBaseValue(12.0);
		AttributeInstance scale = boss.getAttribute(Attributes.SCALE);
		if (scale != null) scale.setBaseValue(4.0);
		boss.setHealth(boss.getMaxHealth());
		if (!world.addFreshEntity(boss)) return;
		data.setBoss(boss.getUUID());
		makeBossBar();
		for (ServerPlayer p : server.getPlayerList().getPlayers()) bossBar.addPlayer(p);
		server.getPlayerList().broadcastSystemMessage(Component.literal("THE SKELETON KING HAS RISEN! Defeat it for a valuable drop.")
				.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), false);
	}

	private static void makeBossBar() {
		bossBar = new ServerBossEvent(Component.literal("Skeleton King").withStyle(ChatFormatting.DARK_RED),
				BossEvent.BossBarColor.RED, BossEvent.BossBarOverlay.PROGRESS);
	}

	private static void onDeath(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource source) {
		if (!(entity instanceof Skeleton) || entity.getServer() == null) return;
		MinecraftServer server = entity.getServer();
		BossData data = BossData.get(server);
		if (!entity.getUUID().equals(data.boss())) return;
		data.setBoss(null);
		if (bossBar != null) { bossBar.removeAllPlayers(); bossBar = null; }
		ItemStack reward = pickDrop();
		ItemEntity drop = new ItemEntity(entity.level(), entity.getX(), entity.getY() + 0.4, entity.getZ(), reward);
		entity.level().addFreshEntity(drop);
		Entity attacker = source.getEntity();
		if (attacker instanceof net.minecraft.world.entity.projectile.Projectile projectile) attacker = projectile.getOwner();
		if (attacker instanceof ServerPlayer player) {
			Money.give(server, player.getUUID(), 2_000_000L);
			player.displayClientMessage(Component.literal("Skeleton King defeated · +$2M").withStyle(ChatFormatting.GOLD), true);
		}
		server.getPlayerList().broadcastSystemMessage(Component.literal("The Skeleton King was defeated. Its reward: " + reward.getHoverName().getString())
				.withStyle(ChatFormatting.GOLD), false);
	}

	private static ItemStack pickDrop() {
		int roll = ThreadLocalRandom.current().nextInt(100);
		ItemStack stack = new ItemStack(roll < 35 ? Items.NETHERITE_INGOT
				: roll < 60 ? Items.NETHER_STAR
				: roll < 78 ? Items.BEACON
				: roll < 90 ? Items.NETHERITE_BLOCK
				: roll < 95 ? Items.DRAGON_HEAD
				: roll < 99 ? Items.DRAGON_EGG : Items.ELYTRA);
		stack.set(DataComponents.CUSTOM_NAME, Component.literal("Skeleton King Reward").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
		return stack;
	}
}
