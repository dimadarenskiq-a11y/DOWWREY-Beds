package com.dowwrey.beds.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ConfirmActionScreen extends Screen {
    private final Screen parent;
    private final Component body;
    private final Runnable confirmAction;
    public ConfirmActionScreen(Screen parent, Component title, Component body, Runnable confirmAction) {
        super(title); this.parent=parent; this.body=body; this.confirmAction=confirmAction;
    }
    private int panelWidth(){return Math.min(480,Math.max(300,width-32));}
    private int left(){return (width-panelWidth())/2;}
    private int top(){return Math.max(24,(height-150)/2);}
    @Override protected void init(){
        int l=left(),t=top(),w=panelWidth(),gap=8,bw=(w-gap)/2;
        addRenderableWidget(Button.builder(Component.translatable("dowwrey_beds.confirm.cancel"),b->onClose()).bounds(l,t+100,bw,20).build());
        addRenderableWidget(Button.builder(Component.translatable("dowwrey_beds.confirm.ok"),b->{confirmAction.run();}).bounds(l+bw+gap,t+100,bw,20).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float pt){
        super.extractBackground(g,mx,my,pt);int l=left(),t=top(),w=panelWidth();g.fill(Math.max(0,l-12),t,Math.min(width,l+w+12),t+134,0xE6161616);g.fill(Math.max(0,l-12),t,Math.min(width,l+w+12),t+2,0xFFD14C4C);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){
        g.centeredText(font,title,width/2,top()+18,0xFFFFFFFF);g.centeredText(font,body,width/2,top()+55,0xFFD0D0D0);super.extractRenderState(g,mx,my,pt);
    }
    @Override public boolean shouldCloseOnEsc(){return true;}
    @Override public void onClose(){minecraft.gui.setScreen(parent);}
}
