package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Group;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.fabric.FabricChatNotes;
import net.minecraft.client.gui.screens.Screen;

public class GroupsScreen extends AbstractGroupsScreen {

    public GroupsScreen(AbstractNotesScreen parent, NoteStore store) {
        super(parent, store, FabricChatNotes.platform());
    }

    @Override
    protected void openScreen(Screen screen) {
        minecraft.gui.setScreen(screen);
    }

    @Override
    protected Screen newEditor(Group group) {
        return new GroupEditScreen(this, store, group);
    }

    @Override
    protected Screen newDelete(Group group) {
        return new GroupDeleteScreen(this, store, group);
    }
}
