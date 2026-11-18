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
import com.imjpfeliciano.chatnotes.core.text.FormattedDocument;
import com.imjpfeliciano.chatnotes.core.text.TextFormat;
import com.imjpfeliciano.chatnotes.core.text.TextSpan;
import com.imjpfeliciano.chatnotes.mc.ExtractorPainter;
import com.imjpfeliciano.chatnotes.mc.FontTextMeasurer;
import com.imjpfeliciano.chatnotes.mc.MinecraftTranslator;
import com.imjpfeliciano.chatnotes.mc.UiPlatform;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/** Create or edit a single note. The code field is what Save writes; chat is single-line. */
public abstract class AbstractNoteEditScreen extends Screen {

    private static final int TITLE_MAX = 64;
    private static final int TEXT_MAX = 4096;
    private static final int BUTTON_HEIGHT = 20;
    private static final int PADDING = 4;
    private static final int SWATCH_SIZE = 14;
    private static final int SWATCH_GAP = 3;

    private static final SwatchSpec[] SWATCHES = {
        new SwatchSpec(TextFormat.BLACK, 0x000000, "screen.chatnotes.color_black"),
        new SwatchSpec(TextFormat.DARK_BLUE, 0x0000AA, "screen.chatnotes.color_dark_blue"),
        new SwatchSpec(TextFormat.DARK_GREEN, 0x00AA00, "screen.chatnotes.color_dark_green"),
        new SwatchSpec(TextFormat.DARK_AQUA, 0x00AAAA, "screen.chatnotes.color_dark_aqua"),
        new SwatchSpec(TextFormat.DARK_RED, 0xAA0000, "screen.chatnotes.color_dark_red"),
        new SwatchSpec(TextFormat.DARK_PURPLE, 0xAA00AA, "screen.chatnotes.color_dark_purple"),
        new SwatchSpec(TextFormat.GOLD, 0xFFAA00, "screen.chatnotes.color_gold"),
        new SwatchSpec(TextFormat.GRAY, 0xAAAAAA, "screen.chatnotes.color_gray"),
        new SwatchSpec(TextFormat.DARK_GRAY, 0x555555, "screen.chatnotes.color_dark_gray"),
        new SwatchSpec(TextFormat.BLUE, 0x5555FF, "screen.chatnotes.color_blue"),
        new SwatchSpec(TextFormat.GREEN, 0x55FF55, "screen.chatnotes.color_green"),
        new SwatchSpec(TextFormat.AQUA, 0x55FFFF, "screen.chatnotes.color_aqua"),
        new SwatchSpec(TextFormat.RED, 0xFF5555, "screen.chatnotes.color_red"),
        new SwatchSpec(TextFormat.LIGHT_PURPLE, 0xFF55FF, "screen.chatnotes.color_light_purple"),
        new SwatchSpec(TextFormat.YELLOW, 0xFFFF55, "screen.chatnotes.color_yellow"),
        new SwatchSpec(TextFormat.WHITE, 0xFFFFFF, "screen.chatnotes.color_white")
    };

    private final AbstractNotesScreen parent;
    private final NoteStore store;
    private final Note existing;
    private final ClipboardAccess clipboard;
    private final ModLog log;
    private final List<PlacedSwatch> swatches = new ArrayList<>();

    private EditBox titleBox;
    private NoteTextArea plainBox;
    private NoteTextArea codeBox;
    private Button saveButton;
    private FormattedDocument document;
    private String selectedGroupId;
    private boolean updating;
    private int captureStart;
    private int captureEnd;
    private int fieldWidth = 320;
    private int fieldLeft;
    private int columnWidth = 160;
    private int outputX;
    private int titleBoxY;
    private int plainLabelY;
    private int codeLabelY;
    private int counterY;
    private int colorNameX;
    private int colorNameY;
    private int colorNameWidth;

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
        boolean first = plainBox == null;
        if (document == null) {
            document = FormattedDocument.parse(existing == null ? "" : existing.text());
        }
        String keptTitle = titleBox == null
                ? (existing == null ? "" : existing.title())
                : titleBox.getValue();

