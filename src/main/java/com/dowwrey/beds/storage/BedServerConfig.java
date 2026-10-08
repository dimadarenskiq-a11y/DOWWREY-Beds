package com.dowwrey.beds.storage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.WeakHashMap;

/** Server-authoritative gameplay configuration. Edit the generated JSON and restart the server. */
public final class BedServerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<MinecraftServer, BedServerConfig> CACHE = new WeakHashMap<>();

    public int maxBeds = 10;
    public boolean friendBedsEnabled = true;
    public boolean teleportEnabled = true;
    public boolean brokenBedTeleportEnabled = true;
    public int teleportCooldownSeconds = 25;
    public int slowTeleportSeconds = 6;
    public int slowTeleportCost = 1;
    public int fastTeleportSeconds = 3;
    public int fastTeleportCost = 2;
    public int instantTeleportSeconds = 1;
    public int instantTeleportCost = 3;
    public int brokenTeleportSeconds = 5;
    public int brokenTeleportCost = 3;
    public int friendRequestLifetimeSeconds = 300;
    public int listRequestCooldownTicks = 10;
    public int friendRequestCooldownTicks = 20;
    public int interactionRangeBlocks = 5;

    private BedServerConfig() {}

    public static synchronized BedServerConfig get(MinecraftServer server) {
        BedServerConfig config = CACHE.get(server);
        if (config != null) return config;
        config = load(server);
        CACHE.put(server, config);
        return config;
    }

    private static BedServerConfig load(MinecraftServer server) {
        Path file = server.getFile("config/dowwrey_beds_server.json");
        BedServerConfig config = new BedServerConfig();
        try {
            Files.createDirectories(file.getParent());
            if (Files.isRegularFile(file)) {
                JsonObject root = com.google.gson.JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                apply(root, config);
                config.clamp();
                config.save(file);
                return config;
            }
            config.save(file);
        } catch (Exception error) {
            // Never prevent a server from starting; defaults are safe and deterministic.
            com.dowwrey.beds.DowwreyBeds.LOGGER.warn(
                    "Failed to load DOWWREY Beds server config; using safe defaults", error
            );
            try { config.save(file); } catch (Exception ignoredAgain) {
                com.dowwrey.beds.DowwreyBeds.LOGGER.warn(
                        "Failed to rewrite DOWWREY Beds server config with defaults", ignoredAgain
                );
            }
        }
        return config;
    }

    private static void apply(JsonObject root, BedServerConfig c) {
        c.maxBeds = intValue(root,"maxBeds",c.maxBeds);
        c.friendBedsEnabled = boolValue(root,"friendBedsEnabled",c.friendBedsEnabled);
        c.teleportEnabled = boolValue(root,"teleportEnabled",c.teleportEnabled);
        c.brokenBedTeleportEnabled = boolValue(root,"brokenBedTeleportEnabled",c.brokenBedTeleportEnabled);
        c.teleportCooldownSeconds = intValue(root,"teleportCooldownSeconds",c.teleportCooldownSeconds);
        c.slowTeleportSeconds = intValue(root,"slowTeleportSeconds",c.slowTeleportSeconds);
        c.slowTeleportCost = intValue(root,"slowTeleportCost",c.slowTeleportCost);
        c.fastTeleportSeconds = intValue(root,"fastTeleportSeconds",c.fastTeleportSeconds);
        c.fastTeleportCost = intValue(root,"fastTeleportCost",c.fastTeleportCost);
        c.instantTeleportSeconds = intValue(root,"instantTeleportSeconds",c.instantTeleportSeconds);
        c.instantTeleportCost = intValue(root,"instantTeleportCost",c.instantTeleportCost);
        c.brokenTeleportSeconds = intValue(root,"brokenTeleportSeconds",c.brokenTeleportSeconds);
        c.brokenTeleportCost = intValue(root,"brokenTeleportCost",c.brokenTeleportCost);
        c.friendRequestLifetimeSeconds = intValue(root,"friendRequestLifetimeSeconds",c.friendRequestLifetimeSeconds);
        c.listRequestCooldownTicks = intValue(root,"listRequestCooldownTicks",c.listRequestCooldownTicks);
        c.friendRequestCooldownTicks = intValue(root,"friendRequestCooldownTicks",c.friendRequestCooldownTicks);
        c.interactionRangeBlocks = intValue(root,"interactionRangeBlocks",c.interactionRangeBlocks);
    }

    private void clamp() {
        maxBeds = Math.max(1, Math.min(100, maxBeds));
        teleportCooldownSeconds = Math.max(0, Math.min(3600, teleportCooldownSeconds));
        slowTeleportSeconds = Math.max(1, Math.min(60, slowTeleportSeconds));
        fastTeleportSeconds = Math.max(1, Math.min(60, fastTeleportSeconds));
        instantTeleportSeconds = Math.max(1, Math.min(60, instantTeleportSeconds));
        slowTeleportCost = Math.max(1, Math.min(64, slowTeleportCost));
        fastTeleportCost = Math.max(1, Math.min(64, fastTeleportCost));
        instantTeleportCost = Math.max(1, Math.min(64, instantTeleportCost));
        brokenTeleportSeconds = Math.max(1, Math.min(60, brokenTeleportSeconds));
        brokenTeleportCost = Math.max(1, Math.min(64, brokenTeleportCost));
        friendRequestLifetimeSeconds = Math.max(30, Math.min(86400, friendRequestLifetimeSeconds));
        listRequestCooldownTicks = Math.max(5, Math.min(100, listRequestCooldownTicks));
        friendRequestCooldownTicks = Math.max(1, Math.min(200, friendRequestCooldownTicks));
        interactionRangeBlocks = Math.max(2, Math.min(16, interactionRangeBlocks));
    }

    private void save(Path file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("configVersion", 1);
        root.addProperty("maxBeds", maxBeds);
        root.addProperty("friendBedsEnabled", friendBedsEnabled);
        root.addProperty("teleportEnabled", teleportEnabled);
        root.addProperty("brokenBedTeleportEnabled", brokenBedTeleportEnabled);
        root.addProperty("teleportCooldownSeconds", teleportCooldownSeconds);
        root.addProperty("slowTeleportSeconds", slowTeleportSeconds);
        root.addProperty("slowTeleportCost", slowTeleportCost);
        root.addProperty("fastTeleportSeconds", fastTeleportSeconds);
        root.addProperty("fastTeleportCost", fastTeleportCost);
        root.addProperty("instantTeleportSeconds", instantTeleportSeconds);
        root.addProperty("instantTeleportCost", instantTeleportCost);
        root.addProperty("brokenTeleportSeconds", brokenTeleportSeconds);
        root.addProperty("brokenTeleportCost", brokenTeleportCost);
        root.addProperty("friendRequestLifetimeSeconds", friendRequestLifetimeSeconds);
        root.addProperty("listRequestCooldownTicks", listRequestCooldownTicks);
        root.addProperty("friendRequestCooldownTicks", friendRequestCooldownTicks);
        root.addProperty("interactionRangeBlocks", interactionRangeBlocks);
        Path temp = file.resolveSibling(file.getFileName()+".tmp");
        Files.writeString(temp, GSON.toJson(root));
        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static int intValue(JsonObject root, String key, int fallback) {
        try { return root.has(key) ? root.get(key).getAsInt() : fallback; } catch (Exception e) { return fallback; }
    }
    private static boolean boolValue(JsonObject root, String key, boolean fallback) {
        try { return root.has(key) ? root.get(key).getAsBoolean() : fallback; } catch (Exception e) { return fallback; }
    }
}
