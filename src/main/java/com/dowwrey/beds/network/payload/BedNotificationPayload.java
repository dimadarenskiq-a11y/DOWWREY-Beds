package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client-side presentation notification. The server sends only a translation key and safe string arguments. */
public record BedNotificationPayload(String translationKey, String arg1, String arg2, int style) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("bed_notification");
    public static final Type<BedNotificationPayload> TYPE = new Type<>(ID);

    public static final StreamCodec<RegistryFriendlyByteBuf, BedNotificationPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(96), BedNotificationPayload::translationKey,
            ByteBufCodecs.stringUtf8(64), BedNotificationPayload::arg1,
            ByteBufCodecs.stringUtf8(64), BedNotificationPayload::arg2,
            ByteBufCodecs.VAR_INT, BedNotificationPayload::style,
            BedNotificationPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
