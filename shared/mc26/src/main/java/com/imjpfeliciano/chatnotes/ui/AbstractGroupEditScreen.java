package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Group;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.core.render.GroupsPainting;
import com.imjpfeliciano.chatnotes.core.render.Painter;
import com.imjpfeliciano.chatnotes.mc.ExtractorPainter;
import com.imjpfeliciano.chatnotes.mc.MinecraftTranslator;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Create or rename a single group. */
public abstract class AbstractGroupEditScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 120;
    private static final int FIELD_WIDTH = 280;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    private final AbstractGroupsScreen parent;
    private final NoteStore store;
    private final Group existing;

    private EditBox nameBox;
    private Button saveButton;

    protected AbstractGroupEditScreen(AbstractGroupsScreen parent, NoteStore store, Group existing) {
        super(existing == null
                ? Component.translatable("screen.chatnotes.group_edit_new")
                : Component.translatable("screen.chatnotes.group_edit_existing"));
        this.parent = parent;
        this.store = store;
        this.existing = existing;
    }

    protected abstract void openScreen(Screen screen);

    private void show(Screen screen) {
        if (minecraft != null) {
            openScreen(screen);
        }
    }

    @Override
    protected void init() {
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        int left = width / 2 - FIELD_WIDTH / 2;
        int y = panelY + 40;

        nameBox = new EditBox(font, left, y, FIELD_WIDTH, 20, Component.translatable("screen.chatnotes.group_name"));
        nameBox.setMaxLength(64);
        nameBox.setHint(Component.translatable("screen.chatnotes.group_name_hint"));
        if (existing != null) {
            nameBox.setValue(existing.name());
        }
        nameBox.setResponder(value -> refreshSaveState());
        addRenderableWidget(nameBox);

        int buttonY = panelY + PANEL_HEIGHT - 32;
        saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.save"), b -> save())
                .bounds(width / 2 - BUTTON_WIDTH - 4, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(width / 2 + 4, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        refreshSaveState();
        // Keep focus off initially so the hint/placeholder stays visible.
    }

    private void refreshSaveState() {
        if (saveButton != null) {
            saveButton.active = nameBox != null && !nameBox.getValue().isBlank();
        }
    }

    private void save() {
        String name = nameBox.getValue() == null ? "" : nameBox.getValue().trim();
        if (name.isBlank()) {
            return;
        }

        String selectId;
        if (existing == null) {
            Group created = store.addGroup(name);
            selectId = created.id();
            parent.log().info("[Chat Notes] Created group {} ({})", created.name(), created.id());
        } else {
            Group renamed = store.renameGroup(existing.id(), name).orElse(existing);
            selectId = renamed.id();
            parent.log().info("[Chat Notes] Renamed group {}", renamed.id());
        }

        parent.onGroupsChanged(selectId);
        show(parent);
    }

    @Override
    public void onClose() {
        show(parent);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        // Drawn under widgets so the field and buttons stay clickable and visible.
        int panelX = width / 2 - PANEL_WIDTH / 2;
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        GroupsPainting.panel(new ExtractorPainter(graphics, font), panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Painter painter = new ExtractorPainter(graphics, font);
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        GroupsPainting.title(painter, title.getString(), width / 2, panelY + 14);
        GroupsPainting.fieldLabel(
                painter,
                MinecraftTranslator.INSTANCE.tr("screen.chatnotes.group_name"),
                width / 2 - FIELD_WIDTH / 2,
                panelY + 28);
    }
}
