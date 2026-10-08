package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record BedGiftRequestsPayload(List<RequestEntry> requests) implements CustomPacketPayload {
    public record RequestEntry(UUID id, String senderName, String bedName, int color, int icon, int remainingSeconds) {
        public static final StreamCodec<RegistryFriendlyByteBuf, RequestEntry> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, RequestEntry::id,
                ByteBufCodecs.stringUtf8(32), RequestEntry::senderName,
                ByteBufCodecs.stringUtf8(32), RequestEntry::bedName,
                ByteBufCodecs.INT, RequestEntry::color,
                ByteBufCodecs.VAR_INT, RequestEntry::icon,
                ByteBufCodecs.VAR_INT, RequestEntry::remainingSeconds,
                RequestEntry::new
        );
    }

    public static final Identifier ID = DowwreyBeds.id("bed_gift_requests");
    public static final Type<BedGiftRequestsPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BedGiftRequestsPayload> CODEC =
            RequestEntry.CODEC.apply(ByteBufCodecs.list(32))
                    .map(BedGiftRequestsPayload::new, BedGiftRequestsPayload::requests);

    public BedGiftRequestsPayload {
        requests = List.copyOf(new ArrayList<>(requests));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
