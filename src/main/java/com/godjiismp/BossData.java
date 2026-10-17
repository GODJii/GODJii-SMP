package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.UUID;

/** Persistent active boss guard so restarts cannot create a second Skeleton King. */
public final class BossData extends SavedData {
	public static final SavedData.Factory<BossData> FACTORY = new SavedData.Factory<>(BossData::new, BossData::load, null);
	private UUID boss;
	public static BossData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_boss"); }
	public UUID boss() { return boss; }
	public void setBoss(UUID value) { boss = value; setDirty(); }
	public static BossData load(CompoundTag tag, HolderLookup.Provider lookup) {
		BossData data = new BossData();
		try { if (tag.hasUUID("boss")) data.boss = tag.getUUID("boss"); } catch (Exception ignored) { }
		return data;
	}
	@Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		if (boss != null) tag.putUUID("boss", boss);
		return tag;
	}
}
