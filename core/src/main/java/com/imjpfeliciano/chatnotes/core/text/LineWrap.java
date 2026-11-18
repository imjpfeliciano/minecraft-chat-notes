package com.imjpfeliciano.chatnotes.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Breaks one chat line into rows that fit a pixel width. Breaks prefer a space, matching
 * Minecraft's splitter: the space itself is not drawn on either row. A word with no space
 * breaks on the glyph that would pass the width, except the first glyph of a row, which stays
 * even when it is wider than the row.
 */
public final class LineWrap {

    private LineWrap() {}

    public record Line(int begin, int end) {}

    @FunctionalInterface
    public interface Width {
        /** Pixel width of {@code text.substring(from, to)}. {@code 0} when {@code from >= to}. */
        int of(int from, int to);
    }

    public static List<Line> wrap(String text, int maxWidth, Width width) {
        if (text == null || text.isEmpty()) {
            return List.of(new Line(0, 0));
        }
        int limit = Math.max(1, maxWidth);
        List<Line> lines = new ArrayList<>();
        int start = 0;
        int length = text.length();
        while (start < length) {
            int end = breakAt(text, start, limit, width);
            lines.add(new Line(start, end));
            if (end >= length) {
                break;
            }
            int next = end;
            int codePoint = text.codePointAt(end);
            if (codePoint == '\n' || codePoint == ' ') {
                next = end + Character.charCount(codePoint);
            }
            if (next <= start) {
                next = start + Character.charCount(text.codePointAt(start));
            }
            start = next;
        }
        if (text.charAt(length - 1) == '\n') {
            lines.add(new Line(length, length));
        }
        return List.copyOf(lines);
    }

    private static int breakAt(String text, int start, int maxWidth, Width width) {
        int lastSpace = -1;
        boolean hadNonZero = false;
        int index = start;
        int length = text.length();
        while (index < length) {
            int codePoint = text.codePointAt(index);
            if (codePoint == '\n') {
                return index;
            }
            if (codePoint == ' ') {
                lastSpace = index;
            }
            int next = index + Character.charCount(codePoint);
            if (hadNonZero && width.of(start, next) > maxWidth) {
                return lastSpace >= start ? lastSpace : index;
            }
            hadNonZero |= width.of(index, next) != 0;
            index = next;
        }
        return length;
    }
}
