package com.imjpfeliciano.chatnotes.ui;

import com.imjpfeliciano.chatnotes.ChatNotesClient;
import com.imjpfeliciano.chatnotes.data.Note;
import com.imjpfeliciano.chatnotes.data.NoteStore;
import com.imjpfeliciano.chatnotes.util.ClipboardUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.CommonColors;

import java.util.List;

/**
 * Phase 2 vertical slice: the thinnest screen that proves Copy and Use end to end.
 * Phase 3 replaces this with the full list / search / editor UI.
 */
public class NotesScreen extends Screen {

    private static final int BUTTON_WIDTH = 100;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 8;
    private static final int PREVIEW_MAX_CHARS = 80;

    private final NoteStore store;
    private Note note;
    private String statusMessage = "";

    public NotesScreen(NoteStore store) {
        super(Component.translatable("screen.chatnotes.notes"));
        this.store = store;
    }

    @Override
    protected void init() {
        List<Note> notes = store.notes();
        note = notes.isEmpty() ? null : notes.getFirst();

        int centerX = width / 2;
        int buttonY = height / 2 + 40;

        Button copy = addRenderableWidget(Button.builder(
                        Component.translatable("screen.chatnotes.copy"),
                        button -> onCopy())
                .bounds(centerX - BUTTON_WIDTH - PADDING / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        copy.active = note != null;

        Button use = addRenderableWidget(Button.builder(
                        Component.translatable("screen.chatnotes.use"),
                        button -> onUse())
                .bounds(centerX + PADDING / 2, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
        use.active = note != null;

        addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        button -> onClose())
                .bounds(centerX - BUTTON_WIDTH / 2, buttonY + BUTTON_HEIGHT + PADDING, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private void onCopy() {
        if (note == null || minecraft == null) {
            return;
        }
        ClipboardUtil.set(note.text());
        statusMessage = Component.translatable("screen.chatnotes.copied").getString();
        ChatNotesClient.LOGGER.info("[Chat Notes] Copied note {} ({} chars).", note.id(), note.text().length());
    }

    private void onUse() {
        if (note == null || minecraft == null) {
            return;
        }
        // 1.21.11: ChatScreen(String initial, boolean isDraft). Never auto-send.
        ChatNotesClient.LOGGER.info("[Chat Notes] Use note {} ({} chars).", note.id(), note.text().length());
        minecraft.setScreen(new ChatScreen(note.text(), false));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        graphics.drawCenteredString(font, title, width / 2, 40, CommonColors.WHITE);

        int centerX = width / 2;
        int y = height / 2 - 30;

        if (note == null) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.chatnotes.empty"),
                    centerX,
                    y,
                    CommonColors.LIGHT_GRAY);
        } else {
            String label = truncate(note.displayLabel(), PREVIEW_MAX_CHARS);
            graphics.drawCenteredString(font, label, centerX, y, CommonColors.WHITE);

            String preview = truncate(note.text(), PREVIEW_MAX_CHARS);
            graphics.drawCenteredString(font, preview, centerX, y + font.lineHeight + 4, CommonColors.LIGHT_GRAY);

            graphics.drawCenteredString(
                    font,
                    Component.translatable("screen.chatnotes.char_count", note.text().length()),
                    centerX,
                    y + (font.lineHeight + 4) * 2,
                    note.text().length() > 256 ? CommonColors.SOFT_RED : CommonColors.LIGHT_GRAY);
        }

        if (!statusMessage.isEmpty()) {
            graphics.drawCenteredString(font, statusMessage, centerX, height / 2 + 20, CommonColors.GREEN);
        }
    }

    private static String truncate(String value, int max) {
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 1)) + "…";
    }
}
