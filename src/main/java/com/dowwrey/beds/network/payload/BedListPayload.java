package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record BedListPayload(List<BedEntry> beds, boolean openManager) implements CustomPacketPayload {
    public record BedEntry(UUID id, String name, String dimension, BlockPos pos, boolean active, boolean broken, int color, int icon, long createdAtGameTime, long lastUsedGameTime, int status, int distanceBlocks) {
        public static final int STATUS_AVAILABLE = 0;
        public static final int STATUS_OTHER_DIMENSION = 1;
        public static final int STATUS_BROKEN = 2;
        public static final int STATUS_NO_SAFE_SPACE = 3;
        public static final int STATUS_NOT_LOADED = 4;
        public static final StreamCodec<RegistryFriendlyByteBuf, BedEntry> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, BedEntry::id,
                ByteBufCodecs.stringUtf8(32), BedEntry::name,
                ByteBufCodecs.stringUtf8(128), BedEntry::dimension,
                BlockPos.STREAM_CODEC, BedEntry::pos,
                ByteBufCodecs.BOOL, BedEntry::active,
                ByteBufCodecs.BOOL, BedEntry::broken,
                ByteBufCodecs.INT, BedEntry::color,
                ByteBufCodecs.VAR_INT, BedEntry::icon,
                ByteBufCodecs.VAR_LONG, BedEntry::createdAtGameTime,
                ByteBufCodecs.VAR_LONG, BedEntry::lastUsedGameTime,
                ByteBufCodecs.VAR_INT, BedEntry::status,
                ByteBufCodecs.VAR_INT, BedEntry::distanceBlocks,
                BedEntry::new
        );
    }

    public static final Identifier ID = DowwreyBeds.id("bed_list");
    public static final Type<BedListPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, BedListPayload> CODEC = StreamCodec.composite(
            BedEntry.CODEC.apply(ByteBufCodecs.list(100)), BedListPayload::beds,
            ByteBufCodecs.BOOL, BedListPayload::openManager,
            BedListPayload::new
    );

    public BedListPayload {
        beds = List.copyOf(new ArrayList<>(beds));
    }

    public BedListPayload(List<BedEntry> beds) {
        this(beds, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
