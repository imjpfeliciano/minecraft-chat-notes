package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.ChatNotesClient;
import com.imjpfeliciano.chatnotes.data.Group;
import com.imjpfeliciano.chatnotes.data.Note;
import com.imjpfeliciano.chatnotes.data.NoteStore;
import com.imjpfeliciano.chatnotes.util.AmpersandFormat;
import com.imjpfeliciano.chatnotes.util.ClipboardUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.ArrayList;
import java.util.List;

/**
 * Main notes browser: search, group filter, select, Copy / Use / Edit / Delete / reorder.
 */
public class NotesScreen extends Screen {

    /** Sentinel for "show every note". */
    public static final String FILTER_ALL = "__all__";
    /** Sentinel for notes with no group. */
    public static final String FILTER_UNGROUPED = "__ungrouped__";

    private static final int CHAT_SOFT_LIMIT = 256;
    private static final int TOP = 32;
    private static final int ROW_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 74;
    private static final int PADDING = 4;
    private static final int LIST_ITEM_HEIGHT = 36;
    private static final int FOOTER_ROWS = 2;

    private final NoteStore store;
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

    public NotesScreen(NoteStore store) {
        this(store, "", null, fromStoreFilter(store.defaultGroupFilter()));
    }

    public NotesScreen(NoteStore store, String searchQuery, String selectedNoteId) {
        this(store, searchQuery, selectedNoteId, fromStoreFilter(store.defaultGroupFilter()));
    }

    public NotesScreen(NoteStore store, String searchQuery, String selectedNoteId, String groupFilter) {
        super(Component.translatable("screen.chatnotes.notes"));
        this.store = store;
        this.searchQuery = searchQuery == null ? "" : searchQuery;
        this.selectedNoteId = selectedNoteId;
        this.groupFilter = groupFilter == null ? FILTER_ALL : groupFilter;
    }

    /** Store uses null/"" /id; UI uses FILTER_ALL / FILTER_UNGROUPED / id. */
    public static String fromStoreFilter(String storeFilter) {
        if (storeFilter == null) {
            return FILTER_ALL;
        }
        if (storeFilter.isEmpty()) {
            return FILTER_UNGROUPED;
        }
        return storeFilter;
    }

    static String toStoreFilter(String uiFilter) {
        if (FILTER_ALL.equals(uiFilter)) {
            return null;
        }
        if (FILTER_UNGROUPED.equals(uiFilter)) {
            return "";
        }
        return uiFilter;
    }

    String searchQuery() {
        return searchQuery;
    }

    String groupFilter() {
        return groupFilter;
    }

    /** Rebuild after returning from the groups manager so the filter list stays in sync. */
    NotesScreen withRefreshedGroups() {
        if (!FILTER_ALL.equals(groupFilter)
                && !FILTER_UNGROUPED.equals(groupFilter)
                && store.findGroup(groupFilter).isEmpty()) {
            groupFilter = FILTER_ALL;
        }
        return new NotesScreen(store, searchQuery, selectedNoteId, groupFilter);
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

        addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.add"), button -> openEditor(null))
                .bounds(width / 2 + 46, searchY, 70, ROW_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.groups"), button -> openGroups())
                .bounds(width / 2 + 120, searchY, 70, ROW_HEIGHT)
                .build());

        List<String> filterValues = filterOptions();
        if (!filterValues.contains(groupFilter)) {
            groupFilter = FILTER_ALL;
        }
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
                        openEditor(selected);
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
        values.add(FILTER_ALL);
        values.add(FILTER_UNGROUPED);
        for (Group group : store.groups()) {
            values.add(group.id());
        }
        return values;
    }

    private Component filterLabel(String value) {
        Component base;
        if (FILTER_ALL.equals(value)) {
            base = Component.translatable("screen.chatnotes.group_filter_all");
        } else if (FILTER_UNGROUPED.equals(value)) {
            base = Component.translatable("screen.chatnotes.group_filter_ungrouped");
        } else {
            base = store.findGroup(value)
                    .map(group -> Component.literal(group.name()))
                    .orElse(Component.translatable("screen.chatnotes.group_filter_all"));
        }
        if (value.equals(fromStoreFilter(store.defaultGroupFilter()))) {
            return Component.translatable("screen.chatnotes.group_filter_default_mark", base);
        }
        return base;
    }

    private void setCurrentFilterAsDefault() {
        store.setDefaultGroupFilter(toStoreFilter(groupFilter));
        statusMessage = Component.translatable("screen.chatnotes.group_filter_default_saved").getString();
        updateDefaultFilterButton();
        // Rebuild widgets so the cycle button label picks up the ★ mark.
        rebuildWidgets();
    }

    private void updateDefaultFilterButton() {
        if (setDefaultFilterButton == null) {
            return;
        }
        boolean alreadyDefault = groupFilter.equals(fromStoreFilter(store.defaultGroupFilter()));
        setDefaultFilterButton.active = !alreadyDefault;
        setDefaultFilterButton.setMessage(Component.translatable(
                alreadyDefault
                        ? "screen.chatnotes.group_filter_is_default"
                        : "screen.chatnotes.group_filter_set_default"));
    }

    private String storeGroupFilter() {
        if (FILTER_ALL.equals(groupFilter)) {
            return null;
        }
        if (FILTER_UNGROUPED.equals(groupFilter)) {
            return "";
        }
        return groupFilter;
    }

