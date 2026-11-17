package com.imjpfeliciano.chatnotes.core.text;

/**
 * One run of characters that share a style. {@code color} is a {@code 0xRRGGBB} value, or
 * {@code null} when the run should be drawn in the caller's fallback colour. Flags mirror the
 * legacy formatting codes ({@code &l}, {@code &o}, {@code &n}, {@code &m}, {@code &k}).
 */
public record TextSpan(
        String text,
        Integer color,
        boolean bold,
        boolean italic,
        boolean underlined,
        boolean strikethrough,
        boolean obfuscated) {

    public TextSpan {
        text = text == null ? "" : text;
    }
}
