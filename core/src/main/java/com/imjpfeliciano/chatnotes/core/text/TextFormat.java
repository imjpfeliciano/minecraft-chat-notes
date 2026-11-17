package com.imjpfeliciano.chatnotes.core.text;

/**
 * One toolbar action. Colour values are the vanilla chat codes, so yellow is always {@code &e}
 * and bold is always {@code &l}.
 */
public enum TextFormat {
    BOLD,
    ITALIC,
    UNDERLINED,
    STRIKETHROUGH,
    OBFUSCATED,
    RESET,
    BLACK('0'),
    DARK_BLUE('1'),
    DARK_GREEN('2'),
    DARK_AQUA('3'),
    DARK_RED('4'),
    DARK_PURPLE('5'),
    GOLD('6'),
    GRAY('7'),
    DARK_GRAY('8'),
    BLUE('9'),
    GREEN('a'),
    AQUA('b'),
    RED('c'),
    LIGHT_PURPLE('d'),
    YELLOW('e'),
    WHITE('f');

    private final Character colorCode;

    TextFormat() {
        this.colorCode = null;
    }

    TextFormat(char colorCode) {
        this.colorCode = colorCode;
    }

    /** The legacy colour code, or {@code null} for a decoration or reset. */
    public Character colorCode() {
        return colorCode;
    }

    public boolean isDecoration() {
        return this == BOLD
                || this == ITALIC
                || this == UNDERLINED
                || this == STRIKETHROUGH
                || this == OBFUSCATED;
    }

    /** The colour format for {@code 0-9} or {@code a-f}, or {@code null} when {@code code} is not one. */
    public static TextFormat fromColorCode(char code) {
        char normalized = Character.toLowerCase(code);
        for (TextFormat format : values()) {
            if (format.colorCode != null && format.colorCode == normalized) {
                return format;
            }
        }
        return null;
    }
}
