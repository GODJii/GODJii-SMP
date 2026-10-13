package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Active bounties, saved inside the world. At most one per target. */
public class BountyData extends SavedData {
	/** expiresAt is a game-time tick (overworld.getGameTime()). */
	public record Bounty(UUID placer, String placerName, String targetName, long amount, long expiresAt) {}

	private final Map<UUID, Bounty> bounties = new HashMap<>();

	public static final SavedData.Factory<BountyData> FACTORY =
			new SavedData.Factory<>(BountyData::new, BountyData::load, null);

	public static BountyData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_bounties");
	}

	public Bounty get(UUID target) {
		return bounties.get(target);
	}

	public void set(UUID target, Bounty b) {
		bounties.put(target, b);
		setDirty();
	}

	public Bounty remove(UUID target) {
		Bounty b = bounties.remove(target);
		if (b != null) setDirty();
		return b;
	}

	/** A copy, safe to loop over while removing. */
	public Map<UUID, Bounty> all() {
		return new HashMap<>(bounties);
	}

	public static BountyData load(CompoundTag tag, HolderLookup.Provider lookup) {
		BountyData data = new BountyData();
		CompoundTag all = tag.getCompound("bounties");
		for (String key : all.getAllKeys()) {
			try {
				CompoundTag t = all.getCompound(key);
				data.bounties.put(UUID.fromString(key), new Bounty(UUID.fromString(t.getString("placer")),
						t.getString("placerName"), t.getString("targetName"), t.getLong("amount"), t.getLong("expires")));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		CompoundTag all = new CompoundTag();
		bounties.forEach((target, b) -> {
			CompoundTag t = new CompoundTag();
			t.putString("placer", b.placer().toString());
			t.putString("placerName", b.placerName());
			t.putString("targetName", b.targetName());
			t.putLong("amount", b.amount());
			t.putLong("expires", b.expiresAt());
			all.put(target.toString(), t);
		});
		tag.put("bounties", all);
		return tag;
	}
}
