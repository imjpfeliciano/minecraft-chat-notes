package com.imjpfeliciano.chatnotes.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** Reads and writes {@code notes.json}. The only class that knows the file format exists. */
public final class NoteStoreFile {

    private static final DateTimeFormatter CORRUPT_SUFFIX =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(java.time.ZoneOffset.UTC);

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeAdapter(Instant.class, new InstantAdapter())
            .create();

    private NoteStoreFile() {
    }

    /**
     * Outcome of a load. {@code recoveredFrom} is set when the file could not be parsed and was
     * moved aside, so the UI can tell the user instead of silently showing an empty list.
     */
    public record LoadResult(NotesDocument document, Optional<Path> recoveredFrom) {
        static LoadResult of(NotesDocument document) {
            return new LoadResult(document, Optional.empty());
        }
    }

    /**
     * Loads the document. A missing file is normal on first run and yields an empty document. A
     * corrupt file is never deleted: it is renamed to {@code notes.corrupt-<timestamp>.json} so the
     * user can recover it by hand.
     */
    public static LoadResult load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return LoadResult.of(NotesDocument.empty());
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            NotesDocument document = GSON.fromJson(reader, NotesDocument.class);
            if (document != null) {
                return LoadResult.of(document);
            }
        } catch (JsonParseException | IllegalArgumentException | IllegalStateException e) {
            // Fall through to quarantine: a malformed file must never take the game down.
        }

        Path quarantine = quarantine(file);
        return new LoadResult(NotesDocument.empty(), Optional.of(quarantine));
    }

    /**
     * Writes the document without ever leaving a half-written {@code notes.json} behind: the new
     * contents go to a temp file, the previous file is kept as {@code .bak}, and only then is the
     * temp file moved into place.
     */
    public static void save(Path file, NotesDocument document) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path temp = sibling(file, file.getFileName() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            GSON.toJson(document, NotesDocument.class, writer);
        }

        if (Files.exists(file)) {
            Files.copy(file, sibling(file, file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
        }

        try {
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            // Some filesystems (notably across mount points) cannot move atomically.
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static Path quarantine(Path file) throws IOException {
        Path target = sibling(file, "notes.corrupt-" + CORRUPT_SUFFIX.format(Instant.now()) + ".json");
        int attempt = 1;
        while (Files.exists(target)) {
            target = sibling(file, "notes.corrupt-" + CORRUPT_SUFFIX.format(Instant.now()) + "-" + attempt++ + ".json");
        }
        Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }

    private static Path sibling(Path file, String name) {
        Path parent = file.toAbsolutePath().getParent();
        return parent == null ? Path.of(name) : parent.resolve(name);
    }

    /** Gson has no built-in {@link Instant} support, so timestamps are read and written as ISO-8601. */
    private static final class InstantAdapter extends TypeAdapter<Instant> {

        @Override
        public void write(JsonWriter out, Instant value) throws IOException {
            if (value == null) {
                out.nullValue();
            } else {
                out.value(DateTimeFormatter.ISO_INSTANT.format(value));
            }
        }

        @Override
        public Instant read(JsonReader in) throws IOException {
            if (in.peek() == JsonToken.NULL) {
                in.nextNull();
                return null;
            }
            String value = in.nextString();
            try {
                return Instant.parse(value);
            } catch (DateTimeParseException e) {
                throw new JsonParseException("Not an ISO-8601 instant: " + value, e);
            }
        }
    }
}
