package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.ChatNotesClient;
import com.imjpfeliciano.chatnotes.data.Group;
import com.imjpfeliciano.chatnotes.data.Note;
import com.imjpfeliciano.chatnotes.data.NoteStore;
import com.imjpfeliciano.chatnotes.util.AmpersandFormat;
import com.imjpfeliciano.chatnotes.util.ClipboardUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;

/**
 * Create or edit a single note. Line breaks are collapsed to spaces on save (chat is single-line).
 */
public class NoteEditScreen extends Screen {

    private static final int CHAT_SOFT_LIMIT = 256;
    private static final int TITLE_MAX = 64;
    private static final int TEXT_MAX = 4096;
    private static final int FIELD_WIDTH = 320;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 8;

    private final NotesScreen parent;
    private final NoteStore store;
    private final Note existing;

    private EditBox titleBox;
    private MultiLineEditBox textBox;
    private CycleButton<String> groupButton;
    private Button saveButton;
    private int charCountColor = CommonColors.LIGHT_GRAY;
    private String selectedGroupId = NotesScreen.FILTER_UNGROUPED;

    public NoteEditScreen(NotesScreen parent, NoteStore store, Note existing) {
        super(existing == null
                ? Component.translatable("screen.chatnotes.edit_new")
                : Component.translatable("screen.chatnotes.edit_existing"));
        this.parent = parent;
        this.store = store;
        this.existing = existing;
        if (existing != null && existing.groupId() != null && !existing.groupId().isBlank()) {
            selectedGroupId = existing.groupId();
        } else if (existing == null
                && parent.groupFilter() != null
                && !NotesScreen.FILTER_ALL.equals(parent.groupFilter())
                && !NotesScreen.FILTER_UNGROUPED.equals(parent.groupFilter())) {
            // Creating from a filtered group view defaults the new note into that group.
            selectedGroupId = parent.groupFilter();
        }
    }

