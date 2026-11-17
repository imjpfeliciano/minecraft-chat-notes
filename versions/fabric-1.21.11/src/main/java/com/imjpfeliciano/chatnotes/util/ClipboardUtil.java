package com.imjpfeliciano.chatnotes.util;

import net.minecraft.client.Minecraft;

/**
 * Thin wrapper over Minecraft's clipboard so the rest of the mod never touches
 * {@code keyboardHandler} directly. Keeps the Phase 6 / backport surface small.
 */
public final class ClipboardUtil {

    private ClipboardUtil() {}

    public static String get() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    public static void set(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text == null ? "" : text);
    }
}
