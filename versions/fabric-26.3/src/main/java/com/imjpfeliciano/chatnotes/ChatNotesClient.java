package com.imjpfeliciano.chatnotes;

import com.imjpfeliciano.chatnotes.fabric.FabricChatNotes;
import com.imjpfeliciano.chatnotes.ui.NotesScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

/** Entrypoint. Everything except "which screen this version opens" lives in the shared Fabric layer. */
public class ChatNotesClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        FabricChatNotes.start(NotesScreen::new, screen -> Minecraft.getInstance().gui.setScreen(screen));
    }
}
