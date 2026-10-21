package com.godjiismp;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Persists the small per-player opt-out list for the armor aura. */
public final class AuraPreferences {
	private AuraPreferences() {}
	private static final Set<UUID> DISABLED = new HashSet<>();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("godjiismp_aura.json");

	public static void load() {
		if (!Files.exists(FILE)) return;
		try {
			JsonArray array = JsonParser.parseString(Files.readString(FILE)).getAsJsonArray();
			for (var entry : array) {
				try { DISABLED.add(UUID.fromString(entry.getAsString())); }
				catch (IllegalArgumentException ignored) { }
			}
		} catch (Exception ex) {
			GodjiiSmpMod.LOGGER.error("Could not read godjiismp_aura.json; aura preferences use defaults", ex);
		}
	}

	public static boolean enabled(UUID player) { return !DISABLED.contains(player); }

	public static void setEnabled(UUID player, boolean enabled) {
		if (enabled) DISABLED.remove(player); else DISABLED.add(player);
		JsonArray array = new JsonArray();
		DISABLED.stream().map(UUID::toString).sorted().forEach(array::add);
		try {
			Files.createDirectories(FILE.getParent());
			Files.writeString(FILE, new GsonBuilder().setPrettyPrinting().create().toJson(array) + "\n");
		} catch (Exception ex) {
			GodjiiSmpMod.LOGGER.error("Could not save godjiismp_aura.json", ex);
		}
	}
}
