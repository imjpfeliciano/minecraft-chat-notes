package com.imjpfeliciano.chatnotes.core.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NoteStoreFileTest {

    @TempDir
    Path dir;

    private Path notesFile() {
        return dir.resolve("chatnotes").resolve("notes.json");
    }

    @Test
    void missingFileLoadsAsEmpty() throws IOException {
        NoteStoreFile.LoadResult result = NoteStoreFile.load(notesFile());

        assertEquals(List.of(), result.document().notes());
        assertTrue(result.recoveredFrom().isEmpty());
    }

    @Test
    void roundTripsNotesUnchanged() throws IOException {
        Instant now = Instant.parse("2026-11-13T12:00:00Z");
        String colored = "&lVENDO &e&lKIT ZEUS &a&l550K";
        NotesDocument document = new NotesDocument(1,
                List.of(new Group("g1", "Shop ads", 0)),
                List.of(new Note("n1", "Zeus kit", colored, "g1", 0, now, now)),
                null);

        NoteStoreFile.save(notesFile(), document);
        NotesDocument loaded = NoteStoreFile.load(notesFile()).document();

        assertEquals(document, loaded);
        assertEquals(colored, loaded.notes().get(0).text());
    }

    @Test
    void preservesUnicodeAndMarkupCharacters() throws IOException {
        String text = "<gradient:#FF8800:#00FFAA>hola</gradient> \u00a1\u00f1! \ud83d\ude80";
        NoteStoreFile.save(notesFile(), documentWithText(text));

        assertEquals(text, NoteStoreFile.load(notesFile()).document().notes().get(0).text());
    }

    @Test
    void unknownFieldsAreIgnoredWithoutCrashing() throws IOException {
        Path file = notesFile();
        Files.createDirectories(file.getParent());
        Files.writeString(file, """
                {
                  "schemaVersion": 2,
                  "futureSetting": { "nested": true },
                  "notes": [
                    {
                      "id": "n1",
                      "title": "Kept",
                      "text": "hello",
                      "order": 0,
                      "createdAt": "2026-11-13T12:00:00Z",
                      "updatedAt": "2026-11-13T12:00:00Z",
                      "pinned": true
                    }
                  ]
                }
                """, StandardCharsets.UTF_8);

        NoteStoreFile.LoadResult result = NoteStoreFile.load(file);

        assertTrue(result.recoveredFrom().isEmpty());
        assertEquals(2, result.document().schemaVersion());
        assertEquals("Kept", result.document().notes().get(0).title());
    }

    @Test
    void corruptFileIsQuarantinedRatherThanLost() throws IOException {
        Path file = notesFile();
        Files.createDirectories(file.getParent());
        Files.writeString(file, "{ this is not json", StandardCharsets.UTF_8);

        NoteStoreFile.LoadResult result = NoteStoreFile.load(file);

        assertEquals(List.of(), result.document().notes());
        Path quarantined = result.recoveredFrom().orElseThrow();
        assertTrue(quarantined.getFileName().toString().startsWith("notes.corrupt-"));
        assertEquals("{ this is not json", Files.readString(quarantined));
        assertFalse(Files.exists(file), "the bad file should have been moved aside, not left in place");
    }

    @Test
    void savingKeepsThePreviousFileAsBackupAndLeavesNoTempFile() throws IOException {
        Path file = notesFile();
        NoteStoreFile.save(file, documentWithText("first"));
        NoteStoreFile.save(file, documentWithText("second"));

        assertEquals("second", NoteStoreFile.load(file).document().notes().get(0).text());

        Path backup = file.resolveSibling("notes.json.bak");
        assertTrue(Files.exists(backup));
        assertTrue(Files.readString(backup).contains("first"));
        assertFalse(Files.exists(file.resolveSibling("notes.json.tmp")));
    }

    @Test
    void notesAreLoadedInStoredOrderEvenWhenTheFileIsShuffled() throws IOException {
        Instant now = Instant.parse("2026-11-13T12:00:00Z");
        NoteStoreFile.save(notesFile(), new NotesDocument(1, List.of(), List.of(
                new Note("c", "", "third", null, 2, now, now),
                new Note("a", "", "first", null, 0, now, now),
                new Note("b", "", "second", null, 1, now, now)), null));

        List<Note> loaded = NoteStoreFile.load(notesFile()).document().notes();

        assertEquals(List.of("first", "second", "third"), loaded.stream().map(Note::text).toList());
    }

    private static NotesDocument documentWithText(String text) {
        Instant now = Instant.parse("2026-11-13T12:00:00Z");
        return new NotesDocument(1, List.of(), List.of(new Note("n1", "", text, null, 0, now, now)), null);
    }
}
