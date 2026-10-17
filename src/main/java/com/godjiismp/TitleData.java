package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Permanent title purchases and active title choices. */
public final class TitleData extends SavedData {
	public static final SavedData.Factory<TitleData> FACTORY = new SavedData.Factory<>(TitleData::new, TitleData::load, null);
	private final Map<UUID, Map<String, Boolean>> titles = new HashMap<>();
	private final Map<UUID, String> active = new HashMap<>();

	public static TitleData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_titles");
	}
	public boolean owns(UUID player, String id) { return titles.getOrDefault(player, Map.of()).containsKey(id); }
	public void purchase(UUID player, String id) { titles.computeIfAbsent(player, k -> new HashMap<>()).put(id, true); active.put(player, id); setDirty(); }
	public String active(UUID player) { return active.getOrDefault(player, ""); }
	public void activate(UUID player, String id) { active.put(player, id); setDirty(); }
	public void deactivate(UUID player) { active.remove(player); setDirty(); }

	public static TitleData load(CompoundTag tag, HolderLookup.Provider lookup) {
		TitleData data = new TitleData();
		ListTag list = tag.getList("players", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			try {
				UUID uuid = entry.getUUID("uuid");
				Map<String, Boolean> owned = new HashMap<>();
				ListTag ownedList = entry.getList("owned", Tag.TAG_STRING);
				for (int j = 0; j < ownedList.size(); j++) owned.put(ownedList.getString(j), true);
				data.titles.put(uuid, owned);
				String selected = entry.getString("active");
				if (!selected.isEmpty()) data.active.put(uuid, selected);
			} catch (Exception ignored) { }
		}
		return data;
	}
	@Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		ListTag list = new ListTag();
		titles.forEach((uuid, owned) -> {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("uuid", uuid);
			ListTag ids = new ListTag(); owned.keySet().forEach(id -> ids.add(net.minecraft.nbt.StringTag.valueOf(id)));
			entry.put("owned", ids); entry.putString("active", active.getOrDefault(uuid, "")); list.add(entry);
		});
		tag.put("players", list);
		return tag;
	}
}
