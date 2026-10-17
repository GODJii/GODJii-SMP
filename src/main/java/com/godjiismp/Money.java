package com.godjiismp;

import com.mymarket.MoneyData;
import net.minecraft.server.MinecraftServer;

import java.util.UUID;

/**
 * Direct bridge to the bundled My Market economy. MoneyData owns the same persistent
 * world data as before, so combining the mods does not reset player balances.
 */
public final class Money {
	private Money() {}

	public static long balance(MinecraftServer server, UUID id) {
		return MoneyData.get(server).get(id);
	}

	/** Takes the money. Returns false (and takes nothing) if the player has too little. */
	public static boolean take(MinecraftServer server, UUID id, long amount) {
		return MoneyData.get(server).take(id, amount);
	}

	public static void give(MinecraftServer server, UUID id, long amount) {
		MoneyData.get(server).add(id, amount);
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
