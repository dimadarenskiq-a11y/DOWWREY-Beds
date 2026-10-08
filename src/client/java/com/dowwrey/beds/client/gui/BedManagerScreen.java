package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.DowwreyBedsClient;
import com.dowwrey.beds.network.payload.BedListPayload;
import com.dowwrey.beds.storage.BedIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class BedManagerScreen extends Screen {
    private static final int SIDE_MARGIN = 16;
    private static final int MAX_CARD_WIDTH = 680;
    private static final int MIN_CARD_WIDTH = 288;
    private static final int WIDE_CARD_HEIGHT = 88;
    private static final int NARROW_CARD_HEIGHT = 142;
    private static final int GAP = 7;
    private static final int HEADER_TOP = 42;
    private static final int CARD_TOP = 88;
    private static final int FOOTER_HEIGHT = 24;

    private static final double SCROLL_ANIMATION_SPEED = 18.0D;
    private static final double SCROLL_WHEEL_STEP = 1.15D;

    private List<BedListPayload.BedEntry> beds = List.of();
    private int maxBeds = 10;
    private boolean teleportEnabled = true;
    private boolean brokenTeleportEnabled = true;
    private int teleportCooldownSeconds = 25;
    private final List<List<Button>> cardButtonGroups = new java.util.ArrayList<>();
    private double scrollPosition;
    private double targetScrollPosition;
    private long lastScrollNanos;

    public BedManagerScreen(List<BedListPayload.BedEntry> beds) { this(beds, 10, true, true); }

    public BedManagerScreen(List<BedListPayload.BedEntry> beds, int maxBeds, boolean teleportEnabled, boolean brokenTeleportEnabled) {
        this(beds, maxBeds, teleportEnabled, brokenTeleportEnabled, 25);
    }

    public BedManagerScreen(List<BedListPayload.BedEntry> beds, int maxBeds, boolean teleportEnabled, boolean brokenTeleportEnabled, int teleportCooldownSeconds) {
        super(Component.translatable("dowwrey_beds.manager.title"));
        this.maxBeds = Math.max(1, maxBeds);
        this.teleportEnabled = teleportEnabled;
        this.brokenTeleportEnabled = brokenTeleportEnabled;
        this.teleportCooldownSeconds = Math.max(0, teleportCooldownSeconds);
        setBeds(beds);
    }

    public void setServerRules(int maxBeds, boolean teleportEnabled, boolean brokenTeleportEnabled, int teleportCooldownSeconds) {
        this.maxBeds = Math.max(1, maxBeds);
        this.teleportEnabled = teleportEnabled;
        this.brokenTeleportEnabled = brokenTeleportEnabled;
        this.teleportCooldownSeconds = Math.max(0, teleportCooldownSeconds);
        refreshLayout();
    }

    public void setBeds(List<BedListPayload.BedEntry> beds) {
        this.beds = List.copyOf(beds);
        clampScroll();
        if (this.beds.isEmpty()) {
            scrollPosition = 0.0D;
            targetScrollPosition = 0.0D;
        }
    }

    public void refreshLayout() {
        clampScroll();
        rebuildButtons();
    }

    @Override
    protected void init() {
        clampScroll();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        cardButtonGroups.clear();

        int cardWidth = cardWidth();
        int left = (width - cardWidth) / 2;

        addRenderableWidget(Button.builder(
                Component.literal("⚙"),
                ignored -> DowwreyBedsClient.openSettings()
        ).bounds(left + cardWidth - 24, 48, 20, 20)
                .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.settings")))
                .build());

        int cardHeight = cardHeight();
        for (int i = 0; i < beds.size(); i++) {
            BedListPayload.BedEntry bed = beds.get(i);
            List<Button> group = new java.util.ArrayList<>();
            addCardButtons(bed, left, CARD_TOP + i * (cardHeight + GAP), cardWidth, cardHeight, group);
            cardButtonGroups.add(group);
        }

        positionCardWidgets();
    }

    private void addCardButton(List<Button> group, Button button) {
        group.add(button);
        addRenderableWidget(button);
    }

    private int cardWidth() {
        return Math.min(MAX_CARD_WIDTH, Math.max(MIN_CARD_WIDTH, width - SIDE_MARGIN * 2));
    }

    private boolean narrowLayout() {
        return cardWidth() < 420;
    }

    private int cardHeight() {
        return narrowLayout() ? NARROW_CARD_HEIGHT : WIDE_CARD_HEIGHT;
    }

    private int listBottom() {
        return height - FOOTER_HEIGHT;
    }

    private int availableHeight() {
        return Math.max(1, listBottom() - CARD_TOP);
    }

    private int visibleCards() {
        return Math.max(1, (availableHeight() + GAP) / (cardHeight() + GAP));
    }

    private int maxScrollIndex() {
        return Math.max(0, beds.size() - visibleCards());
    }

    private int scrollExtent() {
        return cardHeight() + GAP;
    }

    private double maxScrollPosition() {
        return maxScrollIndex() * (double) scrollExtent();
    }

    private void clampScroll() {
        double max = maxScrollPosition();
        targetScrollPosition = Math.max(0.0D, Math.min(targetScrollPosition, max));
        scrollPosition = Math.max(0.0D, Math.min(scrollPosition, max));
    }

    private void updateSmoothScroll() {
        long now = System.nanoTime();
        if (lastScrollNanos == 0L) {
            lastScrollNanos = now;
            positionCardWidgets();
            return;
        }

        double deltaSeconds = Math.min(0.05D, Math.max(0.0D, (now - lastScrollNanos) / 1_000_000_000.0D));
        lastScrollNanos = now;

        if (!DowwreyBedsClient.config().smoothInterfaceAnimations()) {
            scrollPosition = targetScrollPosition;
        } else {
            double factor = 1.0D - Math.exp(-SCROLL_ANIMATION_SPEED * deltaSeconds);
            scrollPosition += (targetScrollPosition - scrollPosition) * factor;
            if (Math.abs(targetScrollPosition - scrollPosition) < 0.05D) {
                scrollPosition = targetScrollPosition;
            }
        }

        positionCardWidgets();
    }

    private int cardY(int index) {
        return (int) Math.round(CARD_TOP + index * (double) scrollExtent() - scrollPosition);
    }

    private record TeleportLayout(int x, int y, int width) {}

    private TeleportLayout teleportLayout(BedListPayload.BedEntry bed, int left, int y, int cardWidth, int cardHeight) {
        int buttonGap = 5;
        if (narrowLayout()) {
            int buttonWidth = Math.max(86, (cardWidth - 24 - buttonGap) / 2);
            int rowOneY = y + cardHeight - 62;
            int xLeft = left + 12;
            int xRight = xLeft + buttonWidth + buttonGap;
            return bed.active()
                    ? new TeleportLayout(xLeft, rowOneY, buttonWidth)
                    : new TeleportLayout(xRight, rowOneY, buttonWidth);
        }

        int buttonCount = bed.active() ? 3 : 4;
        int buttonWidth = Math.min(94, Math.max(72, (cardWidth - 44) / 7));
        int totalWidth = buttonWidth * buttonCount + buttonGap * (buttonCount - 1);
        int buttonsLeft = left + cardWidth - totalWidth - 20;
        int buttonY = y + (cardHeight - 20) / 2;
        int teleportX = bed.active() ? buttonsLeft : buttonsLeft + buttonWidth + buttonGap;
        return new TeleportLayout(teleportX, buttonY, buttonWidth);
    }

    private boolean teleportButtonActive(BedListPayload.BedEntry bed) {
        return teleportEnabled
                && (!bed.broken() || brokenTeleportEnabled)
                && bed.status() != BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE
                && !DowwreyBedsClient.isTeleportCooldownActive();
    }

    private Button teleportButton(BedListPayload.BedEntry bed, int x, int y, int width) {
        Component tooltip = teleportTooltip(bed);
        Button button = Button.builder(
                Component.translatable("dowwrey_beds.manager.teleport"),
                ignored -> DowwreyBedsClient.teleportAtBed(bed)
        ).bounds(x, y, width, 20)
                .tooltip(Tooltip.create(tooltip))
                .build();
        button.active = teleportButtonActive(bed);
        return button;
    }

    private Component teleportTooltip(BedListPayload.BedEntry bed) {
        if (!teleportEnabled) return Component.translatable("dowwrey_beds.server.feature_disabled");
        if (DowwreyBedsClient.isTeleportCooldownActive()) {
            return Component.translatable("dowwrey_beds.teleport.cooldown_active", DowwreyBedsClient.teleportCooldownSeconds());
        }
        if (bed.status() == BedListPayload.BedEntry.STATUS_BROKEN) {
            if (!brokenTeleportEnabled) return Component.translatable("dowwrey_beds.server.feature_disabled");
            int cost = DowwreyBedsClient.serverBrokenTeleportCost();
            return Component.translatable("dowwrey_beds.manager.broken_tooltip", cost);
        }
        return switch (bed.status()) {
            case BedListPayload.BedEntry.STATUS_OTHER_DIMENSION -> Component.translatable("dowwrey_beds.manager.status.other_dimension");
            case BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE -> Component.translatable("dowwrey_beds.manager.status.no_safe");
            case BedListPayload.BedEntry.STATUS_NOT_LOADED -> Component.translatable("dowwrey_beds.manager.status.not_loaded");
            default -> bed.distanceBlocks() >= 0
                    ? Component.translatable("dowwrey_beds.manager.distance", bed.distanceBlocks())
                    : Component.translatable("dowwrey_beds.tooltip.teleport");
        };
    }

    private void addCardButtons(BedListPayload.BedEntry bed, int left, int y, int cardWidth, int cardHeight, List<Button> group) {
        int buttonGap = 5;
        boolean active = bed.active();

        if (narrowLayout()) {
            int buttonWidth = Math.max(86, (cardWidth - 24 - buttonGap) / 2);
            int rowOneY = y + cardHeight - 62;
            int rowTwoY = y + cardHeight - 21;
            int xLeft = left + 12;
            int xRight = xLeft + buttonWidth + buttonGap;

            if (!active) {
                addCardButton(group, Button.builder(
                        Component.translatable("dowwrey_beds.manager.select"),
                        button -> DowwreyBedsClient.setActiveBed(bed.id())
                ).bounds(xLeft, rowOneY, buttonWidth, 20)
                        .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.select"))).build());
                addCardButton(group, teleportButton(bed, xRight, rowOneY, buttonWidth));
                addCardButton(group, Button.builder(
                        Component.translatable("dowwrey_beds.manager.edit"),
                        button -> this.minecraft.gui.setScreen(new EditBedScreen(bed))
                ).bounds(xLeft, rowTwoY, buttonWidth, 20)
                        .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.edit"))).build());
                addCardButton(group, Button.builder(
                        Component.translatable("dowwrey_beds.manager.delete"),
                        button -> this.minecraft.gui.setScreen(new ConfirmActionScreen(this,
                        Component.translatable("dowwrey_beds.confirm.delete_title"),
                        Component.translatable("dowwrey_beds.confirm.delete_body", bed.name()),
                        () -> { DowwreyBedsClient.deleteBed(bed.id()); this.minecraft.gui.setScreen(null); }))
                ).bounds(xRight, rowTwoY, buttonWidth, 20)
                        .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.delete"))).build());
            } else {
                addCardButton(group, teleportButton(bed, xLeft, rowOneY, buttonWidth));
                addCardButton(group, Button.builder(
                        Component.translatable("dowwrey_beds.manager.edit"),
                        button -> this.minecraft.gui.setScreen(new EditBedScreen(bed))
                ).bounds(xRight, rowOneY, buttonWidth, 20)
                        .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.edit"))).build());
                addCardButton(group, Button.builder(
                        Component.translatable("dowwrey_beds.manager.delete"),
                        button -> this.minecraft.gui.setScreen(new ConfirmActionScreen(this,
                        Component.translatable("dowwrey_beds.confirm.delete_title"),
                        Component.translatable("dowwrey_beds.confirm.delete_body", bed.name()),
                        () -> { DowwreyBedsClient.deleteBed(bed.id()); this.minecraft.gui.setScreen(null); }))
                ).bounds(xLeft, rowTwoY, cardWidth - 24, 20)
                        .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.delete"))).build());
            }
            return;
        }

        int buttonCount = active ? 3 : 4;
        int buttonWidth = Math.min(94, Math.max(72, (cardWidth - 44) / 7));
        int totalWidth = buttonWidth * buttonCount + buttonGap * (buttonCount - 1);
        int buttonsLeft = left + cardWidth - totalWidth - 20;
        int buttonY = y + (cardHeight - 20) / 2;
        int x = buttonsLeft;

        if (!active) {
            addCardButton(group, Button.builder(
                    Component.translatable("dowwrey_beds.manager.select"),
                    button -> DowwreyBedsClient.setActiveBed(bed.id())
            ).bounds(x, buttonY, buttonWidth, 20)
                    .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.select"))).build());
            x += buttonWidth + buttonGap;
        }
        addCardButton(group, teleportButton(bed, x, buttonY, buttonWidth));
        x += buttonWidth + buttonGap;
        addCardButton(group, Button.builder(
                Component.translatable("dowwrey_beds.manager.edit"),
                button -> this.minecraft.gui.setScreen(new EditBedScreen(bed))
        ).bounds(x, buttonY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.edit")))
                .build());
        x += buttonWidth + buttonGap;
        addCardButton(group, Button.builder(
                Component.translatable("dowwrey_beds.manager.delete"),
                button -> this.minecraft.gui.setScreen(new ConfirmActionScreen(this,
                        Component.translatable("dowwrey_beds.confirm.delete_title"),
                        Component.translatable("dowwrey_beds.confirm.delete_body", bed.name()),
                        () -> { DowwreyBedsClient.deleteBed(bed.id()); this.minecraft.gui.setScreen(null); }))
        ).bounds(x, buttonY, buttonWidth, 20)
                .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.delete"))).build());
    }

    private void positionCardWidgets() {
        int cardWidth = cardWidth();
        int left = (width - cardWidth) / 2;
        int cardHeight = cardHeight();
        for (int i = 0; i < cardButtonGroups.size() && i < beds.size(); i++) {
            BedListPayload.BedEntry bed = beds.get(i);
            List<Button> group = cardButtonGroups.get(i);
            int y = cardY(i);
            positionCardButtonGroup(bed, group, left, y, cardWidth, cardHeight);
            for (Button button : group) {
                boolean visible = widgetInsideListViewport(button, left, cardWidth);
                button.visible = visible;
                if (!visible) {
                    button.setFocused(false);
                }
            }
        }
    }

    private void positionCardButtonGroup(BedListPayload.BedEntry bed, List<Button> group, int left, int y, int cardWidth, int cardHeight) {
        int buttonGap = 5;
        boolean active = bed.active();
        int index = 0;

        if (narrowLayout()) {
            int buttonWidth = Math.max(86, (cardWidth - 24 - buttonGap) / 2);
            int rowOneY = y + cardHeight - 62;
            int rowTwoY = y + cardHeight - 21;
            int xLeft = left + 12;
            int xRight = xLeft + buttonWidth + buttonGap;
            if (!active) {
                placeButton(group.get(index++), xLeft, rowOneY, buttonWidth);
                Button teleport = group.get(index++);
                teleport.active = teleportButtonActive(bed);
                placeButton(teleport, xRight, rowOneY, buttonWidth);
                placeButton(group.get(index++), xLeft, rowTwoY, buttonWidth);
                placeButton(group.get(index), xRight, rowTwoY, buttonWidth);
            } else {
                Button teleport = group.get(index++);
                teleport.active = teleportButtonActive(bed);
                placeButton(teleport, xLeft, rowOneY, buttonWidth);
                placeButton(group.get(index++), xRight, rowOneY, buttonWidth);
                placeButton(group.get(index), xLeft, rowTwoY, cardWidth - 24);
            }
            return;
        }

        int buttonCount = active ? 3 : 4;
        int buttonWidth = Math.min(94, Math.max(72, (cardWidth - 44) / 7));
        int totalWidth = buttonWidth * buttonCount + buttonGap * (buttonCount - 1);
        int buttonsLeft = left + cardWidth - totalWidth - 20;
        int buttonY = y + (cardHeight - 20) / 2;
        int x = buttonsLeft;

        if (!active) {
            placeButton(group.get(index++), x, buttonY, buttonWidth);
            x += buttonWidth + buttonGap;
        }
        Button teleport = group.get(index++);
        teleport.active = teleportButtonActive(bed);
        placeButton(teleport, x, buttonY, buttonWidth);
        x += buttonWidth + buttonGap;
        placeButton(group.get(index++), x, buttonY, buttonWidth);
        x += buttonWidth + buttonGap;
        placeButton(group.get(index), x, buttonY, buttonWidth);
    }

    private void placeButton(Button button, int x, int y, int width) {
        button.setX(x);
        button.setY(y);
        button.setWidth(width);
    }

    /**
     * Card widgets are real Minecraft widgets and are rendered after our custom scissor pass.
     * Keep them fully inside the scroll viewport so a partially visible card can never leave
     * interactive buttons hanging over the header or below the panel.
     */
    private boolean widgetInsideListViewport(Button button, int left, int cardWidth) {
        return button.getX() >= left
                && button.getRight() <= left + cardWidth
                && button.getY() >= CARD_TOP
                && button.getBottom() <= listBottom();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        updateSmoothScroll();

        int cardWidth = cardWidth();
        int left = (width - cardWidth) / 2;
        int panelBottom = height - 12;

        graphics.fill(left - 12, HEADER_TOP, left + cardWidth + 12, panelBottom, 0xE6101010);
        graphics.fill(left - 12, HEADER_TOP, left + cardWidth + 12, HEADER_TOP + 2, 0xFFD0B66A);

        int visible = visibleCards();
        int cardHeight = cardHeight();

        graphics.enableScissor(left, CARD_TOP, left + cardWidth, listBottom());
        for (int i = 0; i < beds.size(); i++) {
            BedListPayload.BedEntry bed = beds.get(i);
            int y = cardY(i);
            graphics.fill(left, y, left + cardWidth, y + cardHeight, 0xFF202020);
            int statusColor = bed.broken() ? 0xFFD14C4C : (bed.active() ? 0xFFE1C45A : bed.color());
            graphics.fill(left, y, left + 3, y + cardHeight, statusColor);
        }
        graphics.disableScissor();

        if (beds.size() > visible) {
            int trackX = left + cardWidth + 7;
            int trackTop = CARD_TOP;
            int trackBottom = Math.max(trackTop + 4, listBottom() - 2);
            int trackHeight = trackBottom - trackTop;
            graphics.fill(trackX, trackTop, trackX + 3, trackBottom, 0xFF3A3A3A);

            int thumbHeight = Math.max(16, (int) ((long) trackHeight * visible / beds.size()));
            int travel = trackHeight - thumbHeight;
            int thumbY = trackTop + (maxScrollPosition() <= 0.0D ? 0 : (int) Math.round(travel * scrollPosition / maxScrollPosition()));
            graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, 0xFFD0B66A);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int cardWidth = cardWidth();
        int left = (width - cardWidth) / 2;

        graphics.centeredText(font, title, width / 2, 55, 0xFFFFFFFF);
        graphics.centeredText(
                font,
                Component.translatable("dowwrey_beds.manager.counter", beds.size(), maxBeds),
                width / 2,
                73,
                0xFFD0D0D0
        );

        int cardHeight = cardHeight();
        int contentRight = left + cardWidth - 12;

        if (!beds.isEmpty()) {
            graphics.enableScissor(left, CARD_TOP, left + cardWidth, listBottom());
            for (int i = 0; i < beds.size(); i++) {
                BedListPayload.BedEntry bed = beds.get(i);
                int y = cardY(i);
                int textLeft = left + 12;
                int textTop = y + 11;

                int nameX = textLeft;
                if (bed.active()) {
                    graphics.text(font, Component.literal("★"), textLeft, textTop, 0xFFE1C45A, true);
                    nameX += 14;
                }
                graphics.fakeItem(BedIcons.itemStack(bed.icon()), nameX, textTop - 3);
                nameX += 22;

                int buttonReserve = narrowLayout() ? 0 : 315;
                int availableNameWidth = Math.max(24, contentRight - nameX - buttonReserve);
                String displayName = bed.name();
                if (font.width(displayName) > availableNameWidth) {
                    displayName = font.plainSubstrByWidth(displayName, Math.max(8, availableNameWidth - 6)) + "…";
                }
                graphics.text(font, Component.literal(displayName), nameX, textTop, 0xFFFFFFFF, true);

                graphics.text(font, dimensionText(bed.dimension()), textLeft, textTop + 19, 0xFFB8B8B8, false);
                if (bed.distanceBlocks() >= 0) {
                    graphics.text(font, Component.translatable("dowwrey_beds.manager.distance", bed.distanceBlocks()), textLeft + font.width(dimensionText(bed.dimension())) + 10, textTop + 19, 0xFF8D8D8D, false);
                }
                graphics.text(font, Component.literal(bed.pos().toShortString()), textLeft, textTop + 36, 0xFF909090, false);
                graphics.text(font, statusText(bed), textLeft, textTop + 53, statusColor(bed), false);

                // The selected bed icon is intentionally shown only to the left of the name.
                // Do not duplicate it on hover: the card already communicates its identity clearly.

                TeleportLayout teleport = teleportLayout(bed, left, y, cardWidth, cardHeight);
                if (DowwreyBedsClient.config().showCooldownReport() && DowwreyBedsClient.isTeleportCooldownActive()) {
                    int remaining = DowwreyBedsClient.teleportCooldownSeconds();
                    int reportColor = 0xFFD0B66A;
                    int reportY = teleport.y() + 27;
                    int lineY = teleport.y() + 40;
                    graphics.centeredText(
                            font,
                            Component.translatable("dowwrey_beds.teleport.cooldown_short", remaining),
                            teleport.x() + teleport.width() / 2,
                            reportY,
                            reportColor
                    );
                    graphics.fill(teleport.x(), lineY, teleport.x() + teleport.width(), lineY + 1, 0xFF4C4638);
                    double cooldownMax = Math.max(1.0D, teleportCooldownSeconds);
                    double progress = Math.min(1.0D, Math.max(0.0D, remaining / cooldownMax));
                    int filled = Math.max(1, (int) Math.round(teleport.width() * progress));
                    graphics.fill(teleport.x(), lineY, teleport.x() + filled, lineY + 1, 0xFFD0B66A);
                }
            }
            graphics.disableScissor();
        }

        if (beds.isEmpty()) {
            graphics.centeredText(
                    font,
                    Component.translatable("dowwrey_beds.manager.empty"),
                    width / 2,
                    height / 2,
                    0xFFB8B8B8
            );
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }


    private Component dimensionText(String dimension) {
        return switch (dimension) {
            case "minecraft:overworld" -> Component.translatable("dowwrey_beds.dimension.overworld");
            case "minecraft:the_nether" -> Component.translatable("dowwrey_beds.dimension.nether");
            case "minecraft:the_end" -> Component.translatable("dowwrey_beds.dimension.end");
            default -> Component.literal(dimension);
        };
    }

    private Component statusText(BedListPayload.BedEntry bed) {
        if (bed.broken()) return Component.translatable("dowwrey_beds.manager.status.broken");
        return switch (bed.status()) {
            case BedListPayload.BedEntry.STATUS_OTHER_DIMENSION -> Component.translatable("dowwrey_beds.manager.status.other_dimension");
            case BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE -> Component.translatable("dowwrey_beds.manager.status.no_safe");
            case BedListPayload.BedEntry.STATUS_NOT_LOADED -> Component.translatable("dowwrey_beds.manager.status.not_loaded");
            default -> lastUsedText(bed);
        };
    }

    private int statusColor(BedListPayload.BedEntry bed) {
        if (bed.broken()) return 0xFFD14C4C;
        return switch (bed.status()) {
            case BedListPayload.BedEntry.STATUS_OTHER_DIMENSION -> 0xFF9D9D9D;
            case BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE -> 0xFFD0B66A;
            case BedListPayload.BedEntry.STATUS_NOT_LOADED -> 0xFF9D9D9D;
            default -> 0xFF7FA35A;
        };
    }

    private Component lastUsedText(BedListPayload.BedEntry bed) {
        if (bed.lastUsedGameTime() <= 0L || minecraft.level == null) {
            return Component.translatable("dowwrey_beds.manager.last_used_never");
        }
        long ageTicks = Math.max(0L, minecraft.level.getGameTime() - bed.lastUsedGameTime());
        long minutes = ageTicks / 1200L;
        if (minutes <= 0L) return Component.translatable("dowwrey_beds.manager.last_used_just_now");
        if (minutes < 60L) return Component.translatable("dowwrey_beds.manager.last_used_minutes", minutes);
        long hours = minutes / 60L;
        return Component.translatable("dowwrey_beds.manager.last_used_hours", hours);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!beds.isEmpty() && maxScrollPosition() > 0.0D
                && mouseX >= (width - cardWidth()) / 2.0
                && mouseX <= (width + cardWidth()) / 2.0
                && mouseY >= CARD_TOP
                && mouseY <= listBottom()) {
            targetScrollPosition -= scrollY * scrollExtent() * SCROLL_WHEEL_STEP;
            targetScrollPosition = Math.max(0.0D, Math.min(targetScrollPosition, maxScrollPosition()));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(null);
    }
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (ScreenKeyHandler.handle(this, event)) {
            return true;
        }
        return super.keyPressed(event);
    }

}
