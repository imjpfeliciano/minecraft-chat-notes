package com.imjpfeliciano.chatnotes.core.render;

import com.imjpfeliciano.chatnotes.core.model.NoteRules;
import com.imjpfeliciano.chatnotes.core.text.AmpersandParser;

/**
 * Everything the notes screen draws that is not a vanilla widget. Coordinates come from the
 * screen, which owns the layout; this only places text, badges and spans inside a row.
 */
public final class NotesPainting {

    private NotesPainting() {}

    public static void title(Painter painter, String title, int centerX, int y) {
        painter.centeredText(title, centerX, y, UiColors.WHITE);
    }

    public static void footer(Painter painter, String message, int centerX, int y, boolean warning) {
        painter.centeredText(message, centerX, y, warning ? UiColors.SOFT_RED : UiColors.GREEN);
    }

    public static void empty(Painter painter, String message, int centerX, int y) {
        painter.centeredText(message, centerX, y, UiColors.LIGHT_GRAY);
    }

    public static void counter(Painter painter, String label, int x, int y, int length) {
        painter.text(label, x, y, NoteRules.counterColor(length));
    }

    public static void warning(Painter painter, String message, int x, int y) {
        painter.text(message, x, y, UiColors.SOFT_RED);
    }

    public static void preview(Painter painter, String text, int x, int y, int maxWidth) {
        painter.spans(AmpersandParser.parse(text), x, y, maxWidth, UiColors.WHITE);
    }

    /**
     * One notes-list row: optional section-sign marker and over-limit count on the right, an
     * optional group badge inside those, then the title and coloured preview.
     */
    public static void row(
            Painter painter,
            TextMeasurer measurer,
            int x,
            int y,
            int rowWidth,
            String title,
            String text,
            boolean sectionSign,
            String sectionMarker,
            String charCount,
            String groupBadge) {
        int rightEdge = x + rowWidth - 4;
        int lineHeight = measurer.lineHeight();

        if (sectionSign) {
            rightEdge -= measurer.width(sectionMarker);
            painter.text(sectionMarker, rightEdge, y + 2, UiColors.SOFT_RED);
            rightEdge -= 4;
        }

        if (NoteRules.overSoftLimit(text.length())) {
            rightEdge -= measurer.width(charCount);
            painter.text(charCount, rightEdge, y + 2, UiColors.SOFT_RED);
            rightEdge -= 4;
        }

        if (groupBadge != null) {
            String clipped = measurer.clip(groupBadge, 90);
            int padX = 3;
            int badgeW = measurer.width(clipped) + padX * 2;
            int badgeH = lineHeight + 2;
            int badgeX = rightEdge - badgeW;
            int badgeY = y + 1;
            painter.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, UiColors.BADGE_FILL);
            painter.outline(badgeX, badgeY, badgeW, badgeH, UiColors.BADGE_OUTLINE);
            painter.text(clipped, badgeX + padX, badgeY + 1, UiColors.BADGE_TEXT);
            rightEdge = badgeX - 4;
        }

        int textWidth = Math.max(16, rightEdge - (x + 4));
        if (title.isBlank()) {
            painter.spans(AmpersandParser.parse(text), x + 4, y + 2, textWidth, UiColors.WHITE);
        } else {
            painter.text(measurer.clip(title, textWidth), x + 4, y + 2, UiColors.WHITE);
            painter.spans(
                    AmpersandParser.parse(text),
                    x + 4,
                    y + 2 + lineHeight + 1,
                    rowWidth - 8,
                    UiColors.LIGHT_GRAY);
        }
    }
}
