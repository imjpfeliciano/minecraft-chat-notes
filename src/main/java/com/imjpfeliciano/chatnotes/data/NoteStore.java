package com.imjpfeliciano.chatnotes.data;

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

    private NoteStore(Path file, Clock clock, NoteStoreFile.LoadResult loaded) {
        this.file = file;
        this.clock = clock;
        this.notes.addAll(loaded.document().notes());
        this.groups.addAll(loaded.document().groups());
        this.recoveredFrom = loaded.recoveredFrom();
        renumber();
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

    public List<Note> search(String query) {
        return notes.stream().filter(note -> note.matches(query)).toList();
    }

    public Note add(String title, String text) {
        return add(title, text, null);
    }

    public Note add(String title, String text, String groupId) {
        Note note = Note.create(title, text, groupId, notes.size(), now());
        notes.add(note);
        save();
        return note;
    }

    public Optional<Note> update(String id, String title, String text) {
        return replace(id, note -> note.withContent(title, text, now()));
    }

    public Optional<Note> moveToGroup(String id, String groupId) {
        return replace(id, note -> note.withGroup(groupId, now()));
    }

    public boolean delete(String id) {
        boolean removed = notes.removeIf(note -> note.id().equals(id));
        if (removed) {
            renumber();
            save();
        }
        return removed;
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
            NoteStoreFile.save(file, new NotesDocument(NotesDocument.CURRENT_SCHEMA_VERSION, groups(), notes()));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not save " + file, e);
        }
    }
}
