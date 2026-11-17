package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Group;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.core.render.GroupsPainting;
import com.imjpfeliciano.chatnotes.mc.ExtractorPainter;
import com.imjpfeliciano.chatnotes.mc.MinecraftTranslator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Delete a group with an explicit choice: move its notes to Ungrouped, or delete those notes too.
 */
public abstract class AbstractGroupDeleteScreen extends Screen {

    private static final int BUTTON_WIDTH = 220;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 6;

    private final AbstractGroupsScreen parent;
    private final NoteStore store;
    private final Group group;
    private final long noteCount;

    protected AbstractGroupDeleteScreen(AbstractGroupsScreen parent, NoteStore store, Group group) {
        super(Component.translatable("screen.chatnotes.group_delete_title"));
        this.parent = parent;
        this.store = store;
        this.group = group;
        this.noteCount = store.countNotesInGroup(group.id());
    }

    protected abstract void openScreen(Screen screen);

    private void show(Screen screen) {
        if (minecraft != null) {
            openScreen(screen);
        }
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
        parent.log().info(
                "[Chat Notes] Deleted group {} (deleteNotes={}, notesAffected={})",
                group.id(),
                deleteNotes,
                noteCount);
        parent.onGroupsChanged(null);
        show(parent);
    }

    @Override
    public void onClose() {
        show(parent);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        var painter = new ExtractorPainter(graphics, font);
        GroupsPainting.title(painter, title.getString(), width / 2, height / 2 - 40);
        GroupsPainting.subtitle(
                painter,
                MinecraftTranslator.INSTANCE.tr("screen.chatnotes.group_delete_message", group.name(), noteCount),
                width / 2,
                height / 2 - 24);
    }
}
