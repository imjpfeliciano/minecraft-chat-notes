package com.imjpfeliciano.chatnotes.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteStoreTest {

    private static final Instant CREATED = Instant.parse("2026-11-13T12:00:00Z");
    private static final Instant EDITED = Instant.parse("2026-11-14T08:30:00Z");

    @TempDir
    Path dir;

    private Path file;

    @BeforeEach
    void setUp() {
        file = dir.resolve("chatnotes").resolve("notes.json");
    }

    private NoteStore store(Instant now) throws IOException {
        return NoteStore.open(file, Clock.fixed(now, ZoneOffset.UTC));
    }

    @Test
    void addedNotesSurviveReopening() throws IOException {
        NoteStore store = store(CREATED);
        store.add("Zeus kit", "&lVENDO &e&lKIT ZEUS &a&l550K");

        List<Note> reloaded = store(CREATED).notes();

        assertEquals(1, reloaded.size());
        assertEquals("&lVENDO &e&lKIT ZEUS &a&l550K", reloaded.get(0).text());
        assertEquals(CREATED, reloaded.get(0).createdAt());
    }

    @Test
    void updateChangesContentAndTimestampButKeepsIdAndCreatedAt() throws IOException {
        NoteStore store = store(CREATED);
        Note original = store.add("Old", "old text");

        Note updated = store(EDITED).update(original.id(), "New", "new text").orElseThrow();

        assertEquals(original.id(), updated.id());
        assertEquals(CREATED, updated.createdAt());
        assertEquals(EDITED, updated.updatedAt());
        assertEquals("new text", updated.text());
    }

    @Test
    void deleteRemovesTheNoteAndClosesTheGapInOrder() throws IOException {
        NoteStore store = store(CREATED);
        store.add("", "a");
        Note second = store.add("", "b");
        store.add("", "c");

        assertTrue(store.delete(second.id()));
        assertFalse(store.delete(second.id()));

        List<Note> remaining = store(CREATED).notes();
        assertEquals(List.of("a", "c"), remaining.stream().map(Note::text).toList());
        assertEquals(List.of(0, 1), remaining.stream().map(Note::order).toList());
    }

    @Test
    void movingReordersAndPersists() throws IOException {
        NoteStore store = store(CREATED);
        store.add("", "a");
        Note b = store.add("", "b");
        store.add("", "c");

        assertTrue(store.moveUp(b.id()));
        assertEquals(List.of("b", "a", "c"), store.notes().stream().map(Note::text).toList());

        assertTrue(store.moveDown(b.id()));
        assertEquals(List.of("a", "b", "c"), store(CREATED).notes().stream().map(Note::text).toList());
    }

    @Test
    void movingPastTheEdgeOfTheListDoesNothing() throws IOException {
        NoteStore store = store(CREATED);
        Note first = store.add("", "a");
        Note last = store.add("", "b");

        assertFalse(store.moveUp(first.id()));
        assertFalse(store.moveDown(last.id()));
        assertFalse(store.moveUp("no-such-id"));
        assertEquals(List.of("a", "b"), store.notes().stream().map(Note::text).toList());
    }

    @Test
    void searchMatchesTitleAndTextCaseInsensitivelyAndEmptyQueryMatchesAll() throws IOException {
        NoteStore store = store(CREATED);
        store.add("Shop", "&lVENDO kit");
        store.add("Greeting", "hola mundo");

        assertEquals(List.of("Shop"), titles(store.search("vendo")));
        assertEquals(List.of("Shop"), titles(store.search("SHOP")));
        assertEquals(List.of("Greeting"), titles(store.search("mundo")));
        assertEquals(List.of("Shop", "Greeting"), titles(store.search("")));
        assertEquals(List.of(), titles(store.search("nothing here")));
    }

    @Test
    void lineBreaksAreCollapsedBecauseChatIsSingleLine() throws IOException {
        NoteStore store = store(CREATED);

        Note note = store.add("", "line one\n  line two\r\nline three");

        assertEquals("line one line two line three", note.text());
    }

    @Test
    void groupIdRoundTripsThroughSave() throws IOException {
        NoteStore store = store(CREATED);
        Group group = store.addGroup("Shop ads");
        Note note = store.add("Ad", "text", group.id());

        assertEquals(group.id(), store(CREATED).find(note.id()).orElseThrow().groupId());
        assertEquals("Shop ads", store(CREATED).findGroup(group.id()).orElseThrow().name());
    }

    @Test
    void searchCanFilterByGroupOrUngrouped() throws IOException {
        NoteStore store = store(CREATED);
        Group shop = store.addGroup("Shop");
        store.add("A", "one", shop.id());
        store.add("B", "two", null);
        store.add("C", "three", shop.id());

        assertEquals(List.of("A", "C"), titles(store.search("", shop.id())));
        assertEquals(List.of("B"), titles(store.search("", "")));
        assertEquals(List.of("A", "B", "C"), titles(store.search("", null)));
        assertEquals(List.of("A"), titles(store.search("one", shop.id())));
    }

    @Test
    void renamingAndDeletingGroups() throws IOException {
        NoteStore store = store(CREATED);
        Group shop = store.addGroup("Shop");
        Group farm = store.addGroup("Farm");
        Note kept = store.add("Ad", "shop text", shop.id());
        Note orphan = store.add("Farm ad", "farm text", farm.id());

        assertEquals("Shop ads", store.renameGroup(shop.id(), "Shop ads").orElseThrow().name());

        assertTrue(store.deleteGroup(farm.id(), false));
        assertEquals(null, store.find(orphan.id()).orElseThrow().groupId());
        assertTrue(store.findGroup(farm.id()).isEmpty());

        assertTrue(store.deleteGroup(shop.id(), true));
        assertTrue(store.find(kept.id()).isEmpty());
        assertEquals(1, store.notes().size());
        assertEquals(orphan.id(), store.notes().get(0).id());
    }

    @Test
    void defaultGroupFilterPersistsAndClearsWhenGroupIsDeleted() throws IOException {
        NoteStore store = store(CREATED);
        Group shop = store.addGroup("Shop");

        store.setDefaultGroupFilter(shop.id());
        assertEquals(shop.id(), store(CREATED).defaultGroupFilter());

        store(CREATED).setDefaultGroupFilter("");
        assertEquals("", store(CREATED).defaultGroupFilter());

        store(CREATED).setDefaultGroupFilter(null);
        assertEquals(null, store(CREATED).defaultGroupFilter());

        NoteStore again = store(CREATED);
        Group farm = again.addGroup("Farm");
        again.setDefaultGroupFilter(farm.id());
        again.deleteGroup(farm.id(), false);
        assertEquals(null, store(CREATED).defaultGroupFilter());
    }

    @Test
    void updateCanChangeGroupMembership() throws IOException {
        NoteStore store = store(CREATED);
        Group shop = store.addGroup("Shop");
        Note note = store.add("Ad", "text", null);

        Note moved = store.update(note.id(), "Ad", "text", shop.id(), true).orElseThrow();
        assertEquals(shop.id(), moved.groupId());

        Note cleared = store.update(note.id(), "Ad", "text", null, true).orElseThrow();
        assertEquals(null, cleared.groupId());
    }

    @Test
    void aCorruptFileIsReportedSoTheUiCanWarnInsteadOfShowingAnEmptyList() throws IOException {
        java.nio.file.Files.createDirectories(file.getParent());
        java.nio.file.Files.writeString(file, "not json at all");

        NoteStore store = store(CREATED);

        assertTrue(store.recoveredFrom().isPresent());
        assertEquals(List.of(), store.notes());
    }

    @Test
    void displayLabelFallsBackToTheTextWhenThereIsNoTitle() throws IOException {
        NoteStore store = store(CREATED);

        assertEquals("Titled", store.add("Titled", "body").displayLabel());
        assertEquals("body only", store.add("", "body only").displayLabel());
    }

    private static List<String> titles(List<Note> notes) {
        return notes.stream().map(Note::title).toList();
    }
}
