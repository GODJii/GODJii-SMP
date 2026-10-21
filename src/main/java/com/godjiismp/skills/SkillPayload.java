package com.godjiismp.skills;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SkillPayload(int points,int[] levels,int[] costs,String[][] perks) implements CustomPacketPayload {
 public static final Type<SkillPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath("godjiismp_newdays","skills"));
 public static final StreamCodec<RegistryFriendlyByteBuf,SkillPayload> CODEC=StreamCodec.of((b,v)->v.write(b),SkillPayload::new);
 private SkillPayload(RegistryFriendlyByteBuf b){this(b.readVarInt(),readInts(b,6),readInts(b,10),readStrings(b));}
 private void write(RegistryFriendlyByteBuf b){b.writeVarInt(points);writeInts(b,levels);writeInts(b,costs);for(String[] line:perks)for(String s:line)b.writeUtf(s,512);}
 private static int[] readInts(RegistryFriendlyByteBuf b,int n){int[] a=new int[n];for(int i=0;i<n;i++)a[i]=b.readVarInt();return a;}
 private static void writeInts(RegistryFriendlyByteBuf b,int[] a){for(int i:a)b.writeVarInt(i);}
 private static String[][] readStrings(RegistryFriendlyByteBuf b){String[][] a=new String[6][10];for(int i=0;i<6;i++)for(int j=0;j<10;j++)a[i][j]=b.readUtf(512);return a;}
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
