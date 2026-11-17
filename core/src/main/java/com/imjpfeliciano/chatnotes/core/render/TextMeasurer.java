package com.imjpfeliciano.chatnotes.core.render;

/** Width and clipping for plain strings. Styled-run clipping stays inside {@link Painter#spans}. */
public interface TextMeasurer {

    int width(String text);

    int lineHeight();

    /** The longest prefix of {@code text} that fits in {@code maxWidth} pixels. */
    String clip(String text, int maxWidth);
}
