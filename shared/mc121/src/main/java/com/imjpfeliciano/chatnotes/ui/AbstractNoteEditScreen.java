package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Group;
import com.imjpfeliciano.chatnotes.core.data.Note;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.core.model.GroupFilter;
import com.imjpfeliciano.chatnotes.core.model.NoteRules;
import com.imjpfeliciano.chatnotes.core.platform.ClipboardAccess;
import com.imjpfeliciano.chatnotes.core.platform.ModLog;
import com.imjpfeliciano.chatnotes.core.render.NotesPainting;
import com.imjpfeliciano.chatnotes.core.render.Painter;
import com.imjpfeliciano.chatnotes.core.render.TextMeasurer;
import com.imjpfeliciano.chatnotes.core.render.UiColors;
import com.imjpfeliciano.chatnotes.core.text.AmpersandParser;
import com.imjpfeliciano.chatnotes.mc.FontTextMeasurer;
import com.imjpfeliciano.chatnotes.mc.GuiGraphicsPainter;
import com.imjpfeliciano.chatnotes.mc.MinecraftTranslator;
import com.imjpfeliciano.chatnotes.mc.UiPlatform;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Create or edit a single note. Line breaks are collapsed to spaces on save (chat is single-line). */
public abstract class AbstractNoteEditScreen extends Screen {

    private static final int TITLE_MAX = 64;
    private static final int TEXT_MAX = 4096;
    private static final int FIELD_WIDTH = 320;
    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 8;

    private final AbstractNotesScreen parent;
    private final NoteStore store;
    private final Note existing;
    private final ClipboardAccess clipboard;
    private final ModLog log;

    private EditBox titleBox;
    private MultiLineEditBox textBox;
    private Button saveButton;
    private String selectedGroupId;

    protected AbstractNoteEditScreen(AbstractNotesScreen parent, NoteStore store, Note existing, UiPlatform platform) {
        super(existing == null
                ? Component.translatable("screen.chatnotes.edit_new")
                : Component.translatable("screen.chatnotes.edit_existing"));
        this.parent = parent;
        this.store = store;
        this.existing = existing;
        this.clipboard = platform.clipboard();
        this.log = platform.log();
        this.selectedGroupId = GroupFilter.initialGroup(existing == null, existing == null ? null : existing.groupId(), parent.groupFilter());
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
            selectedGroupId = GroupFilter.UNGROUPED;
        }
        addRenderableWidget(CycleButton.builder(this::groupLabel, selectedGroupId)
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
        values.add(GroupFilter.UNGROUPED);
        for (Group group : store.groups()) {
            values.add(group.id());
        }
        return values;
    }

    private Component groupLabel(String value) {
        if (GroupFilter.UNGROUPED.equals(value)) {
            return Component.translatable("screen.chatnotes.group_filter_ungrouped");
        }
        return store.findGroup(value)
                .map(group -> Component.literal(group.name()))
                .orElse(Component.translatable("screen.chatnotes.group_filter_ungrouped"));
    }

    private void pasteFromClipboard() {
        String clip = clipboard.get();
        String current = textBox.getValue();
        String pasted = NoteRules.paste(current, clip);
        if (pasted.equals(current == null ? "" : current)) {
            return;
        }
        textBox.setValue(pasted);
        refreshSaveState();
    }

    private void refreshSaveState() {
        if (saveButton != null) {
            saveButton.active = NoteRules.canSave(Note.normalizeText(textBox == null ? "" : textBox.getValue()));
        }
    }

    private void save() {
        String title = titleBox.getValue() == null ? "" : titleBox.getValue().trim();
        String text = Note.normalizeText(textBox.getValue());
        if (!NoteRules.canSave(text)) {
            return;
        }

        String groupId = GroupFilter.toGroupId(selectedGroupId);
        Note saved;
        if (existing == null) {
            saved = store.add(title, text, groupId);
            log.info("[Chat Notes] Added note {} ({} chars).", saved.id(), saved.text().length());
        } else {
            saved = store.update(existing.id(), title, text, groupId, true).orElse(existing);
            log.info("[Chat Notes] Updated note {} ({} chars).", saved.id(), saved.text().length());
        }

        show(parent.reopen(parent.searchQuery(), saved.id(), parent.groupFilter()));
    }

    @Override
    public void onClose() {
        show(parent);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Painter painter = new GuiGraphicsPainter(graphics, font);
        TextMeasurer measurer = new FontTextMeasurer(font);
        MinecraftTranslator translator = MinecraftTranslator.INSTANCE;

        NotesPainting.title(painter, title.getString(), width / 2, 15);

        int left = width / 2 - FIELD_WIDTH / 2;
        painter.text(
                translator.tr("screen.chatnotes.field_title"),
                left,
                40 - measurer.lineHeight() - 2,
                UiColors.LIGHT_GRAY);

        String raw = textBox == null ? "" : textBox.getValue();
        String normalized = Note.normalizeText(raw);
        int metaY = height - 28 - measurer.lineHeight() - 4;

        NotesPainting.counter(
                painter,
                translator.tr("screen.chatnotes.char_counter", normalized.length(), NoteRules.SOFT_LIMIT),
                left,
                metaY,
                normalized.length());

        if (AmpersandParser.containsSectionSign(normalized)) {
            NotesPainting.warning(
                    painter,
                    translator.tr("screen.chatnotes.section_sign_warning"),
                    left,
                    metaY - measurer.lineHeight() - 2);
            metaY -= measurer.lineHeight() + 2;
        }

        if (!normalized.isBlank()) {
            NotesPainting.preview(painter, normalized, left, metaY - measurer.lineHeight() - 2, FIELD_WIDTH);
        }
    }
}
