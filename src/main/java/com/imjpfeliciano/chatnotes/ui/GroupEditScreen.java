package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.ChatNotesClient;
import com.imjpfeliciano.chatnotes.data.Group;
import com.imjpfeliciano.chatnotes.data.NoteStore;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

/** Create or rename a single group. */
public class GroupEditScreen extends Screen {

    private static final int PANEL_WIDTH = 320;
    private static final int PANEL_HEIGHT = 120;
    private static final int FIELD_WIDTH = 280;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;

    private final GroupsScreen parent;
    private final NoteStore store;
    private final Group existing;

    private EditBox nameBox;
    private Button saveButton;

    public GroupEditScreen(GroupsScreen parent, NoteStore store, Group existing) {
        super(existing == null
                ? Component.translatable("screen.chatnotes.group_edit_new")
                : Component.translatable("screen.chatnotes.group_edit_existing"));
        this.parent = parent;
        this.store = store;
        this.existing = existing;
    }

    @Override
    protected void init() {
        int panelX = width / 2 - PANEL_WIDTH / 2;
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
            ChatNotesClient.LOGGER.info("[Chat Notes] Created group {} ({})", created.name(), created.id());
        } else {
            Group renamed = store.renameGroup(existing.id(), name).orElse(existing);
            selectId = renamed.id();
            ChatNotesClient.LOGGER.info("[Chat Notes] Renamed group {}", renamed.id());
        }

        if (minecraft != null) {
            parent.onGroupsChanged(selectId);
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
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        // Drawn under widgets so the field/buttons stay clickable and visible.
        int panelX = width / 2 - PANEL_WIDTH / 2;
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xCC1A1A1A);
        graphics.renderOutline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0x88FFFFFF);
        graphics.fill(panelX, panelY, panelX + 3, panelY + PANEL_HEIGHT, 0xFF5B8DEF);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int panelY = height / 2 - PANEL_HEIGHT / 2;
        graphics.drawCenteredString(font, title, width / 2, panelY + 14, CommonColors.WHITE);
        graphics.drawString(
                font,
                Component.translatable("screen.chatnotes.group_name"),
                width / 2 - FIELD_WIDTH / 2,
                panelY + 28,
                CommonColors.LIGHT_GRAY);
    }
}
