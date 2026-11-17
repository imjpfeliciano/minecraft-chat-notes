package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Group;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import net.minecraft.client.gui.screens.Screen;

public class GroupEditScreen extends AbstractGroupEditScreen {

    public GroupEditScreen(AbstractGroupsScreen parent, NoteStore store, Group existing) {
        super(parent, store, existing);
    }

    @Override
    protected void openScreen(Screen screen) {
        minecraft.gui.setScreen(screen);
    }
}
