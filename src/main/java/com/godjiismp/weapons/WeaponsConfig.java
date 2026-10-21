package com.godjiismp.weapons;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Server-side balance for legendary weapons. */
public final class WeaponsConfig {
	private WeaponsConfig() {}

	public static final String[] IDS = {"skeleton_king_blade", "stormcaller_spear", "earthbreaker_hammer",
			"crimson_moon_scythe", "frostbite_bow", "emberfang_axe"};
	private static final int BALANCE_VERSION = 4;
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	// Target about 80% above Sharpness V Netherite benchmarks: sword-like 19.8, axe-like 23.4.
	private static final double[] DEFAULT_DAMAGE = {16.8, 16.8, 20.4, 16.8, 16.8, 20.4};
	private static final double[] DEFAULT_SPEED = {1.6, 1.4, 1.0, 1.5, 1.6, 1.0};
	private static final int[] DEFAULT_DURABILITY = {2031, 2031, 2031, 2031, 2031, 2031};
	private static final int[] DEFAULT_ENCHANTABILITY = {15, 15, 15, 15, 1, 15};
	private static final long[] DEFAULT_SELL_PRICE = {400_000_000L, 380_000_000L, 400_000_000L,
			350_000_000L, 350_000_000L, 370_000_000L};

	public static double[] damage = DEFAULT_DAMAGE.clone();
	public static double[] speed = DEFAULT_SPEED.clone();
	public static double[] pvpMultiplier = {.5, .5, .5, .5, .5, .5};
	public static int[] durability = DEFAULT_DURABILITY.clone();
	public static int[] enchantability = DEFAULT_ENCHANTABILITY.clone();
	public static long[] sellPrice = DEFAULT_SELL_PRICE.clone();
	public static int[] requiredAttack = {5, 7, 9, 0, 0, 0};
	public static int[] cooldownSeconds = {12, 6, 15, 0, 0, 0};
	public static double[] abilityDamage = {14.4, 10.8, 10.8, 0, 0, 0};
	public static double[] radius = {8, 6, 5, 0, 0, 0};
	public static double[] specialVsUndead = {1.5, 1, 1, 1, 1, 1};
	public static double bowProjectileDamageMultiplier = 1.8;
	public static int[] chainTargets = {1, 2, 0, 0, 0, 0};
	public static int comboHits = 3, comboWindowSeconds = 4;
	public static double waveHalfWidth = .8, hammerPushHorizontal = .8, hammerPushVertical = .45;

	public static void load() {
		Path path = FabricLoader.getInstance().getConfigDir().resolve("godjiismp_weapons.json");
		try {
			JsonObject config = defaults();
			int oldVersion = 0;
			if (Files.exists(path)) {
				JsonObject previous = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
				oldVersion = previous.has("balanceVersion") ? previous.get("balanceVersion").getAsInt() : 0;
				previous.entrySet().forEach(entry -> config.add(entry.getKey(), entry.getValue()));
			}
			if (oldVersion < BALANCE_VERSION) {
				config.add("damage", GSON.toJsonTree(DEFAULT_DAMAGE));
				config.add("enchantability", GSON.toJsonTree(DEFAULT_ENCHANTABILITY));
				config.add("abilityDamage", GSON.toJsonTree(new double[]{14.4, 10.8, 10.8, 0, 0, 0}));
				config.addProperty("bowProjectileDamageMultiplier", 1.8);
			}
			config.addProperty("balanceVersion", BALANCE_VERSION);
			if (!Files.exists(path)) Files.createDirectories(path.getParent());

			damage = doubleArray(config, "damage", DEFAULT_DAMAGE);
			speed = doubleArray(config, "attackSpeed", DEFAULT_SPEED);
			durability = intArray(config, "durability", DEFAULT_DURABILITY);
			enchantability = intArray(config, "enchantability", DEFAULT_ENCHANTABILITY);
			sellPrice = longArray(config, "sellPrice", DEFAULT_SELL_PRICE);
			requiredAttack = intArray(config, "requiredAttackLevel", requiredAttack);
			cooldownSeconds = intArray(config, "cooldownSeconds", cooldownSeconds);
			abilityDamage = doubleArray(config, "abilityDamage", abilityDamage);
			radius = doubleArray(config, "abilityRadius", radius);
			chainTargets = intArray(config, "chainTargets", chainTargets);
			comboHits = config.get("comboHits").getAsInt();
			comboWindowSeconds = config.get("comboWindowSeconds").getAsInt();
			waveHalfWidth = config.get("waveHalfWidth").getAsDouble();
			hammerPushHorizontal = config.get("hammerPushHorizontal").getAsDouble();
			hammerPushVertical = config.get("hammerPushVertical").getAsDouble();
			specialVsUndead = doubleArray(config, "undeadDamageMultiplier", specialVsUndead);
			pvpMultiplier = doubleArray(config, "pvpDamageMultiplier", pvpMultiplier);
			bowProjectileDamageMultiplier = config.get("bowProjectileDamageMultiplier").getAsDouble();
			Files.writeString(path, GSON.toJson(config) + "\n");
		} catch (Exception exception) {
			throw new IllegalStateException("Invalid godjiismp_weapons.json", exception);
		}
	}

	private static JsonObject defaults() {
		JsonObject config = new JsonObject();
		config.addProperty("balanceVersion", BALANCE_VERSION);
		config.add("damage", GSON.toJsonTree(DEFAULT_DAMAGE));
		config.add("attackSpeed", GSON.toJsonTree(DEFAULT_SPEED));
		config.add("durability", GSON.toJsonTree(DEFAULT_DURABILITY));
		config.add("enchantability", GSON.toJsonTree(DEFAULT_ENCHANTABILITY));
		config.add("sellPrice", GSON.toJsonTree(DEFAULT_SELL_PRICE));
		config.add("requiredAttackLevel", GSON.toJsonTree(requiredAttack));
		config.add("cooldownSeconds", GSON.toJsonTree(cooldownSeconds));
		config.add("abilityDamage", GSON.toJsonTree(abilityDamage));
		config.add("abilityRadius", GSON.toJsonTree(radius));
		config.add("chainTargets", GSON.toJsonTree(chainTargets));
		config.addProperty("comboHits", comboHits);
		config.addProperty("comboWindowSeconds", comboWindowSeconds);
		config.addProperty("waveHalfWidth", waveHalfWidth);
		config.addProperty("hammerPushHorizontal", hammerPushHorizontal);
		config.addProperty("hammerPushVertical", hammerPushVertical);
		config.add("undeadDamageMultiplier", GSON.toJsonTree(specialVsUndead));
		config.add("pvpDamageMultiplier", GSON.toJsonTree(pvpMultiplier));
		config.addProperty("bowProjectileDamageMultiplier", bowProjectileDamageMultiplier);
		return config;
	}

	private static double[] doubleArray(JsonObject config, String key, double[] defaults) {
		double[] values = GSON.fromJson(config.get(key), double[].class);
		if (values.length != defaults.length) throw new IllegalArgumentException(key + " array length");
		return values;
	}
	private static int[] intArray(JsonObject config, String key, int[] defaults) {
		int[] values = GSON.fromJson(config.get(key), int[].class);
		if (values.length != defaults.length) throw new IllegalArgumentException(key + " array length");
		return values;
	}
	private static long[] longArray(JsonObject config, String key, long[] defaults) {
		long[] values = GSON.fromJson(config.get(key), long[].class);
		if (values.length != defaults.length) throw new IllegalArgumentException(key + " array length");
		return values;
	}
}
