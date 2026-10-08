package com.dowwrey.beds.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Client-only preferences. Gameplay rules remain server-authoritative. */
public final class BedClientConfig {
    private static final int CURRENT_VERSION = 3;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean showNotifications = true;
    private boolean showTeleportEffects = true;
    private boolean showCooldownReport = true;
    private boolean smoothInterfaceAnimations = true;
    private int keybindMigrationVersion = 0;
    private boolean hasSeenOnboarding = false;

    private BedClientConfig() {}

    public static BedClientConfig createDefault() {
        return new BedClientConfig();
    }

    public boolean showNotifications() {
        return showNotifications;
    }

    public boolean showTeleportEffects() {
        return showTeleportEffects;
    }

    public boolean showCooldownReport() {
        return showCooldownReport;
    }

    public boolean smoothInterfaceAnimations() {
        return smoothInterfaceAnimations;
    }

    public int keybindMigrationVersion() {
        return keybindMigrationVersion;
    }

    public boolean hasSeenOnboarding() { return hasSeenOnboarding; }

    public void markOnboardingSeen() {
        hasSeenOnboarding = true;
        save();
    }

    public void markKeybindMigrationVersion(int version) {
        keybindMigrationVersion = Math.max(keybindMigrationVersion, version);
        save();
    }

    public void setShowNotifications(boolean value) {
        showNotifications = value;
        save();
    }

    public void setShowTeleportEffects(boolean value) {
        showTeleportEffects = value;
        save();
    }

    public void setShowCooldownReport(boolean value) {
        showCooldownReport = value;
        save();
    }

    public void setSmoothInterfaceAnimations(boolean value) {
        smoothInterfaceAnimations = value;
        save();
    }

    public void resetToDefaults() {
        showNotifications = true;
        showTeleportEffects = true;
        showCooldownReport = true;
        smoothInterfaceAnimations = true;
        save();
    }

    private Path configPath() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config")
                .resolve("dowwrey_beds.json");
    }

    public void save() {
        Path path = configPath();
        try {
            Files.createDirectories(path.getParent());
            JsonObject root = new JsonObject();
            root.addProperty("configVersion", CURRENT_VERSION);
            root.addProperty("showNotifications", showNotifications);
            root.addProperty("showTeleportEffects", showTeleportEffects);
            root.addProperty("showCooldownReport", showCooldownReport);
            root.addProperty("smoothInterfaceAnimations", smoothInterfaceAnimations);
            root.addProperty("keybindMigrationVersion", keybindMigrationVersion);
            root.addProperty("hasSeenOnboarding", hasSeenOnboarding);

            Path temp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(root));
            try {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveUnsupported) {
                Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ignored) {
            // Client preferences must never prevent Minecraft from starting.
        }
    }

    public static BedClientConfig load() {
        BedClientConfig config = createDefault();
        Path path = config.configPath();
        if (!Files.isRegularFile(path)) {
            config.save();
            return config;
        }

        try {
            JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            if (root.has("showNotifications")) {
                config.showNotifications = root.get("showNotifications").getAsBoolean();
            }
            if (root.has("showTeleportEffects")) {
                config.showTeleportEffects = root.get("showTeleportEffects").getAsBoolean();
            }
            if (root.has("showCooldownReport")) {
                config.showCooldownReport = root.get("showCooldownReport").getAsBoolean();
            }
            if (root.has("smoothInterfaceAnimations")) {
                config.smoothInterfaceAnimations = root.get("smoothInterfaceAnimations").getAsBoolean();
            }
            if (root.has("keybindMigrationVersion")) {
                config.keybindMigrationVersion = root.get("keybindMigrationVersion").getAsInt();
            }
            if (root.has("hasSeenOnboarding")) {
                config.hasSeenOnboarding = root.get("hasSeenOnboarding").getAsBoolean();
            }
        } catch (Exception ignored) {
            config.resetToDefaults();
        }
        return config;
    }
}
