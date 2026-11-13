package com.imjpfeliciano.chatnotes.input;

import com.imjpfeliciano.chatnotes.ChatNotesClient;
import com.imjpfeliciano.chatnotes.ui.NotesScreen;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

/**
 * The only class that registers key bindings. Isolate version-sensitive KeyMapping
 * category differences here so a 1.21.8 backport is a one-file change.
 */
public final class KeyBindings {

    public static KeyMapping openNotes;

    private KeyBindings() {}

    public static void register() {
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
                ChatNotesClient.LOGGER.debug("[Chat Notes] Open-notes key pressed.");
                client.setScreen(new NotesScreen(ChatNotesClient.getStore()));
            }
        });
    }
}
