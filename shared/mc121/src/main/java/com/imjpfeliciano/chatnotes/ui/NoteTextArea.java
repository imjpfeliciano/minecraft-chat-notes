package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.text.LineWrap;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

/**
 * A three-line chat field. Enter does not insert a line break, because the saved message is one
 * chat line and the box wraps it. An optional {@link StyledText} paints the input in place and
 * wraps with those same widths, so bold glyphs move onto the next row instead of being clipped.
 */
final class NoteTextArea extends MultiLineEditBox {

    private static final int TEXT_COLOR = -2039584;
    private static final int CURSOR_COLOR = -3092272;
    private static final long CLICK_WINDOW_MILLIS = 400L;
    private static final int EDIT_SHORTCUTS = InputConstants.MOD_CONTROL | InputConstants.MOD_SUPER;

    private final Font textFont;
    private StyledText styledText;
    private int clicks;
    private long lastClickMillis;
    private double lastClickX = Double.NaN;
    private double lastClickY = Double.NaN;

    NoteTextArea(Font font, int x, int y, int width, int height, Component message, Component placeholder) {
        super(font, x, y, width, height, placeholder, message, TEXT_COLOR, true, CURSOR_COLOR, true, true);
        this.textFont = font;
    }

    void setStyledText(StyledText styledText) {
        this.styledText = styledText;
        reflowStyledLines();
    }

    @Override
    public void setValue(String value) {
        super.setValue(value);
        reflowStyledLines();
    }

    /**
     * Vanilla rows are measured with an empty style, and bold is a pixel wider than that, so a
     * long coloured line paints past the padding and the scissor cuts it off. Replace those rows
     * with breaks from the widths this field actually draws.
     */
    void reflowStyledLines() {
        if (styledText == null) {
            return;
        }
        String value = textField.value();
        int maxWidth = Math.max(1, getWidth() - totalInnerPadding());
        textField.displayLines.clear();
        for (LineWrap.Line line : LineWrap.wrap(value, maxWidth, (from, to) -> styledText.width(value, from, to))) {
            textField.displayLines.add(new MultilineTextField.StringView(line.begin(), line.end()));
        }
        scrollCaretIntoView();
    }

    int[] selection() {
        if (!textField.hasSelection()) {
            int cursor = textField.cursor();
            return new int[] {cursor, cursor};
        }
        MultilineTextField.StringView view = textField.getSelected();
        return new int[] {view.beginIndex(), view.endIndex()};
    }

    void moveCursorTo(int index) {
        textField.setSelecting(false);
        textField.seekCursor(Whence.ABSOLUTE, index);
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
        if (styledText == null) {
            super.onClick(event, clicks >= 2);
            return;
        }
        if (clicks >= 2) {
            textField.selectWordAtCursor();
            return;
        }
        textField.setSelecting(event.hasShiftDown());
        seekStyled(event.x(), event.y());
    }

    @Override
    protected void onDrag(MouseButtonEvent event, double dragX, double dragY) {
        if (styledText == null) {
            super.onDrag(event, dragX, dragY);
            return;
        }
        textField.setSelecting(true);
        seekStyled(event.x(), event.y());
        textField.setSelecting(event.hasShiftDown());
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == InputConstants.KEY_RETURN || event.key() == InputConstants.KEY_NUMPADENTER) {
            return true;
        }
        if (isSelectAll(event)) {
            selectAllText();
            return true;
        }
        if (styledText != null && (event.key() == InputConstants.KEY_UP || event.key() == InputConstants.KEY_DOWN)) {
            if (!event.hasControlDownWithQuirk()) {
                moveStyledLine(event.key() == InputConstants.KEY_UP ? -1 : 1, event.hasShiftDown());
            }
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

    @Override
    protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (styledText == null || textField.value().isEmpty()) {
            super.renderContents(graphics, mouseX, mouseY, partialTick);
            return;
        }
        drawStyled(graphics);
    }

    private void drawStyled(GuiGraphics graphics) {
        String value = textField.value();
        int cursor = textField.cursor();
        boolean showCursor = isFocused() && (Util.getMillis() / 300L) % 2L == 0L;
        boolean cursorInside = cursor < value.length();
        int lineHeight = textFont.lineHeight;
        int y = getInnerTop();
        int endX = getInnerLeft();
        int endY = y;
        boolean cursorDrawn = false;
        for (MultilineTextField.StringView line : textField.iterateLines()) {
            boolean visible = withinContentAreaTopBottom(y, y + lineHeight);
            endY = y;
            if (visible) {
                int x = getInnerLeft();
                if (showCursor && cursorInside && cursor >= line.beginIndex() && cursor <= line.endIndex() && !cursorDrawn) {
                    drawRange(graphics, value, line.beginIndex(), cursor, x, y);
                    x += widthOf(value, line.beginIndex(), cursor);
                    graphics.fill(x, y - 1, x + 1, y + lineHeight, CURSOR_COLOR);
                    cursorDrawn = true;
                    drawRange(graphics, value, cursor, line.endIndex(), x, y);
                } else {
                    drawRange(graphics, value, line.beginIndex(), line.endIndex(), x, y);
                }
                endX = getInnerLeft() + widthOf(value, line.beginIndex(), line.endIndex());
            }
            y += lineHeight;
        }
        if (showCursor && !cursorInside && withinContentAreaTopBottom(endY, endY + lineHeight)) {
            graphics.fill(endX, endY - 1, endX + 1, endY + lineHeight, CURSOR_COLOR);
        }
        if (!textField.hasSelection()) {
            return;
        }
        MultilineTextField.StringView selected = textField.getSelected();
        int highlightY = getInnerTop();
        int left = getInnerLeft();
        int innerRight = getX() + getWidth() - innerPadding();
        for (MultilineTextField.StringView line : textField.iterateLines()) {
            if (selected.beginIndex() >= line.endIndex()) {
                highlightY += lineHeight;
                continue;
            }
            if (line.beginIndex() >= selected.endIndex()) {
                break;
            }
            if (withinContentAreaTopBottom(highlightY, highlightY + lineHeight)) {
                int from = Math.max(line.beginIndex(), selected.beginIndex());
                int to = Math.min(line.endIndex(), selected.endIndex());
                int x1 = left + widthOf(value, line.beginIndex(), from);
                int x2 = to >= line.endIndex() && selected.endIndex() > line.endIndex()
                        ? innerRight
                        : left + widthOf(value, line.beginIndex(), to);
                if (x2 > x1) {
                    graphics.textHighlight(x1, highlightY, x2, highlightY + lineHeight, false);
                }
            }
            highlightY += lineHeight;
        }
    }

