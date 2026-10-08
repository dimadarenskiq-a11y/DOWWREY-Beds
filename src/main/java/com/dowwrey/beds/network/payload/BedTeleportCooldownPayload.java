package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BedTeleportCooldownPayload(int remainingTicks) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("bed_teleport_cooldown");
    public static final Type<BedTeleportCooldownPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BedTeleportCooldownPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, BedTeleportCooldownPayload::remainingTicks,
                    BedTeleportCooldownPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
