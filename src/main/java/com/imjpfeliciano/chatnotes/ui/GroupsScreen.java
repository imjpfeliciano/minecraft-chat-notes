package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.data.Group;
import com.imjpfeliciano.chatnotes.data.NoteStore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

/**
 * Create, rename, and delete single-level note groups.
 */
public class GroupsScreen extends Screen {

    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 100;
    private static final int PADDING = 4;
    private static final int LIST_ITEM_HEIGHT = 40;
    private static final int ACCENT_WIDTH = 3;

    /** Soft folder-like accents cycled per row so the list feels less flat. */
    private static final int[] ACCENT_COLORS = {
            0xFF5B8DEF,
            0xFF3FAE7A,
            0xFFE0A03A,
            0xFFC86BDB,
            0xFFE25B5B,
            0xFF4DB6C6
    };

    private final NotesScreen parent;
    private final NoteStore store;
    private String selectedGroupId;

    private GroupList list;
    private Button renameButton;
    private Button deleteButton;
    private Button setDefaultButton;

    public GroupsScreen(NotesScreen parent, NoteStore store) {
        super(Component.translatable("screen.chatnotes.groups"));
        this.parent = parent;
        this.store = store;
    }

    @Override
    protected void init() {
        int footerHeight = BUTTON_HEIGHT * 2 + PADDING * 3;
        int listTop = 48;
        int listHeight = Math.max(LIST_ITEM_HEIGHT, height - footerHeight - listTop);

        list = new GroupList(minecraft, width, listHeight, listTop);
        addRenderableWidget(list);
        rebuildList();

        int row1 = height - footerHeight + PADDING;
        int row2 = row1 + BUTTON_HEIGHT + PADDING;
        int left = width / 2 - (4 * BUTTON_WIDTH + 3 * PADDING) / 2;

        addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.group_add"), b -> openEditor(null))
                .bounds(left, row1, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        renameButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.group_rename"), b -> {
                    Group selected = selectedGroup();
                    if (selected != null) {
                        openEditor(selected);
                    }
                })
                .bounds(left + BUTTON_WIDTH + PADDING, row1, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        deleteButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.group_delete"), b -> onDelete())
                .bounds(left + 2 * (BUTTON_WIDTH + PADDING), row1, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        setDefaultButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.chatnotes.group_filter_set_default"),
                        b -> setSelectedAsDefault())
                .bounds(left + 3 * (BUTTON_WIDTH + PADDING), row1, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());

        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(width / 2 - BUTTON_WIDTH, row2, BUTTON_WIDTH * 2 + PADDING, BUTTON_HEIGHT)
                .build());

