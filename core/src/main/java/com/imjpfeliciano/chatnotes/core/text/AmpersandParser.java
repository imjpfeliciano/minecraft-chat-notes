package com.imjpfeliciano.chatnotes.core.text;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns server-style ampersand codes into styled runs, with no Minecraft types involved.
 *
 * <p>Supports legacy {@code &a}/{@code &l} codes, {@code &#RRGGBB}, and {@code &x&R&R&G&G&B&B}.
 * Also accepts {@code §} as a format prefix so a preview can show it, but the input string is
 * never rewritten. Sending {@code §} in chat can disconnect the player; callers warn via
 * {@link #containsSectionSign(String)}.
 *
 * <p>A colour code or {@code &r} replaces the whole style, matching {@code applyLegacyFormat}.
 * A decoration code ({@code &l}, {@code &o}, {@code &n}, {@code &m}, {@code &k}) stacks on the
 * current style, matching {@code applyFormat}.
 */
public final class AmpersandParser {

    private AmpersandParser() {}

    public static boolean containsSectionSign(String text) {
        return text != null && text.indexOf('§') >= 0;
    }

    public static List<TextSpan> parse(String text) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }

        List<TextSpan> spans = new ArrayList<>();
        Style style = Style.NONE;
        StringBuilder plain = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char next = text.charAt(i + 1);

                // Escaped prefix: && or &§ / §§ shown as a literal prefix character.
                if (next == '&' || next == '§') {
                    flush(plain, style, spans);
                    plain.append(c);
                    i++;
                    continue;
                }

                // &#RRGGBB
                if (next == '#' && i + 7 < text.length() && isHexRgb(text, i + 2)) {
                    flush(plain, style, spans);
                    int rgb = Integer.parseInt(text.substring(i + 2, i + 8), 16);
                    style = Style.color(rgb);
                    i += 7;
                    continue;
                }

                // &x&R&R&G&G&B&B (14 chars total including the leading &)
                if ((next == 'x' || next == 'X') && isLegacyHex(text, i)) {
                    flush(plain, style, spans);
                    style = Style.color(parseLegacyHex(text, i));
                    i += 13;
                    continue;
                }

                Style formatted = style.apply(Character.toLowerCase(next));
                if (formatted != null) {
                    flush(plain, style, spans);
                    style = formatted;
                    i++;
                    continue;
                }
            }

            plain.append(c);
        }

        flush(plain, style, spans);
        return List.copyOf(spans);
    }

    private static void flush(StringBuilder plain, Style style, List<TextSpan> spans) {
        if (plain.isEmpty()) {
            return;
        }
        spans.add(style.span(plain.toString()));
        plain.setLength(0);
    }

    private static boolean isHexRgb(String text, int start) {
        if (start + 6 > text.length()) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (Character.digit(text.charAt(start + i), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Pattern at {@code i}: {@code &x&R&R&G&G&B&B} where each colour nibble is prefixed by {@code &} or {@code §}.
     */
    private static boolean isLegacyHex(String text, int i) {
        if (i + 13 >= text.length()) {
            return false;
        }
        for (int n = 0; n < 6; n++) {
            int prefixAt = i + 2 + n * 2;
            char prefix = text.charAt(prefixAt);
            if (prefix != '&' && prefix != '§') {
                return false;
            }
            if (Character.digit(text.charAt(prefixAt + 1), 16) < 0) {
                return false;
            }
        }
        return true;
    }

    private static int parseLegacyHex(String text, int i) {
        StringBuilder hex = new StringBuilder(6);
        for (int n = 0; n < 6; n++) {
            hex.append(text.charAt(i + 3 + n * 2));
        }
        return Integer.parseInt(hex.toString(), 16);
    }

    /**
     * The style carried between codes. Colour values are the vanilla legacy palette, so a preview
     * matches what the server would render.
     */
    private record Style(
            Integer color,
            boolean bold,
            boolean italic,
            boolean underlined,
            boolean strikethrough,
            boolean obfuscated) {

        static final Style NONE = new Style(null, false, false, false, false, false);

        static Style color(int rgb) {
            return new Style(rgb, false, false, false, false, false);
        }

        TextSpan span(String text) {
            return new TextSpan(text, color, bold, italic, underlined, strikethrough, obfuscated);
        }

        /** {@code null} when {@code code} is not a formatting code, so the caller keeps it literal. */
        Style apply(char code) {
            Integer legacy = legacyColor(code);
            if (legacy != null) {
                return color(legacy);
            }
            return switch (code) {
                case 'k' -> new Style(color, bold, italic, underlined, strikethrough, true);
                case 'l' -> new Style(color, true, italic, underlined, strikethrough, obfuscated);
                case 'm' -> new Style(color, bold, italic, underlined, true, obfuscated);
                case 'n' -> new Style(color, bold, italic, true, strikethrough, obfuscated);
                case 'o' -> new Style(color, bold, true, underlined, strikethrough, obfuscated);
                case 'r' -> NONE;
                default -> null;
            };
        }

        private static Integer legacyColor(char code) {
            return switch (code) {
                case '0' -> 0x000000;
                case '1' -> 0x0000AA;
                case '2' -> 0x00AA00;
                case '3' -> 0x00AAAA;
                case '4' -> 0xAA0000;
                case '5' -> 0xAA00AA;
                case '6' -> 0xFFAA00;
                case '7' -> 0xAAAAAA;
                case '8' -> 0x555555;
                case '9' -> 0x5555FF;
                case 'a' -> 0x55FF55;
                case 'b' -> 0x55FFFF;
                case 'c' -> 0xFF5555;
                case 'd' -> 0xFF55FF;
                case 'e' -> 0xFFFF55;
                case 'f' -> 0xFFFFFF;
                default -> null;
            };
        }
    }
}
