package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.BedClientConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Vanilla-styled client settings for DOWWREY Beds. */
public final class BedSettingsScreen extends Screen {
    private static final int MAX_WIDTH = 560;
    private static final int MIN_WIDTH = 300;
    private static final int TOP = 40;
    private static final int ROW_TOP = 92;
    private static final int ROW_HEIGHT = 52;
    private static final int GAP = 7;

    private final Screen parent;
    private final BedClientConfig config;

    public BedSettingsScreen(Screen parent) {
        super(Component.translatable("dowwrey_beds.settings.title"));
        this.parent = parent;
        this.config = BedClientConfigHolder.config();
    }

    private boolean compactLayout() { return height < 300; }

    private int panelWidth() {
        return Math.min(MAX_WIDTH, Math.max(MIN_WIDTH, width - 28));
    }

    @Override
    protected void init() {
        clearWidgets();
        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int toggleWidth = Math.min(124, Math.max(92, panelWidth / 4));
        int toggleX = left + panelWidth - toggleWidth - 18;
        int y = compactLayout() ? 72 : ROW_TOP;

        addOptionButton(toggleX, y, toggleWidth,
                "dowwrey_beds.settings.notifications",
                config.showNotifications(),
                () -> config.setShowNotifications(!config.showNotifications()));
        y += compactLayout() ? 33 : ROW_HEIGHT + GAP;

        addOptionButton(toggleX, y, toggleWidth,
                "dowwrey_beds.settings.teleport_effects",
                config.showTeleportEffects(),
                () -> config.setShowTeleportEffects(!config.showTeleportEffects()));
        y += compactLayout() ? 33 : ROW_HEIGHT + GAP;

        addOptionButton(toggleX, y, toggleWidth,
                "dowwrey_beds.settings.cooldown_report",
                config.showCooldownReport(),
                () -> config.setShowCooldownReport(!config.showCooldownReport()));
        y += compactLayout() ? 33 : ROW_HEIGHT + GAP;

        addOptionButton(toggleX, y, toggleWidth,
                "dowwrey_beds.settings.smooth_animations",
                config.smoothInterfaceAnimations(),
                () -> config.setSmoothInterfaceAnimations(!config.smoothInterfaceAnimations()));

        int footerY = height - 28;
        int footerGap = 7;
        int resetWidth = Math.min(150, Math.max(110, (panelWidth - 24) / 3));
        int backWidth = Math.min(150, Math.max(110, (panelWidth - 24) / 3));
        int totalWidth = resetWidth + footerGap + backWidth;
        int footerLeft = (width - totalWidth) / 2;

        addRenderableWidget(Button.builder(
                Component.translatable("dowwrey_beds.settings.reset"),
                ignored -> {
                    config.resetToDefaults();
                    init();
                }
        ).bounds(footerLeft, footerY, resetWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("dowwrey_beds.settings.done"),
                ignored -> this.minecraft.gui.setScreen(parent)
        ).bounds(footerLeft + resetWidth + footerGap, footerY, backWidth, 20).build());
    }

    private void addOptionButton(int x, int y, int width, String key, boolean enabled, Runnable action) {
        String descKey = key + ".desc";
        addRenderableWidget(Button.builder(
                Component.translatable(enabled ? "dowwrey_beds.settings.enabled" : "dowwrey_beds.settings.disabled"),
                ignored -> { action.run(); init(); }
        ).bounds(x, y + (compactLayout() ? 5 : 12), width, 20)
                .tooltip(Tooltip.create(Component.translatable(descKey)))
                .build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = panelWidth();
        int left = (width - panelWidth) / 2;
        int panelBottom = height - 10;

        graphics.fill(Math.max(0, left - 12), compactLayout() ? 36 : TOP, Math.min(width, left + panelWidth + 12), panelBottom, 0xE6101010);
        graphics.fill(Math.max(0, left - 12), compactLayout() ? 36 : TOP, Math.min(width, left + panelWidth + 12), (compactLayout() ? 36 : TOP) + 2, 0xFFD0B66A);

        int y = compactLayout() ? 72 : ROW_TOP;
        drawRow(graphics, left, panelWidth, y, "dowwrey_beds.settings.notifications", "dowwrey_beds.settings.notifications.desc");
        y += compactLayout() ? 33 : ROW_HEIGHT + GAP;
        drawRow(graphics, left, panelWidth, y, "dowwrey_beds.settings.teleport_effects", "dowwrey_beds.settings.teleport_effects.desc");
        y += compactLayout() ? 33 : ROW_HEIGHT + GAP;
        drawRow(graphics, left, panelWidth, y, "dowwrey_beds.settings.cooldown_report", "dowwrey_beds.settings.cooldown_report.desc");
        y += compactLayout() ? 33 : ROW_HEIGHT + GAP;
        drawRow(graphics, left, panelWidth, y, "dowwrey_beds.settings.smooth_animations", "dowwrey_beds.settings.smooth_animations.desc");
    }

    private void drawRow(GuiGraphicsExtractor graphics, int left, int panelWidth, int y, String titleKey, String descriptionKey) {
        int rowHeight = compactLayout() ? 30 : ROW_HEIGHT;
        graphics.fill(left + 8, y, left + panelWidth - 8, y + rowHeight, 0xFF1D1D1D);
        int textLeft = left + 20;
        int textRight = left + panelWidth - Math.min(124, Math.max(92, panelWidth / 4)) - 30;
        int available = Math.max(72, textRight - textLeft);

        Component title = Component.translatable(titleKey);
        String titleText = title.getString();
        if (font.width(titleText) > available) {
            titleText = font.plainSubstrByWidth(titleText, Math.max(12, available - 6)) + "…";
        }
        graphics.text(font, Component.literal(titleText), textLeft, y + (compactLayout() ? 9 : 8), 0xFFFFFFFF, true);

        if (compactLayout()) return;

        String description = Component.translatable(descriptionKey).getString();
        if (font.width(description) > available) {
            description = font.plainSubstrByWidth(description, Math.max(12, available - 6)) + "…";
        }
        graphics.text(font, Component.literal(description), textLeft, y + 26, 0xFFAAAAAA, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(font, title, width / 2, 55, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }

    /** Small indirection to avoid duplicating a mutable global reference in screens. */
    private static final class BedClientConfigHolder {
        private static BedClientConfig config() {
            return com.dowwrey.beds.client.DowwreyBedsClient.config();
        }
    }
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ScreenKeyHandler.handle(this, event)) {
            return true;
        }
        return super.keyPressed(event);
    }

}
