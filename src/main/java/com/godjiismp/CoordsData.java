package com.godjiismp;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Players who switched the coordinates bar OFF (everyone else sees it). Saved inside the world. */
public class CoordsData extends SavedData {
	private final Set<UUID> hidden = new HashSet<>();

	public static final SavedData.Factory<CoordsData> FACTORY =
			new SavedData.Factory<>(CoordsData::new, CoordsData::load, null);

	public static CoordsData get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(FACTORY, "godjiismp_coords");
	}

	public boolean isHidden(UUID id) {
		return hidden.contains(id);
	}

	/** Flips the setting. Returns true if the bar is now visible. */
	public boolean toggle(UUID id) {
		boolean nowVisible = hidden.remove(id);
		if (!nowVisible) hidden.add(id);
		setDirty();
		return nowVisible;
	}

	public static CoordsData load(CompoundTag tag, HolderLookup.Provider lookup) {
		CoordsData data = new CoordsData();
		ListTag list = tag.getList("hidden", Tag.TAG_STRING);
		for (int i = 0; i < list.size(); i++) {
			try {
				data.hidden.add(UUID.fromString(list.getString(i)));
			} catch (IllegalArgumentException ignored) {
			}
		}
		return data;
	}

	@Override
	public CompoundTag save(CompoundTag tag, HolderLookup.Provider lookup) {
		ListTag list = new ListTag();
		for (UUID id : hidden) list.add(StringTag.valueOf(id.toString()));
		tag.put("hidden", list);
		return tag;
	}
}
