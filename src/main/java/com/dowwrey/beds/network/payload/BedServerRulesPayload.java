package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BedServerRulesPayload(
        int maxBeds,
        boolean friendBedsEnabled,
        boolean teleportEnabled,
        boolean brokenBedTeleportEnabled,
        int teleportCooldownSeconds,
        int slowTeleportSeconds,
        int slowTeleportCost,
        int fastTeleportSeconds,
        int fastTeleportCost,
        int instantTeleportSeconds,
        int instantTeleportCost,
        int brokenTeleportSeconds,
        int brokenTeleportCost
) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("server_rules");
    public static final Type<BedServerRulesPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BedServerRulesPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::maxBeds,
            ByteBufCodecs.BOOL, BedServerRulesPayload::friendBedsEnabled,
            ByteBufCodecs.BOOL, BedServerRulesPayload::teleportEnabled,
            ByteBufCodecs.BOOL, BedServerRulesPayload::brokenBedTeleportEnabled,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::teleportCooldownSeconds,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::slowTeleportSeconds,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::slowTeleportCost,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::fastTeleportSeconds,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::fastTeleportCost,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::instantTeleportSeconds,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::instantTeleportCost,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::brokenTeleportSeconds,
            ByteBufCodecs.VAR_INT, BedServerRulesPayload::brokenTeleportCost,
            BedServerRulesPayload::new
    );
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
