package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * /sethome   save your base here (also your respawn point)
 * /home      pays and teleports you to YOUR home (never to anyone else's)
 * /delhome   forget your home
 */
public final class HomeCommands {
	private HomeCommands() {}

	/** When each player last teleported home (server time, ms). Resets on restart; that's fine. */
	private static final Map<UUID, Long> LAST_TRIP = new HashMap<>();

	public static void register(CommandDispatcher<CommandSourceStack> d, CommandBuildContext ctx,
								Commands.CommandSelection env) {
		d.register(Commands.literal("sethome").executes(HomeCommands::setHome));
		d.register(Commands.literal("delhome").executes(HomeCommands::delHome));
		d.register(Commands.literal("home").executes(HomeCommands::goHome));
	}

	private static Component msg(String s, ChatFormatting color) {
		return Component.literal(s).withStyle(color);
	}

	private static int setHome(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		ServerLevel level = p.serverLevel();
		HomeData.get(c.getSource().getServer()).set(p.getUUID(), new HomeData.Home(
				level.dimension().location().toString(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()));

		if (Config.respawnAtHome) {
			p.setRespawnPosition(level.dimension(), BlockPos.containing(p.position()), p.getYRot(), true, false);
		}
		p.sendSystemMessage(msg("Home set at " + p.blockPosition().toShortString()
				+ (Config.respawnAtHome ? ". You will also respawn here." : "."), ChatFormatting.GREEN));
		p.sendSystemMessage(msg("Use /home to come back for $" + Money.fmt(Config.cost) + ".", ChatFormatting.GRAY));
		return 1;
	}

	private static int delHome(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		boolean had = HomeData.get(c.getSource().getServer()).remove(p.getUUID());
		p.sendSystemMessage(had ? msg("Your home was removed.", ChatFormatting.YELLOW)
				: msg("You have no home.", ChatFormatting.RED));
		return had ? 1 : 0;
	}

	/** Charges the price and teleports the player to their own home. */
	private static int goHome(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
		ServerPlayer p = c.getSource().getPlayerOrException();
		var server = c.getSource().getServer();
		HomeData.Home home = HomeData.get(server).get(p.getUUID());
		if (home == null) {
			p.sendSystemMessage(msg("You have no home yet. Stand at your base and type /sethome", ChatFormatting.RED));
			return 0;
		}

		// cooldown
		long now = System.currentTimeMillis();
		Long last = LAST_TRIP.get(p.getUUID());
		if (last != null && Config.cooldownSeconds > 0) {
			long wait = last + Config.cooldownSeconds * 1000L - now;
			if (wait > 0) {
				p.sendSystemMessage(msg("Wait " + ((wait + 999) / 1000) + "s before the next trip.", ChatFormatting.RED));
				return 0;
			}
		}

		// make sure the home world still exists BEFORE taking any money
		ServerLevel target = server.getLevel(ResourceKey.create(Registries.DIMENSION,
				ResourceLocation.parse(home.dimension())));
		if (target == null) {
			p.sendSystemMessage(msg("Your home's dimension is not available.", ChatFormatting.RED));
			return 0;
		}

		if (!Money.take(server, p.getUUID(), Config.cost)) {
			p.sendSystemMessage(msg("Not enough money. You need $" + Money.fmt(Config.cost)
					+ " (you have $" + Money.fmt(Money.balance(server, p.getUUID())) + ").", ChatFormatting.RED));
			return 0;
		}

		p.teleportTo(target, home.x(), home.y(), home.z(), home.yaw(), home.pitch());
		p.fallDistance = 0;
		LAST_TRIP.put(p.getUUID(), now);
		p.sendSystemMessage(msg("Welcome home! -$" + Money.fmt(Config.cost), ChatFormatting.GREEN));
		return 1;
	}
}
