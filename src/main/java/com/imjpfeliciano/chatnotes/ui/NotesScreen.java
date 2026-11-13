package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.ChatNotesClient;
import com.imjpfeliciano.chatnotes.data.Note;
import com.imjpfeliciano.chatnotes.data.NoteStore;
import com.imjpfeliciano.chatnotes.util.AmpersandFormat;
import com.imjpfeliciano.chatnotes.util.ClipboardUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.List;

/**
 * Main notes browser: search, select, Copy / Use / Edit / Delete / reorder.
 */
public class NotesScreen extends Screen {

    private static final int CHAT_SOFT_LIMIT = 256;
    private static final int TOP = 32;
    private static final int SEARCH_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 74;
    private static final int PADDING = 4;
    private static final int LIST_ITEM_HEIGHT = 36;
    private static final int FOOTER_ROWS = 2;

    private final NoteStore store;
    private String searchQuery;
    private String selectedNoteId;
    private String statusMessage = "";

    private EditBox searchBox;
    private NoteList list;
    private Button useButton;
    private Button copyButton;
    private Button editButton;
    private Button deleteButton;
    private Button moveUpButton;
    private Button moveDownButton;

    public NotesScreen(NoteStore store) {
        this(store, "", null);
    }

    public NotesScreen(NoteStore store, String searchQuery, String selectedNoteId) {
        super(Component.translatable("screen.chatnotes.notes"));
        this.store = store;
        this.searchQuery = searchQuery == null ? "" : searchQuery;
        this.selectedNoteId = selectedNoteId;
    }

    String searchQuery() {
        return searchQuery;
    }

    @Override
    protected void init() {
        int footerHeight = FOOTER_ROWS * (BUTTON_HEIGHT + PADDING) + PADDING;
        int searchY = TOP;
        int listTop = searchY + SEARCH_HEIGHT + PADDING;
        int listBottom = height - footerHeight;
        int listHeight = Math.max(LIST_ITEM_HEIGHT, listBottom - listTop);

        searchBox = new EditBox(font, width / 2 - 160, searchY, 240, SEARCH_HEIGHT, Component.translatable("screen.chatnotes.search"));
        searchBox.setMaxLength(128);
        searchBox.setHint(Component.translatable("screen.chatnotes.search_hint"));
        searchBox.setValue(searchQuery);
        searchBox.setResponder(query -> {
            searchQuery = query;
            rebuildList();
        });
        addRenderableWidget(searchBox);

        addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.add"), button -> openEditor(null))
                .bounds(width / 2 + 86, searchY, 74, SEARCH_HEIGHT)
                .build());

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
        setInitialFocus(searchBox);
    }

    private void rebuildList() {
        if (list == null) {
            return;
        }
        List<Note> notes = store.search(searchQuery);
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
        if (useButton != null) {
            useButton.active = hasSelection;
            copyButton.active = hasSelection;
            editButton.active = hasSelection;
            deleteButton.active = hasSelection;
            moveUpButton.active = hasSelection && searchQuery.isBlank();
            moveDownButton.active = hasSelection && searchQuery.isBlank();
        }
    }

    private void openEditor(Note note) {
        if (minecraft == null) {
            return;
        }
        minecraft.setScreen(new NoteEditScreen(this, store, note));
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
            minecraft.setScreen(new NotesScreen(store, searchQuery, selectedNoteId));
        }, title, message));
    }

    private void onMove(int direction) {
        Note note = selectedNote();
        if (note == null || !searchQuery.isBlank()) {
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
            Component empty = searchQuery.isBlank()
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

        // Package-visible bridge so entries can keep the selected row on screen after rebuilds.
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
