package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Player-to-player transfers using the existing My Market balance store. */
public final class EconomyCommands {
	private EconomyCommands() {}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher,
								CommandBuildContext context, Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("pay")
				.then(Commands.argument("player", EntityArgument.player())
						.then(Commands.argument("amount", StringArgumentType.word())
								.executes(EconomyCommands::pay))));
	}

	private static int pay(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer sender = context.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(context, "player");
		MinecraftServer server = context.getSource().getServer();
		long amount = BountyCommands.parseAmount(StringArgumentType.getString(context, "amount"));

		if (sender.getUUID().equals(target.getUUID())) return error(sender, "You cannot pay yourself.");
		if (amount < 1) return error(sender, "Invalid amount. Use 250K, 1.5M, 2B or a whole number.");
		long senderBalance = Money.balance(server, sender.getUUID());
		if (amount > senderBalance) return error(sender, "Not enough money. Your balance is $" + Money.fmt(senderBalance) + ".");
		long targetBalance = Money.balance(server, target.getUUID());
		if (Long.MAX_VALUE - targetBalance < amount) return error(sender, "That transfer would exceed the recipient's balance limit.");

		// Command handlers run on the server thread, so no other transfer can interleave these steps.
		if (!Money.take(server, sender.getUUID(), amount)) return error(sender, "Your balance changed; payment was not sent.");
		Money.give(server, target.getUUID(), amount);
		if (Money.balance(server, target.getUUID()) != targetBalance + amount) {
			Money.give(server, sender.getUUID(), amount);
			GodjiiSmpMod.LOGGER.error("Payment rollback: could not credit {} ({}) from {} ({}) amount {}",
					target.getName().getString(), target.getUUID(), sender.getName().getString(), sender.getUUID(), amount);
			return error(sender, "Payment failed and was refunded. Please contact a server operator.");
		}

		sender.sendSystemMessage(Component.literal("You sent $" + Money.fmt(amount) + " to "
				+ target.getName().getString() + ".").withStyle(ChatFormatting.GREEN));
		target.sendSystemMessage(Component.literal(sender.getName().getString() + " sent you $" + Money.fmt(amount) + ".")
				.withStyle(ChatFormatting.GOLD));
		GodjiiSmpMod.LOGGER.info("Money transfer: {} ({}) -> {} ({}) amount {}",
				sender.getName().getString(), sender.getUUID(), target.getName().getString(), target.getUUID(), amount);
		return 1;
	}

	private static int error(ServerPlayer player, String message) {
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
		return 0;
	}
}
