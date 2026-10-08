package com.dowwrey.beds.storage;

import com.dowwrey.beds.DowwreyBeds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persistent requests for beds offered by one player to another. */
public final class BedGiftRequestSavedData extends SavedData {
    public static final int MAX_PENDING_REQUESTS_PER_PLAYER = 32;
    public static final long REQUEST_LIFETIME_TICKS = 5L * 60L * 20L;

    public record Request(
            UUID id,
            UUID sender,
            UUID recipient,
            String senderName,
            String bedName,
            String dimension,
            int x,
            int y,
            int z,
            long expiresAtGameTime,
            int color,
            int icon
    ) {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        public static final Codec<Request> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUID_CODEC.fieldOf("id").forGetter(Request::id),
                UUID_CODEC.fieldOf("sender").forGetter(Request::sender),
                UUID_CODEC.fieldOf("recipient").forGetter(Request::recipient),
                Codec.STRING.fieldOf("sender_name").forGetter(Request::senderName),
                Codec.STRING.fieldOf("bed_name").forGetter(Request::bedName),
                Codec.STRING.fieldOf("dimension").forGetter(Request::dimension),
                Codec.INT.fieldOf("x").forGetter(Request::x),
                Codec.INT.fieldOf("y").forGetter(Request::y),
                Codec.INT.fieldOf("z").forGetter(Request::z),
                Codec.LONG.optionalFieldOf("expires_at", 0L).forGetter(Request::expiresAtGameTime),
                Codec.INT.optionalFieldOf("color", BedColors.DEFAULT).forGetter(Request::color),
                Codec.INT.optionalFieldOf("icon", BedIcons.DEFAULT).forGetter(Request::icon)
        ).apply(instance, Request::new));

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }
    }

    private static final Codec<BedGiftRequestSavedData> CODEC = Request.CODEC.listOf()
            .xmap(BedGiftRequestSavedData::new, data -> data.requests);

    private static final SavedDataType<BedGiftRequestSavedData> TYPE = new SavedDataType<>(
            DowwreyBeds.id("bed_gift_requests"),
            BedGiftRequestSavedData::new,
            CODEC,
            null
    );

    private final List<Request> requests;

    public BedGiftRequestSavedData() {
        this.requests = new ArrayList<>();
    }

    private BedGiftRequestSavedData(List<Request> requests) {
        this.requests = new ArrayList<>(requests);
    }

    public static BedGiftRequestSavedData get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) {
            return new BedGiftRequestSavedData();
        }
        return level.getDataStorage().computeIfAbsent(TYPE);
    }


    public List<Request> cleanupExpired(long currentGameTime) {
        List<Request> expired = new ArrayList<>();
        for (int i = requests.size() - 1; i >= 0; i--) {
            Request request = requests.get(i);
            if (request.expiresAtGameTime() > 0L && currentGameTime >= request.expiresAtGameTime()) {
                expired.add(request);
                requests.remove(i);
            }
        }
        if (!expired.isEmpty()) {
            setDirty();
        }
        return List.copyOf(expired);
    }

    public List<Request> listFor(UUID recipient) {
        List<Request> result = new ArrayList<>();
        for (Request request : requests) {
            if (request.recipient().equals(recipient)) {
                result.add(request);
            }
        }
        return List.copyOf(result);
    }

    public Request findFor(UUID recipient, UUID requestId) {
        for (Request request : requests) {
            if (request.recipient().equals(recipient) && request.id().equals(requestId)) {
                return request;
            }
        }
        return null;
    }

    public boolean hasDuplicate(UUID sender, UUID recipient, String bedName, String dimension, BlockPos pos) {
        String normalizedName = BedSavedData.normalizeName(bedName);
        for (Request request : requests) {
            if (request.sender().equals(sender)
                    && request.recipient().equals(recipient)
                    && BedSavedData.normalizeName(request.bedName()).equals(normalizedName)
                    && request.dimension().equals(dimension)
                    && request.pos().equals(pos)) {
                return true;
            }
        }
        return false;
    }

    public boolean canAddFor(UUID recipient) {
        int count = 0;
        for (Request request : requests) {
            if (request.recipient().equals(recipient) && ++count >= MAX_PENDING_REQUESTS_PER_PLAYER) {
                return false;
            }
        }
        return true;
    }

    public void add(Request request) {
        requests.add(request);
        setDirty();
    }

    public Request removeFor(UUID recipient, UUID requestId) {
        for (int i = 0; i < requests.size(); i++) {
            Request request = requests.get(i);
            if (request.recipient().equals(recipient) && request.id().equals(requestId)) {
                requests.remove(i);
                setDirty();
                return request;
            }
        }
        return null;
    }
}
