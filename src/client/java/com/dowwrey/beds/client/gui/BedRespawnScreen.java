package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.DowwreyBedsClient;
import com.dowwrey.beds.network.payload.BedListPayload;
import com.dowwrey.beds.storage.BedIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class BedRespawnScreen extends Screen {
    private static final int SIDE_MARGIN = 16;
    private static final int MAX_CARD_WIDTH = 620;
    private static final int MIN_CARD_WIDTH = 280;
    private static final int CARD_TOP = 82;
    private static final int GAP = 7;
    private static final int WIDE_CARD_HEIGHT = 58;
    private static final int NARROW_CARD_HEIGHT = 86;
    private static final int FOOTER_HEIGHT = 36;
    private static final double SCROLL_ANIMATION_SPEED = 18.0D;
    private static final double SCROLL_WHEEL_STEP = 1.15D;

    private final Screen parent;
    private List<BedListPayload.BedEntry> beds = List.of();
    private final List<Button> bedButtons = new ArrayList<>();
    private Button cancelButton;
    private double scrollPosition;
    private double targetScrollPosition;
    private long lastScrollNanos;

    public BedRespawnScreen(Screen parent, List<BedListPayload.BedEntry> beds) {
        super(Component.translatable("dowwrey_beds.respawn.title"));
        this.parent = parent;
        setBeds(beds);
    }

    public void setBeds(List<BedListPayload.BedEntry> beds) {
        this.beds = beds.stream().filter(bed -> !bed.broken()).toList();
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

    private int visibleCards() {
        return Math.max(1, (Math.max(1, listBottom() - CARD_TOP) + GAP) / (cardHeight() + GAP));
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

    private int cardY(int index) {
        return (int) Math.round(CARD_TOP + index * (double) scrollExtent() - scrollPosition);
    }

    private void updateSmoothScroll() {
        long now = System.nanoTime();
        if (lastScrollNanos == 0L) {
            lastScrollNanos = now;
            positionButtons();
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

        positionButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        bedButtons.clear();

        int left = (width - cardWidth()) / 2;
        int cardWidth = cardWidth();
        int cardHeight = cardHeight();

        for (BedListPayload.BedEntry bed : beds) {
            int buttonWidth = narrowLayout() ? cardWidth - 24 : Math.min(132, cardWidth / 4);
            Button button = Button.builder(
                    Component.translatable("dowwrey_beds.respawn.stand"),
                    ignored -> DowwreyBedsClient.respawnAtBed(bed.id())
            ).bounds(0, 0, buttonWidth, 20)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.translatable(bed.status() == BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE
                                    ? "dowwrey_beds.manager.status.no_safe"
                                    : bed.status() == BedListPayload.BedEntry.STATUS_NOT_LOADED
                                        ? "dowwrey_beds.manager.status.not_loaded"
                                        : "dowwrey_beds.tooltip.select")
                    ))
                    .build();
            button.active = bed.status() != BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE;
            bedButtons.add(button);
            addRenderableWidget(button);
        }

        int footerWidth = Math.min(220, Math.max(170, cardWidth));
        cancelButton = Button.builder(
                Component.translatable("dowwrey_beds.respawn.cancel"),
                button -> onClose()
        ).bounds(left + (cardWidth - footerWidth) / 2, height - 28, footerWidth, 20).build();
        addRenderableWidget(cancelButton);

        positionButtons();
    }

    private void positionButtons() {
        int left = (width - cardWidth()) / 2;
        int cardWidth = cardWidth();
        int cardHeight = cardHeight();

        for (int i = 0; i < bedButtons.size(); i++) {
            Button button = bedButtons.get(i);
            int y = cardY(i);
            int buttonWidth = narrowLayout() ? cardWidth - 24 : Math.min(132, cardWidth / 4);
            int buttonX = narrowLayout() ? left + 12 : left + cardWidth - buttonWidth - 12;
            int buttonY = narrowLayout() ? y + cardHeight - 27 : y + (cardHeight - 20) / 2;

            button.setX(buttonX);
            button.setY(buttonY);
            button.setWidth(buttonWidth);
            boolean visible = button.getY() >= CARD_TOP && button.getBottom() <= listBottom();
            button.visible = visible;
            if (!visible) button.setFocused(false);
        }

        if (cancelButton != null) {
            int footerWidth = Math.min(220, Math.max(170, cardWidth));
            cancelButton.setX(left + (cardWidth - footerWidth) / 2);
            cancelButton.setY(height - 28);
            cancelButton.visible = true;
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        updateSmoothScroll();

        int cardWidth = cardWidth();
        int left = (width - cardWidth) / 2;
        int panelBottom = height - 12;
        graphics.fill(left - 12, 42, left + cardWidth + 12, panelBottom, 0xE6101010);
        graphics.fill(left - 12, 42, left + cardWidth + 12, 44, 0xFFD0B66A);

        int visible = visibleCards();
        int cardHeight = cardHeight();
        graphics.enableScissor(left, CARD_TOP, left + cardWidth, listBottom());
        for (int i = 0; i < beds.size(); i++) {
            BedListPayload.BedEntry bed = beds.get(i);
            int y = cardY(i);
            graphics.fill(left, y, left + cardWidth, y + cardHeight, 0xFF202020);
            graphics.fill(left, y, left + 3, y + cardHeight, bed.active() ? 0xFFE1C45A : 0xFF444444);
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
                Component.translatable("dowwrey_beds.respawn.subtitle"),
                width / 2,
                73,
                0xFFC8C8C8
        );

        int cardHeight = cardHeight();

        if (!beds.isEmpty()) {
            graphics.enableScissor(left, CARD_TOP, left + cardWidth, listBottom());
            for (int i = 0; i < beds.size(); i++) {
                BedListPayload.BedEntry bed = beds.get(i);
                int y = cardY(i);
                int textLeft = left + 12;
                int textTop = y + 11;
                int buttonReserve = narrowLayout() ? 12 : 156;
                int contentRight = left + cardWidth - buttonReserve;

                int nameX = textLeft;
                if (bed.active()) {
                    graphics.text(font, Component.literal("★"), textLeft, textTop, 0xFFE1C45A, true);
                    nameX += 14;
                }
                graphics.fakeItem(BedIcons.itemStack(bed.icon()), nameX, textTop - 3);
                nameX += 22;

                int availableNameWidth = Math.max(32, contentRight - nameX);
                String name = bed.name();
                if (font.width(name) > availableNameWidth) {
                    name = font.plainSubstrByWidth(name, Math.max(8, availableNameWidth - 6)) + "…";
                }
                graphics.text(font, Component.literal(name), nameX, textTop, 0xFFFFFFFF, true);
                graphics.text(font, dimensionText(bed.dimension()), textLeft, textTop + 19, 0xFFB8B8B8, false);
                if (bed.distanceBlocks() >= 0) {
                    graphics.text(font, Component.translatable("dowwrey_beds.manager.distance", bed.distanceBlocks()),
                            textLeft + font.width(dimensionText(bed.dimension())) + 10, textTop + 19, 0xFF8D8D8D, false);
                } else if (bed.status() == BedListPayload.BedEntry.STATUS_OTHER_DIMENSION) {
                    graphics.text(font, Component.translatable("dowwrey_beds.manager.status.other_dimension"),
                            textLeft, textTop + 34, 0xFF9D9D9D, false);
                } else if (bed.status() == BedListPayload.BedEntry.STATUS_NOT_LOADED) {
                    graphics.text(font, Component.translatable("dowwrey_beds.manager.status.not_loaded"),
                            textLeft, textTop + 34, 0xFF9D9D9D, false);
                }
                if (!narrowLayout()) {
                    boolean secondaryStatus = bed.status() == BedListPayload.BedEntry.STATUS_OTHER_DIMENSION
                            || bed.status() == BedListPayload.BedEntry.STATUS_NOT_LOADED;
                    graphics.text(font, Component.literal(bed.pos().toShortString()), textLeft, textTop + 34 + (secondaryStatus ? 14 : 0), 0xFF909090, false);
                }
                if (bed.status() == BedListPayload.BedEntry.STATUS_NO_SAFE_SPACE) {
                    graphics.text(font, Component.translatable("dowwrey_beds.manager.status.no_safe"), textLeft, textTop + 50, 0xFFD0B66A, false);
                }
            }
            graphics.disableScissor();
        } else {
            graphics.centeredText(
                    font,
                    Component.translatable("dowwrey_beds.respawn.empty"),
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
