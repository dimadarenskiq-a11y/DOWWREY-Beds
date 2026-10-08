package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;


public record SetActiveBedPayload(UUID bedId) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("set_active_bed");
    public static final Type<SetActiveBedPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, SetActiveBedPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, SetActiveBedPayload::bedId,
                    SetActiveBedPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
