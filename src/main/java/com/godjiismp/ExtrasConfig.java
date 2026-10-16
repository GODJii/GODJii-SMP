package com.godjiismp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/godjiismp_extras.json, created on first run. */
public final class ExtrasConfig {
	private ExtrasConfig() {}

	/** Price of removing the trim from the armor piece in your hand. */
	public static long trimRemoveCost = 100_000L;
	/** Price of dyeing (or re-dyeing) an armor piece. Turning it back to normal is free. */
	public static long armorColorCost = 250_000L;
	/** How often the coordinates bar is refreshed, in ticks (20 ticks = 1 second). */
	public static int coordsIntervalTicks = 5;
	/** When the server starts, turn "reducedDebugInfo" off (the arena turns it on to hide coordinates). */
	public static boolean fixReducedDebugOnStart = true;

	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("godjiismp_extras.json");
		try {
			if (Files.exists(file)) {
				try (Reader r = Files.newBufferedReader(file)) {
					JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
					if (o.has("trimRemoveCost")) trimRemoveCost = Math.max(0, o.get("trimRemoveCost").getAsLong());
					if (o.has("armorColorCost")) armorColorCost = Math.max(0, o.get("armorColorCost").getAsLong());
					if (o.has("coordsIntervalTicks")) coordsIntervalTicks = Math.max(1, Math.min(40, o.get("coordsIntervalTicks").getAsInt()));
					if (o.has("fixReducedDebugOnStart")) fixReducedDebugOnStart = o.get("fixReducedDebugOnStart").getAsBoolean();
				}
			} else {
				JsonObject o = new JsonObject();
				o.addProperty("trimRemoveCost", trimRemoveCost);
				o.addProperty("armorColorCost", armorColorCost);
				o.addProperty("coordsIntervalTicks", coordsIntervalTicks);
				o.addProperty("fixReducedDebugOnStart", fixReducedDebugOnStart);
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Files.writeString(file, gson.toJson(o) + "\n");
			}
		} catch (Exception ex) {
			GodjiiSmpMod.LOGGER.error("Could not read godjiismp_extras.json, using defaults", ex);
		}
	}
}
