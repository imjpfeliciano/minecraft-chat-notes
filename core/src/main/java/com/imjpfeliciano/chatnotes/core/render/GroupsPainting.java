package com.imjpfeliciano.chatnotes.core.render;

import com.imjpfeliciano.chatnotes.core.model.GroupAccent;

/** The groups screen's custom drawing: rows, the empty panel, and the edit dialog panel. */
public final class GroupsPainting {

    private static final int ACCENT_WIDTH = 3;

    private GroupsPainting() {}

    public static void title(Painter painter, String title, int centerX, int y) {
        painter.centeredText(title, centerX, y, UiColors.WHITE);
    }

    public static void subtitle(Painter painter, String subtitle, int centerX, int y) {
        painter.centeredText(subtitle, centerX, y, UiColors.LIGHT_GRAY);
    }

    public static void empty(Painter painter, String title, String hint, int centerX, int centerY, int screenWidth) {
        int boxW = Math.min(320, screenWidth - 40);
        int boxH = 64;
        int boxX = centerX - boxW / 2;
        int boxY = centerY - boxH / 2;
        painter.fill(boxX, boxY, boxX + boxW, boxY + boxH, UiColors.EMPTY_FILL);
        painter.outline(boxX, boxY, boxW, boxH, UiColors.EMPTY_OUTLINE);
        painter.centeredText(title, centerX, boxY + 18, UiColors.WHITE);
        painter.centeredText(hint, centerX, boxY + 36, UiColors.LIGHT_GRAY);
    }

    public static void panel(Painter painter, int x, int y, int width, int height) {
        painter.fill(x, y, x + width, y + height, UiColors.PANEL_FILL);
        painter.outline(x, y, width, height, UiColors.PANEL_OUTLINE);
        painter.fill(x, y, x + 3, y + height, UiColors.PANEL_ACCENT);
    }

    public static void fieldLabel(Painter painter, String label, int x, int y) {
        painter.text(label, x, y, UiColors.LIGHT_GRAY);
    }

    public static void row(
            Painter painter,
            TextMeasurer measurer,
            int x,
            int y,
            int width,
            int height,
            boolean selected,
            boolean hovering,
            String groupId,
            String name,
            String countLabel,
            boolean isDefault,
            String defaultBadge) {
        int rowHeight = height - 2;
        int background = selected ? UiColors.GROUP_ROW_SELECTED : hovering ? UiColors.GROUP_ROW_HOVER : UiColors.GROUP_ROW_IDLE;
        painter.fill(x, y, x + width, y + rowHeight, background);
        painter.outline(x, y, width, rowHeight, selected ? UiColors.GROUP_OUTLINE_SELECTED : UiColors.GROUP_OUTLINE_IDLE);
        painter.fill(x, y, x + ACCENT_WIDTH, y + rowHeight, GroupAccent.colorFor(groupId));

        int textX = x + ACCENT_WIDTH + 8;
        int lineHeight = measurer.lineHeight();
        painter.text(measurer.clip(name, width - 90), textX, y + 6, UiColors.WHITE);
        painter.text(countLabel, textX, y + 6 + lineHeight + 2, UiColors.LIGHT_GRAY);

        if (isDefault) {
            int badgeW = measurer.width(defaultBadge) + 8;
            int badgeH = lineHeight + 4;
            int badgeX = x + width - badgeW - 8;
            int badgeY = y + (rowHeight - badgeH) / 2;
            painter.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, UiColors.DEFAULT_BADGE_FILL);
            painter.outline(badgeX, badgeY, badgeW, badgeH, UiColors.DEFAULT_BADGE_OUTLINE);
            painter.text(defaultBadge, badgeX + 4, badgeY + 2, UiColors.DEFAULT_BADGE_TEXT);
        }
    }
}
