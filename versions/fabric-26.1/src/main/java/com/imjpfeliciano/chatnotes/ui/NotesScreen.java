package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Note;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.core.model.GroupFilter;
import com.imjpfeliciano.chatnotes.fabric.FabricChatNotes;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;

/** 26.1 notes screen. Opening a screen uses {@code Minecraft.setScreen}. */
public class NotesScreen extends AbstractNotesScreen {

    public NotesScreen(NoteStore store) {
        this(store, "", null, GroupFilter.fromStore(store.defaultGroupFilter()));
    }

    public NotesScreen(NoteStore store, String searchQuery, String selectedNoteId, String groupFilter) {
        super(store, searchQuery, selectedNoteId, groupFilter, FabricChatNotes.platform());
    }

    @Override
    protected void openScreen(Screen screen) {
        minecraft.setScreen(screen);
    }

    @Override
    protected Screen chatScreenWith(String text) {
        return new ChatScreen(text, false);
    }

    @Override
    protected Screen newEditor(Note note) {
        return new NoteEditScreen(this, store, note);
    }

    @Override
    protected Screen newGroups() {
        return new GroupsScreen(this, store);
    }

    @Override
    protected Screen reopen(String searchQuery, String selectedNoteId, String groupFilter) {
        return new NotesScreen(store, searchQuery, selectedNoteId, groupFilter);
    }
}
