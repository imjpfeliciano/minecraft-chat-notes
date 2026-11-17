package com.imjpfeliciano.chatnotes.fabric;

import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.core.platform.ModLog;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * The only class that registers key bindings. The screen it opens is supplied by the version
 * module, so a future 1.21 build can change the screen without forking the registration.
 */
public final class KeyBindings {

    public static KeyMapping openNotes;

    private KeyBindings() {}

    public static void register(FabricChatNotes.NotesOpener opener, NoteStore store, ModLog log) {
        openNotes = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.chatnotes.open",
                        InputConstants.Type.KEYSYM,
                        GLFW.GLFW_KEY_K,
                        KeyMapping.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openNotes.consumeClick()) {
                // In-world only: the title screen has no player, and the done criteria is "pressing K in-world".
                if (client.player == null) {
                    continue;
                }
                log.debug("[Chat Notes] Open-notes key pressed.");
                client.setScreen(opener.open(store));
            }
        });
    }
}
