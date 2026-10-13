package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** One home per player, saved inside the world. */
public class HomeData extends SavedData {
	public record Home(String dimension, double x, double y, double z, float yaw, float pitch) {}

	private final Map<UUID, Home> homes = new HashMap<>();

	public static final SavedData.Factory<HomeData> FACTORY =
			new SavedData.Factory<>(HomeData::new, HomeData::load, null);

	public static HomeData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_homes");
	}

	public Home get(UUID id) {
		return homes.get(id);
	}

	public void set(UUID id, Home home) {
		homes.put(id, home);
		setDirty();
	}

	public boolean remove(UUID id) {
		boolean had = homes.remove(id) != null;
		if (had) setDirty();
		return had;
	}

	public static HomeData load(CompoundTag tag, HolderLookup.Provider lookup) {
		HomeData data = new HomeData();
		CompoundTag all = tag.getCompound("homes");
		for (String key : all.getAllKeys()) {
			try {
				CompoundTag h = all.getCompound(key);
				data.homes.put(UUID.fromString(key), new Home(h.getString("dim"),
						h.getDouble("x"), h.getDouble("y"), h.getDouble("z"), h.getFloat("yaw"), h.getFloat("pitch")));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		CompoundTag all = new CompoundTag();
		homes.forEach((id, h) -> {
			CompoundTag t = new CompoundTag();
			t.putString("dim", h.dimension());
			t.putDouble("x", h.x());
			t.putDouble("y", h.y());
			t.putDouble("z", h.z());
			t.putFloat("yaw", h.yaw());
			t.putFloat("pitch", h.pitch());
			all.put(id.toString(), t);
		});
		tag.put("homes", all);
		return tag;
	}
}
