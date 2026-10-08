package com.dowwrey.beds.client;

import com.dowwrey.beds.DowwreyBeds;
import com.dowwrey.beds.client.gui.BedGiftRequestsScreen;
import com.dowwrey.beds.client.gui.BedManagerScreen;
import com.dowwrey.beds.client.gui.BedRespawnScreen;
import com.dowwrey.beds.client.gui.BedTeleportOptionsScreen;
import com.dowwrey.beds.client.gui.PlayerSelectScreen;
import com.dowwrey.beds.client.gui.SaveBedScreen;
import com.dowwrey.beds.client.gui.BedSettingsScreen;
import com.dowwrey.beds.client.gui.BedOnboardingScreen;
import com.dowwrey.beds.network.payload.BedGiftRequestsPayload;
import com.dowwrey.beds.network.payload.BedListPayload;
import com.dowwrey.beds.network.payload.BedNotificationPayload;
import com.dowwrey.beds.network.payload.CreateBedPayload;
import com.dowwrey.beds.network.payload.CreateFriendBedPayload;
import com.dowwrey.beds.network.payload.DeleteBedPayload;
import com.dowwrey.beds.network.payload.OpenPlayerSelectPayload;
import com.dowwrey.beds.network.payload.OpenSaveBedPayload;
import com.dowwrey.beds.network.payload.RenameBedPayload;
import com.dowwrey.beds.network.payload.RequestBedGiftPayload;
import com.dowwrey.beds.network.payload.RequestBedListPayload;
import com.dowwrey.beds.network.payload.RespondBedGiftPayload;
import com.dowwrey.beds.network.payload.RespawnAtBedPayload;
import com.dowwrey.beds.network.payload.RespawnAtBedApprovedPayload;
import com.dowwrey.beds.network.payload.BedTeleportStatePayload;
import com.dowwrey.beds.network.payload.BedTeleportCooldownPayload;
import com.dowwrey.beds.network.payload.BedServerRulesPayload;
import com.dowwrey.beds.network.payload.StartBedTeleportPayload;
import com.dowwrey.beds.network.payload.SetActiveBedPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public final class DowwreyBedsClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            DowwreyBeds.id("controls")
    );

    private static KeyMapping openManagerKey;
    private static List<BedGiftRequestsPayload.RequestEntry> pendingGiftRequests = List.of();
    private static List<BedListPayload.BedEntry> latestBeds = List.of();
    private static boolean openGiftRequestsWhenPossible;
    private static Button deathRespawnButton;
    private static long teleportCooldownUntilNanos;
    private static BedClientConfig config;
    private static int serverMaxBeds = 10;
    private static boolean serverFriendBedsEnabled = true;
    private static boolean serverTeleportEnabled = true;
    private static boolean serverBrokenTeleportEnabled = true;
    private static int serverTeleportCooldownSeconds = 25;
    private static int serverSlowTeleportSeconds = 6;
    private static int serverSlowTeleportCost = 1;
    private static int serverFastTeleportSeconds = 3;
    private static int serverFastTeleportCost = 2;
    private static int serverInstantTeleportSeconds = 1;
    private static int serverInstantTeleportCost = 3;
    private static int serverBrokenTeleportSeconds = 5;
    private static int serverBrokenTeleportCost = 3;

    @Override
    public void onInitializeClient() {
        config = BedClientConfig.load();

        openManagerKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.dowwrey_beds.open_manager",
                InputConstants.KEY_F4,
                CATEGORY
        ));
        migrateLegacyDefaultKeybind(client());

        HudElementRegistry.addLast(DowwreyBeds.id("notifications"), BedNotificationHud::extract);
        HudElementRegistry.addLast(DowwreyBeds.id("teleport"), BedTeleportHud::extract);

        ClientPlayNetworking.registerGlobalReceiver(BedTeleportStatePayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    if (payload.state() == BedTeleportStatePayload.STARTED) {
                        BedTeleportHud.start(payload);
                    } else {
                        BedTeleportHud.finish(payload.state() == BedTeleportStatePayload.COMPLETED);
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(BedTeleportCooldownPayload.TYPE, (payload, context) ->
                context.client().execute(() -> setTeleportCooldownTicks(payload.remainingTicks()))
        );

        ClientPlayNetworking.registerGlobalReceiver(BedServerRulesPayload.TYPE, (payload, context) -> context.client().execute(() -> {
            serverMaxBeds = Math.max(1, payload.maxBeds());
            serverFriendBedsEnabled = payload.friendBedsEnabled();
            serverTeleportEnabled = payload.teleportEnabled();
            serverBrokenTeleportEnabled = payload.brokenBedTeleportEnabled();
            serverTeleportCooldownSeconds = Math.max(0, payload.teleportCooldownSeconds());
            serverSlowTeleportSeconds = Math.max(1, payload.slowTeleportSeconds());
            serverSlowTeleportCost = Math.max(1, payload.slowTeleportCost());
            serverFastTeleportSeconds = Math.max(1, payload.fastTeleportSeconds());
            serverFastTeleportCost = Math.max(1, payload.fastTeleportCost());
            serverInstantTeleportSeconds = Math.max(1, payload.instantTeleportSeconds());
            serverInstantTeleportCost = Math.max(1, payload.instantTeleportCost());
            serverBrokenTeleportSeconds = Math.max(1, payload.brokenTeleportSeconds());
            serverBrokenTeleportCost = Math.max(1, payload.brokenTeleportCost());
            if (context.client().gui.screen() instanceof BedManagerScreen manager) {
                manager.setServerRules(
                        serverMaxBeds,
                        serverTeleportEnabled,
                        serverBrokenTeleportEnabled,
                        serverTeleportCooldownSeconds
                );
            }
        }));

        ClientPlayNetworking.registerGlobalReceiver(BedNotificationPayload.TYPE, (payload, context) ->
                context.client().execute(() -> BedNotificationHud.push(
                        payload.translationKey(), payload.arg1(), payload.arg2(), payload.style()
                ))
        );

        ClientPlayNetworking.registerGlobalReceiver(RespawnAtBedApprovedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    BedNotificationHud.push(
                            "dowwrey_beds.respawn.success",
                            payload.bedName(),
                            "",
                            1
                    );
                    // Use the exact order of the vanilla DeathScreen: send the respawn request
                    // first, then remove the current screen. This lets the vanilla connection
                    // state transition complete normally.
                    if (context.client().player != null && context.client().player.isDeadOrDying()) {
                        context.client().player.respawn();
                    }
                    context.client().gui.setScreen(null);
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(OpenSaveBedPayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().gui.setScreen(new SaveBedScreen(payload.pos())))
        );

        ClientPlayNetworking.registerGlobalReceiver(OpenPlayerSelectPayload.TYPE, (payload, context) ->
                context.client().execute(() -> context.client().gui.setScreen(
                        new PlayerSelectScreen(payload.pos(), payload.bedName(), payload.color(), payload.icon(), payload.players())
                ))
        );

        ClientPlayNetworking.registerGlobalReceiver(BedListPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    latestBeds = List.copyOf(payload.beds());
                    updateDeathButton();

                    if (context.client().gui.screen() instanceof BedManagerScreen manager) {
                        manager.setBeds(payload.beds());
                        manager.refreshLayout();
                    } else if (context.client().gui.screen() instanceof BedRespawnScreen respawnScreen) {
                        respawnScreen.setBeds(payload.beds());
                        respawnScreen.refreshLayout();
                    } else if (payload.openManager()) {
                        context.client().gui.setScreen(new BedManagerScreen(payload.beds(), serverMaxBeds, serverTeleportEnabled, serverBrokenTeleportEnabled, serverTeleportCooldownSeconds));
                    }
                })
        );

        ClientPlayNetworking.registerGlobalReceiver(BedGiftRequestsPayload.TYPE, (payload, context) ->
                context.client().execute(() -> {
                    int previousCount = pendingGiftRequests.size();
                    pendingGiftRequests = List.copyOf(payload.requests());
                    if (pendingGiftRequests.size() > previousCount && !pendingGiftRequests.isEmpty()) {
                        BedGiftRequestsPayload.RequestEntry newest = pendingGiftRequests.getFirst();
                        BedNotificationHud.push(
                                "dowwrey_beds.friend.request.new",
                                newest.senderName(),
                                "",
                                0
                        );
                    }
                    if (context.client().gui.screen() instanceof BedGiftRequestsScreen requestsScreen) {
                        requestsScreen.setRequests(pendingGiftRequests);
                        requestsScreen.refreshLayout();
                    } else if (context.client().gui.screen() instanceof BedManagerScreen manager) {
                        manager.refreshLayout();
                    } else if (!pendingGiftRequests.isEmpty()) {
                        openGiftRequestsWhenPossible = true;
                    }
                })
        );

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof DeathScreen) || client.level == null || client.level.getLevelData().isHardcore()) {
                return;
            }

            int buttonWidth = Math.min(220, Math.max(180, scaledWidth - 32));
            int buttonY = Math.min(scaledHeight - 50, scaledHeight / 2 + 54);
            Button button = Button.builder(
                    Component.translatable("dowwrey_beds.respawn.button"),
                    ignored -> openRespawnScreen()
            ).bounds((scaledWidth - buttonWidth) / 2, buttonY, buttonWidth, 20).build();
            button.active = hasAvailableBeds();
            Screens.getWidgets(screen).add(button);
            deathRespawnButton = button;
            requestBedList(false);

            ScreenEvents.remove(screen).register(removed -> {
                if (deathRespawnButton == button) {
                    deathRespawnButton = null;
                }
            });
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            client.execute(() -> {
                if (!config.hasSeenOnboarding() && client.gui.screen() == null) {
                    client.gui.setScreen(new BedOnboardingScreen(null));
                }
            });
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            boolean cooldownBefore = isTeleportCooldownActive();
            BedTeleportHud.tick();
            while (openManagerKey.consumeClick()) {
                if (client.player == null) {
                    continue;
                }

                Screen current = client.gui.screen();
                if (isDowwreyScreen(current)) {
                    // The configured DOWWREY key is a true toggle: while any DOWWREY
                    // screen is open, pressing it closes the whole menu stack.
                    client.gui.setScreen(null);
                    continue;
                }

                // Do not hijack vanilla screens such as inventory, chat, pause, etc.
                if (current != null) {
                    continue;
                }

                if (!pendingGiftRequests.isEmpty()) {
                    client.gui.setScreen(new BedGiftRequestsScreen(pendingGiftRequests));
                } else {
                    requestBedList(true);
                }
            }

            if (cooldownBefore && !isTeleportCooldownActive() && client.gui.screen() instanceof BedManagerScreen manager) {
                manager.refreshLayout();
            }

            if (openGiftRequestsWhenPossible && !pendingGiftRequests.isEmpty() && client.gui.screen() == null) {
                openGiftRequestsWhenPossible = false;
                client.gui.setScreen(new BedGiftRequestsScreen(pendingGiftRequests));
            }
        });
    }

    private static net.minecraft.client.Minecraft client() {
        return net.minecraft.client.Minecraft.getInstance();
    }

    private static void migrateLegacyDefaultKeybind(net.minecraft.client.Minecraft client) {
        if (config == null || config.keybindMigrationVersion() >= 1 || openManagerKey == null) {
            return;
        }

        InputConstants.Key legacyDefault = InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_B);
        InputConstants.Key configuredKey = KeyMappingHelper.getBoundKeyOf(openManagerKey);
        if (configuredKey.equals(legacyDefault)) {
            openManagerKey.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_F4));
            client.options.save();
        }
        config.markKeybindMigrationVersion(1);
    }

    public static boolean matchesOpenManagerKey(net.minecraft.client.input.KeyEvent event) {
        return openManagerKey != null && openManagerKey.matches(event);
    }

    public static int serverMaxBeds() { return serverMaxBeds; }
    public static boolean serverFriendBedsEnabled() { return serverFriendBedsEnabled; }
    public static boolean serverTeleportEnabled() { return serverTeleportEnabled; }
    public static boolean serverBrokenTeleportEnabled() { return serverBrokenTeleportEnabled; }
    public static int serverTeleportCooldownSeconds() { return serverTeleportCooldownSeconds; }
    public static int serverSlowTeleportSeconds() { return serverSlowTeleportSeconds; }
    public static int serverSlowTeleportCost() { return serverSlowTeleportCost; }
    public static int serverFastTeleportSeconds() { return serverFastTeleportSeconds; }
    public static int serverFastTeleportCost() { return serverFastTeleportCost; }
    public static int serverInstantTeleportSeconds() { return serverInstantTeleportSeconds; }
    public static int serverInstantTeleportCost() { return serverInstantTeleportCost; }
    public static int serverBrokenTeleportSeconds() { return serverBrokenTeleportSeconds; }
    public static int serverBrokenTeleportCost() { return serverBrokenTeleportCost; }

    public static BedClientConfig config() {
        if (config == null) {
            config = BedClientConfig.createDefault();
        }
        return config;
    }

    public static void openSettings() {
        var client = net.minecraft.client.Minecraft.getInstance();
        client.gui.setScreen(new BedSettingsScreen(client.gui.screen()));
    }

    public static void saveBed(net.minecraft.core.BlockPos pos, String name, boolean forFriend, int color, int icon) {
        ClientPlayNetworking.send(new CreateBedPayload(pos, name, forFriend, color, icon));
    }


    public static void clientNotification(String translationKey, int style, String arg1, String arg2) {
        BedNotificationHud.push(translationKey, arg1, arg2, style);
    }

    public static void requestBedList() {
        requestBedList(false);
    }

    public static void requestBedList(boolean openManager) {
        ClientPlayNetworking.send(new RequestBedListPayload(openManager));
    }

    public static void openRespawnScreen() {
        var client = net.minecraft.client.Minecraft.getInstance();
        Screen parent = client.gui.screen();
        List<BedListPayload.BedEntry> available = latestBeds.stream()
                .filter(bed -> !bed.broken())
                .toList();
        client.gui.setScreen(new BedRespawnScreen(parent, available));
        requestBedList(false);
    }

    public static void respawnAtBed(UUID bedId) {
        ClientPlayNetworking.send(new RespawnAtBedPayload(bedId));
    }

    private static boolean hasAvailableBeds() {
        return latestBeds.stream().anyMatch(bed -> !bed.broken());
    }

    private static void updateDeathButton() {
        if (deathRespawnButton != null) {
            deathRespawnButton.active = hasAvailableBeds();
        }
    }

    public static void setActiveBed(UUID bedId) {
        ClientPlayNetworking.send(new SetActiveBedPayload(bedId));
    }

    public static void renameBed(UUID bedId, String name, int color, int icon) {
        ClientPlayNetworking.send(new RenameBedPayload(bedId, name, color, icon));
    }

    public static void deleteBed(UUID bedId) {
        ClientPlayNetworking.send(new DeleteBedPayload(bedId));
    }

    public static void createFriendBed(net.minecraft.core.BlockPos pos, String name, UUID targetPlayer, int color, int icon) {
        ClientPlayNetworking.send(new CreateFriendBedPayload(pos, name, targetPlayer, color, icon));
    }

    public static void respondToBedGift(UUID requestId, boolean accepted) {
        ClientPlayNetworking.send(new RespondBedGiftPayload(requestId, accepted));
    }

    public static void teleportAtBed(BedListPayload.BedEntry bed) {
        var client = net.minecraft.client.Minecraft.getInstance();
        if (bed.broken()) {
            startBedTeleport(bed.id(), 3);
            return;
        }
        Screen parent = client.gui.screen();
        client.gui.setScreen(new BedTeleportOptionsScreen(parent, bed));
    }

    public static void startBedTeleport(UUID bedId, int mode) {
        ClientPlayNetworking.send(new StartBedTeleportPayload(bedId, mode));
    }

    public static void setTeleportCooldownTicks(int remainingTicks) {
        if (remainingTicks <= 0) {
            teleportCooldownUntilNanos = 0L;
        } else {
            teleportCooldownUntilNanos = System.nanoTime() + remainingTicks * 50_000_000L;
        }

        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.gui.screen() instanceof BedManagerScreen manager) {
            manager.refreshLayout();
        }
    }

    public static boolean isTeleportCooldownActive() {
        return teleportCooldownUntilNanos > System.nanoTime();
    }

    public static int teleportCooldownSeconds() {
        if (!isTeleportCooldownActive()) return 0;
        long remaining = teleportCooldownUntilNanos - System.nanoTime();
        return Math.max(1, (int) Math.ceil(remaining / 1_000_000_000.0D));
    }

    public static void clearOpenManagerKeyClicks() {
        while (openManagerKey != null && openManagerKey.consumeClick()) {
            // Drain any queued toggle press so the same physical key event cannot reopen the menu.
        }
    }

    public static boolean isDowwreyScreen(Screen screen) {
        return screen instanceof BedManagerScreen
                || screen instanceof BedRespawnScreen
                || screen instanceof BedSettingsScreen
                || screen instanceof BedTeleportOptionsScreen
                || screen instanceof BedGiftRequestsScreen
                || screen instanceof PlayerSelectScreen
                || screen instanceof SaveBedScreen
                || screen instanceof com.dowwrey.beds.client.gui.EditBedScreen;
    }

    public int pendingGiftRequestCount() {
        return pendingGiftRequests.size();
    }

    public static int enderEyeCount() {
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null) return 0;
        int total = 0;
        for (int slot = 0; slot < client.player.getInventory().getContainerSize(); slot++) {
            var stack = client.player.getInventory().getItem(slot);
            if (stack.is(net.minecraft.world.item.Items.ENDER_EYE)) total += stack.getCount();
        }
        return total;
    }
}

