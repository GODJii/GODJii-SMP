package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;

public final class StatsPanel {
	private StatsPanel() {}
	public static void init() {
		PayloadTypeRegistry.playS2C().register(StatsPayload.TYPE, StatsPayload.CODEC);
		CommandRegistrationCallback.EVENT.register(StatsPanel::register);
		ServerTickEvents.END_SERVER_TICK.register(server -> { if (server.overworld().getGameTime() % 20 == 0) for (ServerPlayer p : server.getPlayerList().getPlayers()) send(p); });
	}
	private static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext c, Commands.CommandSelection e) {
		d.register(Commands.literal("sidebar").executes(ctx -> {
			ServerPlayer p = ctx.getSource().getPlayerOrException();
			boolean visible = StatsPanelData.get(p.getServer()).toggle(p.getUUID());
			send(p);
			p.sendSystemMessage(Component.literal("Stats panel " + (visible ? "shown" : "hidden") + "."));
			return 1;
		}));
	}
	static void send(ServerPlayer p) {
		if (ServerPlayNetworking.canSend(p, StatsPayload.TYPE)) {
			var s = p.getStats();
			ServerPlayNetworking.send(p, new StatsPayload(p.getGameProfile().getName(), Money.balance(p.getServer(), p.getUUID()),
					s.getValue(Stats.CUSTOM, Stats.MOB_KILLS), s.getValue(Stats.CUSTOM, Stats.PLAYER_KILLS),
					s.getValue(Stats.CUSTOM, Stats.DEATHS), s.getValue(Stats.CUSTOM, Stats.PLAY_TIME),
					StatsPanelData.get(p.getServer()).isVisible(p.getUUID())));
		}
	}
}
