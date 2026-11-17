package com.imjpfeliciano.chatnotes.mc;

import com.imjpfeliciano.chatnotes.core.platform.ClipboardAccess;
import net.minecraft.client.Minecraft;

/** The clipboard behind {@code Minecraft.keyboardHandler}, which is unchanged across 1.21. */
public final class MinecraftClipboard implements ClipboardAccess {

    @Override
    public String get() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void set(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text == null ? "" : text);
    }
}
