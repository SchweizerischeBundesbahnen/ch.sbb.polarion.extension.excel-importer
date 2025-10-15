package ch.sbb.polarion.extension.excel_importer.service.htmltable;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class CssColorMapperTest {

    private static Stream<Arguments> getNamedColorParameters() {
        return Stream.of(
                // Basic HTML colors
                Arguments.of("red", "#FF0000"),
                Arguments.of("blue", "#0000FF"),
                Arguments.of("green", "#008000"),
                Arguments.of("white", "#FFFFFF"),
                Arguments.of("black", "#000000"),
                Arguments.of("yellow", "#FFFF00"),
                Arguments.of("cyan", "#00FFFF"),
                Arguments.of("magenta", "#FF00FF"),

                // Grey/gray aliases
                Arguments.of("gray", "#808080"),
                Arguments.of("grey", "#808080"),
                Arguments.of("darkgray", "#A9A9A9"),
                Arguments.of("darkgrey", "#A9A9A9"),
                Arguments.of("lightgray", "#D3D3D3"),
                Arguments.of("lightgrey", "#D3D3D3"),

                // Aqua/cyan and fuchsia/magenta aliases
                Arguments.of("aqua", "#00FFFF"),
                Arguments.of("cyan", "#00FFFF"),
                Arguments.of("fuchsia", "#FF00FF"),
                Arguments.of("magenta", "#FF00FF"),

                // Extended colors
                Arguments.of("aliceblue", "#F0F8FF"),
                Arguments.of("antiquewhite", "#FAEBD7"),
                Arguments.of("coral", "#FF7F50"),
                Arguments.of("cornflowerblue", "#6495ED"),
                Arguments.of("crimson", "#DC143C"),
                Arguments.of("darkslateblue", "#483D8B"),
                Arguments.of("darkslategray", "#2F4F4F"),
                Arguments.of("darkslategrey", "#2F4F4F"),
                Arguments.of("deeppink", "#FF1493"),
                Arguments.of("gold", "#FFD700"),
                Arguments.of("hotpink", "#FF69B4"),
                Arguments.of("indigo", "#4B0082"),
                Arguments.of("lavender", "#E6E6FA"),
                Arguments.of("limegreen", "#32CD32"),
                Arguments.of("mediumaquamarine", "#66CDAA"),
                Arguments.of("orange", "#FFA500"),
                Arguments.of("orchid", "#DA70D6"),
                Arguments.of("peru", "#CD853F"),
                Arguments.of("plum", "#DDA0DD"),
                Arguments.of("salmon", "#FA8072"),
                Arguments.of("tomato", "#FF6347"),
                Arguments.of("violet", "#EE82EE")
        );
    }

    private static Stream<Arguments> getCaseInsensitiveParameters() {
        return Stream.of(
                Arguments.of("Red", "#FF0000"),
                Arguments.of("RED", "#FF0000"),
                Arguments.of("rEd", "#FF0000"),
                Arguments.of("Blue", "#0000FF"),
                Arguments.of("BLUE", "#0000FF"),
                Arguments.of("DarkSlateGray", "#2F4F4F"),
                Arguments.of("DARKSLATEGRAY", "#2F4F4F"),
                Arguments.of("LightBlue", "#ADD8E6"),
                Arguments.of("LIGHTBLUE", "#ADD8E6")
        );
    }

    @ParameterizedTest
    @MethodSource("getNamedColorParameters")
    void testNamedColorToHex(String colorName, String expectedHex) {
        assertEquals(expectedHex, CssColorMapper.toHex(colorName));
    }

    @ParameterizedTest
    @MethodSource("getCaseInsensitiveParameters")
    void testCaseInsensitiveColorNames(String colorName, String expectedHex) {
        assertEquals(expectedHex, CssColorMapper.toHex(colorName));
    }

    @Test
    void testHexColorPassthrough() {
        assertEquals("#FF0000", CssColorMapper.toHex("#FF0000"));
        assertEquals("#00FF00", CssColorMapper.toHex("#00ff00"));
        assertEquals("#ABCDEF", CssColorMapper.toHex("#abcdef"));
        assertEquals("#123456", CssColorMapper.toHex("#123456"));
    }

    @Test
    void testHexColorWithWhitespace() {
        assertEquals("#FF0000", CssColorMapper.toHex("  #FF0000  "));
        assertEquals("#00FF00", CssColorMapper.toHex(" #00ff00 "));
    }

    @Test
    void testNamedColorWithWhitespace() {
        assertEquals("#FF0000", CssColorMapper.toHex("  red  "));
        assertEquals("#0000FF", CssColorMapper.toHex(" blue "));
    }

    @Test
    void testTransparentColor() {
        assertNull(CssColorMapper.toHex("transparent"));
        assertNull(CssColorMapper.toHex("Transparent"));
        assertNull(CssColorMapper.toHex("TRANSPARENT"));
    }

    @Test
    void testInvalidColors() {
        assertNull(CssColorMapper.toHex("notacolor"));
        assertNull(CssColorMapper.toHex("rgb(255,0,0)"));
        assertNull(CssColorMapper.toHex(""));
        assertNull(CssColorMapper.toHex("   "));
        assertNull(CssColorMapper.toHex(null));
    }

    @Test
    void testInvalidHexFormats() {
        // Short hex format not supported
        assertNull(CssColorMapper.toHex("#f00"));
        // Invalid characters
        assertNull(CssColorMapper.toHex("#GGGGGG"));
        // Missing #
        assertNull(CssColorMapper.toHex("FF0000"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"red", "blue", "darkslategray", "DarkSlateGray", "RED"})
    void testIsValidColorWithNamedColors(String color) {
        assertTrue(CssColorMapper.isValidColor(color));
    }

    @ParameterizedTest
    @ValueSource(strings = {"#FF0000", "#00ff00", "#ABCDEF", "#123456"})
    void testIsValidColorWithHexColors(String color) {
        assertTrue(CssColorMapper.isValidColor(color));
    }

    @ParameterizedTest
    @ValueSource(strings = {"notacolor", "rgb(255,0,0)", "#f00", "#GGGGGG", "", "   ", "FF0000"})
    void testIsValidColorWithInvalidColors(String color) {
        assertFalse(CssColorMapper.isValidColor(color));
    }

    @Test
    void testIsValidColorWithNull() {
        assertFalse(CssColorMapper.isValidColor(null));
    }

    @Test
    void testIsValidColorWithTransparent() {
        assertTrue(CssColorMapper.isValidColor("transparent"));
        assertTrue(CssColorMapper.isValidColor("Transparent"));
        assertTrue(CssColorMapper.isValidColor("TRANSPARENT"));
    }

    @Test
    void testAllBasicHtmlColors() {
        // Verify all 16 basic HTML 4.01 colors are supported
        assertNotNull(CssColorMapper.toHex("black"));
        assertNotNull(CssColorMapper.toHex("silver"));
        assertNotNull(CssColorMapper.toHex("gray"));
        assertNotNull(CssColorMapper.toHex("white"));
        assertNotNull(CssColorMapper.toHex("maroon"));
        assertNotNull(CssColorMapper.toHex("red"));
        assertNotNull(CssColorMapper.toHex("purple"));
        assertNotNull(CssColorMapper.toHex("fuchsia"));
        assertNotNull(CssColorMapper.toHex("green"));
        assertNotNull(CssColorMapper.toHex("lime"));
        assertNotNull(CssColorMapper.toHex("olive"));
        assertNotNull(CssColorMapper.toHex("yellow"));
        assertNotNull(CssColorMapper.toHex("navy"));
        assertNotNull(CssColorMapper.toHex("blue"));
        assertNotNull(CssColorMapper.toHex("teal"));
        assertNotNull(CssColorMapper.toHex("aqua"));
    }

    @Test
    void testColorAliases() {
        // Test that aliases return the same hex value
        assertEquals(CssColorMapper.toHex("gray"), CssColorMapper.toHex("grey"));
        assertEquals(CssColorMapper.toHex("darkgray"), CssColorMapper.toHex("darkgrey"));
        assertEquals(CssColorMapper.toHex("lightgray"), CssColorMapper.toHex("lightgrey"));
        assertEquals(CssColorMapper.toHex("aqua"), CssColorMapper.toHex("cyan"));
        assertEquals(CssColorMapper.toHex("fuchsia"), CssColorMapper.toHex("magenta"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"#FF0000", "#00ff00", "#ABCDEF", "#123456", "#aabbcc"})
    void testIsHexWithValidHex(String color) {
        assertTrue(CssColorMapper.isHex(color));
    }

    @ParameterizedTest
    @ValueSource(strings = {"#f00", "#GGGGGG", "FF0000", "red", "", "   "})
    void testIsHexWithInvalidHex(String color) {
        assertFalse(CssColorMapper.isHex(color));
    }

    @Test
    void testIsHexWithNull() {
        assertFalse(CssColorMapper.isHex(null));
    }
}
