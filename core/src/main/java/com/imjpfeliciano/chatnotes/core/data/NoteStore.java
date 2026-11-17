package com.imjpfeliciano.chatnotes.core.data;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The notes, in memory, backed by a file. Loaded once at client start and saved after every
 * change, which is cheap for the sizes involved and means a crash can never lose more than the
 * edit in progress.
 *
 * <p>Deliberately free of Minecraft imports so it can be unit tested without launching the game.
 */
public final class NoteStore {

    private final Path file;
    private final Clock clock;
    private final List<Note> notes = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();
    private final Optional<Path> recoveredFrom;
    /**
     * Preferred group filter when opening the notes screen: {@code null} = all, empty = ungrouped,
     * otherwise a group id.
     */
    private String defaultGroupFilter;

    private NoteStore(Path file, Clock clock, NoteStoreFile.LoadResult loaded) {
        this.file = file;
        this.clock = clock;
        this.notes.addAll(loaded.document().notes());
        this.groups.addAll(loaded.document().groups());
        this.defaultGroupFilter = loaded.document().defaultGroupFilter();
        this.recoveredFrom = loaded.recoveredFrom();
        renumber();
        // Drop a stale default if that group no longer exists.
        String resolved = resolvedDefaultGroupFilter();
        if (!java.util.Objects.equals(resolved, this.defaultGroupFilter)) {
            this.defaultGroupFilter = resolved;
            save();
        }
    }

    public static NoteStore open(Path file) throws IOException {
        return open(file, Clock.systemUTC());
    }

    public static NoteStore open(Path file, Clock clock) throws IOException {
        return new NoteStore(file, clock, NoteStoreFile.load(file));
    }

    /** Set when the file on disk was unreadable and got moved aside, so the UI can warn. */
    public Optional<Path> recoveredFrom() {
        return recoveredFrom;
    }

    public List<Note> notes() {
        return List.copyOf(notes);
    }

    public List<Group> groups() {
        return List.copyOf(groups);
    }

    public Optional<Note> find(String id) {
        return notes.stream().filter(note -> note.id().equals(id)).findFirst();
    }

    public Optional<Group> findGroup(String id) {
        return groups.stream().filter(group -> group.id().equals(id)).findFirst();
    }

    /**
     * @return {@code null} for all notes, empty string for ungrouped, otherwise a group id
     */
    public String defaultGroupFilter() {
        return resolvedDefaultGroupFilter();
    }

    /**
     * @param filter {@code null} = all notes, empty string = ungrouped, otherwise a group id
     */
    public void setDefaultGroupFilter(String filter) {
        if (filter != null && !filter.isEmpty() && findGroup(filter).isEmpty()) {
            throw new IllegalArgumentException("Unknown group id: " + filter);
        }
        if (filter == null) {
            this.defaultGroupFilter = null;
        } else if (filter.isEmpty()) {
            this.defaultGroupFilter = "";
        } else {
            this.defaultGroupFilter = filter;
        }
        save();
    }

    public List<Note> search(String query) {
        return search(query, null);
    }

    /**
     * @param groupFilter {@code null} = all notes; empty string = ungrouped only; otherwise a group id
     */
    public List<Note> search(String query, String groupFilter) {
        return notes.stream()
                .filter(note -> matchesGroupFilter(note, groupFilter))
                .filter(note -> note.matches(query))
                .toList();
    }

    public Note add(String title, String text) {
        return add(title, text, null);
    }

    public Note add(String title, String text, String groupId) {
        Note note = Note.create(title, text, normalizeGroupId(groupId), notes.size(), now());
        notes.add(note);
        save();
        return note;
    }

    public Optional<Note> update(String id, String title, String text) {
        return update(id, title, text, null, false);
    }

    /**
     * @param changeGroup when true, {@code groupId} is written (null/blank clears the group)
     */
    public Optional<Note> update(String id, String title, String text, String groupId, boolean changeGroup) {
        return replace(id, note -> {
            Note updated = note.withContent(title, text, now());
            if (changeGroup) {
                updated = updated.withGroup(normalizeGroupId(groupId), now());
            }
            return updated;
        });
    }

    public Optional<Note> moveToGroup(String id, String groupId) {
        return replace(id, note -> note.withGroup(normalizeGroupId(groupId), now()));
    }

