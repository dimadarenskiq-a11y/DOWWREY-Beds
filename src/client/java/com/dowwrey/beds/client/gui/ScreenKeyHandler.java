package com.dowwrey.beds.client.gui;

import com.dowwrey.beds.client.DowwreyBedsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;

/** Shared keyboard behavior for DOWWREY screens. */
public final class ScreenKeyHandler {
    private ScreenKeyHandler() {}

    public static boolean handle(Screen screen, KeyEvent event) {
        if (event.isEscape()) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }

        if (DowwreyBedsClient.matchesOpenManagerKey(event)
                && !(screen.getFocused() instanceof EditBox)) {
            Minecraft.getInstance().gui.setScreen(null);
            return true;
        }

        return false;
    }
}
