package com.imjpfeliciano.chatnotes.core.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LineWrapTest {

    @Test
    void emptyTextIsOneEmptyLine() {
        assertEquals(List.of(new LineWrap.Line(0, 0)), LineWrap.wrap("", 20, (from, to) -> to - from));
    }

    @Test
    void textThatFitsStaysOnOneLine() {
        assertEquals(List.of(new LineWrap.Line(0, 5)), LineWrap.wrap("hello", 5, (from, to) -> to - from));
    }

    @Test
    void wrapsAtTheSpaceAndSkipsIt() {
        List<LineWrap.Line> lines = LineWrap.wrap("aaaa bbbb", 4, (from, to) -> to - from);
        assertEquals(List.of(new LineWrap.Line(0, 4), new LineWrap.Line(5, 9)), lines);
    }

    @Test
    void widerGlyphsWrapBeforeNarrowOnes() {
        String text = "aaaa bbbb";
        assertEquals(List.of(new LineWrap.Line(0, 9)), LineWrap.wrap(text, 9, (from, to) -> to - from));
        assertEquals(
                List.of(new LineWrap.Line(0, 4), new LineWrap.Line(5, 9)),
                LineWrap.wrap(text, 9, (from, to) -> (to - from) * 2));
    }

    @Test
    void aWordWiderThanTheRowBreaksMidWord() {
        List<LineWrap.Line> lines = LineWrap.wrap("abcdefghij", 6, (from, to) -> (to - from) * 2);
        assertEquals(List.of(new LineWrap.Line(0, 3), new LineWrap.Line(3, 6), new LineWrap.Line(6, 9), new LineWrap.Line(9, 10)), lines);
        for (LineWrap.Line line : lines) {
            int glyphs = line.end() - line.begin();
            if (glyphs > 1) {
                assertTrue((glyphs * 2) <= 6, () -> line + " is wider than the row");
            }
        }
    }

    @Test
    void theFirstGlyphStaysEvenWhenItIsWiderThanTheRow() {
        assertEquals(List.of(new LineWrap.Line(0, 1)), LineWrap.wrap("A", 1, (from, to) -> 10));
    }

    @Test
    void aNewlineStartsANewRowAndATrailingNewlineAddsAnEmptyOne() {
        assertEquals(
                List.of(new LineWrap.Line(0, 2), new LineWrap.Line(3, 5)),
                LineWrap.wrap("ab\ncd", 20, (from, to) -> to - from));
        assertEquals(
                List.of(new LineWrap.Line(0, 2), new LineWrap.Line(3, 3)),
                LineWrap.wrap("ab\n", 20, (from, to) -> to - from));
    }
}
