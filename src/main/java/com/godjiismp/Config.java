package com.godjiismp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** config/godjiismp.json, created on first run. */
public final class Config {
	private Config() {}

	/** Price of one trip home. */
	public static long cost = 1_000_000L;
	/** Seconds you must wait between two trips home. 0 = no waiting. */
	public static int cooldownSeconds = 30;
	/** Smallest bounty you can put on someone. */
	public static long minBounty = 10_000L;
	/** How long a bounty lasts, in game ticks. 24000 = one Minecraft day = 20 real minutes. */
	public static long bountyTicks = 24_000L;
	/** /sethome also makes this your respawn point after death (instead of the bed / world spawn). */
	public static boolean respawnAtHome = true;

	public static void load() {
		Path file = FabricLoader.getInstance().getConfigDir().resolve("godjiismp.json");
		try {
			if (Files.exists(file)) {
				try (Reader r = Files.newBufferedReader(file)) {
					JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
					if (o.has("cost")) cost = Math.max(0, o.get("cost").getAsLong());
					if (o.has("cooldownSeconds")) cooldownSeconds = Math.max(0, o.get("cooldownSeconds").getAsInt());
					if (o.has("minBounty")) minBounty = Math.max(1, o.get("minBounty").getAsLong());
					if (o.has("bountyTicks")) bountyTicks = Math.max(20, o.get("bountyTicks").getAsLong());
					if (o.has("respawnAtHome")) respawnAtHome = o.get("respawnAtHome").getAsBoolean();
				}
			} else {
				JsonObject o = new JsonObject();
				o.addProperty("cost", cost);
				o.addProperty("cooldownSeconds", cooldownSeconds);
				o.addProperty("minBounty", minBounty);
				o.addProperty("bountyTicks", bountyTicks);
				o.addProperty("respawnAtHome", respawnAtHome);
				Gson gson = new GsonBuilder().setPrettyPrinting().create();
				Files.writeString(file, gson.toJson(o) + "\n");
			}
		} catch (Exception ex) {
			GodjiiSmpMod.LOGGER.error("Could not read godjiismp.json, using defaults", ex);
		}
	}
}
