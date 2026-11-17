package com.imjpfeliciano.chatnotes.core.data;

import java.time.Instant;
import java.util.UUID;

/**
 * One stored snippet. Immutable, so a change always produces a new instance and no caller can
 * mutate a note the store still holds.
 *
 * <p>{@code text} is kept exactly as the user pasted it, apart from line breaks: chat is
 * single-line, so they are collapsed by {@link #normalizeText(String)}.
 */
public record Note(
        String id,
        String title,
        String text,
        String groupId,
        int order,
        Instant createdAt,
        Instant updatedAt) {

    public Note {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Note id must not be blank");
        }
        title = title == null ? "" : title;
        text = text == null ? "" : text;
        createdAt = createdAt == null ? Instant.EPOCH : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    public static Note create(String title, String text, String groupId, int order, Instant now) {
        return new Note(UUID.randomUUID().toString(), title, normalizeText(text), groupId, order, now, now);
    }

    public Note withContent(String title, String text, Instant now) {
        return new Note(id, title, normalizeText(text), groupId, order, createdAt, now);
    }

    public Note withGroup(String groupId, Instant now) {
        return new Note(id, title, text, groupId, order, createdAt, now);
    }

    /** Reordering is bookkeeping, not an edit, so it deliberately leaves {@code updatedAt} alone. */
    public Note withOrder(int order) {
        return new Note(id, title, text, groupId, order, createdAt, updatedAt);
    }

    /** What the list shows for this note: the title, or the text when there is no title. */
    public String displayLabel() {
        return title.isBlank() ? text : title;
    }

    public boolean matches(String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.toLowerCase();
        return title.toLowerCase().contains(needle) || text.toLowerCase().contains(needle);
    }

    /**
     * Collapses every run of line breaks and surrounding whitespace into a single space. Any other
     * character, including the colour codes the user cares about, is left untouched.
     */
    public static String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("\\s*\\R\\s*", " ");
    }
}