        fieldWidth = Math.max(220, width - 32);
        fieldLeft = (width - fieldWidth) / 2;
        int left = fieldLeft;
        int line = font.lineHeight;
        int labelH = line + 4;
        int gap = 8;
        int y = 18;

        titleBoxY = y + labelH;
        int rowInner = fieldWidth - gap;
        int titleWidth = rowInner * 75 / 100;
        titleBox = new NoteLineEditBox(font, left, titleBoxY, titleWidth, 20, Component.translatable("screen.chatnotes.field_title"));
        titleBox.setMaxLength(TITLE_MAX);
        titleBox.setHint(Component.translatable("screen.chatnotes.field_title_hint"));
        titleBox.setValue(keptTitle == null ? "" : keptTitle);
        addRenderableWidget(titleBox);

        List<String> groupValues = groupOptions();
        if (!groupValues.contains(selectedGroupId)) {
            selectedGroupId = GroupFilter.UNGROUPED;
        }
        int groupX = left + titleWidth + gap;
        addRenderableWidget(CycleButton.builder(this::groupLabel, selectedGroupId)
                .withValues(groupValues)
                .create(
                        groupX,
                        titleBoxY,
                        rowInner - titleWidth,
                        20,
                        Component.translatable("screen.chatnotes.field_group"),
                        (button, value) -> selectedGroupId = value));

        y = titleBoxY + 20 + gap;
        y = placeToolbar(left, y);

        plainLabelY = y;
        codeLabelY = y;
        y = plainLabelY + labelH;
        columnWidth = (fieldWidth - gap) / 2;
        outputX = left + columnWidth + gap;
        int areaHeight = line * 3 + 8;
        plainBox = new NoteTextArea(
                font,
                left,
                y,
                columnWidth,
                areaHeight,
                Component.translatable("screen.chatnotes.field_plain"),
                Component.translatable("screen.chatnotes.field_plain_hint"));
        plainBox.setCharacterLimit(TEXT_MAX);
        plainBox.setStyledText(new NoteTextArea.StyledText() {
            @Override
            public net.minecraft.util.FormattedCharSequence format(String value, int from, int to) {
                if (value == null || from >= to) {
                    return null;
                }
                return formatInput(value.substring(from, to), from);
            }

            @Override
            public int width(String value, int from, int to) {
                return styledWidth(value, from, to);
            }
        });
        addRenderableWidget(plainBox);

        codeBox = new NoteTextArea(
                font,
                outputX,
                y,
                columnWidth,
                areaHeight,
                Component.translatable("screen.chatnotes.field_code"),
                Component.translatable("screen.chatnotes.field_code_hint"));
        codeBox.setCharacterLimit(TEXT_MAX);
        addRenderableWidget(codeBox);

        updating = true;
        plainBox.setValueListener(this::onPlainChanged);
        codeBox.setValueListener(this::onCodeChanged);
        plainBox.setValue(document.plain());
        codeBox.setValue(document.serialize());
        updating = false;

        int actionY = height - 28;
        counterY = actionY - line - 6;
        placeActionRow(left, actionY, fieldWidth);

