package com.godjiismp;

import net.minecraft.server.MinecraftServer;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Talks to the money of the My Market mod (class com.mymarket.MoneyData).
 * Uses reflection so this mod builds on its own and only needs My Market at runtime.
 */
public final class Money {
	private Money() {}

	private static Method getData, getBalance, take, add;

	private static void init() throws ReflectiveOperationException {
		if (getData != null) return;
		Class<?> c = Class.forName("com.mymarket.MoneyData");
		getData = c.getMethod("get", MinecraftServer.class);
		getBalance = c.getMethod("get", UUID.class);
		take = c.getMethod("take", UUID.class, long.class);
		add = c.getMethod("add", UUID.class, long.class);
	}

	public static long balance(MinecraftServer server, UUID id) {
		try {
			init();
			return (long) getBalance.invoke(getData.invoke(null, server), id);
		} catch (ReflectiveOperationException | RuntimeException e) {
			GodjiiSmpMod.LOGGER.error("Could not read money from My Market", e);
			return 0;
		}
	}

	/** Takes the money. Returns false (and takes nothing) if the player has too little. */
	public static boolean take(MinecraftServer server, UUID id, long amount) {
		try {
			init();
			return (boolean) take.invoke(getData.invoke(null, server), id, amount);
		} catch (ReflectiveOperationException | RuntimeException e) {
			GodjiiSmpMod.LOGGER.error("Could not take money from My Market", e);
			return false;
		}
	}

	public static void give(MinecraftServer server, UUID id, long amount) {
		try {
			init();
			add.invoke(getData.invoke(null, server), id, amount);
		} catch (ReflectiveOperationException | RuntimeException e) {
			GodjiiSmpMod.LOGGER.error("Could not give money back via My Market", e);
		}
	}

	/** 1-999, then 1K, 1.5M, 2.25B ... (rounds down) */
	public static String fmt(long v) {
		String[] suffix = {"", "K", "M", "B", "T", "Qa", "Qi"};
		if (v < 1000) return Long.toString(v);
		int idx = 0;
		long pow = 1;
		while (idx < suffix.length - 1 && v / pow >= 1000) {
			pow *= 1000;
			idx++;
		}
		int frac = (int) Math.floor((double) (v % pow) / pow * 100);
		String f = frac == 0 ? "" : (frac % 10 == 0 ? "." + (frac / 10) : String.format(java.util.Locale.US, ".%02d", frac));
		return (v / pow) + f + suffix[idx];
	}
}
