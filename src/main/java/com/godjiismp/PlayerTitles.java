package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.List;
import java.util.Locale;

/** Permanent money titles shown in player names and the tab list, without scoreboard-team changes. */
public final class PlayerTitles {
	private PlayerTitles() {}
	private record Title(String id, String label, long cost, ChatFormatting color) {}
	private static final List<Title> TITLES = List.of(
			new Title("wealthy", "Wealthy", 10_000_000L, ChatFormatting.GREEN),
			new Title("millionaire", "Millionaire", 50_000_000L, ChatFormatting.AQUA),
			new Title("tycoon", "Tycoon", 250_000_000L, ChatFormatting.GOLD),
			new Title("billionaire", "Billionaire", 1_000_000_000L, ChatFormatting.LIGHT_PURPLE));

	public static void init() {
		CommandRegistrationCallback.EVENT.register(PlayerTitles::register);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> apply(handler.player));
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
							 Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("title")
				.executes(c -> show(c.getSource().getPlayerOrException()))
				.then(Commands.literal("off").executes(c -> off(c.getSource().getPlayerOrException())))
				.then(Commands.literal("set").then(Commands.argument("id", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(TITLES.stream().map(Title::id), b))
						.executes(c -> set(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id")))))
				.then(Commands.literal("buy").then(Commands.argument("id", StringArgumentType.word())
						.suggests((c, b) -> SharedSuggestionProvider.suggest(TITLES.stream().map(Title::id), b))
						.executes(c -> buy(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "id"))))));
	}

	private static int show(ServerPlayer player) {
		TitleData data = TitleData.get(player.getServer());
		String current = data.active(player.getUUID());
		player.sendSystemMessage(Component.literal("Your title: " + (current.isEmpty() ? "none" : label(current))
				+ ". Buy: /title buy <wealthy|millionaire|tycoon|billionaire>; choose: /title set <id>; hide: /title off.")
				.withStyle(ChatFormatting.GOLD));
		for (Title t : TITLES) player.sendSystemMessage(Component.literal("[" + t.label() + "] $" + Money.fmt(t.cost())
				+ (data.owns(player.getUUID(), t.id()) ? " (owned)" : "")).withStyle(t.color()));
		return 1;
	}

	private static int buy(ServerPlayer player, String typed) {
		Title title = find(typed);
		if (title == null) return fail(player, "Unknown title. Use /title to see the list.");
		TitleData data = TitleData.get(player.getServer());
		if (data.owns(player.getUUID(), title.id())) return fail(player, "You already own this permanent title.");
		long balance = Money.balance(player.getServer(), player.getUUID());
		if (balance < title.cost()) return fail(player, "You need $" + Money.fmt(title.cost()) + ". Your balance is $" + Money.fmt(balance) + ".");
		if (!Money.take(player.getServer(), player.getUUID(), title.cost())) return fail(player, "Your balance changed; title was not purchased.");
		data.purchase(player.getUUID(), title.id());
		apply(player);
		player.sendSystemMessage(Component.literal("Purchased [" + title.label() + "] permanently for $" + Money.fmt(title.cost()) + ".")
				.withStyle(title.color()));
		return 1;
	}

	private static int set(ServerPlayer player, String typed) {
		Title title = find(typed);
		if (title == null) return fail(player, "Unknown title. Use /title to see the list.");
		TitleData data = TitleData.get(player.getServer());
		if (!data.owns(player.getUUID(), title.id())) return fail(player, "You do not own that title. Use /title buy " + title.id() + ".");
		data.activate(player.getUUID(), title.id());
		apply(player);
		player.sendSystemMessage(Component.literal("Title set to [" + title.label() + "].").withStyle(title.color()));
		return 1;
	}

	private static int off(ServerPlayer player) {
		TitleData.get(player.getServer()).deactivate(player.getUUID());
		apply(player);
		player.sendSystemMessage(Component.literal("Your title is hidden. Purchases remain permanent.").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static void apply(ServerPlayer player) {
		TitleData data = TitleData.get(player.getServer());
		String active = data.active(player.getUUID());
		Title title = find(active);
		Scoreboard scoreboard = player.getServer().getScoreboard();
		String entry = player.getScoreboardName();
		String ownTeamName = titleTeamName(player);
		if (title == null) {
			PlayerTeam current = scoreboard.getPlayersTeam(entry);
			if (current != null && current.getName().equals(ownTeamName)) scoreboard.removePlayerFromTeam(entry, current);
			String previousName = data.previousTeam(player.getUUID());
			if (!previousName.isEmpty()) {
				PlayerTeam previous = scoreboard.getPlayerTeam(previousName);
				if (previous != null) scoreboard.addPlayerToTeam(entry, previous);
			}
			data.clearPreviousTeam(player.getUUID());
			PlayerTeam oldTitleTeam = scoreboard.getPlayerTeam(ownTeamName);
			if (oldTitleTeam != null && oldTitleTeam.getPlayers().isEmpty()) scoreboard.removePlayerTeam(oldTitleTeam);
			return;
		}

		PlayerTeam current = scoreboard.getPlayersTeam(entry);
		if (current == null || !current.getName().equals(ownTeamName)) {
			if (!data.hasPreviousTeam(player.getUUID())) data.recordPreviousTeam(player.getUUID(), current == null ? "" : current.getName());
			if (current != null) scoreboard.removePlayerFromTeam(entry, current);
		}
		PlayerTeam team = scoreboard.getPlayerTeam(ownTeamName);
		if (team == null) team = scoreboard.addPlayerTeam(ownTeamName);
		String previousName = data.previousTeam(player.getUUID());
		PlayerTeam previous = previousName.isEmpty() ? null : scoreboard.getPlayerTeam(previousName);
		team.setPlayerPrefix(Component.literal("[" + title.label() + "] ").withStyle(title.color())
				.append(previous == null ? Component.empty() : previous.getPlayerPrefix()));
		if (previous != null) {
			team.setPlayerSuffix(previous.getPlayerSuffix());
			team.setAllowFriendlyFire(previous.isAllowFriendlyFire());
			team.setSeeFriendlyInvisibles(previous.canSeeFriendlyInvisibles());
			team.setNameTagVisibility(previous.getNameTagVisibility());
			team.setDeathMessageVisibility(previous.getDeathMessageVisibility());
			team.setColor(previous.getColor());
			team.setCollisionRule(previous.getCollisionRule());
		}
		scoreboard.addPlayerToTeam(entry, team);
	}

	private static String titleTeamName(ServerPlayer player) {
		return "gt" + player.getUUID().toString().replace("-", "").substring(0, 14);
	}

	private static Title find(String id) {
		String normalized = id == null ? "" : id.toLowerCase(Locale.ROOT);
		return TITLES.stream().filter(t -> t.id().equals(normalized)).findFirst().orElse(null);
	}
	private static String label(String id) { Title t = find(id); return t == null ? id : t.label(); }
	private static int fail(ServerPlayer player, String message) {
		player.sendSystemMessage(Component.literal(message).withStyle(ChatFormatting.RED));
		return 0;
	}
}
