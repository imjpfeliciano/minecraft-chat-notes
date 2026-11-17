package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.core.data.Group;
import com.imjpfeliciano.chatnotes.core.data.NoteStore;
import com.imjpfeliciano.chatnotes.core.model.GroupFilter;
import com.imjpfeliciano.chatnotes.core.platform.ModLog;
import com.imjpfeliciano.chatnotes.core.render.GroupsPainting;
import com.imjpfeliciano.chatnotes.core.render.Painter;
import com.imjpfeliciano.chatnotes.mc.FontTextMeasurer;
import com.imjpfeliciano.chatnotes.mc.ExtractorPainter;
import com.imjpfeliciano.chatnotes.mc.MinecraftTranslator;
import com.imjpfeliciano.chatnotes.mc.UiPlatform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Create, rename, and delete single-level note groups. */
public abstract class AbstractGroupsScreen extends Screen {

    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_WIDTH = 100;
    private static final int PADDING = 4;
    private static final int LIST_ITEM_HEIGHT = 40;

    private final AbstractNotesScreen parent;
    protected final NoteStore store;
    private final ModLog log;
    private String selectedGroupId;

    private GroupList list;
    private Button renameButton;
    private Button deleteButton;
    private Button setDefaultButton;

    protected AbstractGroupsScreen(AbstractNotesScreen parent, NoteStore store, UiPlatform platform) {
        super(Component.translatable("screen.chatnotes.groups"));
        this.parent = parent;
        this.store = store;
        this.log = platform.log();
    }

    protected abstract void openScreen(Screen screen);

    protected abstract Screen newEditor(Group group);

    protected abstract Screen newDelete(Group group);

    protected final void show(Screen screen) {
        if (minecraft != null) {
            openScreen(screen);
        }
    }

    protected ModLog log() {
        return log;
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

        addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.group_add"), b -> show(newEditor(null)))
                .bounds(left, row1, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        renameButton = addRenderableWidget(Button.builder(Component.translatable("screen.chatnotes.group_rename"), b -> {
                    Group selected = selectedGroup();
                    if (selected != null) {
                        show(newEditor(selected));
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
            boolean isDefault = hasSelection && GroupFilter.isDefault(selected.id(), store.defaultGroupFilter());
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

    private void onDelete() {
        Group group = selectedGroup();
        if (group != null) {
            show(newDelete(group));
        }
    }

    protected void onGroupsChanged(String selectGroupId) {
        selectedGroupId = selectGroupId;
        rebuildList();
    }

    @Override
    public void onClose() {
        show(parent.withRefreshedGroups());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        Painter painter = new ExtractorPainter(graphics, font);
        MinecraftTranslator translator = MinecraftTranslator.INSTANCE;
        GroupsPainting.title(painter, title.getString(), width / 2, 12);
        GroupsPainting.subtitle(painter, translator.tr("screen.chatnotes.groups_subtitle"), width / 2, 26);

        if (list != null && list.children().isEmpty()) {
            GroupsPainting.empty(
                    painter,
                    translator.tr("screen.chatnotes.groups_empty"),
                    translator.tr("screen.chatnotes.groups_empty_hint"),
                    width / 2,
                    height / 2,
                    width);
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
        public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovering, float partialTick) {
            MinecraftTranslator translator = MinecraftTranslator.INSTANCE;
            long count = store.countNotesInGroup(group.id());
            String countLabel = count == 1
                    ? translator.tr("screen.chatnotes.group_notes_one")
                    : translator.tr("screen.chatnotes.group_notes_many", count);
            GroupsPainting.row(
                    new ExtractorPainter(graphics, font),
                    new FontTextMeasurer(font),
                    getContentX(),
                    getContentY(),
                    getContentWidth(),
                    getContentHeight(),
                    list.getSelected() == this,
                    hovering,
                    group.id(),
                    group.name(),
                    countLabel,
                    group.id().equals(store.defaultGroupFilter()),
                    translator.tr("screen.chatnotes.group_default_badge"));
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            list.setSelected(this);
            if (doubleClick) {
                show(newEditor(group));
            }
            return true;
        }

        @Override
        public Component getNarration() {
            return Component.translatable("narrator.select", group.name());
        }
    }
}
