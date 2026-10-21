package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Restart-safe Blood Moon state and once-per-night scheduling markers. */
public final class BloodMoonData extends SavedData {
	public static final SavedData.Factory<BloodMoonData> FACTORY = new SavedData.Factory<>(BloodMoonData::new, BloodMoonData::load, null);
	private boolean active;
	private long warnedDay = Long.MIN_VALUE;
	private long checkedDay = Long.MIN_VALUE;
	private long startedDay = Long.MIN_VALUE;
	public static BloodMoonData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_blood_moon");
	}
	public boolean active() { return active; }
	public long warnedDay() { return warnedDay; }
	public long checkedDay() { return checkedDay; }
	public long startedDay() { return startedDay; }
	public void setActive(boolean value, long day) { active = value; if (value) startedDay = day; setDirty(); }
	public void setWarnedDay(long day) { warnedDay = day; setDirty(); }
	public void setCheckedDay(long day) { checkedDay = day; setDirty(); }
	public static BloodMoonData load(CompoundTag tag, HolderLookup.Provider lookup) {
		BloodMoonData data = new BloodMoonData();
		data.active = tag.getBoolean("active"); data.warnedDay = tag.getLong("warnedDay");
		data.checkedDay = tag.getLong("checkedDay"); data.startedDay = tag.getLong("startedDay");
		return data;
	}
	@Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		tag.putBoolean("active", active); tag.putLong("warnedDay", warnedDay);
		tag.putLong("checkedDay", checkedDay); tag.putLong("startedDay", startedDay);
		return tag;
	}
}
