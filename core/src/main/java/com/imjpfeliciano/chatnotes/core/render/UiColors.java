package com.imjpfeliciano.chatnotes.core.render;

/**
 * The ARGB values the screens used to read from {@code CommonColors} and from literals.
 * Kept here so a version layer never has to agree on a colour name.
 */
public final class UiColors {

    public static final int WHITE = 0xFFFFFFFF;
    public static final int LIGHT_GRAY = 0xFFA0A0A0;
    public static final int SOFT_RED = 0xFFDF5050;
    public static final int GREEN = 0xFF00FF00;

    public static final int BADGE_FILL = 0xAA2A2A2A;
    public static final int BADGE_OUTLINE = 0x66FFFFFF;
    public static final int BADGE_TEXT = 0xFFC8C8C8;

    public static final int GROUP_ROW_SELECTED = 0xAA3A4558;
    public static final int GROUP_ROW_HOVER = 0x88303338;
    public static final int GROUP_ROW_IDLE = 0x661E1E1E;
    public static final int GROUP_OUTLINE_SELECTED = 0xAAFFFFFF;
    public static final int GROUP_OUTLINE_IDLE = 0x33FFFFFF;

    public static final int DEFAULT_BADGE_FILL = 0xAA3A5A2A;
    public static final int DEFAULT_BADGE_OUTLINE = 0x8860A050;
    public static final int DEFAULT_BADGE_TEXT = 0xFFB6F5A0;

    public static final int EMPTY_FILL = 0xAA1A1A1A;
    public static final int EMPTY_OUTLINE = 0x66FFFFFF;

    public static final int PANEL_FILL = 0xCC1A1A1A;
    public static final int PANEL_OUTLINE = 0x88FFFFFF;
    public static final int PANEL_ACCENT = 0xFF5B8DEF;

    private UiColors() {}
}
