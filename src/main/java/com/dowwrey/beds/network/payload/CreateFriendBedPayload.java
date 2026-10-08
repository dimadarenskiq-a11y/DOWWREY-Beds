package com.dowwrey.beds.network.payload;

import com.dowwrey.beds.DowwreyBeds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.util.UUID;

public record CreateFriendBedPayload(BlockPos pos, String bedName, UUID targetPlayer, int color, int icon) implements CustomPacketPayload {
    public static final Identifier ID = DowwreyBeds.id("create_friend_bed");
    public static final Type<CreateFriendBedPayload> TYPE = new Type<>(ID);
    public static final StreamCodec<RegistryFriendlyByteBuf, CreateFriendBedPayload> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CreateFriendBedPayload::pos,
            ByteBufCodecs.stringUtf8(32), CreateFriendBedPayload::bedName,
            UUIDUtil.STREAM_CODEC, CreateFriendBedPayload::targetPlayer,
            ByteBufCodecs.INT, CreateFriendBedPayload::color,
            ByteBufCodecs.VAR_INT, CreateFriendBedPayload::icon,
            CreateFriendBedPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
