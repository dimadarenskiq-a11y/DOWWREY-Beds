package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.DowwreyBedsClient;
import com.dowwrey.beds.network.payload.BedListPayload;
import com.dowwrey.beds.storage.BedColors;
import com.dowwrey.beds.storage.BedIcons;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public final class EditBedScreen extends Screen {
    private static final int ICON_COLUMNS = 5;
    private static final int ICON_SIZE = 22;
    private static final int ICON_GAP = 6;
    private static final int SWATCH_SIZE = 20;
    private static final int SWATCH_GAP = 5;
    private final BedListPayload.BedEntry bed;
    private EditBox nameBox;
    private int selectedColor;
    private int selectedIcon;
    private final List<Button> iconButtons = new ArrayList<>();
    private final List<Button> colorButtons = new ArrayList<>();

    public EditBedScreen(BedListPayload.BedEntry bed) {
        super(Component.translatable("dowwrey_beds.edit.title"));
        this.bed = bed;
        this.selectedColor = BedColors.isAllowed(bed.color()) ? bed.color() : BedColors.DEFAULT;
        this.selectedIcon = BedIcons.sanitize(bed.icon());
    }

    private int panelWidth() { return Math.min(460, Math.max(300, width - 32)); }
    private int left() { return (width - panelWidth()) / 2; }
    private boolean compactLayout() { return height < 300; }
    private int panelHeight() { return compactLayout() ? 224 : 244; }
    private int panelTop() { return Math.max(compactLayout() ? 8 : 12, (height - panelHeight()) / 2); }

    @Override
    protected void init() {
        clearWidgets(); iconButtons.clear(); colorButtons.clear();
        int w = panelWidth(); int l = left(); int t = panelTop(); int gap = 8; int buttonWidth = (w-gap)/2;
        nameBox = new EditBox(font, l, t+(compactLayout()?40:48), w, 20, Component.translatable("dowwrey_beds.edit.name"));
        nameBox.setMaxLength(32); nameBox.setValue(bed.name()); addRenderableWidget(nameBox);

        int paletteWidth = BedColors.PALETTE.length * SWATCH_SIZE + (BedColors.PALETTE.length - 1) * SWATCH_GAP;
        int paletteLeft = l + (w - paletteWidth)/2;
        for (int i=0;i<BedColors.PALETTE.length;i++) {
            final int color=BedColors.PALETTE[i]; int x=paletteLeft+i*(SWATCH_SIZE+SWATCH_GAP);
            Button b=Button.builder(Component.literal(" "), ignored->selectedColor=color)
                    .bounds(x,t+(compactLayout()?106:120),SWATCH_SIZE,SWATCH_SIZE)
                    .tooltip(Tooltip.create(Component.translatable("dowwrey_beds.save_bed.color_choice"))).build();
            colorButtons.add(b); addRenderableWidget(b);
        }

        int iconGridWidth = ICON_COLUMNS*ICON_SIZE + (ICON_COLUMNS-1)*ICON_GAP;
        int iconLeft = l + (w-iconGridWidth)/2;
        for (int i=0;i<BedIcons.count();i++) {
            final int icon=i; int col=i%ICON_COLUMNS; int row=i/ICON_COLUMNS;
            int x=iconLeft+col*(ICON_SIZE+ICON_GAP); int y=t+(compactLayout()?142:157)+row*(ICON_SIZE+ICON_GAP);
            Button b=Button.builder(Component.literal(" "), ignored->selectedIcon=icon)
                    .bounds(x,y,ICON_SIZE,ICON_SIZE)
                    .tooltip(Tooltip.create(Component.translatable(BedIcons.translationKey(icon)))).build();
            iconButtons.add(b); addRenderableWidget(b);
        }

        int buttonY=Math.min(t+(compactLayout()?198:211),height-28);
        addRenderableWidget(Button.builder(Component.translatable("dowwrey_beds.edit.save"), ignored->save()).bounds(l,buttonY,buttonWidth,20).build());
                addRenderableWidget(Button.builder(Component.translatable("dowwrey_beds.edit.cancel"), ignored->onClose()).bounds(l+buttonWidth+gap,buttonY,buttonWidth,20).build());
        setInitialFocus(nameBox); nameBox.setFocused(true);
    }

    private void save() {
        String value=nameBox.getValue().trim();
        if(value.isEmpty()) { DowwreyBedsClient.clientNotification("dowwrey_beds.save_bed.empty_name",2,"",""); return; }
        if(!BedColors.isAllowed(selectedColor)||!BedIcons.isAllowed(selectedIcon)) return;
        DowwreyBedsClient.renameBed(bed.id(), value, selectedColor, selectedIcon);
        DowwreyBedsClient.requestBedList(true);
        onClose();
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){
        super.extractBackground(graphics,mouseX,mouseY,partialTick);
        int l=left(),t=panelTop(),w=panelWidth();
        graphics.fill(Math.max(0,l-12),t,Math.min(width,l+w+12),Math.min(t+panelHeight(),height-8),0xE6161616); graphics.fill(Math.max(0,l-12),t,Math.min(width,l+w+12),t+2,0xFFD0B66A);
    }

    @Override public void extractRenderState(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){
        int l=left(),t=panelTop();
        graphics.centeredText(font,title,width/2,t+(compactLayout()?11:15),0xFFFFFFFF);
        graphics.text(font,Component.translatable("dowwrey_beds.edit.name"),l,t+(compactLayout()?31:40),0xFFD0D0D0,false);
        graphics.centeredText(font,Component.translatable("dowwrey_beds.edit.color"),width/2,t+(compactLayout()?94:108),0xFFB8B8B8);
        graphics.centeredText(font,Component.translatable("dowwrey_beds.edit.icon"),width/2,t+(compactLayout()?130:145),0xFFB8B8B8);
        super.extractRenderState(graphics,mouseX,mouseY,partialTick);
        for(int i=0;i<iconButtons.size();i++){
            Button b=iconButtons.get(i); graphics.fakeItem(BedIcons.itemStack(i),b.getX()+3,b.getY()+3);
            if(i==selectedIcon)drawBorder(graphics,b.getX(),b.getY(),b.getWidth(),b.getHeight(),0xFFFFFFFF);
        }
        for(int i=0;i<colorButtons.size();i++){
            Button b=colorButtons.get(i); int c=BedColors.PALETTE[i]; graphics.fill(b.getX()+2,b.getY()+2,b.getRight()-2,b.getBottom()-2,c);
            if(c==selectedColor)drawBorder(graphics,b.getX(),b.getY(),b.getWidth(),b.getHeight(),0xFFFFFFFF);
        }
    }
    private static void drawBorder(GuiGraphicsExtractor g,int x,int y,int w,int h,int color){
        g.fill(x-1,y-1,x+w+1,y,color);g.fill(x-1,y+h,x+w+1,y+h+1,color);g.fill(x-1,y,x,y+h,color);g.fill(x+w,y,x+w+1,y+h,color);
    }
    @Override public boolean shouldCloseOnEsc(){return true;}
    @Override public void onClose(){this.minecraft.gui.setScreen(null);}
    @Override public boolean keyPressed(KeyEvent event){if(ScreenKeyHandler.handle(this,event))return true;return super.keyPressed(event);}
}
