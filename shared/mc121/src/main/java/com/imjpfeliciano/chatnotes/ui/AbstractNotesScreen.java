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
import com.imjpfeliciano.chatnotes.core.render.Translator;
import com.imjpfeliciano.chatnotes.core.text.AmpersandParser;
import com.imjpfeliciano.chatnotes.mc.FontTextMeasurer;
import com.imjpfeliciano.chatnotes.mc.GuiGraphicsPainter;
import com.imjpfeliciano.chatnotes.mc.MinecraftTranslator;
import com.imjpfeliciano.chatnotes.mc.UiPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Main notes browser. Version modules subclass this to supply {@link #openScreen} and
 * {@link #chatScreenWith}, which are the only calls that differ between 1.21 and the 26.x line.
 */
public abstract class AbstractNotesScreen extends Screen {

    private static final int TOP = 32;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 74;
    private static final int PADDING = 4;
    private static final int LIST_ITEM_HEIGHT = 36;
    private static final int FOOTER_ROWS = 2;

    protected final NoteStore store;
    private final ClipboardAccess clipboard;
    private final ModLog log;

    private String searchQuery;
    private String selectedNoteId;
    private String groupFilter;
    private String statusMessage = "";

    private EditBox searchBox;
    private NoteList list;
    private Button useButton;
    private Button copyButton;
    private Button editButton;
    private Button deleteButton;
    private Button moveUpButton;
    private Button moveDownButton;
    private Button setDefaultFilterButton;

    protected AbstractNotesScreen(NoteStore store, String searchQuery, String selectedNoteId, String groupFilter, UiPlatform platform) {
        super(Component.translatable("screen.chatnotes.notes"));
        this.store = store;
        this.clipboard = platform.clipboard();
        this.log = platform.log();
        this.searchQuery = searchQuery == null ? "" : searchQuery;
        this.selectedNoteId = selectedNoteId;
        this.groupFilter = groupFilter == null ? GroupFilter.ALL : groupFilter;
    }

    protected abstract void openScreen(Screen screen);

    protected abstract Screen chatScreenWith(String text);

    protected abstract Screen newEditor(Note note);

    protected abstract Screen newGroups();

    /** A fresh notes screen carrying the current search, selection and filter. */
    protected abstract Screen reopen(String searchQuery, String selectedNoteId, String groupFilter);

    protected final void show(Screen screen) {
        if (minecraft != null) {
            openScreen(screen);
        }
    }

    protected String searchQuery() {
        return searchQuery;
    }

    protected String groupFilter() {
        return groupFilter;
    }

    /** Rebuild after returning from the groups manager so the filter list stays in sync. */
    public Screen withRefreshedGroups() {
        groupFilter = GroupFilter.resolve(groupFilter, store.findGroup(groupFilter).isPresent());
        return reopen(searchQuery, selectedNoteId, groupFilter);
    }

    @Override
    protected void init() {
        int footerHeight = FOOTER_ROWS * (BUTTON_HEIGHT + PADDING) + PADDING;
        int searchY = TOP;
        int filterY = searchY + ROW_HEIGHT + PADDING;
        int listTop = filterY + ROW_HEIGHT + PADDING;
        int listBottom = height - footerHeight;
        int listHeight = Math.max(LIST_ITEM_HEIGHT, listBottom - listTop);

        searchBox = new EditBox(font, width / 2 - 160, searchY, 200, ROW_HEIGHT, Component.translatable("screen.chatnotes.search"));
        searchBox.setMaxLength(128);
        searchBox.setHint(Component.translatable("screen.chatnotes.search_hint"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(query -> {
            searchQuery = query;
            rebuildList();
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.add"), button -> show(newEditor(null)))
                .bounds(width / 2 + 46, searchY, 70, ROW_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.groups"), button -> show(newGroups()))
                .bounds(width / 2 + 120, searchY, 70, ROW_HEIGHT)
                .build());

        List<String> filterValues = filterOptions();
        groupFilter = GroupFilter.resolve(groupFilter, filterValues.contains(groupFilter));
        addRenderableWidget(CycleButton.builder(this::filterLabel, groupFilter)
                .withValues(filterValues)
                .create(
                        width / 2 - 160,
                        filterY,
                        240,
                        ROW_HEIGHT,
                        Component.translatable("screen.chatnotes.group_filter"),
                        (button, value) -> {
                            groupFilter = value;
                            updateDefaultFilterButton();
                            rebuildList();
                        }));
        setDefaultFilterButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.chatnotes.group_filter_set_default"),
                        b -> setCurrentFilterAsDefault())
                .bounds(width / 2 + 86, filterY, 104, ROW_HEIGHT)
                .build());
        updateDefaultFilterButton();

        list = new NoteList(minecraft, width, listHeight, listTop);
        addRenderableWidget(list);
        rebuildList();

        int row1Y = height - footerHeight + PADDING;
        int row2Y = row1Y + BUTTON_HEIGHT + PADDING;
        int left = width / 2 - (4 * BUTTON_WIDTH + 3 * PADDING) / 2;

        useButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.use"), b -> onUse())
                .bounds(left, row1Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        copyButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.copy"), b -> onCopy())
                .bounds(left + BUTTON_WIDTH + PADDING, row1Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        editButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.edit"), b -> {
                    Note selected = selectedNote();
                    if (selected != null) {
                        show(newEditor(selected));
                    }
                })
                .bounds(left + 2 * (BUTTON_WIDTH + PADDING), row1Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.delete"), b -> onDelete())
                .bounds(left + 3 * (BUTTON_WIDTH + PADDING), row1Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());

        moveUpButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.move_up"), b -> onMove(-1))
                .bounds(left, row2Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        moveDownButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.move_down"), b -> onMove(1))
                .bounds(left + BUTTON_WIDTH + PADDING, row2Y, BUTTON_WIDTH, BUTTON_HEIGHT).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(left + 2 * (BUTTON_WIDTH + PADDING), row2Y, BUTTON_WIDTH * 2 + PADDING, BUTTON_HEIGHT)
                .build());

        updateActionButtons();
        // Do not focus the search box on open: EditBox only draws its hint when unfocused and empty.
    }

    private List<String> filterOptions() {
        List<String> values = new ArrayList<>();
        values.add(GroupFilter.ALL);
        values.add(GroupFilter.UNGROUPED);
        for (Group group : store.groups()) {
            values.add(group.id());
        }
        return values;
    }

    private Component filterLabel(String value) {
        Component base;
        if (GroupFilter.ALL.equals(value)) {
            base = Component.translatable("screen.chatnotes.group_filter_all");
        } else if (GroupFilter.UNGROUPED.equals(value)) {
            base = Component.translatable("screen.chatnotes.group_filter_ungrouped");
        } else {
            base = store.findGroup(value)
                    .map(group -> Component.literal(group.name()))
                    .orElse(Component.translatable("screen.chatnotes.group_filter_all"));
        }
        if (GroupFilter.isDefault(value, store.defaultGroupFilter())) {
            return Component.translatable("screen.chatnotes.group_filter_default_mark", base);
        }
        return base;
    }

    private void setCurrentFilterAsDefault() {
        store.setDefaultGroupFilter(GroupFilter.toStore(groupFilter));
        statusMessage = MinecraftTranslator.INSTANCE.tr("screen.chatnotes.group_filter_default_saved");
        updateDefaultFilterButton();
        // Rebuild widgets so the cycle button label picks up the ★ mark.
        rebuildWidgets();
    }

    private void updateDefaultFilterButton() {
        if (setDefaultFilterButton == null) {
            return;
        }
        boolean alreadyDefault = GroupFilter.isDefault(groupFilter, store.defaultGroupFilter());
        setDefaultFilterButton.active = !alreadyDefault;
        setDefaultFilterButton.setMessage(Component.translatable(
                alreadyDefault
                        ? "screen.chatnotes.group_filter_is_default"
                        : "screen.chatnotes.group_filter_set_default"));
    }

    private void rebuildList() {
        if (list == null) {
            return;
        }
        List<Note> notes = store.search(searchQuery, GroupFilter.toStore(groupFilter));
        list.replaceEntries(notes.stream().map(NoteEntry::new).toList());

        NoteEntry match = null;
        if (selectedNoteId != null) {
            for (NoteEntry entry : list.children()) {
                if (entry.note.id().equals(selectedNoteId)) {
                    match = entry;
                    break;
                }
            }
        }
        list.setSelected(match);
        if (match != null) {
            list.ensureVisible(match);
        }
        updateActionButtons();
    }

    private Note selectedNote() {
        NoteEntry entry = list == null ? null : list.getSelected();
        return entry == null ? null : entry.note;
    }

    private void updateActionButtons() {
        boolean hasSelection = selectedNote() != null;
        boolean reorder = GroupFilter.canReorder(searchQuery, groupFilter);
        if (useButton != null) {
            useButton.active = hasSelection;
            copyButton.active = hasSelection;
            editButton.active = hasSelection;
            deleteButton.active = hasSelection;
            moveUpButton.active = hasSelection && reorder;
            moveDownButton.active = hasSelection && reorder;
        }
    }

    private void onCopy() {
        Note note = selectedNote();
        if (note == null) {
            return;
        }
        clipboard.set(note.text());
        statusMessage = MinecraftTranslator.INSTANCE.tr("screen.chatnotes.copied");
        log.info("[Chat Notes] Copied note {} ({} chars).", note.id(), note.text().length());
    }

    private void onUse() {
        Note note = selectedNote();
        if (note == null) {
            return;
        }
        log.info("[Chat Notes] Use note {} ({} chars).", note.id(), note.text().length());
        show(chatScreenWith(note.text()));
    }

    private void onDelete() {
        Note note = selectedNote();
        if (note == null) {
            return;
        }
        Component title = Component.translatable("screen.chatnotes.delete_confirm_title");
        Component message = Component.translatable("screen.chatnotes.delete_confirm_message", note.displayLabel());
        show(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                store.delete(note.id());
                if (note.id().equals(selectedNoteId)) {
                    selectedNoteId = null;
                }
                log.info("[Chat Notes] Deleted note {}.", note.id());
            }
            show(reopen(searchQuery, selectedNoteId, groupFilter));
        }, title, message));
    }

    private void onMove(int direction) {
        Note note = selectedNote();
        if (note == null || !GroupFilter.canReorder(searchQuery, groupFilter)) {
            return;
        }
        boolean moved = direction < 0 ? store.moveUp(note.id()) : store.moveDown(note.id());
        if (moved) {
            selectedNoteId = note.id();
            rebuildList();
        }
    }

    void selectNote(Note note) {
        selectedNoteId = note.id();
        statusMessage = "";
        updateActionButtons();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        Painter painter = new GuiGraphicsPainter(graphics, font);
        TextMeasurer measurer = new FontTextMeasurer(font);
        Translator translator = MinecraftTranslator.INSTANCE;

        NotesPainting.title(painter, title.getString(), width / 2, 12);

        int footerTop = height - FOOTER_ROWS * (BUTTON_HEIGHT + PADDING);
        int messageY = footerTop - measurer.lineHeight() - 2;
        Note selected = selectedNote();
        if (selected != null && AmpersandParser.containsSectionSign(selected.text())) {
            NotesPainting.footer(painter, translator.tr("screen.chatnotes.section_sign_warning"), width / 2, messageY, true);
        } else if (!statusMessage.isEmpty()) {
            NotesPainting.footer(painter, statusMessage, width / 2, messageY, false);
        }
        if (list != null && list.children().isEmpty()) {
            NotesPainting.empty(
                    painter,
                    translator.tr(NoteRules.emptyMessageKey(searchQuery, groupFilter)),
                    width / 2,
                    height / 2 - 10);
        }
    }

    private class NoteList extends ObjectSelectionList<NoteEntry> {
        NoteList(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, LIST_ITEM_HEIGHT);
        }

        @Override
        public void setSelected(NoteEntry entry) {
            super.setSelected(entry);
            if (entry != null) {
                selectNote(entry.note);
            } else {
                selectedNoteId = null;
                updateActionButtons();
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(340, width - 40);
        }

        void ensureVisible(NoteEntry entry) {
            scrollToEntry(entry);
        }
    }

    private class NoteEntry extends ObjectSelectionList.Entry<NoteEntry> {
        private final Note note;

        NoteEntry(Note note) {
            this.note = note;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            Translator translator = MinecraftTranslator.INSTANCE;
            String groupName = store.findGroup(note.groupId()).map(Group::name).orElse(null);
            NotesPainting.row(
                    new GuiGraphicsPainter(graphics, font),
                    new FontTextMeasurer(font),
                    getContentX(),
                    getContentY(),
                    getContentWidth(),
                    note.title(),
                    note.text(),
                    AmpersandParser.containsSectionSign(note.text()),
                    translator.tr("screen.chatnotes.section_sign_marker"),
                    translator.tr("screen.chatnotes.char_count", note.text().length()),
                    NoteRules.groupBadge(note.groupId(), groupName));
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            list.setSelected(this);
            if (doubleClick) {
                onUse();
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", note.displayLabel());
        }
    }
}
