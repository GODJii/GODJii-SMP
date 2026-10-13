package com.godjiismp;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;
import java.util.UUID;

/** Bounty rules: pay on set, pay the killer, refund on expiry. */
public final class Bounties {
	private Bounties() {}

	public static void init() {
		ServerLivingEntityEvents.AFTER_DEATH.register(Bounties::onDeath);
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTickCount() % 20 == 0) expire(server);
		});
	}

	public static long now(MinecraftServer server) {
		return server.overworld().getGameTime();
	}

	// ------------------------------------------------------------------ events

	private static void onDeath(LivingEntity entity, DamageSource source) {
		if (!(entity instanceof ServerPlayer target)) return;
		MinecraftServer server = target.getServer();
		if (server == null) return;
		BountyData data = BountyData.get(server);
		BountyData.Bounty b = data.get(target.getUUID());
		if (b == null) return;

		ServerPlayer killer = source.getEntity() instanceof ServerPlayer k ? k : null;
		if (killer == null || killer.getUUID().equals(target.getUUID())) {
			// Suicide, mobs, fall, lava, /kill ...: nobody is paid and the bounty stays on.
			return;
		}

		data.remove(target.getUUID());
		Money.give(server, killer.getUUID(), b.amount());
		String killerName = killer.getName().getString();
		broadcast(server, Component.literal(killerName + " killed " + b.targetName() + " and claimed the $"
				+ Money.fmt(b.amount()) + " bounty!").withStyle(ChatFormatting.GOLD));
		title(server,
				Component.literal("BOUNTY CLAIMED").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				Component.literal(killerName + " killed " + b.targetName() + "  +$" + Money.fmt(b.amount()))
						.withStyle(ChatFormatting.YELLOW));
	}

	private static void expire(MinecraftServer server) {
		BountyData data = BountyData.get(server);
		long now = now(server);
		for (Map.Entry<UUID, BountyData.Bounty> e : data.all().entrySet()) {
			BountyData.Bounty b = e.getValue();
			if (now < b.expiresAt()) continue;
			data.remove(e.getKey());
			Money.give(server, b.placer(), b.amount()); // nobody got him: money goes back
			broadcast(server, Component.literal("The $" + Money.fmt(b.amount()) + " bounty on " + b.targetName()
					+ " ran out. " + b.placerName() + " got the money back.").withStyle(ChatFormatting.GRAY));
		}
	}

	// ------------------------------------------------------------------ used by the command

	public static void announceSet(MinecraftServer server, String placerName, String targetName, long amount) {
		broadcast(server, Component.literal(placerName + " put a $" + Money.fmt(amount) + " bounty on "
				+ targetName + "! Kill them to claim it. (" + Config.bountyTicks / 20 / 60 + " min)")
				.withStyle(ChatFormatting.RED));
		title(server,
				Component.literal("BOUNTY").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD),
				Component.literal(targetName + "  $" + Money.fmt(amount)).withStyle(ChatFormatting.RED));
	}

	/** "mm:ss" for a number of ticks. */
	public static String timeLeft(long ticks) {
		long sec = Math.max(0, (ticks + 19) / 20);
		return String.format("%d:%02d", sec / 60, sec % 60);
	}

	// ------------------------------------------------------------------ helpers

	private static void broadcast(MinecraftServer server, Component msg) {
		server.getPlayerList().broadcastSystemMessage(msg, false);
	}

	/** Big text in the middle of every player's screen. */
	private static void title(MinecraftServer server, Component title, Component subtitle) {
		for (ServerPlayer p : server.getPlayerList().getPlayers()) {
			p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 80, 20));
			p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
			p.connection.send(new ClientboundSetTitleTextPacket(title));
		}
	}
}
