package com.imjpfeliciano.chatnotes.core.data;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The whole contents of {@code notes.json}. This mirrors the file one-to-one, so anything that
 * changes here is a schema change.
 *
 * <p>{@code defaultGroupFilter} is a preference: {@code null} = all notes, empty string = ungrouped
 * only, otherwise a group id. Older files omit the field and load as {@code null}.
 */
public record NotesDocument(int schemaVersion, List<Group> groups, List<Note> notes, String defaultGroupFilter) {

    public static final int CURRENT_SCHEMA_VERSION = 1;

    public NotesDocument {
        // A file written by a future version may omit or add fields. Gson leaves those as null or
        // drops them, so fill in sane values instead of letting a null reach the rest of the mod.
        schemaVersion = schemaVersion <= 0 ? CURRENT_SCHEMA_VERSION : schemaVersion;
        groups = sortedCopy(groups, Comparator.comparingInt(Group::order));
        notes = sortedCopy(notes, Comparator.comparingInt(Note::order));
        if (defaultGroupFilter != null && defaultGroupFilter.isBlank()) {
            defaultGroupFilter = "";
        }
    }

    public static NotesDocument empty() {
        return new NotesDocument(CURRENT_SCHEMA_VERSION, List.of(), List.of(), null);
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
