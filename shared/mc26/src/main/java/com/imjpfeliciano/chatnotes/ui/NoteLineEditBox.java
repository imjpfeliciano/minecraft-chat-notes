package com.imjpfeliciano.chatnotes.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.PreeditEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Title field. A double-click takes the word under the pointer, a triple-click takes the whole
 * line, and both Ctrl and Cmd select all.
 */
final class NoteLineEditBox extends EditBox {

    private static final long CLICK_WINDOW_MILLIS = 400L;
    private static final int EDIT_SHORTCUTS = InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER;

    private int clicks;
    private long lastClickMillis;
    private double lastClickX = Double.NaN;
    private double lastClickY = Double.NaN;

    NoteLineEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        long now = Util.getMillis();
        boolean burst = now - lastClickMillis <= CLICK_WINDOW_MILLIS
                && Math.abs(event.x() - lastClickX) <= 3.0
                && Math.abs(event.y() - lastClickY) <= 3.0;
        clicks = burst ? clicks + 1 : 1;
        lastClickMillis = now;
        lastClickX = event.x();
        lastClickY = event.y();
        if (clicks >= 3) {
            selectAllText();
            clicks = 0;
            return;
        }
        super.onClick(event, false);
        if (clicks == 2) {
            selectWordAtCursor();
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (isSelectAll(event)) {
            selectAllText();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean preeditUpdated(PreeditEvent event) {
        if (ClipboardPreedit.isClipboardSuggestion(event)) {
            return super.preeditUpdated(null);
        }
        return super.preeditUpdated(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        if (KeyShortcuts.shortcutDown()) {
            return true;
        }
        return super.charTyped(event);
    }

    private void selectAllText() {
        String value = getValue() == null ? "" : getValue();
        setHighlightPos(value.length());
        setCursorPosition(0);
    }

    private void selectWordAtCursor() {
        String value = getValue() == null ? "" : getValue();
        int cursor = clamp(getCursorPosition(), value.length());
        int start = cursor;
        int end = cursor;
        while (start > 0 && !isWordBreak(value.charAt(start - 1))) {
            start--;
        }
        while (end < value.length() && !isWordBreak(value.charAt(end))) {
            end++;
        }
        if (start == end && cursor < value.length() && isWordBreak(value.charAt(cursor))) {
            while (start > 0 && isWordBreak(value.charAt(start - 1))) {
                start--;
            }
            while (start > 0 && !isWordBreak(value.charAt(start - 1))) {
                start--;
            }
            end = start;
            while (end < value.length() && !isWordBreak(value.charAt(end))) {
                end++;
            }
        }
        setHighlightPos(start);
        setCursorPosition(end);
    }

    private static boolean isSelectAll(KeyEvent event) {
        return event.key() == InputConstants.KEY_A
                && !event.hasShiftDown()
                && !event.hasAltDown()
                && (event.modifiers() & EDIT_SHORTCUTS) != 0;
    }

    /** Spaces split words. Format codes stay with the word they decorate. */
    private static boolean isWordBreak(char character) {
        return Character.isWhitespace(character);
    }

    private static int clamp(int index, int length) {
        if (index < 0) {
            return 0;
        }
        return Math.min(index, length);
    }
}
