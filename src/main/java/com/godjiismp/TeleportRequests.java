package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Safe, paid-on-accept player teleport requests. All state is in memory and expires quickly. */
public final class TeleportRequests {
	private TeleportRequests() {}

	private static final long PRICE = 500_000L;
	private static final long EXPIRES_MS = 60_000L;
	private static final long COOLDOWN_MS = 30_000L;
	private static final Map<UUID, Request> BY_SENDER = new HashMap<>();
	private static final Map<UUID, UUID> LATEST_FOR_TARGET = new HashMap<>();
	private static final Map<UUID, Long> COOLDOWN_UNTIL = new HashMap<>();

	private record Request(UUID sender, UUID target, long expiresAt) {}

	public static void init() {
		ServerTickEvents.END_SERVER_TICK.register(TeleportRequests::expire);
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
								CommandBuildContext context, Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("tpa")
				.then(Commands.literal("cancel").executes(TeleportRequests::cancel))
				.then(Commands.argument("player", EntityArgument.player())
						.executes(TeleportRequests::request)));
		dispatcher.register(Commands.literal("tpaccept").executes(TeleportRequests::accept));
		dispatcher.register(Commands.literal("tpdeny").executes(TeleportRequests::deny));
	}

	private static int request(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer sender = context.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		MinecraftServer server = context.getSource().getServer();
		if (sender.getUUID().equals(target.getUUID())) return fail(sender, "You cannot teleport to yourself.");
		if (!sender.isAlive() || !target.isAlive()) return fail(sender, "Both players must be alive.");
		if (Money.balance(server, sender.getUUID()) < PRICE) return fail(sender, "You need $" + Money.fmt(PRICE) + " to request a teleport.");
		long now = System.currentTimeMillis();
		long wait = COOLDOWN_UNTIL.getOrDefault(sender.getUUID(), 0L) - now;
		if (wait > 0) return fail(sender, "Wait " + ((wait + 999) / 1000) + " seconds before another teleport.");
		if (BY_SENDER.containsKey(sender.getUUID())) return fail(sender, "You already have an open request. Use /tpa cancel first.");

		Request request = new Request(sender.getUUID(), target.getUUID(), now + EXPIRES_MS);
		BY_SENDER.put(sender.getUUID(), request);
		LATEST_FOR_TARGET.put(target.getUUID(), sender.getUUID());
		target.sendSystemMessage(Component.literal(sender.getName().getString() + " wants to teleport to you. ")
				.withStyle(ChatFormatting.YELLOW).append(button("[Accept]", "/tpaccept", ChatFormatting.GREEN))
				.append(Component.literal(" ")).append(button("[Deny]", "/tpdeny", ChatFormatting.RED)));
		sender.sendSystemMessage(Component.literal("Teleport request sent to " + target.getName().getString()
				+ ". It expires in 60 seconds; $" + Money.fmt(PRICE) + " is charged only after a successful teleport.")
				.withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static MutableComponent button(String label, String command, ChatFormatting color) {
		return Component.literal(label).withStyle(style -> style.withColor(color).withUnderlined(true)
				.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
				.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(command))));
	}

	private static int accept(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer target = context.getSource().getPlayerOrException();
		MinecraftServer server = context.getSource().getServer();
		UUID senderId = LATEST_FOR_TARGET.get(target.getUUID());
		Request request = senderId == null ? null : BY_SENDER.get(senderId);
		if (request == null || !request.target().equals(target.getUUID())) return fail(target, "You have no pending teleport request.");
		ServerPlayer sender = server.getPlayerList().getPlayer(request.sender());
		if (sender == null || !sender.isAlive() || !target.isAlive()) {
			remove(request);
			return fail(target, "The request was cancelled because a player is no longer available.");
		}
		if (System.currentTimeMillis() >= request.expiresAt()) {
			remove(request);
			return fail(target, "That teleport request has expired.");
		}
		if (Money.balance(server, sender.getUUID()) < PRICE) {
			remove(request);
			sender.sendSystemMessage(Component.literal("You no longer have enough money for that teleport.").withStyle(ChatFormatting.RED));
			return fail(target, "The sender no longer has enough money.");
		}

		ServerLevel sourceLevel = sender.serverLevel();
		double oldX = sender.getX(), oldY = sender.getY(), oldZ = sender.getZ();
		float oldYaw = sender.getYRot(), oldPitch = sender.getXRot();
		ServerLevel targetLevel = target.serverLevel();
		BlockPos safe = findSafeSpot(targetLevel, target.blockPosition());
		if (safe == null) return fail(target, "There is no safe landing spot near you. Move to an open area and try again.");

		sender.teleportTo(targetLevel, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5, target.getYRot(), target.getXRot());
		if (sender.serverLevel() != targetLevel || !Money.take(server, sender.getUUID(), PRICE)) {
			sender.teleportTo(sourceLevel, oldX, oldY, oldZ, oldYaw, oldPitch);
			return fail(target, "Teleport failed. No money was charged.");
		}
		remove(request);
		COOLDOWN_UNTIL.put(sender.getUUID(), System.currentTimeMillis() + COOLDOWN_MS);
		sender.fallDistance = 0;
		sender.sendSystemMessage(Component.literal("Teleport complete to " + target.getName().getString()
				+ ". Charged $" + Money.fmt(PRICE) + ".").withStyle(ChatFormatting.GREEN));
		target.sendSystemMessage(Component.literal(sender.getName().getString() + " teleported to you.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static BlockPos findSafeSpot(ServerLevel level, BlockPos center) {
		for (int radius = 0; radius <= 4; radius++) {
			for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
				if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) continue;
				for (int dy = -1; dy <= 2; dy++) {
					BlockPos feet = center.offset(dx, dy, dz);
					if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()
							&& level.getBlockState(feet.below()).isFaceSturdy(level, feet.below(), Direction.UP)) return feet;
				}
			}
		}
		return null;
	}

	private static int deny(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer target = context.getSource().getPlayerOrException();
		UUID senderId = LATEST_FOR_TARGET.get(target.getUUID());
		Request request = senderId == null ? null : BY_SENDER.get(senderId);
		if (request == null || !request.target().equals(target.getUUID())) return fail(target, "You have no pending teleport request.");
		remove(request);
		ServerPlayer sender = context.getSource().getServer().getPlayerList().getPlayer(request.sender());
		if (sender != null) sender.sendSystemMessage(Component.literal(target.getName().getString() + " denied your teleport request.").withStyle(ChatFormatting.RED));
		target.sendSystemMessage(Component.literal("Teleport request denied.").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static int cancel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer sender = context.getSource().getPlayerOrException();
		Request request = BY_SENDER.get(sender.getUUID());
		if (request == null) return fail(sender, "You have no open teleport request.");
		remove(request);
		ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayer(request.target());
		if (target != null) target.sendSystemMessage(Component.literal(sender.getName().getString() + " cancelled the teleport request.").withStyle(ChatFormatting.GRAY));
		sender.sendSystemMessage(Component.literal("Teleport request cancelled.").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static void expire(MinecraftServer server) {
		long now = System.currentTimeMillis();
		Iterator<Request> iterator = BY_SENDER.values().iterator();
		while (iterator.hasNext()) {
			Request request = iterator.next();
			ServerPlayer sender = server.getPlayerList().getPlayer(request.sender());
			ServerPlayer target = server.getPlayerList().getPlayer(request.target());
			if (now < request.expiresAt() && sender != null && target != null) continue;
			iterator.remove();
			LATEST_FOR_TARGET.remove(request.target(), request.sender());
			if (sender != null && now >= request.expiresAt()) sender.sendSystemMessage(Component.literal("Your teleport request expired.").withStyle(ChatFormatting.GRAY));
		}
		COOLDOWN_UNTIL.values().removeIf(until -> until <= now);
	}

	private static void remove(Request request) {
		BY_SENDER.remove(request.sender());
		LATEST_FOR_TARGET.remove(request.target(), request.sender());
	}

	private static int fail(ServerPlayer player, String message) {
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
		return 0;
	}
}
