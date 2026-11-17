package com.imjpfeliciano.chatnotes.mc;

import com.imjpfeliciano.chatnotes.core.render.Painter;
import com.imjpfeliciano.chatnotes.core.text.TextSpan;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/** {@link Painter} over the 1.21 {@code GuiGraphics} / {@code drawString} API. */
public final class GuiGraphicsPainter implements Painter {

    private final GuiGraphics graphics;
    private final Font font;

    public GuiGraphicsPainter(GuiGraphics graphics, Font font) {
        this.graphics = graphics;
        this.font = font;
    }

    @Override
    public void text(String text, int x, int y, int argb) {
        graphics.drawString(font, text, x, y, argb);
    }

    @Override
    public void centeredText(String text, int centerX, int y, int argb) {
        graphics.drawCenteredString(font, text, centerX, y, argb);
    }

    @Override
    public void spans(List<TextSpan> spans, int x, int y, int maxWidth, int fallbackArgb) {
        if (spans == null || spans.isEmpty() || maxWidth <= 0) {
            return;
        }
        MutableComponent root = Component.empty();
        for (TextSpan span : spans) {
            if (span.text().isEmpty()) {
                continue;
            }
            root.append(Component.literal(span.text()).setStyle(styleOf(span)));
        }
        FormattedCharSequence ordered = Language.getInstance().getVisualOrder(font.substrByWidth(root, maxWidth));
        graphics.drawString(font, ordered, x, y, fallbackArgb);
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) {
        graphics.fill(x1, y1, x2, y2, argb);
    }

    @Override
    public void outline(int x, int y, int width, int height, int argb) {
        graphics.renderOutline(x, y, width, height, argb);
    }

    private static Style styleOf(TextSpan span) {
        Style style = Style.EMPTY;
        if (span.color() != null) {
            style = style.withColor(TextColor.fromRgb(span.color()));
        }
        if (span.bold()) {
            style = style.withBold(true);
        }
        if (span.italic()) {
            style = style.withItalic(true);
        }
        if (span.underlined()) {
            style = style.withUnderlined(true);
        }
        if (span.strikethrough()) {
            style = style.withStrikethrough(true);
        }
        if (span.obfuscated()) {
            style = style.withObfuscated(true);
        }
        return style;
    }
}
