package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record StartBedTeleportPayload(UUID bedId, int mode) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("start_bed_teleport");
    public static final Type<StartBedTeleportPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, StartBedTeleportPayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC.cast(), StartBedTeleportPayload::bedId,
                    ByteBufCodecs.VAR_INT, StartBedTeleportPayload::mode,
                    StartBedTeleportPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
