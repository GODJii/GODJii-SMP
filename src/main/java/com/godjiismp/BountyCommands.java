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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * /bounty set <player> <amount>   amount can be 50000, 50k, 2.5M, 1B ...
 * /bounty list  (or just /bounty, or /bounties)
 */
public final class BountyCommands {
	private BountyCommands() {}

	private static final Pattern AMOUNT = Pattern.compile("^(\\d+(?:\\.\\d+)?)([kmbtKMBT]?)$");

	public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx,
								Commands.CommandSelection env) {
		d.register(Commands.literal("bounty")
				.executes(BountyCommands::list)
				.then(Commands.literal("list").executes(BountyCommands::list))
				.then(Commands.literal("set")
						.then(Commands.argument("player", EntityArgument.player())
								.then(Commands.argument("amount", StringArgumentType.word())
										.executes(BountyCommands::set)))));
		d.register(Commands.literal("bounties").executes(BountyCommands::list));
	}

	private static Component fail(String s) {
		return Component.literal(s).withStyle(ChatFormatting.RED);
	}

	/** "50k" -> 50000, "2.5M" -> 2500000. Returns -1 if it makes no sense. */
	static long parseAmount(String s) {
		Matcher m = AMOUNT.matcher(s);
		if (!m.matches()) return -1;
		long mult = switch (m.group(2).toLowerCase()) {
			case "k" -> 1_000L;
			case "m" -> 1_000_000L;
			case "b" -> 1_000_000_000L;
			case "t" -> 1_000_000_000_000L;
			default -> 1L;
		};
		BigDecimal v = new BigDecimal(m.group(1)).multiply(BigDecimal.valueOf(mult));
		if (v.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) return -1;
		return v.longValue();
	}

	private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer placer = c.getSource().getPlayerOrException();
		ServerPlayer target = EntityArgument.getPlayer(c, "player");
		MinecraftServer server = c.getSource().getServer();

		long amount = parseAmount(StringArgumentType.getString(c, "amount"));
		if (amount <= 0) {
			placer.sendSystemMessage(fail("Bad amount. Use a number like 50000, 50k, 2.5M or 1B."));
			return 0;
		}
		if (target.getUUID().equals(placer.getUUID())) {
			placer.sendSystemMessage(fail("You can't put a bounty on yourself."));
			return 0;
		}
		if (amount < Config.minBounty) {
			placer.sendSystemMessage(fail("The smallest bounty is $" + Money.fmt(Config.minBounty) + "."));
			return 0;
		}
		BountyData data = BountyData.get(server);
		if (data.get(target.getUUID()) != null) {
			placer.sendSystemMessage(fail(target.getName().getString() + " already has a bounty."));
			return 0;
		}
		// You pay right now. The money comes back to you only if nobody kills them in time.
		if (!Money.take(server, placer.getUUID(), amount)) {
			placer.sendSystemMessage(fail("Not enough money. You have $"
					+ Money.fmt(Money.balance(server, placer.getUUID())) + "."));
			return 0;
		}
		String placerName = placer.getName().getString();
		String targetName = target.getName().getString();
		data.set(target.getUUID(), new BountyData.Bounty(placer.getUUID(), placerName, targetName, amount,
				Bounties.now(server) + Config.bountyTicks));
		Bounties.announceSet(server, placerName, targetName, amount);
		return 1;
	}

	private static int list(CommandContext<CommandSourceStack> c) {
		MinecraftServer server = c.getSource().getServer();
		List<BountyData.Bounty> all = new ArrayList<>(BountyData.get(server).all().values());
		if (all.isEmpty()) {
			c.getSource().sendSuccess(() -> Component.literal("No active bounties.").withStyle(ChatFormatting.GRAY), false);
			return 0;
		}
		all.sort(Comparator.comparingLong(BountyData.Bounty::amount).reversed());
		long now = Bounties.now(server);
		c.getSource().sendSuccess(() -> Component.literal("Active bounties:").withStyle(ChatFormatting.GOLD), false);
		for (BountyData.Bounty b : all) {
			Component line = Component.literal(" " + b.targetName() + "  ").withStyle(ChatFormatting.RED)
					.append(Component.literal("$" + Money.fmt(b.amount())).withStyle(ChatFormatting.GOLD))
					.append(Component.literal("  " + Bounties.timeLeft(b.expiresAt() - now) + " left  (set by "
							+ b.placerName() + ")").withStyle(ChatFormatting.GRAY));
			c.getSource().sendSuccess(() -> line, false);
		}
		return all.size();
	}
}
