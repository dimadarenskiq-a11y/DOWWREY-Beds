package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record RequestBedListPayload(boolean openManager) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("request_bed_list");
    public static final Type<RequestBedListPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestBedListPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, RequestBedListPayload::openManager,
            RequestBedListPayload::new
    );

    public RequestBedListPayload() {
        this(false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
