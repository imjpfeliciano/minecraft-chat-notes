package com.imjpfeliciano.chatnotes;

import com.imjpfeliciano.chatnotes.data.NoteStore;
import com.imjpfeliciano.chatnotes.input.KeyBindings;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

public class ChatNotesClient implements ClientModInitializer {

    public static final String MOD_ID = "chatnotes";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /**
     * Sample used by the Phase 2 vertical-slice check: Use must leave these exact bytes in chat.
     */
    private static final String DEMO_NOTE_TEXT = "&lVENDO &e&lKIT ZEUS &a&l550K";

    private static NoteStore store;

    public static NoteStore getStore() {
        if (store == null) {
            throw new IllegalStateException("NoteStore used before client init");
        }
        return store;
    }

    @Override
    public void onInitializeClient() {
        Path notesFile = FabricLoader.getInstance().getConfigDir().resolve("chatnotes/notes.json");
        try {
            store = NoteStore.open(notesFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not open Chat Notes store at " + notesFile, e);
        }

        store.recoveredFrom().ifPresent(corrupt ->
                LOGGER.warn("[Chat Notes] Previous notes.json was unreadable; moved aside to {}", corrupt));

        if (store.notes().isEmpty()) {
            // Seed so Phase 2 can be verified immediately without a full editor.
            store.add("Demo kit ad", DEMO_NOTE_TEXT);
            LOGGER.info("[Chat Notes] Seeded demo note for vertical-slice testing.");
        }

        KeyBindings.register();
        LOGGER.info("[Chat Notes] Client init complete ({} notes).", store.notes().size());
    }
}