        updateActionButtons();
    }

    private void rebuildList() {
        if (list == null) {
            return;
        }
        list.replaceEntries(store.groups().stream().map(GroupEntry::new).toList());
        GroupEntry match = null;
        if (selectedGroupId != null) {
            for (GroupEntry entry : list.children()) {
                if (entry.group.id().equals(selectedGroupId)) {
                    match = entry;
                    break;
                }
            }
        }
        list.setSelected(match);
        updateActionButtons();
    }

    private Group selectedGroup() {
        GroupEntry entry = list == null ? null : list.getSelected();
        return entry == null ? null : entry.group;
    }

    private void updateActionButtons() {
        Group selected = selectedGroup();
        boolean hasSelection = selected != null;
        if (renameButton != null) {
            renameButton.active = hasSelection;
            deleteButton.active = hasSelection;
            boolean isDefault = hasSelection && selected.id().equals(store.defaultGroupFilter());
            setDefaultButton.active = hasSelection && !isDefault;
            setDefaultButton.setMessage(Component.translatable(
                    isDefault
                            ? "screen.chatnotes.group_filter_is_default"
                            : "screen.chatnotes.group_filter_set_default"));
        }
    }

    private void setSelectedAsDefault() {
        Group selected = selectedGroup();
        if (selected == null) {
            return;
        }
        store.setDefaultGroupFilter(selected.id());
        rebuildList();
    }

    private void openEditor(Group group) {
        if (minecraft != null) {
            minecraft.setScreen(new GroupEditScreen(this, store, group));
        }
    }

    private void onDelete() {
        Group group = selectedGroup();
        if (group == null || minecraft == null) {
            return;
        }
        minecraft.setScreen(new GroupDeleteScreen(this, store, group));
    }

    void onGroupsChanged(String selectGroupId) {
        selectedGroupId = selectGroupId;
        rebuildList();
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent.withRefreshedGroups());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, CommonColors.WHITE);
        graphics.drawCenteredString(
                font,
                Component.translatable("screen.chatnotes.groups_subtitle"),
                width / 2,
                26,
                CommonColors.LIGHT_GRAY);

        if (list != null && list.children().isEmpty()) {
            int boxW = Math.min(320, width - 40);
            int boxH = 64;
            int boxX = width / 2 - boxW / 2;
            int boxY = height / 2 - boxH / 2;
            graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xAA1A1A1A);
            graphics.renderOutline(boxX, boxY, boxW, boxH, 0x66FFFFFF);
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.chatnotes.groups_empty"),
                    width / 2,
                    boxY + 18,
                    CommonColors.WHITE);
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.chatnotes.groups_empty_hint"),
                    width / 2,
                    boxY + 36,
                    CommonColors.LIGHT_GRAY);
        }
    }

    private class GroupList extends ObjectSelectionList<GroupEntry> {
        GroupList(Minecraft client, int width, int height, int y) {
            super(client, width, height, y, LIST_ITEM_HEIGHT);
        }

        @Override
        public void setSelected(GroupEntry entry) {
            super.setSelected(entry);
            selectedGroupId = entry == null ? null : entry.group.id();
            updateActionButtons();
        }

        @Override
        public int getRowWidth() {
            return Math.min(340, width - 40);
        }
    }

    private class GroupEntry extends ObjectSelectionList.Entry<GroupEntry> {
        private final Group group;

        GroupEntry(Group group) {
            this.group = group;
        }

        @Override
        public void renderContent(GuiGraphics graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            int x = getContentX();
            int y = getContentY();
            int w = getContentWidth();
            int h = getContentHeight();

            boolean selected = list.getSelected() == this;
            int bg = selected ? 0xAA3A4558 : hovering ? 0x88303338 : 0x661E1E1E;
            graphics.fill(x, y, x + w, y + h - 2, bg);
            graphics.renderOutline(x, y, w, h - 2, selected ? 0xAAFFFFFF : 0x33FFFFFF);

            int accent = ACCENT_COLORS[Math.floorMod(group.id().hashCode(), ACCENT_COLORS.length)];
            graphics.fill(x, y, x + ACCENT_WIDTH, y + h - 2, accent);

            long count = store.countNotesInGroup(group.id());
            boolean isDefault = group.id().equals(store.defaultGroupFilter());

            int textX = x + ACCENT_WIDTH + 8;
            String name = font.plainSubstrByWidth(group.name(), w - 90);
            graphics.drawString(font, name, textX, y + 6, CommonColors.WHITE);

            Component countLabel = count == 1
                    ? Component.translatable("screen.chatnotes.group_notes_one")
                    : Component.translatable("screen.chatnotes.group_notes_many", count);
            graphics.drawString(font, countLabel, textX, y + 6 + font.lineHeight + 2, CommonColors.LIGHT_GRAY);

            if (isDefault) {
                Component badge = Component.translatable("screen.chatnotes.group_default_badge");
                int badgeW = font.width(badge) + 8;
                int badgeH = font.lineHeight + 4;
                int badgeX = x + w - badgeW - 8;
                int badgeY = y + (h - 2 - badgeH) / 2;
                graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + badgeH, 0xAA3A5A2A);
                graphics.renderOutline(badgeX, badgeY, badgeW, badgeH, 0x8860A050);
                graphics.drawString(font, badge, badgeX + 4, badgeY + 2, 0xFFB6F5A0);
            }
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            list.setSelected(this);
            if (doubleClick) {
                openEditor(group);
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", group.name());
        }
    }
}
