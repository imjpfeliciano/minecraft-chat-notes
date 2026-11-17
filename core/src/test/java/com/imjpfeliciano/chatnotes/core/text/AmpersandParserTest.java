package com.imjpfeliciano.chatnotes.core.text;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmpersandParserTest {

    @Test
    void plainTextIsOneUnstyledSpan() {
        List<TextSpan> spans = AmpersandParser.parse("hello");
        assertEquals(1, spans.size());
        assertEquals("hello", spans.getFirst().text());
        assertNull(spans.getFirst().color());
        assertFalse(spans.getFirst().bold());
    }

    @Test
    void hexRgbSetsColorAndResetsDecorations() {
        List<TextSpan> spans = AmpersandParser.parse("&l&#ff00aaHi");
        assertEquals("Hi", spans.getLast().text());
        assertEquals(0xFF00AA, spans.getLast().color());
        assertFalse(spans.getLast().bold());
    }

    @Test
    void legacyHexReadsNibblesBetweenPrefixes() {
        List<TextSpan> spans = AmpersandParser.parse("&x&F&F&0&0&A&A!");
        assertEquals(0xFF00AA, spans.getFirst().color());
        assertEquals("!", spans.getFirst().text());
    }

    @Test
    void legacyHexAcceptsSectionSignPrefixes() {
        List<TextSpan> spans = AmpersandParser.parse("&x§1§2§3§4§5§6Z");
        assertEquals(0x123456, spans.getFirst().color());
        assertEquals("Z", spans.getFirst().text());
    }

    @Test
    void colorCodeResetsDecorationsAndFormatCodeStacks() {
        TextSpan colored = onlyText(AmpersandParser.parse("&l&aGo"));
        assertEquals(0x55FF55, colored.color());
        assertFalse(colored.bold());

        TextSpan stacked = onlyText(AmpersandParser.parse("&a&l&oGo"));
        assertEquals(0x55FF55, stacked.color());
        assertTrue(stacked.bold());
        assertTrue(stacked.italic());
    }

    @Test
    void resetClearsColorAndDecorations() {
        TextSpan span = onlyText(AmpersandParser.parse("&c&l&rPlain"));
        assertEquals("Plain", span.text());
        assertNull(span.color());
        assertFalse(span.bold());
    }

    @Test
    void decorationCodesMapToFlags() {
        TextSpan span = onlyText(AmpersandParser.parse("&k&m&nX"));
        assertTrue(span.obfuscated());
        assertTrue(span.strikethrough());
        assertTrue(span.underlined());
    }

    @Test
    void escapedPrefixIsLiteralAndUnknownCodeIsKept() {
        assertEquals("&", onlyText(AmpersandParser.parse("&&")).text());
        assertEquals("&z", AmpersandParser.parse("&z").stream().map(TextSpan::text).reduce("", String::concat));
    }

    @Test
    void sectionSignIsDetectedWithoutRewritingTheInput() {
        assertTrue(AmpersandParser.containsSectionSign("a § b"));
        assertFalse(AmpersandParser.containsSectionSign("a &a b"));
        assertFalse(AmpersandParser.containsSectionSign(null));
        assertEquals("Hi", onlyText(AmpersandParser.parse("§aHi")).text());
        assertEquals(0x55FF55, onlyText(AmpersandParser.parse("§aHi")).color());
    }

    @Test
    void emptyAndNullParseToNothing() {
        assertTrue(AmpersandParser.parse(null).isEmpty());
        assertTrue(AmpersandParser.parse("").isEmpty());
    }

    private static TextSpan onlyText(List<TextSpan> spans) {
        return spans.stream().filter(span -> !span.text().isEmpty()).reduce((first, second) -> second).orElseThrow();
    }
}