    @Override
    protected void init() {
        int centerX = width / 2;
        int left = centerX - FIELD_WIDTH / 2;
        int y = 40;

        titleBox = new EditBox(font, left, y, FIELD_WIDTH, 20, Component.translatable("screen.chatnotes.field_title"));
        titleBox.setMaxLength(TITLE_MAX);
        titleBox.setHint(Component.translatable("screen.chatnotes.field_title_hint"));
        if (existing != null) {
            titleBox.setValue(existing.title());
        }
        addRenderableWidget(titleBox);

        y += 20 + PADDING;

        List<String> groupValues = groupOptions();
        if (!groupValues.contains(selectedGroupId)) {
            selectedGroupId = NotesScreen.FILTER_UNGROUPED;
        }
        groupButton = addRenderableWidget(CycleButton.builder(this::groupLabel, selectedGroupId)
                .withValues(groupValues)
                .create(
                        left,
                        y,
                        FIELD_WIDTH,
                        20,
                        Component.translatable("screen.chatnotes.field_group"),
                        (button, value) -> selectedGroupId = value));

        y += 20 + PADDING + font.lineHeight + 2;

        // Leave room for counter, optional § warning, coloured preview, and buttons.
        int textHeight = Math.max(80, height - y - 100);
        textBox = MultiLineEditBox.builder()
                .setX(left)
                .setY(y)
                .setPlaceholder(Component.translatable("screen.chatnotes.field_text_hint"))
                .setShowBackground(true)
                .build(font, FIELD_WIDTH, textHeight, Component.translatable("screen.chatnotes.field_text"));
        textBox.setCharacterLimit(TEXT_MAX);
        textBox.setValueListener(value -> refreshSaveState());
        if (existing != null) {
            textBox.setValue(existing.text());
        }
        addRenderableWidget(textBox);

        int buttonY = height - 28;
        addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.paste"), b -> pasteFromClipboard())
                .bounds(left, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        saveButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.save"), b -> save())
                .bounds(centerX - BUTTON_WIDTH / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(left + FIELD_WIDTH - BUTTON_WIDTH, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        refreshSaveState();
        setInitialFocus(existing == null || existing.title().isBlank() ? titleBox : textBox);
    }

    private List<String> groupOptions() {
        List<String> values = new ArrayList<>();
        values.add(NotesScreen.FILTER_UNGROUPED);
        for (Group group : store.groups()) {
            values.add(group.id());
        }
        return values;
    }

    private Component groupLabel(String value) {
        if (NotesScreen.FILTER_UNGROUPED.equals(value)) {
            return Component.translatable("screen.chatnotes.group_filter_ungrouped");
        }
        return store.findGroup(value)
                .map(group -> Component.literal(group.name()))
                .orElse(Component.translatable("screen.chatnotes.group_filter_ungrouped"));
    }

    private String resolvedGroupId() {
        return NotesScreen.FILTER_UNGROUPED.equals(selectedGroupId) ? null : selectedGroupId;
    }

    private void pasteFromClipboard() {
        String clip = ClipboardUtil.get();
        if (clip == null || clip.isEmpty()) {
            return;
        }
        String current = textBox.getValue();
        if (current == null || current.isBlank()) {
            textBox.setValue(clip);
        } else {
            textBox.setValue(current + " " + clip);
        }
        refreshSaveState();
    }

    private void refreshSaveState() {
        String normalized = Note.normalizeText(textBox == null ? "" : textBox.getValue());
        int length = normalized.length();
        charCountColor = length > CHAT_SOFT_LIMIT ? CommonColors.SOFT_RED : CommonColors.LIGHT_GRAY;
        if (saveButton != null) {
            saveButton.active = !normalized.isBlank();
        }
    }

    private void save() {
        String title = titleBox.getValue() == null ? "" : titleBox.getValue().trim();
        String text = Note.normalizeText(textBox.getValue());
        if (text.isBlank()) {
            return;
        }

        String groupId = resolvedGroupId();
        Note saved;
        if (existing == null) {
            saved = store.add(title, text, groupId);
            ChatNotesClient.LOGGER.info("[Chat Notes] Added note {} ({} chars).", saved.id(), saved.text().length());
        } else {
            saved = store.update(existing.id(), title, text, groupId, true).orElse(existing);
            ChatNotesClient.LOGGER.info("[Chat Notes] Updated note {} ({} chars).", saved.id(), saved.text().length());
        }

        if (minecraft != null) {
            minecraft.setScreen(new NotesScreen(store, parent.searchQuery(), saved.id(), parent.groupFilter()));
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
        graphics.drawCenteredString(font, title, width / 2, 15, CommonColors.WHITE);

        int left = width / 2 - FIELD_WIDTH / 2;
        graphics.drawString(font, Component.translatable("screen.chatnotes.field_title"), left, 40 - font.lineHeight - 2, CommonColors.LIGHT_GRAY);

        String raw = textBox == null ? "" : textBox.getValue();
        String normalized = Note.normalizeText(raw);
        int metaY = height - 28 - font.lineHeight - 4;

        Component counter = Component.translatable("screen.chatnotes.char_counter", normalized.length(), CHAT_SOFT_LIMIT);
        graphics.drawString(font, counter, left, metaY, charCountColor);

        if (AmpersandFormat.containsSectionSign(normalized)) {
            graphics.drawString(
                    font,
                    Component.translatable("screen.chatnotes.section_sign_warning"),
                    left,
                    metaY - font.lineHeight - 2,
                    CommonColors.SOFT_RED);
            metaY -= font.lineHeight + 2;
        }

        if (!normalized.isBlank()) {
            AmpersandFormat.drawClipped(
                    graphics,
                    font,
                    normalized,
                    left,
                    metaY - font.lineHeight - 2,
                    FIELD_WIDTH,
                    CommonColors.WHITE);
        }
    }
}