    public boolean delete(String id) {
        boolean removed = notes.removeIf(note -> note.id().equals(id));
        if (removed) {
            renumber();
            save();
        }
        return removed;
    }

    public Group addGroup(String name) {
        Group group = Group.create(name, groups.size());
        groups.add(group);
        renumber();
        save();
        return group;
    }

    public Optional<Group> renameGroup(String id, String name) {
        int index = indexOfGroup(id);
        if (index < 0) {
            return Optional.empty();
        }
        Group renamed = groups.get(index).withName(name);
        if (renamed.name().isBlank()) {
            return Optional.empty();
        }
        groups.set(index, renamed);
        save();
        return Optional.of(renamed);
    }

    /**
     * Removes a group. When {@code deleteNotes} is false, its notes move to Ungrouped; when true,
     * those notes are deleted with the group.
     */
    public boolean deleteGroup(String id, boolean deleteNotes) {
        if (indexOfGroup(id) < 0) {
            return false;
        }
        groups.removeIf(group -> group.id().equals(id));
        if (deleteNotes) {
            notes.removeIf(note -> id.equals(note.groupId()));
        } else {
            Instant stamp = now();
            for (int i = 0; i < notes.size(); i++) {
                Note note = notes.get(i);
                if (id.equals(note.groupId())) {
                    notes.set(i, note.withGroup(null, stamp));
                }
            }
        }
        if (id.equals(defaultGroupFilter)) {
            defaultGroupFilter = null;
        }
        renumber();
        save();
        return true;
    }

    public long countNotesInGroup(String groupId) {
        if (groupId == null || groupId.isBlank()) {
            return notes.stream().filter(note -> note.groupId() == null || note.groupId().isBlank()).count();
        }
        return notes.stream().filter(note -> groupId.equals(note.groupId())).count();
    }

    public boolean moveUp(String id) {
        return swap(indexOf(id), indexOf(id) - 1);
    }

    public boolean moveDown(String id) {
        return swap(indexOf(id), indexOf(id) + 1);
    }

    private Optional<Note> replace(String id, java.util.function.UnaryOperator<Note> change) {
        int index = indexOf(id);
        if (index < 0) {
            return Optional.empty();
        }
        Note updated = change.apply(notes.get(index));
        notes.set(index, updated);
        save();
        return Optional.of(updated);
    }

    private boolean swap(int from, int to) {
        if (from < 0 || to < 0 || from >= notes.size() || to >= notes.size()) {
            return false;
        }
        Note moved = notes.set(to, notes.get(from));
        notes.set(from, moved);
        renumber();
        save();
        return true;
    }

    private int indexOf(String id) {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private int indexOfGroup(String id) {
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean matchesGroupFilter(Note note, String groupFilter) {
        if (groupFilter == null) {
            return true;
        }
        String noteGroup = note.groupId();
        boolean ungrouped = noteGroup == null || noteGroup.isBlank();
        if (groupFilter.isEmpty()) {
            return ungrouped;
        }
        return groupFilter.equals(noteGroup);
    }

    private static String normalizeGroupId(String groupId) {
        return groupId == null || groupId.isBlank() ? null : groupId;
    }

    private String resolvedDefaultGroupFilter() {
        if (defaultGroupFilter == null) {
            return null;
        }
        if (defaultGroupFilter.isEmpty()) {
            return "";
        }
        return findGroup(defaultGroupFilter).isPresent() ? defaultGroupFilter : null;
    }

    /** Keeps the stored {@code order} fields in step with the list, which is the source of truth. */
    private void renumber() {
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).order() != i) {
                notes.set(i, notes.get(i).withOrder(i));
            }
        }
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).order() != i) {
                groups.set(i, groups.get(i).withOrder(i));
            }
        }
    }

    private Instant now() {
        return clock.instant();
    }

    /**
     * Callers are UI event handlers that cannot do anything useful with an {@link IOException}, so
     * it is wrapped. The screen catches this and shows a message rather than crashing the game.
     */
    public void save() {
        try {
            NoteStoreFile.save(
                    file,
                    new NotesDocument(NotesDocument.CURRENT_SCHEMA_VERSION, groups(), notes(), defaultGroupFilter()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }
}
