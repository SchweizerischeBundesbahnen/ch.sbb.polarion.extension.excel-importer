package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.htmltable.StyleUtil;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StyleUtilTest {

    @Test
    void testParseStyleNullAttribute() {
        Map<String, String> result = StyleUtil.parseStyleAttribute(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseStyleEmptyAttribute() {
        Map<String, String> result = StyleUtil.parseStyleAttribute("");
        assertTrue(result.isEmpty());
    }

    @Test
    void testParseStyleValidAttribute() {
        String style = "color: red; font-weight: bold; background-color: blue;";
        Map<String, String> result = StyleUtil.parseStyleAttribute(style);

        assertEquals(3, result.size());
        assertEquals("red", result.get("color"));
        assertEquals("bold", result.get("font-weight"));
        assertEquals("blue", result.get("background-color"));
    }

    @Test
    void testParseStyleUpperCased() {
        String style = "color: RED; font-weight: BOLD; BACKGROUND-COLOR: blue;";
        Map<String, String> result = StyleUtil.parseStyleAttribute(style);

        assertEquals(3, result.size());
        assertEquals("red", result.get("color"));
        assertEquals("bold", result.get("font-weight"));
        assertEquals("blue", result.get("background-color"));
    }

    @Test
    void testParseStyleWithExtraSpaces() {
        String style = "   color  :   red  ;   font-weight :bold  ;   ";
        Map<String, String> result = StyleUtil.parseStyleAttribute(style);

        assertEquals(2, result.size());
        assertEquals("red", result.get("color"));
        assertEquals("bold", result.get("font-weight"));
    }

    @Test
    void testParseStyleWithDuplicateKeys() {
        String style = "color: red; font-weight: bold; color: blue;";
        Map<String, String> result = StyleUtil.parseStyleAttribute(style);

        assertEquals(2, result.size()); // "color" should be replaced with "blue"
        assertEquals("blue", result.get("color"));
        assertEquals("bold", result.get("font-weight"));
    }

    @Test
    void testParseStyleWithInvalidEntries() {
        String style = "color: red; invalid-entry; font-weight: bold;";
        Map<String, String> result = StyleUtil.parseStyleAttribute(style);

        assertEquals(2, result.size()); // "invalid-entry" should be ignored
        assertEquals("red", result.get("color"));
        assertEquals("bold", result.get("font-weight"));
    }

    @Test
    void testParseStyleWithNoSemicolon() {
        String style = "color: red";
        Map<String, String> result = StyleUtil.parseStyleAttribute(style);

        assertEquals(1, result.size());
        assertEquals("red", result.get("color"));
    }

    @Test
    void testUnsupportedPropertiesSkipped() {
        String style = "color: red; display: inline-block; font-weight: normal;";
        Map<String, String> result = StyleUtil.parseStyleAttribute(style);

        assertEquals(2, result.size());
        assertEquals("red", result.get("color"));
        assertEquals("normal", result.get("font-weight"));
    }

//    @Test
//    public void testValidColor() {
//        // Test for a valid color
//        assertEquals(0xF800, StyleUtil.parseColor("#FF0000")); // Red
//        assertEquals(0x07E0, StyleUtil.parseColor("#00FF00")); // Green
//        assertEquals(0x001F, StyleUtil.parseColor("#0000FF")); // Blue
//        assertEquals(0x7C1F, StyleUtil.parseColor("#FFFF00")); // Yellow
//        assertEquals(0x4C1F, StyleUtil.parseColor("#FF00FF")); // Magenta
//        assertEquals(0x3E1F, StyleUtil.parseColor("#00FFFF")); // Cyan
//    }
//
//    @Test
//    public void testInvalidColor() {
//        // Test for invalid color formats
//        assertEquals(0x0000, StyleUtil.parseColor("invalidColor"));  // No '#' and invalid format
//        assertEquals(0x0000, StyleUtil.parseColor("#12345"));      // Not enough digits
//        assertEquals(0x0000, StyleUtil.parseColor("#ZZZZZZ"));     // Invalid hex characters
//        assertEquals(0x0000, StyleUtil.parseColor("#G12345"));     // Invalid hex character ('G')
//    }
//
//    @Test
//    public void testEdgeCases() {
//        // Test for edge cases
//        assertEquals(0x0000, StyleUtil.parseColor("#000000")); // Black
//        assertEquals(0xFFFF, StyleUtil.parseColor("#FFFFFF")); // White
//        assertEquals(0x0000, StyleUtil.parseColor("#123ABC")); // Random valid color
//    }

    @Test
    void testIsBoldWithBold() {
        assertTrue(StyleUtil.isBold("bold"), "Expected 'bold' to return true");
    }

    @Test
    void testIsBoldWithNumericWeight700() {
        assertTrue(StyleUtil.isBold("700"), "Expected '700' to return true");
    }

    @Test
    void testIsBoldWithNumericWeightGreaterThan700() {
        assertTrue(StyleUtil.isBold("800"), "Expected '800' to return true");
    }

    @Test
    void testIsBoldWithNumericWeightLessThan700() {
        assertFalse(StyleUtil.isBold("600"), "Expected '600' to return false");
    }

    @Test
    void testIsBoldWithNonNumeric() {
        assertFalse(StyleUtil.isBold("not-bold"), "Expected non-numeric input to return false");
    }

    @Test
    void testIsBoldWithNull() {
        assertFalse(StyleUtil.isBold(null), "Expected null to return false");
    }

    @Test
    void testIsBoldWithWhitespace() {
        assertFalse(StyleUtil.isBold(" "), "Expected whitespace to return false");
    }
}
