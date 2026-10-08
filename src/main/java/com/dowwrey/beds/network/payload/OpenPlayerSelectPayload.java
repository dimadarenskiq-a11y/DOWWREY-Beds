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

public record OpenPlayerSelectPayload(BlockPos pos, String bedName, int color, int icon, List<PlayerEntry> players) implements CustomPacketPayload {
    public record PlayerEntry(UUID id, String name, int bedCount, int bedLimit, boolean online) {
        public static final StreamCodec<RegistryFriendlyByteBuf, PlayerEntry> CODEC = StreamCodec.composite(
                UUIDUtil.STREAM_CODEC, PlayerEntry::id,
                ByteBufCodecs.stringUtf8(32), PlayerEntry::name,
                ByteBufCodecs.VAR_INT, PlayerEntry::bedCount,
                ByteBufCodecs.VAR_INT, PlayerEntry::bedLimit,
                ByteBufCodecs.BOOL, PlayerEntry::online,
                PlayerEntry::new
        );
    }

    public static final Identifier ID = DowwreyBeds.id("open_player_select");
    public static final Type<OpenPlayerSelectPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenPlayerSelectPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, OpenPlayerSelectPayload::pos,
            ByteBufCodecs.stringUtf8(32), OpenPlayerSelectPayload::bedName,
            ByteBufCodecs.INT, OpenPlayerSelectPayload::color,
            ByteBufCodecs.VAR_INT, OpenPlayerSelectPayload::icon,
            PlayerEntry.CODEC.apply(ByteBufCodecs.list(256)), OpenPlayerSelectPayload::players,
            OpenPlayerSelectPayload::new
    );

    public OpenPlayerSelectPayload {
        players = List.copyOf(new ArrayList<>(players));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
