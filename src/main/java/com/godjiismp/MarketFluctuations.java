package com.godjiismp;

import com.mojang.brigadier.CommandDispatcher;
import com.mymarket.Prices;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** Daily randomized sell-price events. Shop buy prices are never modified. */
public final class MarketFluctuations {
	private MarketFluctuations() {}
	private static final long PERIOD_MS = Duration.ofHours(24).toMillis();
	private static volatile String hotCategory = "";
	private static volatile String coldCategory = "";
	private static volatile int hotPercent;
	private static volatile int coldPercent;
	private static volatile long nextResetAt;

	public static void init() {
		CommandRegistrationCallback.EVENT.register(MarketFluctuations::register);
		ServerTickEvents.END_SERVER_TICK.register(MarketFluctuations::tick);
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			MarketEventData data = MarketEventData.get(server);
			copy(data);
			if (!hotCategory.isEmpty()) sendCurrent(handler.player);
		});
	}

	private static void register(CommandDispatcher<CommandSourceStack> dispatcher,
								 CommandBuildContext context, Commands.CommandSelection environment) {
		dispatcher.register(Commands.literal("market").executes(c -> {
			ServerPlayer player = c.getSource().getPlayerOrException();
			MarketEventData data = MarketEventData.get(c.getSource().getServer());
			copy(data);
			sendCurrent(player);
			return 1;
		}));
	}

	private static void tick(MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) return;
		MarketEventData data = MarketEventData.get(server);
		if (data.nextResetAt() <= System.currentTimeMillis()) {
			List<String> categories = categoryNames();
			if (categories.isEmpty()) return; // Prices reloads after server startup; retry on the next second.
			roll(server, data, categories);
		} else {
			copy(data);
		}
	}

	private static List<String> categoryNames() {
		List<String> names = new ArrayList<>();
		try {
			Prices.categories().forEach(category -> {
				String name = category.name().getString().trim();
				if (!name.isEmpty() && !names.contains(name)) names.add(name);
			});
		} catch (RuntimeException ex) {
			GodjiiSmpMod.LOGGER.warn("Market categories are not ready yet", ex);
		}
		return names;
	}

	private static void roll(MinecraftServer server, MarketEventData data, List<String> categories) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		String hot = categories.get(random.nextInt(categories.size()));
		String cold = "";
		if (categories.size() > 1 && random.nextBoolean()) {
			List<String> others = categories.stream().filter(name -> !name.equals(hot)).toList();
			cold = others.get(random.nextInt(others.size()));
		}
		int up = random.nextInt(10, 31);
		int down = cold.isEmpty() ? 0 : random.nextInt(10, 21);
		data.set(hot, cold, up, down, System.currentTimeMillis() + PERIOD_MS);
		copy(data);
		String message = "Market today: " + hot + " are HOT (sell +" + up + "%).";
		if (!cold.isEmpty()) message += " " + cold + " are COLD (sell -" + down + "%).";
		server.getPlayerList().broadcastSystemMessage(Component.literal(message).withStyle(ChatFormatting.GOLD), false);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) sendCurrent(player);
	}

	private static void copy(MarketEventData data) {
		hotCategory = data.hotCategory();
		coldCategory = data.coldCategory();
		hotPercent = data.hotPercent();
		coldPercent = data.coldPercent();
		nextResetAt = data.nextResetAt();
	}

	private static void sendCurrent(ServerPlayer player) {
		String text = hotCategory.isEmpty() ? "Market prices are loading." :
				"Market: " + hotCategory + " HOT +" + hotPercent + "%" +
				(coldCategory.isEmpty() ? "" : ", " + coldCategory + " COLD -" + coldPercent + "%") +
				". Resets in " + remaining(nextResetAt) + ".";
		player.sendSystemMessage(Component.literal(text).withStyle(ChatFormatting.GOLD));
	}

	private static String remaining(long timestamp) {
		long seconds = Math.max(0, (timestamp - System.currentTimeMillis()) / 1000);
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		return hours + "h " + minutes + "m";
	}

	/** Mixin hook for Prices.unitSell; the shop's buyPrice path remains untouched. */
	public static long applySellPrice(ItemStack stack, long basePrice) {
		if (stack.isEmpty() || basePrice <= 0) return basePrice;
		String categoryName = categoryFor(stack);
		if (categoryName.equals(hotCategory) && hotPercent > 0) {
			return Math.max(1, Math.round(basePrice * (100.0 + hotPercent) / 100.0));
		}
		if (categoryName.equals(coldCategory) && coldPercent > 0) {
			return Math.max(1, Math.round(basePrice * (100.0 - coldPercent) / 100.0));
		}
		return basePrice;
	}

	private static String categoryFor(ItemStack stack) {
		try {
			for (var category : Prices.categories()) {
				if (category.entries().stream().anyMatch(entry -> entry.item() == stack.getItem())) {
					return category.name().getString().trim();
				}
			}
		} catch (RuntimeException ignored) { }
		return "";
	}
}
