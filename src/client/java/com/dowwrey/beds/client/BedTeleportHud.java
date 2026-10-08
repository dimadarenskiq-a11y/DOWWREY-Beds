package com.dowwrey.beds.client;

import com.dowwrey.beds.network.payload.BedTeleportStatePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Small, restrained teleport overlay and particles. No HUD is shown outside a live teleport transaction. */
public final class BedTeleportHud {
    private static boolean active;
    private static long startedAtNanos;
    private static long durationNanos;
    private static String bedName = "";
    private static int cost;
    private static int lingeringTicks;
    private static boolean cancelledFinish;
    private static int lastDisplayedTenths = -1;
    private static String lastRemainingText = "0.0";

    private BedTeleportHud() {}

    public static void start(BedTeleportStatePayload payload) {
        active = true;
        startedAtNanos = System.nanoTime();
        durationNanos = Math.max(1L, payload.durationTicks()) * 50_000_000L;
        bedName = payload.bedName();
        cost = payload.cost();
        lingeringTicks = 0;
        cancelledFinish = false;
    }

    public static void finish(boolean success) {
        active = false;
        lingeringTicks = success ? 18 : 6;
        cancelledFinish = !success;
        startedAtNanos = System.nanoTime();
    }

    public static void tick() {
        if (lingeringTicks > 0) {
            if (DowwreyBedsClient.config().showTeleportEffects()) {
                Minecraft client = Minecraft.getInstance();
                if (client.level != null && client.player != null) {
                    double spread = lingeringTicks > 10 ? 0.55D : 0.35D;
                    int count = lingeringTicks > 10 ? 4 : 2;
                    for (int i = 0; i < count; i++) {
                        double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0D;
                        double x = client.player.getX() + Math.cos(angle) * spread;
                        double y = client.player.getY() + 0.5D + ThreadLocalRandom.current().nextDouble() * client.player.getBbHeight();
                        double z = client.player.getZ() + Math.sin(angle) * spread;
                        client.level.addParticle(ParticleTypes.PORTAL, x, y, z, 0.0D, 0.02D, 0.0D);
                    }
                }
            }
            lingeringTicks--;
        }

        if (!active || !DowwreyBedsClient.config().showTeleportEffects()) return;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) return;

        for (int i = 0; i < 2; i++) {
            double angle = ThreadLocalRandom.current().nextDouble() * Math.PI * 2.0D;
            double radius = 0.28D + ThreadLocalRandom.current().nextDouble() * 0.25D;
            client.level.addParticle(ParticleTypes.PORTAL,
                    client.player.getX() + Math.cos(angle) * radius,
                    client.player.getY() + 0.15D + ThreadLocalRandom.current().nextDouble() * client.player.getBbHeight(),
                    client.player.getZ() + Math.sin(angle) * radius,
                    -Math.cos(angle) * 0.02D, 0.02D, -Math.sin(angle) * 0.02D);
        }
    }

    public static void extract(GuiGraphicsExtractor graphics, net.minecraft.client.DeltaTracker delta) {
        Minecraft client = Minecraft.getInstance();
        if (client.font == null) return;

        if (active) {
            long elapsed = System.nanoTime() - startedAtNanos;
            double progress = Math.min(1.0D, elapsed / (double) durationNanos);
            double remaining = Math.max(0.0D, (durationNanos - elapsed) / 1_000_000_000.0D);
            int tenths = Math.max(0, (int) Math.ceil(remaining * 10.0D));
            if (tenths != lastDisplayedTenths) {
                lastDisplayedTenths = tenths;
                lastRemainingText = String.format(Locale.ROOT, "%.1f", tenths / 10.0D);
            }

            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();
            int panelWidth = Math.min(360, Math.max(250, width - 32));
            int left = (width - panelWidth) / 2;
            int top = 24;

            if (DowwreyBedsClient.config().showTeleportEffects()) {
                double pulse = 0.5D + 0.5D * Math.sin(elapsed / 180_000_000.0D);
                int shade = (int) (0x20 + pulse * 0x10 + progress * 0x18);
                graphics.fill(0, 0, width, height, (shade << 24) | 0x0A0A0A);
            }

            graphics.fill(left, top, left + panelWidth, top + 50, 0xCC111111);
            graphics.fill(left, top, left + panelWidth, top + 2, 0xFFD0B66A);
            graphics.centeredText(client.font,
                    Component.translatable("dowwrey_beds.teleport.progress", bedName),
                    width / 2, top + 10, 0xFFFFFFFF);
            graphics.centeredText(client.font,
                    Component.translatable("dowwrey_beds.teleport.time",
                            lastRemainingText, cost),
                    width / 2, top + 26, 0xFFC8C8C8);

            int barLeft = left + 16;
            int barRight = left + panelWidth - 16;
            graphics.fill(barLeft, top + 41, barRight, top + 44, 0xFF3A3A3A);
            graphics.fill(barLeft, top + 41,
                    barLeft + (int) ((barRight - barLeft) * progress),
                    top + 44, 0xFFD0B66A);
        }

        if (lingeringTicks > 0 && DowwreyBedsClient.config().showTeleportEffects()) {
            int width = client.getWindow().getGuiScaledWidth();
            int height = client.getWindow().getGuiScaledHeight();
            float fade = Math.min(1.0F, lingeringTicks / (cancelledFinish ? 6.0F : 18.0F));
            int alpha = (int) (fade * 52.0F);
            int tint = cancelledFinish ? 0x260E0E : 0xF0D0B0;
            graphics.fill(0, 0, width, height, (alpha << 24) | tint);
        }
    }
}
