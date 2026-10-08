package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

/** Sent after the server has validated and armed the selected saved bed as the respawn position. */
public record RespawnAtBedApprovedPayload(UUID bedId, String bedName) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("respawn_at_bed_approved");
    public static final Type<RespawnAtBedApprovedPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RespawnAtBedApprovedPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, RespawnAtBedApprovedPayload::bedId,
                    ByteBufCodecs.stringUtf8(32), RespawnAtBedApprovedPayload::bedName,
                    RespawnAtBedApprovedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
