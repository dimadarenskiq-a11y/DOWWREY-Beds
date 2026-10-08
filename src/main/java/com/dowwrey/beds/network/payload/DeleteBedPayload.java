package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;


public record DeleteBedPayload(UUID bedId) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("delete_bed");
    public static final Type<DeleteBedPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteBedPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, DeleteBedPayload::bedId,
                    DeleteBedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
