package com.godjiismp.skills;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Per-player skill state stored in the overworld SavedData. */
public final class SkillData extends SavedData {
 public static final SavedData.Factory<SkillData> FACTORY=new SavedData.Factory<>(SkillData::new,SkillData::load,null);
 private final Map<UUID,Entry> entries=new HashMap<>();
 public static SkillData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(FACTORY,"godjiismp_skills");}
 public Entry entry(UUID id){return entries.computeIfAbsent(id,k->new Entry());}
 public void changed(){setDirty();}
 public static SkillData load(CompoundTag tag,HolderLookup.Provider lookup){SkillData d=new SkillData();ListTag list=tag.getList("players",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){CompoundTag t=list.getCompound(i);try{Entry e=new Entry();e.points=t.getInt("points");e.spent=t.contains("spent",Tag.TAG_INT)?t.getInt("spent"):0;e.resetAt=t.getLong("resetAt");e.defenseHealAt=t.getLong("defenseHealAt");int[] a=t.getIntArray("levels");for(int j=0;j<Math.min(a.length,6);j++)e.levels[j]=Math.max(0,Math.min(10,a[j]));if(!t.contains("spent",Tag.TAG_INT))e.spent=e.totalSpent();d.entries.put(t.getUUID("id"),e);}catch(Exception ignored){}}return d;}
 @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){ListTag list=new ListTag();entries.forEach((id,e)->{CompoundTag t=new CompoundTag();t.putUUID("id",id);t.putInt("points",e.points);t.putInt("spent",e.spent);t.putIntArray("levels",e.levels);t.putLong("resetAt",e.resetAt);t.putLong("defenseHealAt",e.defenseHealAt);list.add(t);});tag.put("players",list);return tag;}
 public static final class Entry {public int points,spent;public final int[] levels=new int[6];public long resetAt,defenseHealAt;public int totalSpent(){int n=0;for(int level:levels)for(int i=0;i<level;i++)n+=SkillsConfig.costs[i];return n;}}
}
