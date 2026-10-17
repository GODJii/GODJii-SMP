package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent per-player daily quest state. The reset day rolls at 04:00 server local time. */
public final class DailyQuestData extends SavedData {
	public static final SavedData.Factory<DailyQuestData> FACTORY = new SavedData.Factory<>(DailyQuestData::new, DailyQuestData::load, null);
	private final Map<UUID, Quest> quests = new HashMap<>();
	private final Set<String> placedBlocks = new HashSet<>();

	public static DailyQuestData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_daily_quests");
	}

	public Quest get(UUID id) { return quests.get(id); }
	public void set(UUID id, Quest quest) { quests.put(id, quest); setDirty(); }
	public boolean isPlayerPlaced(String key) { return placedBlocks.contains(key); }
	public void markPlaced(String key) { if (placedBlocks.add(key)) setDirty(); }
	public void unmarkPlaced(String key) { if (placedBlocks.remove(key)) setDirty(); }
	public void clearPlaced() { if (!placedBlocks.isEmpty()) { placedBlocks.clear(); setDirty(); } }
	public void progress(UUID id, int amount) {
		Quest q = quests.get(id);
		if (q == null || q.complete()) return;
		int value = Math.min(q.target(), q.progress() + amount);
		quests.put(id, new Quest(q.id(), q.label(), q.target(), value, q.day(), value >= q.target()));
		setDirty();
	}
	public boolean markPaid(UUID id) {
		Quest q = quests.get(id);
		if (q == null || !q.complete() || q.paid()) return false;
		quests.put(id, new Quest(q.id(), q.label(), q.target(), q.progress(), q.day(), true, true));
		setDirty();
		return true;
	}

	public static DailyQuestData load(CompoundTag tag, HolderLookup.Provider lookup) {
		DailyQuestData data = new DailyQuestData();
		ListTag list = tag.getList("quests", Tag.TAG_COMPOUND);
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompound(i);
			try {
				UUID id = entry.getUUID("player");
				data.quests.put(id, new Quest(entry.getString("id"), entry.getString("label"), entry.getInt("target"),
						entry.getInt("progress"), entry.getLong("day"), entry.getBoolean("complete"), entry.getBoolean("paid")));
			} catch (Exception ignored) { }
		}
		ListTag placed = tag.getList("placedBlocks", Tag.TAG_STRING);
		for (int i = 0; i < placed.size(); i++) data.placedBlocks.add(placed.getString(i));
		return data;
	}

	@Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		ListTag list = new ListTag();
		quests.forEach((id, q) -> {
			CompoundTag entry = new CompoundTag();
			entry.putUUID("player", id); entry.putString("id", q.id()); entry.putString("label", q.label());
			entry.putInt("target", q.target()); entry.putInt("progress", q.progress()); entry.putLong("day", q.day());
			entry.putBoolean("complete", q.complete()); entry.putBoolean("paid", q.paid()); list.add(entry);
		});
		tag.put("quests", list);
		ListTag placed = new ListTag();
		placedBlocks.forEach(key -> placed.add(net.minecraft.nbt.StringTag.valueOf(key)));
		tag.put("placedBlocks", placed);
		return tag;
	}

	public record Quest(String id, String label, int target, int progress, long day, boolean complete, boolean paid) {
		public Quest(String id, String label, int target, int progress, long day, boolean complete) {
			this(id, label, target, progress, day, complete, false);
		}
	}
}
