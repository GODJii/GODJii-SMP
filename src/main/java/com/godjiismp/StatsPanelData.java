package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class StatsPanelData extends SavedData {
	public static final SavedData.Factory<StatsPanelData> FACTORY = new SavedData.Factory<>(StatsPanelData::new, StatsPanelData::load, null);
	private final Set<UUID> hidden = new HashSet<>();
	public static StatsPanelData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_stats_panel"); }
	public boolean isVisible(UUID id) { return !hidden.contains(id); }
	public boolean toggle(UUID id) { if (hidden.remove(id)) { setDirty(); return true; } hidden.add(id); setDirty(); return false; }
	public static StatsPanelData load(CompoundTag tag, HolderLookup.Provider lookup) {
		StatsPanelData data = new StatsPanelData(); ListTag list = tag.getList("hidden", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) try { data.hidden.add(UUID.fromString(list.getString(i))); } catch (Exception ignored) {}
		return data;
	}
	@Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		ListTag list = new ListTag(); hidden.forEach(id -> list.add(net.minecraft.nbt.StringTag.valueOf(id.toString()))); tag.put("hidden", list); return tag;
	}
}
