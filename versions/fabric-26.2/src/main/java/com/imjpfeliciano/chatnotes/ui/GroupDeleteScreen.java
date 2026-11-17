package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Group;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import net.minecraft.client.gui.screens.Screen;

public class GroupDeleteScreen extends AbstractGroupDeleteScreen {

    public GroupDeleteScreen(AbstractGroupsScreen parent, NoteStore store, Group group) {
        super(parent, store, group);
    }

    @Override
    protected void openScreen(Screen screen) {
        minecraft.gui.setScreen(screen);
    }
}