        refreshSaveState();
        if (first) {
            setInitialFocus(keptTitle == null || keptTitle.isBlank() ? titleBox : plainBox);
        }
    }

    private void placeActionRow(int left, int y, int rowWidth) {
        int gap = 4;
        Button paste = Button.builder(Component.translatable("screen.chatnotes.paste"), b -> pasteFromClipboard())
                .bounds(left, y, 1, BUTTON_HEIGHT)
                .build();
        saveButton = Button.builder(Component.translatable("screen.chatnotes.save"), b -> save())
                .bounds(left, y, 1, BUTTON_HEIGHT)
                .build();
        Button cancel = Button.builder(Component.translatable("gui.cancel"), b -> onClose())
                .bounds(left, y, 1, BUTTON_HEIGHT)
                .build();
        Button[] actions = {addRenderableWidget(paste), addRenderableWidget(saveButton), addRenderableWidget(cancel)};
        int each = (rowWidth - gap * (actions.length - 1)) / actions.length;
        int x = left;
        for (Button action : actions) {
            action.setX(x);
            action.setWidth(each);
            x += each + gap;
        }
    }

    private int placeToolbar(int left, int y) {
        addStyleButtons(left, y);
        int swatchY = y + BUTTON_HEIGHT + 6;
        int swatchBottom = placeSwatches(left, swatchY);
        colorNameX = left + swatchRowWidth() + 8;
        colorNameY = swatchY + 3;
        colorNameWidth = Math.max(0, fieldWidth - swatchRowWidth() - 8);
        return swatchBottom + 8;
    }

    private static int swatchRowWidth() {
        return 8 * (SWATCH_SIZE + SWATCH_GAP) - SWATCH_GAP;
    }

    private int addStyleButtons(int left, int y) {
        int x = left;
        x = addFormatButton(x, y, 22, "screen.chatnotes.format_bold", "screen.chatnotes.format_bold_tip", TextFormat.BOLD);
        x = addFormatButton(x, y, 22, "screen.chatnotes.format_italic", "screen.chatnotes.format_italic_tip", TextFormat.ITALIC);
        x = addFormatButton(x, y, 22, "screen.chatnotes.format_underline", "screen.chatnotes.format_underline_tip", TextFormat.UNDERLINED);
        x = addFormatButton(x, y, 22, "screen.chatnotes.format_strike", "screen.chatnotes.format_strike_tip", TextFormat.STRIKETHROUGH);
        x = addFormatButton(x, y, 22, "screen.chatnotes.format_obfuscated", "screen.chatnotes.format_obfuscated_tip", TextFormat.OBFUSCATED);
        addFormatButton(x, y, 64, "screen.chatnotes.format_reset", "screen.chatnotes.format_reset_tip", TextFormat.RESET);
        return y + BUTTON_HEIGHT;
    }

    private int addFormatButton(int x, int y, int width, String labelKey, String tipKey, TextFormat format) {
        Button button = Button.builder(Component.translatable(labelKey), b -> applyFormat(format))
                .bounds(x, y, width, BUTTON_HEIGHT)
                .build();
        button.setTooltip(Tooltip.create(Component.translatable(tipKey)));
        addRenderableWidget(button);
        return x + width + PADDING;
    }

    private int placeSwatches(int left, int y) {
        swatches.clear();
        int stride = SWATCH_SIZE + SWATCH_GAP;
        for (int i = 0; i < SWATCHES.length; i++) {
            int column = i % 8;
            int row = i / 8;
            swatches.add(new PlacedSwatch(left + column * stride, y + row * stride, SWATCHES[i]));
        }
        return y + stride + SWATCH_SIZE;
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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        captureSelection();
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && applySwatch(event.x(), event.y())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void captureSelection() {
        if (plainBox == null) {
            return;
        }
        int[] range = plainBox.selection();
        captureStart = clampToLength(range[0], plainBox.getValue().length());
        captureEnd = clampToLength(range[1], plainBox.getValue().length());
    }

    private static int clampToLength(int index, int length) {
        if (index < 0) {
            return 0;
        }
        return Math.min(index, length);
    }

    private boolean applySwatch(double mouseX, double mouseY) {
        for (PlacedSwatch swatch : swatches) {
            if (mouseX >= swatch.x && mouseX < swatch.x + SWATCH_SIZE && mouseY >= swatch.y && mouseY < swatch.y + SWATCH_SIZE) {
                applyFormat(swatch.spec.format);
                return true;
            }
        }
        return false;
    }

    private void applyFormat(TextFormat format) {
        if (document == null) {
            return;
        }
        document.apply(captureStart, captureEnd, format);
        syncFields();
    }

    private void onPlainChanged(String value) {
        if (updating || document == null) {
            return;
        }
        String current = document.plain();
        String next = value == null ? "" : value;
        if (next.equals(current)) {
            return;
        }
        int prefix = sharedPrefix(current, next);
        int suffix = sharedSuffix(current, next, prefix);
        int deleteEnd = current.length() - suffix;
        int insertEnd = next.length() - suffix;
        if (deleteEnd > prefix) {
            document.delete(prefix, deleteEnd);
        }
        if (insertEnd > prefix) {
            document.insert(prefix, next.substring(prefix, insertEnd));
        }
        syncFields();
    }

    private void onCodeChanged(String value) {
        if (updating) {
            return;
        }
        String next = value == null ? "" : value;
        if (document != null && next.equals(document.serialize())) {
            refreshSaveState();
            return;
        }
        document = FormattedDocument.parse(next);
        syncFields();
    }

    private void syncFields() {
        if (plainBox == null || codeBox == null || document == null) {
            return;
        }
        updating = true;
        String plain = document.plain();
        String code = document.serialize();
        if (!plain.equals(plainBox.getValue() == null ? "" : plainBox.getValue())) {
            plainBox.setValue(plain);
        }
        if (!code.equals(codeBox.getValue() == null ? "" : codeBox.getValue())) {
            codeBox.setValue(code);
        }
        updating = false;
        refreshSaveState();
    }

    private void placeCaret(int index) {
        if (plainBox == null || document == null) {
            return;
        }
        int caret = clampToLength(index, document.plain().length());
        plainBox.moveCursorTo(caret);
        captureStart = caret;
        captureEnd = caret;
    }

    private void pasteFromClipboard() {
        String clip = Note.normalizeText(clipboard.get());
        if (clip.isEmpty() || document == null) {
            return;
        }
        int from = Math.min(captureStart, captureEnd);
        int to = Math.max(captureStart, captureEnd);
        if (from != to) {
            document.delete(from, to);
        }
        int before = document.plain().length();
        document.paste(from, clip);
        int caret = from + document.plain().length() - before;
        syncFields();
        placeCaret(caret);
    }

    private static int sharedPrefix(String left, String right) {
        int limit = Math.min(left.length(), right.length());
        int index = 0;
        while (index < limit && left.charAt(index) == right.charAt(index)) {
            index++;
        }
        return index;
    }

    private static int sharedSuffix(String left, String right, int prefix) {
        int index = 0;
        while (index < left.length() - prefix
                && index < right.length() - prefix
                && left.charAt(left.length() - 1 - index) == right.charAt(right.length() - 1 - index)) {
            index++;
        }
        return index;
    }

    private void refreshSaveState() {
        if (saveButton != null) {
            saveButton.active = NoteRules.canSave(Note.normalizeText(codeBox == null ? "" : codeBox.getValue()));
        }
    }

    private void save() {
        String title = titleBox.getValue() == null ? "" : titleBox.getValue().trim();
        String text = Note.normalizeText(codeBox.getValue());
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
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        ExtractorPainter painter = new ExtractorPainter(graphics, font);
        TextMeasurer measurer = new FontTextMeasurer(font);
        MinecraftTranslator translator = MinecraftTranslator.INSTANCE;

        NotesPainting.title(painter, title.getString(), width / 2, 8);

        int left = fieldLeft;
        painter.text(
                translator.tr("screen.chatnotes.field_title"),
                left,
                titleBoxY - measurer.lineHeight() - 3,
                UiColors.LIGHT_GRAY);

        String hovered = null;
        for (PlacedSwatch swatch : swatches) {
            int rgb = 0xFF000000 | swatch.spec.rgb;
            painter.fill(swatch.x, swatch.y, swatch.x + SWATCH_SIZE, swatch.y + SWATCH_SIZE, rgb);
            painter.outline(swatch.x, swatch.y, SWATCH_SIZE, SWATCH_SIZE, UiColors.WHITE);
            if (mouseX >= swatch.x && mouseX < swatch.x + SWATCH_SIZE && mouseY >= swatch.y && mouseY < swatch.y + SWATCH_SIZE) {
                hovered = translator.tr(swatch.spec.nameKey);
            }
        }
        if (hovered != null && colorNameWidth > 8) {
            painter.text(measurer.clip(hovered, colorNameWidth), colorNameX, colorNameY, UiColors.WHITE);
        }

        painter.text(translator.tr("screen.chatnotes.field_plain"), left, plainLabelY, UiColors.LIGHT_GRAY);
        String outputLabel = translator.tr("screen.chatnotes.field_code");
        painter.text(outputLabel, outputX, codeLabelY, UiColors.LIGHT_GRAY);
        int noteX = outputX + measurer.width(outputLabel) + 8;
        int noteWidth = columnWidth - (noteX - outputX);
        if (noteWidth > 8) {
            painter.text(
                    measurer.clip(translator.tr("screen.chatnotes.field_output_note"), noteWidth),
                    noteX,
                    codeLabelY,
                    UiColors.LIGHT_GRAY);
        }

        String code = codeBox == null ? "" : codeBox.getValue();
        String normalized = Note.normalizeText(code);

        if (AmpersandParser.containsSectionSign(normalized)) {
            NotesPainting.warning(
                    painter,
                    translator.tr("screen.chatnotes.section_sign_warning"),
                    left,
                    counterY - measurer.lineHeight() - 2);
        }
        NotesPainting.counter(
                painter,
                translator.tr("screen.chatnotes.char_counter", normalized.length(), NoteRules.SOFT_LIMIT),
                left,
                counterY,
                normalized.length());
    }

    /** Colours the visible slice of the input. {@code start} is that slice's index in the plain string. */
    private FormattedCharSequence formatInput(String slice, int start) {
        if (slice == null || slice.isEmpty() || document == null || plainBox == null) {
            return null;
        }
        String plain = document.plain();
        String shown = plainBox.getValue() == null ? "" : plainBox.getValue();
        if (!plain.equals(shown) || start < 0 || start > plain.length()) {
            return null;
        }
        int end = Math.min(plain.length(), start + slice.length());
        MutableComponent root = Component.empty();
        int index = 0;
        boolean any = false;
        for (TextSpan span : document.spans()) {
            int next = index + span.text().length();
            int from = Math.max(index, start);
            int to = Math.min(next, end);
            if (from < to) {
                root.append(Component.literal(span.text().substring(from - index, to - index)).setStyle(inputStyle(span)));
                any = true;
            }
            index = next;
            if (index >= end) {
                break;
            }
        }
        if (!any) {
            return null;
        }
        return Language.getInstance().getVisualOrder(root);
    }

    private static Style inputStyle(TextSpan span) {
        Style style = Style.EMPTY;
        if (span.color() != null) {
            style = style.withColor(TextColor.fromRgb(span.color()));
        }
        if (span.bold()) {
            style = style.withBold(true);
        }
        if (span.italic()) {
            style = style.withItalic(true);
        }
        if (span.underlined()) {
            style = style.withUnderlined(true);
        }
        if (span.strikethrough()) {
            style = style.withStrikethrough(true);
        }
        if (span.obfuscated()) {
            style = style.withObfuscated(true);
        }
        return style;
    }

    /** Width of {@code value.substring(from, to)} using the same styles the input draws. */
    private int styledWidth(String value, int from, int to) {
        if (value == null || from >= to) {
            return 0;
        }
        from = Math.max(0, from);
        to = Math.min(value.length(), to);
        if (from >= to) {
            return 0;
        }
        String plain = document == null ? "" : document.plain();
        if (!plain.equals(value)) {
            return font.width(value.substring(from, to));
        }
        MutableComponent root = Component.empty();
        int index = 0;
        boolean any = false;
        for (TextSpan span : document.spans()) {
            int next = index + span.text().length();
            int start = Math.max(index, from);
            int end = Math.min(next, to);
            if (start < end) {
                root.append(Component.literal(span.text().substring(start - index, end - index)).setStyle(inputStyle(span)));
                any = true;
            }
            index = next;
            if (index >= to) {
                break;
            }
        }
        if (!any) {
            return font.width(value.substring(from, to));
        }
        return font.width(Language.getInstance().getVisualOrder(root));
    }

    private record SwatchSpec(TextFormat format, int rgb, String nameKey) {}

    private record PlacedSwatch(int x, int y, SwatchSpec spec) {}
}
