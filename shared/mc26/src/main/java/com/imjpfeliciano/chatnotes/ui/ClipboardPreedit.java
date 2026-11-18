package com.imjpfeliciano.chatnotes.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.input.PreeditEvent;

import java.util.List;

/**
 * macOS offers the current clipboard as a preedit when a text field is focused. That draws as a
 * paste button labelled with the clipboard text. Real composition, such as an IME, is left alone.
 */
final class ClipboardPreedit {

    private ClipboardPreedit() {}

    static boolean isClipboardSuggestion(PreeditEvent event) {
        if (event == null) {
            return false;
        }
        String clip = clipboard();
        if (clip.isEmpty()) {
            return false;
        }
        if (same(event.fullText(), clip)) {
            return true;
        }
        List<String> blocks = event.blocks();
        if (blocks == null) {
            return false;
        }
        for (String block : blocks) {
            if (same(block, clip)) {
                return true;
            }
        }
        return false;
    }

    private static boolean same(String shown, String clip) {
        if (shown == null) {
            return false;
        }
        return shown.strip().equals(clip);
    }

    private static String clipboard() {
        String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
        return clip == null ? "" : clip.strip();
    }
}
