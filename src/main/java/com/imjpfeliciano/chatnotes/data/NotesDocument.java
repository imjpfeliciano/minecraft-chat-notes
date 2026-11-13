package com.imjpfeliciano.chatnotes.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The whole contents of {@code notes.json}. This mirrors the file one-to-one, so anything that
 * changes here is a schema change.
 */
public record NotesDocument(int schemaVersion, List<Group> groups, List<Note> notes) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public NotesDocument {
        // A file written by a future version may omit or add fields. Gson leaves those as null or
        // drops them, so fill in sane values instead of letting a null reach the rest of the mod.
        schemaVersion = schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        groups = sortedCopy(groups, Comparator.comparingInt(Group::order));
        notes = sortedCopy(notes, Comparator.comparingInt(Note::order));
    }

    public static NotesDocument empty() {
        return new NotesDocument(CURRENT_SCHEMA_VERSION, List.of(), List.of());
    }

    private static <T> List<T> sortedCopy(List<T> values, Comparator<T> comparator) {
        if (values == null) {
            return List.of();
        }
        List<T> copy = new ArrayList<>(values.size());
        for (T value : values) {
            if (value != null) {
                copy.add(value);
            }
        }
        copy.sort(comparator);
        return List.copyOf(copy);
    }
}
