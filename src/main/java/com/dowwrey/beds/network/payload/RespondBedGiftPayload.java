package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RespondBedGiftPayload(UUID requestId, boolean accepted) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("respond_bed_gift");
    public static final Type<RespondBedGiftPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RespondBedGiftPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RespondBedGiftPayload::requestId,
            ByteBufCodecs.BOOL, RespondBedGiftPayload::accepted,
            RespondBedGiftPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
