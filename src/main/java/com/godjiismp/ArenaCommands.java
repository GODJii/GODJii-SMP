package com.godjiismp;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public final class ArenaCommands {
	private ArenaCommands() {}
	public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx, Commands.CommandSelection env) {
		d.register(Commands.literal("arena")
			.then(Commands.literal("challenge").then(Commands.argument("player", EntityArgument.player())
				.then(Commands.argument("stake", LongArgumentType.longArg(10_000)).executes(c -> {
					ServerPlayer from=c.getSource().getPlayerOrException();
					ArenaManager.challenge(from, EntityArgument.getPlayer(c,"player"), LongArgumentType.getLong(c,"stake"));return 1;
				}))))
			.then(Commands.literal("accept").executes(c -> ArenaManager.accept(c.getSource().getPlayerOrException())?1:0))
			.then(Commands.literal("join").executes(c -> {ArenaManager.join(c.getSource().getPlayerOrException());return 1;}))
			.then(Commands.literal("leave").executes(c -> {ArenaManager.leave(c.getSource().getPlayerOrException());return 1;}))
			.then(Commands.literal("start").requires(s -> s.hasPermission(4)).executes(c -> {ArenaManager.startEvent(c.getSource().getServer(), c.getSource().getEntity() instanceof ServerPlayer p ? p : null);return 1;}))
			.then(Commands.literal("stop").requires(s -> s.hasPermission(4)).executes(c -> {ArenaManager.stop(c.getSource().getServer());return 1;}))
		);
	}
}
