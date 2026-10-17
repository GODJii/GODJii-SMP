package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.AgeableBlock;
import net.minecraft.world.entity.player.Player;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Persistent daily mining, hunting, farming, and logging quests. */
public final class DailyQuests {
	private DailyQuests() {}
	private record Template(String id, String label, int target) {}
	private static final List<Template> POOL = List.of(
			new Template("diamond", "Mine 20 diamond ore", 20),
			new Template("iron", "Mine 100 iron ore", 100),
			new Template("coal", "Mine 200 coal ore", 200),
			new Template("zombie", "Kill 50 zombies", 50),
			new Template("skeleton", "Kill 30 skeletons", 30),
			new Template("creeper", "Kill 15 creepers", 15),
			new Template("farm", "Harvest 200 wheat or carrots", 200),
			new Template("logs", "Chop 150 logs", 150));

	public static void init() {
		CommandRegistrationCallback.EVENT.register(DailyQuests::register);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ensureQuest(server, handler.player));
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (!(player instanceof ServerPlayer sp)) return;
			DailyQuestData data = DailyQuestData.get(sp.getServer());
			String key = key(world, pos);
			if (data.isPlayerPlaced(key)) { data.unmarkPlaced(key); return; }
			DailyQuestData.Quest q = current(sp.getServer(), sp);
			String id = q.id();
			if (id.equals("diamond") && (state.is(net.minecraft.world.level.block.Blocks.DIAMOND_ORE) || state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE_DIAMOND_ORE))) add(sp, 1);
			else if (id.equals("iron") && (state.is(net.minecraft.world.level.block.Blocks.IRON_ORE) || state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE_IRON_ORE))) add(sp, 1);
			else if (id.equals("coal") && (state.is(net.minecraft.world.level.block.Blocks.COAL_ORE) || state.is(net.minecraft.world.level.block.Blocks.DEEPSLATE_COAL_ORE))) add(sp, 1);
			else if (id.equals("logs") && state.is(BlockTags.LOGS)) add(sp, 1);
			else if (id.equals("farm") && isMatureCrop(state)) add(sp, 1);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			Entity attacker = source.getEntity();
			if (attacker instanceof Projectile projectile) attacker = projectile.getOwner();
			if (!(attacker instanceof ServerPlayer player)) return;
			String id = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath();
			if (id.equals("zombie") || id.equals("skeleton") || id.equals("creeper")) {
				DailyQuestData.Quest q = current(player.getServer(), player);
				if (q.id().equals(id)) add(player, 1);
			}
		});
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
							 Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("quest").executes(c -> {
			ServerPlayer player = c.getSource().getPlayerOrException();
			DailyQuestData.Quest q = current(c.getSource().getServer(), player);
			String message = q.label() + " — " + q.progress() + "/" + q.target()
					+ (q.paid() ? " (completed)" : q.complete() ? " (reward processing)" : "");
			player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.GOLD));
			return 1;
		}));
	}

	private static DailyQuestData.Quest current(MinecraftServer server, ServerPlayer player) {
		long day = dailyKey();
		DailyQuestData data = DailyQuestData.get(server);
		DailyQuestData.Quest q = data.get(player.getUUID());
		if (q == null || q.day() != day) {
			Template t = POOL.get(ThreadLocalRandom.current().nextInt(POOL.size()));
			q = new DailyQuestData.Quest(t.id(), t.label(), t.target(), 0, day, false);
			data.set(player.getUUID(), q);
			data.clearPlaced();
			player.sendSystemMessage(Component.literal("Daily quest: " + t.label() + ". Reward: $1M–$3M.").withStyle(ChatFormatting.GOLD));
		}
		return q;
	}

	private static void ensureQuest(MinecraftServer server, ServerPlayer player) { current(server, player); }
	private static long dailyKey() {
		return LocalDate.now(ZoneId.systemDefault()).minusDays(java.time.LocalTime.now(ZoneId.systemDefault()).getHour() < 4 ? 1 : 0).toEpochDay();
	}
	private static String key(Level world, BlockPos pos) { return world.dimension().location() + ":" + pos.asLong(); }
	public static void markPlaced(Level world, BlockPos pos, Player player) {
		if (!world.isClientSide && player instanceof ServerPlayer sp) {
			DailyQuestData.get(sp.getServer()).markPlaced(key(world, pos));
		}
	}
	private static boolean isMatureCrop(BlockState state) {
		return (state.is(net.minecraft.world.level.block.Blocks.WHEAT) || state.is(net.minecraft.world.level.block.Blocks.CARROTS))
				&& state.getValue(AgeableBlock.AGE) >= 7;
	}
	private static void add(ServerPlayer player, int amount) {
		DailyQuestData data = DailyQuestData.get(player.getServer());
		DailyQuestData.Quest before = current(player.getServer(), player);
		if (before.complete()) return;
		data.progress(player.getUUID(), amount);
		DailyQuestData.Quest after = data.get(player.getUUID());
		if (after.complete() && data.markPaid(player.getUUID())) {
			long reward = ThreadLocalRandom.current().nextLong(1_000_000L, 3_000_001L);
			Money.give(player.getServer(), player.getUUID(), reward);
			player.sendSystemMessage(Component.literal("Daily quest complete! You received $" + Money.fmt(reward) + ".").withStyle(ChatFormatting.GREEN));
			player.displayClientMessage(Component.literal("Quest complete · +$" + Money.fmt(reward)).withStyle(ChatFormatting.GOLD), true);
		} else if (after.progress() % 10 == 0 || after.complete()) {
			player.displayClientMessage(Component.literal("Quest: " + after.progress() + "/" + after.target()).withStyle(ChatFormatting.YELLOW), true);
		}
	}
}
