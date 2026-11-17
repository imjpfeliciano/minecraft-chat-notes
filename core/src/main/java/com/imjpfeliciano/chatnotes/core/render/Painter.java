package com.imjpfeliciano.chatnotes.core.render;

import com.imjpfeliciano.chatnotes.core.text.TextSpan;

import java.util.List;

/**
 * The drawing operations the screens need, and nothing else. Each Minecraft version implements
 * this over whatever replaced the last one's graphics type.
 */
public interface Painter {

    void text(String text, int x, int y, int argb);

    void centeredText(String text, int centerX, int y, int argb);

    /** Draws styled runs, clipped to {@code maxWidth} pixels. Runs with no colour use {@code fallbackArgb}. */
    void spans(List<TextSpan> spans, int x, int y, int maxWidth, int fallbackArgb);

    void fill(int x1, int y1, int x2, int y2, int argb);

    /** {@code (x, y, width, height)}, matching the old {@code renderOutline}, not {@link #fill}'s corner pairs. */
    void outline(int x, int y, int width, int height, int argb);
}
