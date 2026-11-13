package com.imjpfeliciano.chatnotes.util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

/**
 * Client-side preview helper for server-style ampersand colour codes.
 *
 * <p>Supports legacy {@code &a}/{@code &l} codes, {@code &#RRGGBB}, and
 * {@code &x&R&R&G&G&B&B}. Also accepts {@code §} as a format prefix for preview
 * only — stored text is never rewritten. Sending {@code §} in chat can disconnect
 * the player; callers should warn via {@link #containsSectionSign(String)}.
 */
public final class AmpersandFormat {

    private AmpersandFormat() {}

    public static boolean containsSectionSign(String text) {
        return text != null && text.indexOf('§') >= 0;
    }

    /**
     * Turns ampersand (and section-sign) codes into a styled component for list/editor preview.
     * The input string itself is not modified.
     */
    public static Component toComponent(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }

        MutableComponent root = Component.empty();
        Style style = Style.EMPTY;
        StringBuilder plain = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '&' || c == '§') && i + 1 < text.length()) {
                char next = text.charAt(i + 1);

                // Escaped prefix: && or &§ / §§ shown as a literal prefix char.
                if (next == '&' || next == '§') {
                    flush(plain, style, root);
                    plain.append(c);
                    i++;
                    continue;
                }

                // &#RRGGBB
                if (next == '#' && i + 7 < text.length() && isHexRgb(text, i + 2)) {
                    flush(plain, style, root);
                    int rgb = Integer.parseInt(text.substring(i + 2, i + 8), 16);
                    style = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
                    i += 7;
                    continue;
                }

                // &x&R&R&G&G&B&B (14 chars total including the leading &)
                if ((next == 'x' || next == 'X') && isLegacyHex(text, i)) {
                    flush(plain, style, root);
                    int rgb = parseLegacyHex(text, i);
                    style = Style.EMPTY.withColor(TextColor.fromRgb(rgb));
                    i += 13;
                    continue;
                }

                ChatFormatting formatting = ChatFormatting.getByCode(Character.toLowerCase(next));
                if (formatting != null) {
                    flush(plain, style, root);
                    if (formatting.isColor() || formatting == ChatFormatting.RESET) {
                        style = Style.EMPTY.applyLegacyFormat(formatting);
                    } else {
                        style = style.applyFormat(formatting);
                    }
                    i++;
                    continue;
                }
            }

            plain.append(c);
        }

        flush(plain, style, root);
        return root;
    }

    /** Draws {@code text} with ampersand colours, clipped to {@code maxWidth} pixels. */
    public static void drawClipped(GuiGraphics graphics, Font font, String text, int x, int y, int maxWidth, int fallbackColor) {
        Component parsed = toComponent(text);
        FormattedCharSequence ordered = Language.getInstance().getVisualOrder(font.substrByWidth(parsed, maxWidth));
        graphics.drawString(font, ordered, x, y, fallbackColor);
    }

    private static void flush(StringBuilder plain, Style style, MutableComponent root) {
        if (plain.isEmpty()) {
            return;
        }
        root.append(Component.literal(plain.toString()).setStyle(style));
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
            int digitAt = prefixAt + 1;
            char prefix = text.charAt(prefixAt);
            if (prefix != '&' && prefix != '§') {
                return false;
            }
            if (Character.digit(text.charAt(digitAt), 16) < 0) {
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
}
