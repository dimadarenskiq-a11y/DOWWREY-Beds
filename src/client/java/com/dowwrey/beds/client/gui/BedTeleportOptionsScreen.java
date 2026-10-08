package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.DowwreyBedsClient;
import com.dowwrey.beds.network.payload.BedListPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Vanilla-style speed selection. The actual cost/duration is always validated by the server. */
public final class BedTeleportOptionsScreen extends Screen {
    private final Screen parent;
    private final BedListPayload.BedEntry bed;

    public BedTeleportOptionsScreen(Screen parent, BedListPayload.BedEntry bed) {
        super(Component.translatable("dowwrey_beds.teleport.title"));
        this.parent = parent;
        this.bed = bed;
    }

    @Override
    protected void init() {
        int panelWidth = Math.min(440, Math.max(280, width - 32));
        int left = (width - panelWidth) / 2;
        int centerY = height / 2;
        int gap = 6;
        int buttonWidth = Math.max(82, (panelWidth - gap * 2) / 3);

        Button slow = Button.builder(
                Component.translatable("dowwrey_beds.teleport.time", DowwreyBedsClient.serverSlowTeleportSeconds(), DowwreyBedsClient.serverSlowTeleportCost()),
                button -> start(0)
        ).bounds(left, centerY - 4, buttonWidth, 22)
                .tooltip(Tooltip.create(optionTooltip(DowwreyBedsClient.serverSlowTeleportCost()))).build();
        slow.active = canStart(DowwreyBedsClient.serverSlowTeleportCost());
        addRenderableWidget(slow);
        Button fast = Button.builder(
                Component.translatable("dowwrey_beds.teleport.time", DowwreyBedsClient.serverFastTeleportSeconds(), DowwreyBedsClient.serverFastTeleportCost()),
                button -> start(1)
        ).bounds(left + buttonWidth + gap, centerY - 4, buttonWidth, 22)
                .tooltip(Tooltip.create(optionTooltip(DowwreyBedsClient.serverFastTeleportCost()))).build();
        fast.active = canStart(DowwreyBedsClient.serverFastTeleportCost());
        addRenderableWidget(fast);
        Button instant = Button.builder(
                Component.translatable("dowwrey_beds.teleport.time", DowwreyBedsClient.serverInstantTeleportSeconds(), DowwreyBedsClient.serverInstantTeleportCost()),
                button -> start(2)
        ).bounds(left + (buttonWidth + gap) * 2, centerY - 4, buttonWidth, 22)
                .tooltip(Tooltip.create(optionTooltip(DowwreyBedsClient.serverInstantTeleportCost()))).build();
        instant.active = canStart(DowwreyBedsClient.serverInstantTeleportCost());
        addRenderableWidget(instant);
        addRenderableWidget(Button.builder(
                Component.translatable("dowwrey_beds.teleport.cancel"),
                button -> onClose()
        ).bounds(left, centerY + 30, panelWidth, 20).build());
    }

    private Component optionTooltip(int cost) {
        if (DowwreyBedsClient.isTeleportCooldownActive()) {
            return Component.translatable("dowwrey_beds.teleport.cooldown_active", DowwreyBedsClient.teleportCooldownSeconds());
        }
        if (bed.status() == BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE) {
            return Component.translatable("dowwrey_beds.manager.status.no_safe");
        }
        if (bed.status() == BedListPayload.BedEntry.STATUS_NOT_LOADED) {
            return Component.translatable("dowwrey_beds.manager.status.not_loaded");
        }
        int eyes = DowwreyBedsClient.enderEyeCount();
        if (eyes < cost) {
            return Component.translatable("dowwrey_beds.teleport.not_enough_eyes", cost);
        }
        return Component.translatable("dowwrey_beds.tooltip.teleport_option");
    }

    private boolean canStart(int cost) {
        if (DowwreyBedsClient.isTeleportCooldownActive()) return false;
        if (bed.broken() && !DowwreyBedsClient.serverBrokenTeleportEnabled()) return false;
        if (bed.status() == BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE) return false;
        return DowwreyBedsClient.enderEyeCount() >= cost;
    }

    private void start(int mode) {
        DowwreyBedsClient.startBedTeleport(bed.id(), mode);
        this.minecraft.gui.setScreen(null);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int panelWidth = Math.min(440, Math.max(280, width - 32));
        int left = (width - panelWidth) / 2;
        int centerY = height / 2;
        graphics.fill(left - 12, centerY - 74, left + panelWidth + 12, centerY + 62, 0xE6141414);
        graphics.fill(left - 12, centerY - 74, left + panelWidth + 12, centerY - 72, 0xFFD0B66A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.centeredText(font, title, width / 2, height / 2 - 61, 0xFFFFFFFF);
        graphics.centeredText(
                font,
                Component.translatable("dowwrey_beds.teleport.for_bed", bed.name()),
                width / 2,
                height / 2 - 43,
                0xFFC8C8C8
        );
        Component hint = DowwreyBedsClient.isTeleportCooldownActive()
                ? Component.translatable("dowwrey_beds.teleport.cooldown_active", DowwreyBedsClient.teleportCooldownSeconds())
                : (bed.status() == BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE
                    ? Component.translatable("dowwrey_beds.manager.status.no_safe")
                    : Component.translatable("dowwrey_beds.teleport.cooldown_hint", DowwreyBedsClient.serverTeleportCooldownSeconds()));
        graphics.centeredText(
                font,
                hint,
                width / 2,
                height / 2 + 55,
                0xFF909090
        );
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
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ScreenKeyHandler.handle(this, event)) {
            return true;
        }
        return super.keyPressed(event);
    }

}
