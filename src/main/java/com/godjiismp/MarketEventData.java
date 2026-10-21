package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Persists the current market sale-only event across restarts. */
public final class MarketEventData extends SavedData {
	public static final SavedData.Factory<MarketEventData> FACTORY =
			new SavedData.Factory<>(MarketEventData::new, MarketEventData::load, null);

	private String hotCategory = "";
	private String coldCategory = "";
	private int hotPercent;
	private int coldPercent;
	private long nextResetAt;

	public static MarketEventData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_market_event");
	}

	public String hotCategory() { return hotCategory; }
	public String coldCategory() { return coldCategory; }
	public int hotPercent() { return hotPercent; }
	public int coldPercent() { return coldPercent; }
	public long nextResetAt() { return nextResetAt; }

	public void set(String hot, String cold, int hotAmount, int coldAmount, long resetAt) {
		hotCategory = hot;
		coldCategory = cold;
		hotPercent = hotAmount;
		coldPercent = coldAmount;
		nextResetAt = resetAt;
		setDirty();
	}

	public static MarketEventData load(CompoundTag tag, HolderLookup.Provider lookup) {
		MarketEventData data = new MarketEventData();
		data.hotCategory = tag.getString("hotCategory");
		data.coldCategory = tag.getString("coldCategory");
		data.hotPercent = tag.getInt("hotPercent");
		data.coldPercent = tag.getInt("coldPercent");
		data.nextResetAt = tag.getLong("nextResetAt");
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		tag.putString("hotCategory", hotCategory);
		tag.putString("coldCategory", coldCategory);
		tag.putInt("hotPercent", hotPercent);
		tag.putInt("coldPercent", coldPercent);
		tag.putLong("nextResetAt", nextResetAt);
		return tag;
	}
}
