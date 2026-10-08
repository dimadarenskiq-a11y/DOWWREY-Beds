package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.DowwreyBedsClient;
import com.dowwrey.beds.storage.BedIcons;
import com.dowwrey.beds.network.payload.OpenPlayerSelectPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;

public final class PlayerSelectScreen extends Screen {
    private static final int MAX_PANEL_WIDTH = 520;
    private static final int MIN_PANEL_WIDTH = 280;
    private static final int TOP = 40;
    private static final int SEARCH_TOP = 82;
    private static final int LIST_TOP = 116;
    private static final int ROW_HEIGHT = 26;
    private static final int GAP = 5;
    private static final int FOOTER = 34;

    private final BlockPos bedPos;
    private final String bedName;
    private final int color;
    private final int icon;
    private final List<OpenPlayerSelectPayload.PlayerEntry> players;
    private EditBox searchBox;
    private int page;
    private int pageCount = 1;
    private boolean rebuilding;

    public PlayerSelectScreen(BlockPos bedPos, String bedName, int color, int icon, List<OpenPlayerSelectPayload.PlayerEntry> players) {
        super(Component.translatable("dowwrey_beds.friend.select.title"));
        this.bedPos = bedPos;
        this.bedName = bedName;
        this.color = color;
        this.icon = BedIcons.sanitize(icon);
        this.players = List.copyOf(players);
    }

    private int panelWidth() {
        return Math.min(MAX_PANEL_WIDTH, Math.max(MIN_PANEL_WIDTH, width - 32));
    }

    private int left() {
        return (width - panelWidth()) / 2;
    }

    private List<OpenPlayerSelectPayload.PlayerEntry> filteredPlayers() {
        String query = searchBox == null ? "" : searchBox.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return players;
        }
        return players.stream()
                .filter(player -> player.name().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private int visibleRows() {
        return Math.max(1, Math.min(8, (height - LIST_TOP - FOOTER - 12) / (ROW_HEIGHT + GAP)));
    }

    private void recalculatePages() {
        int visible = visibleRows();
        pageCount = Math.max(1, (filteredPlayers().size() + visible - 1) / visible);
        page = Math.min(page, pageCount - 1);
    }

    @Override
    protected void init() {
        rebuildWidgets();
    }

    @Override
    protected void rebuildWidgets() {
        String currentSearch = searchBox == null ? "" : searchBox.getValue();
        clearWidgets();

        int left = left();
        int panelWidth = panelWidth();
        searchBox = new EditBox(font, left, SEARCH_TOP, panelWidth, 20, Component.translatable("dowwrey_beds.friend.select.search"));
        searchBox.setMaxLength(32);
        searchBox.setHint(Component.translatable("dowwrey_beds.friend.select.search_hint"));
        rebuilding = true;
        searchBox.setValue(currentSearch);
        rebuilding = false;
        searchBox.setResponder(value -> {
            if (rebuilding) {
                return;
            }
            page = 0;
            rebuildWidgets();
            searchBox.setFocused(true);
        });
        addRenderableWidget(searchBox);

        recalculatePages();
        List<OpenPlayerSelectPayload.PlayerEntry> filtered = filteredPlayers();
        int visible = visibleRows();
        int start = page * visible;
        int end = Math.min(start + visible, filtered.size());

        for (int i = start; i < end; i++) {
            OpenPlayerSelectPayload.PlayerEntry player = filtered.get(i);
            int y = LIST_TOP + (i - start) * (ROW_HEIGHT + GAP);
            boolean full = player.bedCount() >= player.bedLimit();
            Component label = Component.translatable(full
                    ? "dowwrey_beds.friend.select.player_full"
                    : "dowwrey_beds.friend.select.player_row", player.name(), player.bedCount(), player.bedLimit());
            Button row = Button.builder(label, button -> selectPlayer(player))
                    .bounds(left, y, panelWidth, 20)
                    .tooltip(Tooltip.create(Component.translatable(full
                            ? "dowwrey_beds.friend.select.full_tooltip"
                            : "dowwrey_beds.tooltip.friend_bed")))
                    .build();
            row.active = !full;
            addRenderableWidget(row);
        }

        int footerY = height - 28;
        int navWidth = Math.min(120, Math.max(82, (panelWidth - 8) / 2));
        addRenderableWidget(Button.builder(
                Component.translatable("dowwrey_beds.friend.select.cancel"),
                button -> onClose()
        ).bounds(left, footerY, navWidth, 20).build());

        addRenderableWidget(Button.builder(
                Component.translatable("dowwrey_beds.friend.select.page", page + 1, pageCount),
                button -> {
                    if (page + 1 < pageCount) {
                        page++;
                        rebuildWidgets();
                    }
                }
        ).bounds(left + panelWidth - navWidth, footerY, navWidth, 20).build());

        setInitialFocus(searchBox);
    }

    private void selectPlayer(OpenPlayerSelectPayload.PlayerEntry player) {
        this.minecraft.gui.setScreen(new ConfirmActionScreen(
                this,
                Component.translatable("dowwrey_beds.confirm.transfer_title"),
                Component.translatable("dowwrey_beds.confirm.transfer_body", bedName, player.name()),
                () -> {
                    DowwreyBedsClient.createFriendBed(bedPos, bedName, player.id(), color, icon);
                    this.minecraft.gui.setScreen(null);
                }
        ));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        int left = left();
        int panelWidth = panelWidth();
        int bottom = height - 12;
        graphics.fill(left - 12, TOP, left + panelWidth + 12, bottom, 0xE6161616);
        graphics.fill(left - 12, TOP, left + panelWidth + 12, TOP + 2, 0xFFD0B66A);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = left();
        int panelWidth = panelWidth();

        graphics.centeredText(font, title, width / 2, 55, 0xFFFFFFFF);
        graphics.fakeItem(BedIcons.itemStack(icon), width / 2 - 8, 82);
        graphics.centeredText(font, Component.translatable("dowwrey_beds.friend.select.for_bed", bedName), width / 2, 73, 0xFFC8C8C8);

        if (filteredPlayers().isEmpty()) {
            graphics.text(font, Component.translatable("dowwrey_beds.friend.select.empty"), width / 2, height / 2, 0xFFB8B8B8, true);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
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
