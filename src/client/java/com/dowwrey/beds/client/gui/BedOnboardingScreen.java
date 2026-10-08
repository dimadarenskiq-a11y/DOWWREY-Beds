package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.DowwreyBedsClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class BedOnboardingScreen extends Screen {
    private final Screen parent;
    public BedOnboardingScreen(Screen parent) { super(Component.translatable("dowwrey_beds.onboarding.title")); this.parent = parent; }
    private int panelWidth(){ return Math.min(520, Math.max(300, width-32)); }
    private int left(){ return (width-panelWidth())/2; }
    private int top(){ return Math.max(20,(height-190)/2); }
    @Override protected void init(){
        int l=left(), t=top(), w=panelWidth();
        addRenderableWidget(Button.builder(Component.translatable("dowwrey_beds.onboarding.ok"), b->{DowwreyBedsClient.config().markOnboardingSeen(); onClose();})
                .bounds(l+(w-140)/2,t+140,140,20).build());
    }
    @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float pt){
        super.extractBackground(g,mx,my,pt); int l=left(),t=top(),w=panelWidth();
        g.fill(Math.max(0,l-12),t,Math.min(width,l+w+12),t+174,0xE6141414); g.fill(Math.max(0,l-12),t,Math.min(width,l+w+12),t+2,0xFFD0B66A);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float pt){
        int l=left(),t=top(),w=panelWidth();
        g.centeredText(font,title,width/2,t+18,0xFFFFFFFF);
        g.centeredText(font,Component.translatable("dowwrey_beds.onboarding.line1"),width/2,t+54,0xFFD8D8D8);
        g.centeredText(font,Component.translatable("dowwrey_beds.onboarding.line2"),width/2,t+75,0xFFB8B8B8);
        g.centeredText(font,Component.translatable("dowwrey_beds.onboarding.line3"),width/2,t+96,0xFFB8B8B8);
        g.centeredText(font,Component.translatable("dowwrey_beds.onboarding.note"),width/2,t+116,0xFF8F8F8F);
        super.extractRenderState(g,mx,my,pt);
    }
    @Override public boolean shouldCloseOnEsc(){return true;}
    @Override public void onClose(){this.minecraft.gui.setScreen(parent);}
}
