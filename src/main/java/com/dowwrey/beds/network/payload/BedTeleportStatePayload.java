package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record BedTeleportStatePayload(UUID bedId, String bedName, int durationTicks, int cost, int state) implements CustomPacketPayload {
    public static final int STARTED = 0;
    public static final int COMPLETED = 1;
    public static final int CANCELLED = 2;

    public static final Identifier ID = DowwreyBeds.id("bed_teleport_state");
    public static final Type<BedTeleportStatePayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BedTeleportStatePayload> CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC.cast(), BedTeleportStatePayload::bedId,
                    ByteBufCodecs.stringUtf8(32), BedTeleportStatePayload::bedName,
                    ByteBufCodecs.VAR_INT, BedTeleportStatePayload::durationTicks,
                    ByteBufCodecs.VAR_INT, BedTeleportStatePayload::cost,
                    ByteBufCodecs.VAR_INT, BedTeleportStatePayload::state,
                    BedTeleportStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
