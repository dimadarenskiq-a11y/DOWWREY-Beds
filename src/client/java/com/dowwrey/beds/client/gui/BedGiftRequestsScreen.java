package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.DowwreyBedsClient;
import com.dowwrey.beds.network.payload.BedGiftRequestsPayload;
import com.dowwrey.beds.storage.BedIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class BedGiftRequestsScreen extends Screen {
    private static final int MAX_CARD_WIDTH = 620;
    private static final int MIN_CARD_WIDTH = 288;
    private static final int CARD_TOP = 82;
    private static final int CARD_HEIGHT = 66;
    private static final int GAP = 7;

    private List<BedGiftRequestsPayload.RequestEntry> requests = List.of();
    private int page;
    private int pageCount = 1;

    public BedGiftRequestsScreen(List<BedGiftRequestsPayload.RequestEntry> requests) {
        super(Component.translatable("dowwrey_beds.friend.request.title"));
        setRequests(requests);
    }

    public void setRequests(List<BedGiftRequestsPayload.RequestEntry> requests) {
        this.requests = List.copyOf(requests);
        recalculatePages();
    }

    public void refreshLayout() {
        recalculatePages();
        rebuildButtons();
    }

    private int panelWidth() {
        return Math.min(MAX_CARD_WIDTH, Math.max(MIN_CARD_WIDTH, width - 32));
    }

    private int visibleCards() {
        return Math.max(1, Math.min(6, (height - CARD_TOP - 50) / (CARD_HEIGHT + GAP)));
    }

    private void recalculatePages() {
        int visible = visibleCards();
        pageCount = Math.max(1, (requests.size() + visible - 1) / visible);
        page = Math.min(page, pageCount - 1);
    }

    @Override
    protected void init() {
        recalculatePages();
        rebuildButtons();
    }

    private void rebuildButtons() {
        clearWidgets();
        int left = (width - panelWidth()) / 2;
        int cardWidth = panelWidth();
        int visible = visibleCards();
        int start = page * visible;
        int end = Math.min(start + visible, requests.size());

        for (int i = start; i < end; i++) {
            BedGiftRequestsPayload.RequestEntry request = requests.get(i);
            int y = CARD_TOP + (i - start) * (CARD_HEIGHT + GAP);
            int buttonWidth = Math.min(88, Math.max(74, (cardWidth - 34) / 6));
            int buttonY = y + 34;

            addRenderableWidget(Button.builder(
                    Component.translatable("dowwrey_beds.friend.request.accept"),
                    button -> respond(request, true)
            ).bounds(left + cardWidth - buttonWidth * 2 - 10, buttonY, buttonWidth, 20)
                    .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.accept"))).build());

            addRenderableWidget(Button.builder(
                    Component.translatable("dowwrey_beds.friend.request.decline"),
                    button -> respond(request, false)
            ).bounds(left + cardWidth - buttonWidth - 4, buttonY, buttonWidth, 20)
                    .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.tooltip.decline"))).build());
        }

        int footerY = height - 28;
        int navWidth = Math.min(110, Math.max(78, (cardWidth - 12) / 2));
        addRenderableWidget(Button.builder(
                Component.translatable("dowwrey_beds.manager.prev"),
                button -> {
                    if (page > 0) {
                        page--;
                        rebuildButtons();
                    }
                }
        ).bounds(left, footerY, navWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("dowwrey_beds.manager.next"),
                button -> {
                    if (page + 1 < pageCount) {
                        page++;
                        rebuildButtons();
                    }
                }
        ).bounds(left + cardWidth - navWidth, footerY, navWidth, 20).build());
    }

    private void respond(BedGiftRequestsPayload.RequestEntry request, boolean accepted) {
        DowwreyBedsClient.respondToBedGift(request.id(), accepted);
        onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int cardWidth = panelWidth();
        int left = (width - cardWidth) / 2;
        graphics.fill(left - 12, 42, left + cardWidth + 12, height - 12, 0xE6101010);
        graphics.fill(left - 12, 42, left + cardWidth + 12, 44, 0xFFD0B66A);

        int visible = visibleCards();
        int start = page * visible;
        int end = Math.min(start + visible, requests.size());
        for (int i = start; i < end; i++) {
            int y = CARD_TOP + (i - start) * (CARD_HEIGHT + GAP);
            graphics.fill(left, y, left + cardWidth, y + CARD_HEIGHT, 0xFF202020);
            graphics.fill(left, y, left + 3, y + CARD_HEIGHT, requestColor(requests.get(i)));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int cardWidth = panelWidth();
        int left = (width - cardWidth) / 2;

        graphics.centeredText(font, title, width / 2, 56, 0xFFFFFFFF);

        if (requests.isEmpty()) {
            graphics.text(font, Component.translatable("dowwrey_beds.friend.request.empty"), width / 2, height / 2, 0xFFB8B8B8, true);
        }

        int visible = visibleCards();
        int start = page * visible;
        int end = Math.min(start + visible, requests.size());
        for (int i = start; i < end; i++) {
            BedGiftRequestsPayload.RequestEntry request = requests.get(i);
            int y = CARD_TOP + (i - start) * (CARD_HEIGHT + GAP);
            graphics.text(font, Component.translatable("dowwrey_beds.friend.request.from", request.senderName()), left + 12, y + 10, 0xFFC8C8C8, false);
            graphics.fakeItem(BedIcons.itemStack(request.icon()), left + 12, y + 18);
            graphics.text(font, Component.literal(request.bedName()), left + 34, y + 24, 0xFFFFFFFF, true);
            if (request.remainingSeconds() > 0) {
                graphics.text(font, Component.translatable("dowwrey_beds.friend.request.expires", request.remainingSeconds()), left + 12, y + 53, 0xFFD0B66A, false);
            }
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    private static int requestColor(BedGiftRequestsPayload.RequestEntry request) {
        return com.dowwrey.beds.storage.BedColors.isAllowed(request.color())
                ? request.color()
                : com.dowwrey.beds.storage.BedColors.DEFAULT;
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
