package com.imjpfeliciano.chatnotes.ui;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

/**
 * Single-line field with the selection the editor needs: a double-click takes the word under the
 * pointer, a triple-click takes the whole line, and both Ctrl and Cmd select all.
 */
final class NoteLineEditBox extends EditBox {

    private static final long CLICK_WINDOW_MILLIS = 400L;
    private static final int EDIT_SHORTCUTS = InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER;

    private int clicks;
    private long lastClickMillis;
    private double lastClickX = Double.NaN;
    private double lastClickY = Double.NaN;
    private StyledWidths styledWidths;

    NoteLineEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
    }

    /** When set, clicks, drags, and the selection use this width instead of the plain font width. */
    void setStyledWidths(StyledWidths styledWidths) {
        this.styledWidths = styledWidths;
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cursor = getCursorPosition();
        int highlight = highlightPos;
        boolean selected = styledWidths != null && cursor != highlight;
        if (selected) {
            setHighlightPos(cursor);
        }
        graphics.enableScissor(getX(), getY(), getX() + getWidth(), getY() + getHeight());
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.disableScissor();
        if (!selected) {
            return;
        }
        setHighlightPos(highlight);
        drawSelection(graphics, cursor, highlight);
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (styledWidths == null) {
            super.onDrag(event, dragX, dragY);
            return;
        }
        setCursorPosition(indexAt(event.x()));
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
        if (styledWidths != null) {
            int index = indexAt(event.x());
            if (event.hasShiftDown()) {
                setCursorPosition(index);
            } else {
                setHighlightPos(index);
                setCursorPosition(index);
            }
        }
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
    public boolean charTyped(CharacterEvent event) {
        if ((event.modifiers() & EDIT_SHORTCUTS) != 0) {
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

    private int indexAt(double mouseX) {
        String value = getValue() == null ? "" : getValue();
        int origin = clamp(displayPos, value.length());
        int target = (int) Math.round(mouseX) - (getX() + 4);
        int best = origin;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = origin; index <= value.length(); index++) {
            int width = styledWidths.width(value, origin, index);
            int distance = Math.abs(width - target);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = index;
            } else if (width > target) {
                break;
            }
        }
        return best;
    }

    private void drawSelection(GuiGraphics graphics, int cursor, int highlight) {
        String value = getValue() == null ? "" : getValue();
        int start = Math.min(cursor, highlight);
        int end = Math.max(cursor, highlight);
        int origin = clamp(displayPos, value.length());
        if (end <= origin) {
            return;
        }
        int from = Math.max(start, origin);
        int textStart = getX() + 4;
        int innerRight = getX() + getWidth() - 4;
        int x1 = clampPixel(textStart + styledWidths.width(value, origin, from), textStart, innerRight);
        int x2 = clampPixel(textStart + styledWidths.width(value, origin, end), textStart, innerRight);
        if (x2 <= x1) {
            return;
        }
        int textY = getY() + (getHeight() - 8) / 2;
        graphics.textHighlight(x1, textY - 1, x2 - 1, textY + 9, false);
    }

    private static int clampPixel(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static int clamp(int index, int length) {
        if (index < 0) {
            return 0;
        }
        return Math.min(index, length);
    }

    @FunctionalInterface
    interface StyledWidths {
        int width(String value, int from, int to);
    }
}
