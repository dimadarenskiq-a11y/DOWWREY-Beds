package com.dowwrey.beds.storage;

import com.dowwrey.beds.DowwreyBeds;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import com.mojang.datafixers.util.Either;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class BedSavedData extends SavedData {
    public static final int CURRENT_SCHEMA_VERSION = 3;
    private static final Gson BACKUP_GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final long BACKUP_INTERVAL_TICKS = 600L; // 30 seconds.

    public record SavedBed(
            UUID owner,
            String name,
            String dimension,
            int x,
            int y,
            int z,
            int color,
            int icon,
            long createdAtGameTime,
            long lastUsedGameTime
    ) {
        private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
        public static final Codec<SavedBed> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                UUID_CODEC.fieldOf("owner").forGetter(SavedBed::owner),
                Codec.STRING.fieldOf("name").forGetter(SavedBed::name),
                Codec.STRING.fieldOf("dimension").forGetter(SavedBed::dimension),
                Codec.INT.fieldOf("x").forGetter(SavedBed::x),
                Codec.INT.fieldOf("y").forGetter(SavedBed::y),
                Codec.INT.fieldOf("z").forGetter(SavedBed::z),
                Codec.INT.optionalFieldOf("color", BedColors.DEFAULT).forGetter(SavedBed::color),
                Codec.INT.optionalFieldOf("icon", BedIcons.DEFAULT).forGetter(SavedBed::icon),
                Codec.LONG.optionalFieldOf("created_at_game_time", 0L).forGetter(SavedBed::createdAtGameTime),
                Codec.LONG.optionalFieldOf("last_used_game_time", 0L).forGetter(SavedBed::lastUsedGameTime)
        ).apply(instance, SavedBed::new));

        public BlockPos pos() {
            return new BlockPos(x, y, z);
        }

        /**
         * Stable ID derived from the owner + location. The name is deliberately
         * excluded so renaming a saved bed does not change its identity.
         */
        public UUID id() {
            String key = owner + "|" + dimension + "|" + x + "," + y + "," + z;
            return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
        }
    }

    private record StoredData(int schemaVersion, List<SavedBed> beds) {
        private static final Codec<StoredData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("schema_version", CURRENT_SCHEMA_VERSION).forGetter(StoredData::schemaVersion),
                SavedBed.CODEC.listOf().fieldOf("beds").forGetter(StoredData::beds)
        ).apply(instance, StoredData::new));
    }

    private static final Codec<BedSavedData> CODEC = Codec.either(
            StoredData.CODEC,
            SavedBed.CODEC.listOf()
    ).xmap(
            either -> either.map(stored -> new BedSavedData(stored.beds), BedSavedData::new),
            data -> Either.left(new StoredData(CURRENT_SCHEMA_VERSION, data.beds))
    );

    private static final SavedDataType<BedSavedData> TYPE = new SavedDataType<>(
            DowwreyBeds.id("saved_beds"),
            BedSavedData::new,
            CODEC,
            null
    );

    private final List<SavedBed> beds;
    private boolean backupPending = true;
    private long lastBackupGameTime = Long.MIN_VALUE;

    public BedSavedData() {
        this.beds = new ArrayList<>();
    }

    private BedSavedData(List<SavedBed> beds) {
        this.beds = new ArrayList<>(beds);
    }

    public static BedSavedData get(MinecraftServer server) {
        ServerLevel level = server.getLevel(ServerLevel.OVERWORLD);
        if (level == null) {
            return new BedSavedData();
        }
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public boolean canAdd(UUID owner, int maxBeds) {
        return count(owner) < Math.max(1, maxBeds);
    }

    public int count(UUID owner) {
        int count = 0;
        for (SavedBed bed : beds) {
            if (bed.owner().equals(owner)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Counts beds for several owners in one pass. This avoids O(players * beds)
     * scans when opening the Friend Bed player selector on busy servers.
     */
    public Map<UUID, Integer> countsFor(Collection<UUID> owners) {
        Map<UUID, Integer> counts = new HashMap<>();
        for (UUID owner : owners) {
            counts.put(owner, 0);
        }
        if (counts.isEmpty()) {
            return counts;
        }
        for (SavedBed bed : beds) {
            UUID owner = bed.owner();
            if (counts.containsKey(owner)) {
                counts.computeIfPresent(owner, (key, value) -> value + 1);
            }
        }
        return counts;
    }

    public boolean contains(UUID owner, ServerLevel level, BlockPos pos) {
        String dimension = level.dimension().identifier().toString();
        return beds.stream().anyMatch(bed -> bed.owner().equals(owner)
                && bed.dimension().equals(dimension)
                && bed.pos().equals(pos));
    }


    public boolean hasName(UUID owner, String name) {
        String normalized = normalizeName(name);
        for (SavedBed bed : beds) {
            if (bed.owner().equals(owner) && normalizeName(bed.name()).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNameExcept(UUID owner, UUID excludedId, String name) {
        String normalized = normalizeName(name);
        for (SavedBed bed : beds) {
            if (bed.owner().equals(owner)
                    && bed.id().equals(excludedId) == false
                    && normalizeName(bed.name()).equals(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    public SavedBed findById(UUID owner, UUID id) {
        for (SavedBed bed : beds) {
            if (bed.owner().equals(owner) && bed.id().equals(id)) {
                return bed;
            }
        }
        return null;
    }

    public List<SavedBed> list(UUID owner) {
        List<SavedBed> result = new ArrayList<>();
        for (SavedBed bed : beds) {
            if (bed.owner().equals(owner)) {
                result.add(bed);
            }
        }
        return List.copyOf(result);
    }

    public void add(UUID owner, String name, ServerLevel level, BlockPos pos) {
        add(owner, name, level, pos, BedColors.DEFAULT, BedIcons.DEFAULT, level.getGameTime());
    }

    public void add(UUID owner, String name, ServerLevel level, BlockPos pos, int color) {
        add(owner, name, level, pos, color, BedIcons.DEFAULT, level.getGameTime());
    }

    public void add(UUID owner, String name, ServerLevel level, BlockPos pos, int color, int icon, long createdAtGameTime) {
        beds.add(new SavedBed(
                owner, name, level.dimension().identifier().toString(),
                pos.getX(), pos.getY(), pos.getZ(),
                BedColors.isAllowed(color) ? color : BedColors.DEFAULT,
                BedIcons.sanitize(icon), Math.max(0L, createdAtGameTime), 0L
        ));
        setDirty();
        backupPending = true;
    }

    public boolean rename(UUID owner, UUID id, String newName, int color, int icon) {
        for (int i = 0; i < beds.size(); i++) {
            SavedBed bed = beds.get(i);
            if (bed.owner().equals(owner) && bed.id().equals(id)) {
                beds.set(i, new SavedBed(bed.owner(), newName, bed.dimension(), bed.x(), bed.y(), bed.z(),
                        BedColors.isAllowed(color) ? color : bed.color(),
                        BedIcons.sanitize(icon), bed.createdAtGameTime(), bed.lastUsedGameTime()));
                setDirty();
                backupPending = true;
                return true;
            }
        }
        return false;
    }

    public void backup(MinecraftServer server) {
        Path target = server.getWorldPath(LevelResource.ROOT).resolve("dowwrey-beds-backup.json");
        try {
            Path temp = target.resolveSibling(target.getFileName() + ".tmp");
            Files.writeString(temp, BACKUP_GSON.toJson(beds));
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            backupPending = false;
            lastBackupGameTime = server.overworld().getGameTime();
        } catch (IOException e) {
            DowwreyBeds.LOGGER.warn("Failed to update DOWWREY Beds backup", e);
        }
    }

    public void backupIfDue(MinecraftServer server, long currentGameTime) {
        if (!backupPending) return;
        if (lastBackupGameTime != Long.MIN_VALUE && currentGameTime - lastBackupGameTime < BACKUP_INTERVAL_TICKS) return;
        backup(server);
    }

    public void forceBackup(MinecraftServer server) {
        backup(server);
    }

    public boolean markLastUsed(UUID owner, UUID id, long gameTime) {
        for (int i = 0; i < beds.size(); i++) {
            SavedBed bed = beds.get(i);
            if (bed.owner().equals(owner) && bed.id().equals(id)) {
                beds.set(i, new SavedBed(bed.owner(), bed.name(), bed.dimension(), bed.x(), bed.y(), bed.z(),
                        bed.color(), bed.icon(), bed.createdAtGameTime(), Math.max(0L, gameTime)));
                setDirty();
                backupPending = true;
                return true;
            }
        }
        return false;
    }

    public SavedBed remove(UUID owner, UUID id) {
        for (int i = 0; i < beds.size(); i++) {
            SavedBed bed = beds.get(i);
            if (bed.owner().equals(owner) && bed.id().equals(id)) {
                beds.remove(i);
                setDirty();
                backupPending = true;
                return bed;
            }
        }
        return null;
    }
}
