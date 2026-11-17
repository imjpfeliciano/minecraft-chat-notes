package com.imjpfeliciano.chatnotes.fabric;

import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.core.platform.ModLog;
import com.imjpfeliciano.chatnotes.mc.MinecraftClipboard;
import com.imjpfeliciano.chatnotes.mc.UiPlatform;
import net.minecraft.client.gui.screens.Screen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Client startup shared by every 1.21 Fabric build: config path, store, clipboard, and the
 * keybind. The version module supplies the screen, because that is the class that knows how
 * this Minecraft build opens one.
 */
public final class FabricChatNotes {

    public static final String MOD_ID = "chatnotes";

    /**
     * Sample used by the vertical-slice check: Use must leave these exact bytes in chat.
     */
    private static final String DEMO_NOTE_TEXT = "&lVENDO &e&lKIT ZEUS &a&l550K";

    private static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final ModLog LOG = new Slf4jModLog(LOGGER);

    private static NoteStore store;
    private static UiPlatform platform;

    private FabricChatNotes() {}

    @FunctionalInterface
    public interface NotesOpener {
        Screen open(NoteStore store);
    }

    public static void start(NotesOpener opener) {
        Path notesFile = new FabricConfigPaths().configDir().resolve("chatnotes/notes.json");
        try {
            store = NoteStore.open(notesFile);
        } catch (IOException e) {
            throw new RuntimeException("Could not open Chat Notes store at " + notesFile, e);
        }

        platform = new UiPlatform(new MinecraftClipboard(), LOG);

        store.recoveredFrom().ifPresent(corrupt ->
                LOG.warn("[Chat Notes] Previous notes.json was unreadable; moved aside to {}", corrupt));

        if (store.notes().isEmpty()) {
            // Seed so the vertical slice can be verified immediately without a full editor.
            store.add("Demo kit ad", DEMO_NOTE_TEXT);
            LOG.info("[Chat Notes] Seeded demo note for vertical-slice testing.");
        }

        KeyBindings.register(opener, store, LOG);
        LOG.info("[Chat Notes] Client init complete ({} notes).", store.notes().size());
    }

    public static NoteStore store() {
        if (store == null) {
            throw new IllegalStateException("NoteStore used before client init");
        }
        return store;
    }

    public static UiPlatform platform() {
        if (platform == null) {
            throw new IllegalStateException("Chat Notes used before client init");
        }
        return platform;
    }
}
