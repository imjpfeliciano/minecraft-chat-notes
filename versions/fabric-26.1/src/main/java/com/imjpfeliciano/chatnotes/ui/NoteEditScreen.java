package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Note;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.fabric.FabricChatNotes;
import net.minecraft.client.gui.screens.Screen;

public class NoteEditScreen extends AbstractNoteEditScreen {

    public NoteEditScreen(AbstractNotesScreen parent, NoteStore store, Note existing) {
        super(parent, store, existing, FabricChatNotes.platform());
    }

    @Override
    protected void openScreen(Screen screen) {
        minecraft.setScreen(screen);
    }
}
