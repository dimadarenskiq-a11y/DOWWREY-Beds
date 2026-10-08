package com.dowwrey.beds.storage;

import com.dowwrey.beds.DowwreyBeds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Persists which saved bed is selected for each player. Kept separate so old saved_beds files remain compatible. */
public final class BedSelectionSavedData extends SavedData {
    private record Entry(UUID owner, UUID bedId) {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        private static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUID_CODEC.fieldOf("owner").forGetter(Entry::owner),
                UUID_CODEC.fieldOf("bed_id").forGetter(Entry::bedId)
        ).apply(instance, Entry::new));
    }

    private static final Codec<BedSelectionSavedData> CODEC = Entry.CODEC.listOf()
            .xmap(BedSelectionSavedData::new, data -> data.entries);

    private static final SavedDataType<BedSelectionSavedData> TYPE = new SavedDataType<>(
            DowwreyBeds.id("bed_selection"),
            BedSelectionSavedData::new,
            CODEC,
            null
    );

    private final List<Entry> entries;

    public BedSelectionSavedData() {
        this.entries = new ArrayList<>();
    }

    private BedSelectionSavedData(List<Entry> entries) {
        this.entries = new ArrayList<>(entries);
    }

    public static BedSelectionSavedData get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) {
            return new BedSelectionSavedData();
        }
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public UUID getSelected(UUID owner) {
        for (Entry entry : entries) {
            if (entry.owner().equals(owner)) {
                return entry.bedId();
            }
        }
        return null;
    }

    public void setSelected(UUID owner, UUID bedId) {
        entries.removeIf(entry -> entry.owner().equals(owner));
        entries.add(new Entry(owner, bedId));
        setDirty();
    }

    public void clearIfSelected(UUID owner, UUID bedId) {
        if (entries.removeIf(entry -> entry.owner().equals(owner) && entry.bedId().equals(bedId))) {
            setDirty();
        }
    }
}
