package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.ChatNotesClient;
import com.imjpfeliciano.chatnotes.data.Group;
import com.imjpfeliciano.chatnotes.data.NoteStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

/**
 * Delete a group with an explicit choice: move its notes to Ungrouped, or delete those notes too.
 */
public class GroupDeleteScreen extends Screen {

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 6;

    private final GroupsScreen parent;
    private final NoteStore store;
    private final Group group;
    private final long noteCount;

    public GroupDeleteScreen(GroupsScreen parent, NoteStore store, Group group) {
        super(Component.translatable("screen.chatnotes.group_delete_title"));
        this.parent = parent;
        this.store = store;
        this.group = group;
        this.noteCount = store.countNotesInGroup(group.id());
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int y = height / 2;

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.chatnotes.group_delete_move"),
                        b -> finish(false))
                .bounds(centerX - BUTTON_WIDTH / 2, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(
                        Component.translatable("screen.chatnotes.group_delete_notes"),
                        b -> finish(true))
                .bounds(centerX - BUTTON_WIDTH / 2, y + BUTTON_HEIGHT + PADDING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(centerX - BUTTON_WIDTH / 2, y + 2 * (BUTTON_HEIGHT + PADDING), BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void finish(boolean deleteNotes) {
        store.deleteGroup(group.id(), deleteNotes);
        ChatNotesClient.LOGGER.info(
                "[Chat Notes] Deleted group {} (deleteNotes={}, notesAffected={})",
                group.id(),
                deleteNotes,
                noteCount);
        parent.onGroupsChanged(null);
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, height / 2 - 40, CommonColors.WHITE);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.chatnotes.group_delete_message", group.name(), noteCount),
                width / 2,
                height / 2 - 24,
                CommonColors.LIGHT_GRAY);
    }
}
