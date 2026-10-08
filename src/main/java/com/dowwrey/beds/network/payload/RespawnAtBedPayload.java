package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RespawnAtBedPayload(UUID bedId) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("respawn_at_bed");
    public static final Type<RespawnAtBedPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RespawnAtBedPayload> CODEC =
            UUIDUtil.STREAM_CODEC.map(RespawnAtBedPayload::new, RespawnAtBedPayload::bedId).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
