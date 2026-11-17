package com.imjpfeliciano.chatnotes.core.text;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Plain characters plus the style on each run, and the {@code &} string chat will receive.
 *
 * <p>Indexes are offsets into {@link #plain()}, not into the code string. A colour button sets
 * one of the 16 legacy colours and keeps decorations; the serializer writes the colour code
 * first ({@code &e&l}) because a colour code would otherwise clear them. {@link #serialize()}
 * returns the original text until {@link #apply(int, int, TextFormat)}, {@link #insert},
 * {@link #delete}, or {@link #paste} changes it, so an untouched {@code &#} or {@code &x} note
 * stays byte-for-byte.
 */
public final class FormattedDocument {

    private static final String CODES = "0123456789abcdef";

    /** Same RGB values as {@link AmpersandParser}, in code order {@code 0-9a-f}. */
    private static final int[] PALETTE = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private final String original;
    private final List<Run> runs = new ArrayList<>();
    private boolean pristine;
    private Style pendingStyle;
    private int pendingIndex = -1;

    private FormattedDocument(String original, boolean pristine) {
        this.original = original == null ? "" : original;
        this.pristine = pristine;
    }

    public static FormattedDocument parse(String text) {
        FormattedDocument document = new FormattedDocument(text, true);
        for (TextSpan span : AmpersandParser.parse(text)) {
            if (!span.text().isEmpty()) {
                document.runs.add(new Run(span.text(), Style.from(span)));
            }
        }
        return document;
    }

    /** Visible characters, with formatting codes removed. */
    public String plain() {
        StringBuilder out = new StringBuilder();
        for (Run run : runs) {
            out.append(run.text);
        }
        return out.toString();
    }

    /** Plain runs in order, so the editor can paint the same styles the chat string stores. */
    public List<TextSpan> spans() {
        List<TextSpan> spans = new ArrayList<>(runs.size());
        for (Run run : runs) {
            if (run.text.isEmpty()) {
                continue;
            }
            Style style = run.style;
            spans.add(new TextSpan(
                    run.text,
                    style.color(),
                    style.bold(),
                    style.italic(),
                    style.underlined(),
                    style.strikethrough(),
                    style.obfuscated()));
        }
        return List.copyOf(spans);
    }

    /**
     * The chat string. Untouched notes return the text they were parsed from. After an edit,
     * the result uses only {@code &0}-{@code &f}, {@code &k} {@code &l} {@code &m} {@code &n}
     * {@code &o}, and {@code &r}.
     */
    public String serialize() {
        if (pristine) {
            return original;
        }
        StringBuilder out = new StringBuilder();
        Style cursor = Style.NONE;
        for (Run run : runs) {
            if (run.text.isEmpty()) {
                continue;
            }
            out.append(cursor.transitionTo(run.style));
            out.append(escape(run.text));
            cursor = run.style;
        }
        return out.toString();
    }

    /**
     * Restyle {@code [start, end)}. An empty range does not change the text; the next
     * {@link #insert} at that index uses the style. Reset is the exception: with no
     * selection it clears formatting on the whole message.
     */
    public void apply(int start, int end, TextFormat format) {
        int[] range = clamp(start, end);
        start = range[0];
        end = range[1];
        if (start == end) {
            if (format == TextFormat.RESET) {
                resetAll();
                return;
            }
            armCaret(start, format);
            return;
        }
        pristine = false;
        clearPending();
        boolean turnOn = !format.isDecoration() || !rangeAllHave(start, end, format);
        split(start);
        split(end);
        int offset = 0;
        for (Run run : runs) {
            int next = offset + run.text.length();
            if (offset >= start && next <= end) {
                run.style = run.style.transformed(format, turnOn);
            }
            offset = next;
        }
        merge();
    }

    /** Insert plain characters at {@code index}, inheriting the caret style. */
    public void insert(int index, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        index = clampIndex(index);
        Style style;
        if (pendingStyle != null && pendingIndex == index) {
            style = pendingStyle;
            pendingIndex = index + text.length();
        } else {
            clearPending();
            style = styleBefore(index);
        }
        pristine = false;
        insertAt(index, text, style);
        merge();
    }

    /** Delete plain characters in {@code [start, end)}. */
    public void delete(int start, int end) {
        int[] range = clamp(start, end);
        if (range[0] == range[1]) {
            return;
        }
        pristine = false;
        shiftPending(range[0], range[1]);
        split(range[0]);
        split(range[1]);
        int offset = 0;
        for (int i = 0; i < runs.size(); ) {
            Run run = runs.get(i);
            int next = offset + run.text.length();
            if (offset >= range[0] && next <= range[1]) {
                runs.remove(i);
            } else {
                i++;
            }
            offset = next;
        }
        merge();
    }

    /**
     * Insert clipboard text at {@code index}. A string that contains {@code &} or {@code §}
     * is parsed as formatting; anything else is inserted as plain characters.
     */
    public void paste(int index, String clipboard) {
        if (clipboard == null || clipboard.isEmpty()) {
            return;
        }
        index = clampIndex(index);
        if (clipboard.indexOf('&') < 0 && clipboard.indexOf('§') < 0) {
            insert(index, clipboard);
            return;
        }
        pristine = false;
        clearPending();
        for (Run run : parse(clipboard).runs) {
            insertAt(index, run.text, run.style);
            index += run.text.length();
        }
        merge();
    }

    private void resetAll() {
        pristine = false;
        clearPending();
        for (Run run : runs) {
            run.style = Style.NONE;
        }
        merge();
    }

    private void armCaret(int index, TextFormat format) {
        Style base = pendingStyle != null && pendingIndex == index ? pendingStyle : styleBefore(index);
        boolean turnOn = !format.isDecoration() || !base.has(format);
        pendingStyle = base.transformed(format, turnOn);
        pendingIndex = index;
    }

    private void clearPending() {
        pendingStyle = null;
        pendingIndex = -1;
    }

    private void shiftPending(int start, int end) {
        if (pendingStyle == null) {
            return;
        }
        if (pendingIndex >= end) {
            pendingIndex -= end - start;
        } else if (pendingIndex > start) {
            clearPending();
        }
    }

    private void insertAt(int index, String text, Style style) {
        split(index);
        int offset = 0;
        for (int i = 0; i < runs.size(); i++) {
            if (offset == index) {
                runs.add(i, new Run(text, style));
                return;
            }
            offset += runs.get(i).text.length();
        }
        runs.add(new Run(text, style));
    }

    private void split(int index) {
        int offset = 0;
        for (int i = 0; i < runs.size(); i++) {
            Run run = runs.get(i);
            int next = offset + run.text.length();
            if (index <= offset) {
                return;
            }
            if (index < next) {
                int local = index - offset;
                runs.add(i + 1, new Run(run.text.substring(local), run.style));
                run.text = run.text.substring(0, local);
                return;
            }
            offset = next;
        }
    }

    private void merge() {
        for (int i = 0; i < runs.size(); ) {
            Run run = runs.get(i);
            if (run.text.isEmpty()) {
                runs.remove(i);
                continue;
            }
            if (i > 0 && runs.get(i - 1).style.equals(run.style)) {
                runs.get(i - 1).text = runs.get(i - 1).text + run.text;
                runs.remove(i);
                continue;
            }
            i++;
        }
    }

    private boolean rangeAllHave(int start, int end, TextFormat format) {
        int offset = 0;
        boolean saw = false;
        for (Run run : runs) {
            int next = offset + run.text.length();
            if (next > start && offset < end) {
                saw = true;
                if (!run.style.has(format)) {
                    return false;
                }
            }
            offset = next;
        }
        return saw;
    }

    private Style styleBefore(int index) {
        int offset = 0;
        Style previous = Style.NONE;
        for (Run run : runs) {
            int next = offset + run.text.length();
            if (index <= offset) {
                return previous;
            }
            if (index <= next) {
                return run.style;
            }
            previous = run.style;
            offset = next;
        }
        return previous;
    }

    private int[] clamp(int start, int end) {
        if (start > end) {
            int swap = start;
            start = end;
            end = swap;
        }
        int length = plainLength();
        return new int[] {clampIndex(start, length), clampIndex(end, length)};
    }

    private int clampIndex(int index) {
        return clampIndex(index, plainLength());
    }

    private static int clampIndex(int index, int length) {
        if (index < 0) {
            return 0;
        }
        return Math.min(index, length);
    }

    private int plainLength() {
        int length = 0;
        for (Run run : runs) {
            length += run.text.length();
        }
        return length;
    }

    private static String escape(String text) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '&' || c == '§') {
                out.append(c).append(c);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static int rgbOf(char code) {
        int index = CODES.indexOf(code);
        if (index < 0) {
            throw new IllegalArgumentException("Not a legacy colour: " + code);
        }
        return PALETTE[index];
    }

    private static char legacyCode(int rgb) {
        int exact = -1;
        int nearest = 0;
        long nearestDistance = Long.MAX_VALUE;
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == rgb) {
                exact = i;
                break;
            }
            long distance = distance(rgb, PALETTE[i]);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = i;
            }
        }
        return CODES.charAt(exact >= 0 ? exact : nearest);
    }

    private static long distance(int left, int right) {
        long red = ((left >> 16) & 0xFF) - ((right >> 16) & 0xFF);
        long green = ((left >> 8) & 0xFF) - ((right >> 8) & 0xFF);
        long blue = (left & 0xFF) - (right & 0xFF);
        return red * red + green * green + blue * blue;
    }

    private static final class Run {
        private String text;
        private Style style;

        private Run(String text, Style style) {
            this.text = text;
            this.style = style;
        }
    }

    private record Style(
            Integer color,
            boolean bold,
            boolean italic,
            boolean underlined,
            boolean strikethrough,
            boolean obfuscated) {

        private static final Style NONE = new Style(null, false, false, false, false, false);

        private static Style from(TextSpan span) {
            return new Style(
                    span.color(),
                    span.bold(),
                    span.italic(),
                    span.underlined(),
                    span.strikethrough(),
                    span.obfuscated());
        }

        private boolean isNone() {
            return color == null && !bold && !italic && !underlined && !strikethrough && !obfuscated;
        }

        private boolean has(TextFormat format) {
            return switch (format) {
                case BOLD -> bold;
                case ITALIC -> italic;
                case UNDERLINED -> underlined;
                case STRIKETHROUGH -> strikethrough;
                case OBFUSCATED -> obfuscated;
                default -> false;
            };
        }

        private Style transformed(TextFormat format, boolean turnOn) {
            return switch (format) {
                case RESET -> NONE;
                case BOLD -> new Style(color, turnOn, italic, underlined, strikethrough, obfuscated);
                case ITALIC -> new Style(color, bold, turnOn, underlined, strikethrough, obfuscated);
                case UNDERLINED -> new Style(color, bold, italic, turnOn, strikethrough, obfuscated);
                case STRIKETHROUGH -> new Style(color, bold, italic, underlined, turnOn, obfuscated);
                case OBFUSCATED -> new Style(color, bold, italic, underlined, strikethrough, turnOn);
                default -> new Style(rgbOf(format.colorCode()), bold, italic, underlined, strikethrough, obfuscated);
            };
        }

        private String transitionTo(Style next) {
            if (equals(next)) {
                return "";
            }
            if (next.isNone()) {
                return "&r";
            }
            boolean dropDecoration = (bold && !next.bold)
                    || (italic && !next.italic)
                    || (underlined && !next.underlined)
                    || (strikethrough && !next.strikethrough)
                    || (obfuscated && !next.obfuscated);
            if (!Objects.equals(color, next.color) || dropDecoration) {
                StringBuilder out = new StringBuilder();
                if (next.color == null) {
                    out.append("&r");
                } else {
                    out.append('&').append(legacyCode(next.color));
                }
                next.appendDecorations(out);
                return out.toString();
            }
            StringBuilder out = new StringBuilder();
            next.appendAddedDecorations(out, this);
            return out.toString();
        }

        private void appendDecorations(StringBuilder out) {
            if (obfuscated) {
                out.append("&k");
            }
            if (bold) {
                out.append("&l");
            }
            if (strikethrough) {
                out.append("&m");
            }
            if (underlined) {
                out.append("&n");
            }
            if (italic) {
                out.append("&o");
            }
        }

        private void appendAddedDecorations(StringBuilder out, Style previous) {
            if (obfuscated && !previous.obfuscated) {
                out.append("&k");
            }
            if (bold && !previous.bold) {
                out.append("&l");
            }
            if (strikethrough && !previous.strikethrough) {
                out.append("&m");
            }
            if (underlined && !previous.underlined) {
                out.append("&n");
            }
            if (italic && !previous.italic) {
                out.append("&o");
            }
        }
    }
}
