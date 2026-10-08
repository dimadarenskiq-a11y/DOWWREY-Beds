package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record OpenSaveBedPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("open_save_bed");
    public static final Type<OpenSaveBedPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenSaveBedPayload> CODEC =
            StreamCodec.composite(BlockPos.STREAM_CODEC, OpenSaveBedPayload::pos, OpenSaveBedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
