package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;

import java.util.Locale;

/**
 * Shows "X -38.0, Y 70.0, Z 12.0" in the middle of the screen, just above the health bar (the action bar).
 * /coords switches it on or off for yourself.
 */
public final class CoordsHud {
	private CoordsHud() {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(CoordsHud::tick);
		ServerLifecycleEvents.SERVER_STARTED.register(CoordsHud::onStart);
	}

	public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx,
								Commands.CommandSelection env) {
		d.register(Commands.literal("coords").executes(c -> {
			ServerPlayer p = c.getSource().getPlayerOrException();
			boolean on = CoordsData.get(c.getSource().getServer()).toggle(p.getUUID());
			if (!on) p.displayClientMessage(Component.empty(), true);
			p.sendSystemMessage(Component.literal(on ? "Coordinates ON" : "Coordinates OFF")
					.withStyle(on ? ChatFormatting.GREEN : ChatFormatting.YELLOW));
			return 1;
		}));
	}

	private static void onStart(MinecraftServer server) {
		if (!ExtrasConfig.fixReducedDebugOnStart) return;
		GameRules rules = server.overworld().getGameRules();
		// Left over from the old arena (it turned this on to hide coordinates).
		if (rules.getBoolean(GameRules.RULE_REDUCEDDEBUGINFO)) {
			rules.getRule(GameRules.RULE_REDUCEDDEBUGINFO).set(false, server);
			GodjiiSmpMod.LOGGER.info("reducedDebugInfo was left on after the arena; turned it off so F3 coordinates work");
		}
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % ExtrasConfig.coordsIntervalTicks != 0) return;
		CoordsData data = CoordsData.get(server);
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			if (data.isHidden(p.getUUID()) || !p.isAlive()) continue;
			p.displayClientMessage(line(p), true);
		}
	}

	private static MutableComponent line(ServerPlayer p) {
		return axis("X ", p.getX())
				.append(axis(", Y ", p.getY()))
				.append(axis(", Z ", p.getZ()));
	}

	private static MutableComponent axis(String label, double value) {
		return Component.literal(label).withStyle(ChatFormatting.WHITE)
				.append(Component.literal(String.format(Locale.US, "%.1f", value)).withStyle(ChatFormatting.YELLOW));
	}
}
