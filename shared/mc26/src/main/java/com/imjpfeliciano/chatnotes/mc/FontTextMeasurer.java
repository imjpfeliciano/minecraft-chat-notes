package com.imjpfeliciano.chatnotes.mc;

import com.imjpfeliciano.chatnotes.core.render.TextMeasurer;
import net.minecraft.client.gui.Font;

public final class FontTextMeasurer implements TextMeasurer {

    private final Font font;

    public FontTextMeasurer(Font font) {
        this.font = font;
    }

    @Override
    public int width(String text) {
        return font.width(text == null ? "" : text);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }

    @Override
    public String clip(String text, int maxWidth) {
        return font.plainSubstrByWidth(text == null ? "" : text, maxWidth);
    }
}
