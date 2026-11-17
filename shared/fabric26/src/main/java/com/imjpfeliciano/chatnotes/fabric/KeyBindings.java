package com.imjpfeliciano.chatnotes.fabric;

import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.core.platform.ModLog;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/**
 * The only class that registers key bindings. The screen it opens is supplied by the version
 * module, so a later 26.x build can change the screen without forking the registration.
 */
public final class KeyBindings {

    public static KeyMapping openNotes;

    private KeyBindings() {}

    public static void register(FabricChatNotes.NotesOpener opener, FabricChatNotes.ScreenDisplay display, NoteStore store, ModLog log) {
        openNotes = KeyMappingHelper.registerKeyMapping(
                new KeyMapping(
                        "key.chatnotes.open",
                        InputConstants.KEY_K,
                        KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openNotes.consumeClick()) {
                // In-world only: the title screen has no player, and the done criteria is "pressing K in-world".
                if (client.player == null) {
                    continue;
                }
                log.debug("[Chat Notes] Open-notes key pressed.");
                display.open(opener.open(store));
            }
        });
    }
}
