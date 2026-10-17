package com.godjiismp;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StatsPayload(String name, long money, int mobs, int players, int deaths, long playTicks, boolean visible) implements CustomPacketPayload {
	public static final Type<StatsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("godjiismp_newdays", "stats"));
	public static final StreamCodec<RegistryFriendlyByteBuf, StatsPayload> CODEC = StreamCodec.of((buf, value) -> value.write(buf), StatsPayload::new);
	private StatsPayload(RegistryFriendlyByteBuf b) { this(b.readUtf(64), b.readVarLong(), b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarLong(), b.readBoolean()); }
	private void write(RegistryFriendlyByteBuf b) { b.writeUtf(name, 64); b.writeVarLong(money); b.writeVarInt(mobs); b.writeVarInt(players); b.writeVarInt(deaths); b.writeVarLong(playTicks); b.writeBoolean(visible); }
	@Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
