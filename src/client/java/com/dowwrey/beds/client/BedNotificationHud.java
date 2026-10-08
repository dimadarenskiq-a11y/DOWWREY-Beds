package com.dowwrey.beds.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Lightweight vanilla-inspired top-right notification stack. It deliberately does not touch ChatHud.
 */
public final class BedNotificationHud {
    private static final int MAX_VISIBLE = 4;
    private static final long SHOW_MILLIS = 3600L;
    private static final long ANIMATION_MILLIS = 220L;
    private static final int HEIGHT = 52;
    private static final int GAP = 6;
    private static final int RIGHT = 12;
    private static final int TOP = 12;

    private static final Deque<Notification> NOTIFICATIONS = new ArrayDeque<>();

    private BedNotificationHud() {}

    public static void push(String translationKey, String arg1, String arg2, int style) {
        if (!DowwreyBedsClient.config().showNotifications()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        String[] args = buildArgs(arg1, arg2);
        Component message = Component.translatable(translationKey, (Object[]) args);
        synchronized (NOTIFICATIONS) {
            NOTIFICATIONS.addLast(new Notification(message, clampStyle(style), System.currentTimeMillis()));
            while (NOTIFICATIONS.size() > MAX_VISIBLE + 2) {
                NOTIFICATIONS.removeFirst();
            }
        }
    }

    private static String[] buildArgs(String arg1, String arg2) {
        if (!arg1.isEmpty() && !arg2.isEmpty()) return new String[]{arg1, arg2};
        if (!arg1.isEmpty()) return new String[]{arg1};
        return new String[0];
    }

    private static int clampStyle(int style) {
        return Mth.clamp(style, 0, 2);
    }

    public static void extract(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!DowwreyBedsClient.config().showNotifications()) {
            synchronized (NOTIFICATIONS) {
                NOTIFICATIONS.clear();
            }
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            synchronized (NOTIFICATIONS) {
                NOTIFICATIONS.clear();
            }
            return;
        }

        long now = System.currentTimeMillis();
        synchronized (NOTIFICATIONS) {
            while (!NOTIFICATIONS.isEmpty() && now - NOTIFICATIONS.peekFirst().createdAt() > SHOW_MILLIS + ANIMATION_MILLIS) {
                NOTIFICATIONS.removeFirst();
            }

            if (NOTIFICATIONS.isEmpty()) {
                return;
            }

            int screenWidth = client.getWindow().getGuiScaledWidth();
            int screenHeight = client.getWindow().getGuiScaledHeight();
            int panelWidth = Mth.clamp(screenWidth / 3, 250, 340);
            int x = screenWidth - RIGHT - panelWidth;
            int y = TOP;

            int index = 0;
            for (Notification notification : NOTIFICATIONS) {
                if (index >= MAX_VISIBLE) break;
                long age = now - notification.createdAt();
                float enter = Mth.clamp(age / (float) ANIMATION_MILLIS, 0.0F, 1.0F);
                long remaining = SHOW_MILLIS + ANIMATION_MILLIS - age;
                float exit = Mth.clamp(remaining / (float) ANIMATION_MILLIS, 0.0F, 1.0F);
                float progress = Math.min(enter, exit);
                float eased = 1.0F - (1.0F - progress) * (1.0F - progress);
                boolean animate = DowwreyBedsClient.config().smoothInterfaceAnimations();
                int offset = animate ? Math.round((1.0F - eased) * (panelWidth + 12)) : 0;
                int drawX = x + offset;
                int alpha = animate ? Mth.clamp(Math.round(255.0F * Mth.clamp(exit, 0.0F, 1.0F)), 0, 255) : 255;

                int background = (alpha << 24) | 0x141414;
                int inner = (Math.min(245, alpha) << 24) | 0x1C1C1C;
                int accentRgb = switch (notification.style()) {
                    case 1 -> 0x7DBE63;
                    case 2 -> 0xC95B55;
                    default -> 0xD0B66A;
                };
                int accent = (alpha << 24) | accentRgb;
                int titleColor = (alpha << 24) | 0xE9E1D0;
                int messageColor = (alpha << 24) | 0xF4F4F4;

                graphics.fill(drawX, y, drawX + panelWidth, y + HEIGHT, background);
                graphics.fill(drawX + 2, y + 2, drawX + panelWidth - 2, y + HEIGHT - 2, inner);
                graphics.fill(drawX, y, drawX + 3, y + HEIGHT, accent);

                String icon = switch (notification.style()) {
                    case 1 -> "✓";
                    case 2 -> "!";
                    default -> "i";
                };
                graphics.text(client.font, Component.literal(icon), drawX + 12, y + 11, accent, true);
                graphics.text(client.font, Component.translatable("dowwrey_beds.notification.title"), drawX + 32, y + 8, titleColor, true);

                String text = notification.message().getString();
                int maxMessageWidth = panelWidth - 48;
                if (client.font.width(text) > maxMessageWidth) {
                    text = client.font.plainSubstrByWidth(text, Math.max(16, maxMessageWidth - 7)) + "…";
                }
                graphics.text(client.font, Component.literal(text), drawX + 32, y + 25, messageColor, false);

                y += HEIGHT + GAP;
                if (y > screenHeight - HEIGHT - 8) break;
                index++;
            }
        }
    }

    private record Notification(Component message, int style, long createdAt) {}
}
