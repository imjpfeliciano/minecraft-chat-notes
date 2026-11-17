package com.imjpfeliciano.chatnotes.core.model;

import com.imjpfeliciano.chatnotes.core.render.UiColors;

/** The list and editor decisions that used to live inline in the screens. */
public final class NoteRules {

    public static final int SOFT_LIMIT = 256;

    public static final String EMPTY_KEY = "screen.chatnotes.empty";
    public static final String NO_MATCHES_KEY = "screen.chatnotes.no_matches";

    private NoteRules() {}

    public static boolean overSoftLimit(int length) {
        return length > SOFT_LIMIT;
    }

    /** Counter and the over-limit count both flip to red past {@link #SOFT_LIMIT}. */
    public static int counterColor(int length) {
        return overSoftLimit(length) ? UiColors.SOFT_RED : UiColors.LIGHT_GRAY;
    }

    public static boolean canSave(String normalizedText) {
        return normalizedText != null && !normalizedText.isBlank();
    }

    /** Paste replaces a blank field and otherwise appends after a single space. */
    public static String paste(String current, String clipboard) {
        if (clipboard == null || clipboard.isEmpty()) {
            return current == null ? "" : current;
        }
        if (current == null || current.isBlank()) {
            return clipboard;
        }
        return current + " " + clipboard;
    }

    /** An unfiltered empty list is "no notes yet"; anything else is "no matches". */
    public static String emptyMessageKey(String searchQuery, String uiFilter) {
        boolean showingEverything = (searchQuery == null || searchQuery.isBlank()) && GroupFilter.ALL.equals(uiFilter);
        return showingEverything ? EMPTY_KEY : NO_MATCHES_KEY;
    }

    /** Group name for the row badge, or {@code null} when the note is ungrouped or the group is gone. */
    public static String groupBadge(String groupId, String groupName) {
        if (groupId == null || groupId.isBlank() || groupName == null) {
            return null;
        }
        return groupName;
    }
}