    private void drawRange(GuiGraphics graphics, String value, int from, int to, int x, int y) {
        if (from >= to) {
            return;
        }
        FormattedCharSequence sequence = styledText.format(value, from, to);
        if (sequence == null) {
            graphics.drawString(textFont, value.substring(from, to), x, y, TEXT_COLOR, true);
            return;
        }
        graphics.drawString(textFont, sequence, x, y, TEXT_COLOR, true);
    }

    private int widthOf(String value, int from, int to) {
        if (from >= to) {
            return 0;
        }
        return styledText.width(value, from, to);
    }

    /**
     * Vanilla line hits use the plain font width. Bold glyphs are wider, so a click measured that
     * way lands past the character under the pointer. This uses the same widths the field draws.
     */
    private void seekStyled(double mouseX, double mouseY) {
        String value = textField.value();
        double localX = mouseX - getX() - innerPadding();
        double localY = mouseY - getY() - innerPadding() + scrollAmount();
        int targetLine = (int) Math.floor(localY / textFont.lineHeight);
        int line = 0;
        MultilineTextField.StringView chosen = null;
        int count = 0;
        for (MultilineTextField.StringView view : textField.iterateLines()) {
            if (line == targetLine) {
                chosen = view;
            }
            line++;
            count++;
        }
        if (count == 0 || targetLine < 0) {
            textField.seekCursor(Whence.ABSOLUTE, 0);
            return;
        }
        if (chosen == null) {
            textField.seekCursor(Whence.ABSOLUTE, value.length());
            return;
        }
        int target = (int) Math.floor(localX);
        textField.seekCursor(Whence.ABSOLUTE, closestIndex(value, chosen.beginIndex(), chosen.endIndex(), target));
    }

    private void moveStyledLine(int delta, boolean selecting) {
        String value = textField.value();
        int count = textField.getLineCount();
        if (count == 0) {
            return;
        }
        int line = textField.getLineAtCursor();
        if (line < 0) {
            line = delta < 0 ? 0 : count - 1;
        }
        int targetLine = Math.max(0, Math.min(count - 1, line + delta));
        MultilineTextField.StringView current = textField.getLineView(line);
        MultilineTextField.StringView target = textField.getLineView(targetLine);
        int x = widthOf(value, current.beginIndex(), Math.min(textField.cursor(), current.endIndex()));
        textField.setSelecting(selecting);
        textField.seekCursor(Whence.ABSOLUTE, closestIndex(value, target.beginIndex(), target.endIndex(), x));
    }

    private int closestIndex(String value, int begin, int end, int target) {
        int best = begin;
        int bestDistance = Integer.MAX_VALUE;
        for (int index = begin; index <= end; index++) {
            int width = widthOf(value, begin, index);
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

    private void scrollCaretIntoView() {
        int line = textField.getLineAtCursor();
        if (line < 0) {
            return;
        }
        double scroll = scrollAmount();
        MultilineTextField.StringView top = textField.getLineView((int) (scroll / 9.0));
        if (textField.cursor() <= top.beginIndex()) {
            setScrollAmount(line * 9.0);
            return;
        }
        MultilineTextField.StringView bottom = textField.getLineView((int) ((scroll + getHeight()) / 9.0) - 1);
        if (textField.cursor() > bottom.endIndex()) {
            setScrollAmount(line * 9.0 - getHeight() + 9.0 + totalInnerPadding());
        }
    }

    private void selectAllText() {
        String value = textField.value();
        textField.setSelecting(false);
        textField.seekCursor(Whence.ABSOLUTE, 0);
        textField.setSelecting(true);
        textField.seekCursor(Whence.ABSOLUTE, value.length());
    }

    private static boolean isSelectAll(KeyEvent event) {
        return event.key() == InputConstants.KEY_A
                && !event.hasShiftDown()
                && !event.hasAltDown()
                && (event.modifiers() & EDIT_SHORTCUTS) != 0;
    }

    interface StyledText {
        FormattedCharSequence format(String value, int from, int to);

        int width(String value, int from, int to);
    }
}
