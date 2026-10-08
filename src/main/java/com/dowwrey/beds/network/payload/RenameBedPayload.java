package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record RenameBedPayload(UUID bedId, String name, int color, int icon) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("rename_bed");
    public static final Type<RenameBedPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, RenameBedPayload> CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, RenameBedPayload::bedId,
            ByteBufCodecs.stringUtf8(32), RenameBedPayload::name,
            ByteBufCodecs.INT, RenameBedPayload::color,
            ByteBufCodecs.VAR_INT, RenameBedPayload::icon,
            RenameBedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
