package com.imjpfeliciano.chatnotes.core.text;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class FormattedDocumentTest {

    @Test
    void eachSwatchWritesItsOwnCodeIncludingASingleCharacter() {
        for (TextFormat format : TextFormat.values()) {
            if (format.colorCode() == null) {
                continue;
            }
            FormattedDocument doc = FormattedDocument.parse("A");
            doc.apply(0, 1, format);
            assertEquals("&" + format.colorCode() + "A", doc.serialize(), format.name());
        }
    }

    @Test
    void eachDecorationWritesItsCode() {
        assertEquals("&lA", styled("A", TextFormat.BOLD));
        assertEquals("&oA", styled("A", TextFormat.ITALIC));
        assertEquals("&nA", styled("A", TextFormat.UNDERLINED));
        assertEquals("&mA", styled("A", TextFormat.STRIKETHROUGH));
        assertEquals("&kA", styled("A", TextFormat.OBFUSCATED));
    }

    @Test
    void yellowThenBoldWritesColorBeforeDecoration() {
        FormattedDocument doc = FormattedDocument.parse("GRANJA");
        doc.apply(0, 6, TextFormat.YELLOW);
        doc.apply(0, 6, TextFormat.BOLD);
        assertEquals("&e&lGRANJA", doc.serialize());

        FormattedDocument boldFirst = FormattedDocument.parse("GRANJA");
        boldFirst.apply(0, 6, TextFormat.BOLD);
        boldFirst.apply(0, 6, TextFormat.YELLOW);
        assertEquals("&e&lGRANJA", boldFirst.serialize());
    }

    @Test
    void resetOnAStyledSelectionReturnsPlainText() {
        FormattedDocument doc = FormattedDocument.parse("&e&lGRANJA");
        doc.apply(0, 6, TextFormat.RESET);
        assertEquals("GRANJA", doc.plain());
        assertEquals("GRANJA", doc.serialize());
    }

    @Test
    void boldTogglesOffWhenTheSelectionIsFullyBoldAndOnWhenMixed() {
        FormattedDocument bold = FormattedDocument.parse("&lGo");
        bold.apply(0, 2, TextFormat.BOLD);
        assertEquals("Go", bold.serialize());

        FormattedDocument mixed = FormattedDocument.parse("&lAB&rCD");
        mixed.apply(0, 4, TextFormat.BOLD);
        assertEquals("&lABCD", mixed.serialize());
    }

    @Test
    void aMiddleSelectionLeavesTheSurroundingTextUnchanged() {
        FormattedDocument doc = FormattedDocument.parse("Hello World");
        doc.apply(6, 11, TextFormat.RED);
        assertEquals("Hello &cWorld", doc.serialize());
    }

    @Test
    void neighbouringRunsWithTheSameStyleMergeIntoOneCode() {
        FormattedDocument doc = FormattedDocument.parse("AB");
        doc.apply(0, 1, TextFormat.YELLOW);
        doc.apply(1, 2, TextFormat.YELLOW);
        assertEquals("&eAB", doc.serialize());
    }

    @Test
    void typingInsideAColoredWordInheritsItsColorAndBackspaceShrinksTheRun() {
        FormattedDocument doc = FormattedDocument.parse("&eHello");
        doc.insert(2, "X");
        assertEquals("HeXllo", doc.plain());
        assertEquals("&eHeXllo", doc.serialize());
        doc.delete(2, 3);
        assertEquals("Hello", doc.plain());
        assertEquals("&eHello", doc.serialize());
    }

    @Test
    void anEmptySelectionDoesNotRewriteTheStringAndTypesWithTheCaretStyle() {
        FormattedDocument doc = FormattedDocument.parse("&eHello");
        doc.apply(5, 5, TextFormat.BOLD);
        assertEquals("&eHello", doc.serialize());
        doc.insert(5, "!");
        assertEquals("&eHello&l!", doc.serialize());

        FormattedDocument empty = FormattedDocument.parse("");
        empty.apply(0, 0, TextFormat.YELLOW);
        assertEquals("", empty.serialize());
        empty.insert(0, "Hi");
        assertEquals("&eHi", empty.serialize());
    }

    @Test
    void reversedAndOutOfRangeIndexesDoNotThrowOrCorrupt() {
        FormattedDocument doc = FormattedDocument.parse("Hello");
        doc.apply(4, 1, TextFormat.YELLOW);
        assertEquals("H&eell&ro", doc.serialize());
        doc.apply(40, 50, TextFormat.RED);
        doc.apply(-8, -1, TextFormat.BOLD);
        assertEquals("H&eell&ro", doc.serialize());
    }

    @Test
    void pastingPlainTextAndAmpersandCodesBothLandInTheDocument() {
        FormattedDocument plain = FormattedDocument.parse("Say ");
        plain.paste(4, "hello");
        assertEquals("Say hello", plain.plain());
        assertEquals("Say hello", plain.serialize());

        FormattedDocument codes = FormattedDocument.parse("");
        codes.paste(0, "&a&lHi");
        assertEquals("Hi", codes.plain());
        assertEquals("&a&lHi", codes.serialize());
    }

    @Test
    void serializeThenParseThenSerializeIsStable() {
        FormattedDocument doc = sampleLine();
        String code = doc.serialize();
        assertEquals(code, FormattedDocument.parse(code).serialize());

        FormattedDocument ampersand = FormattedDocument.parse("A");
        ampersand.insert(1, "&");
        assertEquals("A&", ampersand.plain());
        assertEquals("A&&", ampersand.serialize());
        assertEquals("A&&", FormattedDocument.parse(ampersand.serialize()).serialize());
    }

    @Test
    void sampleLineUsesYellowBoldRedAndAqua() {
        assertEquals("NUEVA &e&lGRANJA DE ORO&r - &cS1&r - 72", sampleLine().serialize());

        FormattedDocument second = FormattedDocument.parse("SPAWNERS DE PIGLIN");
        second.apply(0, second.plain().length(), TextFormat.AQUA);
        assertEquals("&bSPAWNERS DE PIGLIN", second.serialize());
    }

    @Test
    void buttonOutputHasNoSectionSignHexOrLegacyHex() {
        String code = sampleLine().serialize();
        assertFalse(code.contains("§"));
        assertFalse(code.contains("&#"));
        assertFalse(code.contains("&x"));
        assertFalse(code.contains("&X"));
    }

    @Test
    void existingHexOrLegacyHexIsKeptUntilApplyTouchesARange() {
        String hex = "&#ff00aaHello";
        FormattedDocument doc = FormattedDocument.parse(hex);
        assertEquals(hex, doc.serialize());
        doc.apply(0, 5, TextFormat.BOLD);
        assertFalse(doc.serialize().contains("&#"));
        assertFalse(doc.serialize().contains("&x"));
        assertFalse(doc.serialize().contains("§"));

        String legacyHex = "&x&F&F&0&0&A&AHello";
        assertEquals(legacyHex, FormattedDocument.parse(legacyHex).serialize());

        FormattedDocument section = FormattedDocument.parse("§aHello");
        assertEquals("§aHello", section.serialize());
        section.apply(0, 5, TextFormat.BOLD);
        assertEquals("&a&lHello", section.serialize());
    }

    @Test
    void plainTextMatchesTheParserAndEmptyInputStaysEmpty() {
        String raw = "&e&lHi &cBye";
        String expected = AmpersandParser.parse(raw).stream().map(TextSpan::text).reduce("", String::concat);
        assertEquals(expected, FormattedDocument.parse(raw).plain());
        assertEquals("", FormattedDocument.parse(null).plain());
        assertEquals("", FormattedDocument.parse(null).serialize());
        assertEquals("", FormattedDocument.parse("").serialize());
        assertNull(TextFormat.fromColorCode('z'));
        assertEquals(TextFormat.YELLOW, TextFormat.fromColorCode('E'));
    }

    private static FormattedDocument sampleLine() {
        FormattedDocument doc = FormattedDocument.parse("NUEVA GRANJA DE ORO - S1 - 72");
        doc.apply(6, 19, TextFormat.YELLOW);
        doc.apply(6, 19, TextFormat.BOLD);
        doc.apply(22, 24, TextFormat.RED);
        return doc;
    }

    private static String styled(String text, TextFormat format) {
        FormattedDocument doc = FormattedDocument.parse(text);
        doc.apply(0, text.length(), format);
        return doc.serialize();
    }
}