    private void rebuildList() {
        if (list == null) {
            return;
        }
        List<Note> notes = store.search(searchQuery, storeGroupFilter());
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

    private boolean canReorder() {
        return searchQuery.isBlank() && FILTER_ALL.equals(groupFilter);
    }

    private void updateActionButtons() {
        boolean hasSelection = selectedNote() != null;
        if (useButton != null) {
            useButton.active = hasSelection;
            copyButton.active = hasSelection;
            editButton.active = hasSelection;
            deleteButton.active = hasSelection;
            moveUpButton.active = hasSelection && canReorder();
            moveDownButton.active = hasSelection && canReorder();
        }
    }

    private void openEditor(Note note) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new NoteEditScreen(this, store, note));
    }

    private void openGroups() {
        if (minecraft != null) {
            minecraft.setScreen(new GroupsScreen(this, store));
        }
    }

    private void onCopy() {
        Note note = selectedNote();
        if (note == null) {
            return;
        }
        ClipboardUtil.set(note.text());
        statusMessage = Component.translatable("screen.chatnotes.copied").getString();
        ChatNotesClient.LOGGER.info("[Chat Notes] Copied note {} ({} chars).", note.id(), note.text().length());
    }

    private void onUse() {
        Note note = selectedNote();
        if (note == null || minecraft == null) {
            return;
        }
        ChatNotesClient.LOGGER.info("[Chat Notes] Use note {} ({} chars).", note.id(), note.text().length());
        minecraft.setScreen(new ChatScreen(note.text(), false));
    }

    private void onDelete() {
        Note note = selectedNote();
        if (note == null || minecraft == null) {
            return;
        }
        Component title = Component.translatable("screen.chatnotes.delete_confirm_title");
        Component message = Component.translatable("screen.chatnotes.delete_confirm_message", note.displayLabel());
        minecraft.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                store.delete(note.id());
                if (note.id().equals(selectedNoteId)) {
                    selectedNoteId = null;
                }
                ChatNotesClient.LOGGER.info("[Chat Notes] Deleted note {}.", note.id());
            }
            minecraft.setScreen(new NotesScreen(store, searchQuery, selectedNoteId, groupFilter));
        }, title, message));
    }

    private void onMove(int direction) {
        Note note = selectedNote();
        if (note == null || !canReorder()) {
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
        graphics.drawCenteredString(font, title, width / 2, 12, CommonColors.WHITE);
        int footerTop = height - FOOTER_ROWS * (BUTTON_HEIGHT + PADDING);
        Note selected = selectedNote();
        if (selected != null && AmpersandFormat.containsSectionSign(selected.text())) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.chatnotes.section_sign_warning"),
                    width / 2,
                    footerTop - font.lineHeight - 2,
                    CommonColors.SOFT_RED);
        } else if (!statusMessage.isEmpty()) {
            graphics.drawCenteredString(
                    font,
                    statusMessage,
                    width / 2,
                    footerTop - font.lineHeight - 2,
                    CommonColors.GREEN);
        }
        if (list != null && list.children().isEmpty()) {
            Component empty = searchQuery.isBlank() && FILTER_ALL.equals(groupFilter)
                    ? Component.translatable("screen.chatnotes.empty")
                    : Component.translatable("screen.chatnotes.no_matches");
            graphics.drawCenteredString(font, empty, width / 2, height / 2 - 10, CommonColors.LIGHT_GRAY);
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
            int x = getContentX();
            int y = getContentY();
            int rowWidth = getContentWidth();

            int rightEdge = x + rowWidth - 4;
            if (AmpersandFormat.containsSectionSign(note.text())) {
                Component bang = Component.translatable("screen.chatnotes.section_sign_marker");
                rightEdge -= font.width(bang);
                graphics.drawString(font, bang, rightEdge, y + 2, CommonColors.SOFT_RED);
                rightEdge -= 4;
            }

            int length = note.text().length();
            if (length > CHAT_SOFT_LIMIT) {
                Component count = Component.translatable("screen.chatnotes.char_count", length);
                rightEdge -= font.width(count);
                graphics.drawString(font, count, rightEdge, y + 2, CommonColors.SOFT_RED);
                rightEdge -= 4;
            }

            String groupLabel = groupBadgeLabel(note);
            if (groupLabel != null) {
                String clipped = font.plainSubstrByWidth(groupLabel, 90);
                int padX = 3;
                int badgeW = font.width(clipped) + padX * 2;
                int badgeH = font.lineHeight + 2;
                int badgeX = rightEdge - badgeW;
                int badgeY = y + 1;
                graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xAA2A2A2A);
                graphics.renderOutline(badgeX, badgeY, badgeW, badgeH, 0x66FFFFFF);
                graphics.drawString(font, clipped, badgeX + padX, badgeY + 1, 0xFFC8C8C8);
                rightEdge = badgeX - 4;
            }

            int textWidth = Math.max(16, rightEdge - (x + 4));
            if (note.title().isBlank()) {
                AmpersandFormat.drawClipped(graphics, font, note.text(), x + 4, y + 2, textWidth, CommonColors.WHITE);
            } else {
                String titleLine = font.plainSubstrByWidth(note.title(), textWidth);
                graphics.drawString(font, titleLine, x + 4, y + 2, CommonColors.WHITE);
                AmpersandFormat.drawClipped(
                        graphics,
                        font,
                        note.text(),
                        x + 4,
                        y + 2 + font.lineHeight + 1,
                        rowWidth - 8,
                        CommonColors.LIGHT_GRAY);
            }
        }

        /** Group name for the row badge, or null when ungrouped (no badge). */
        private String groupBadgeLabel(Note note) {
            String groupId = note.groupId();
            if (groupId == null || groupId.isBlank()) {
                return null;
            }
            return store.findGroup(groupId).map(Group::name).orElse(null);
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
