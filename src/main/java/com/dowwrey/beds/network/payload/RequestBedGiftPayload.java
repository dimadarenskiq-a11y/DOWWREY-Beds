package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestBedGiftPayload() implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("request_bed_gift");
    public static final Type<RequestBedGiftPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBedGiftPayload> CODEC = StreamCodec.unit(new RequestBedGiftPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
