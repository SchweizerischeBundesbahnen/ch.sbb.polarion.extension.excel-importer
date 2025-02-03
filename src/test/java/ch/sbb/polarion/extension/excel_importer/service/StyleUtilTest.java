package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.htmltable.StyleUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class StyleUtilTest {

    private static Stream<Arguments> getParseStyleParameters() {
        return Stream.of(
                Arguments.of(
                        "color: red; font-weight: bold; background-color: blue;",
                        Map.of("color", "red", "font-weight", "bold", "background-color", "blue")
                ), Arguments.of(
                        "color: RED; font-weight: BOLD; BACKGROUND-COLOR: blue;",
                        Map.of("color", "red", "font-weight", "bold", "background-color", "blue")
                ), Arguments.of(
                        "   color  :   red  ;   font-weight :bold  ;   ",
                        Map.of("color", "red", "font-weight", "bold")
                ), Arguments.of(
                        "color: red; font-weight: bold; color: blue;",
                        Map.of("color", "blue", "font-weight", "bold")
                ), Arguments.of(
                        "color: red",
                        Map.of("color", "red")
                ), Arguments.of(
                        "color: red; display: inline-block; font-weight: normal;",
                        Map.of("color", "red", "font-weight", "normal")
                )
        );
    }

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

    @ParameterizedTest
    @MethodSource("getParseStyleParameters")
    void testParseStyle(String style, Map<String, String> attributes) {
        Map<String, String> result = StyleUtil.parseStyleAttribute(style);

        assertEquals(attributes.size(), result.size());
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            assertEquals(entry.getValue(), result.get(entry.getKey()));
        }
    }

    @Test
    void testIsBold() {
        assertTrue(StyleUtil.isBold("bold"), "Expected 'bold' to return true");
        assertTrue(StyleUtil.isBold("700"), "Expected '700' to return true");
        assertTrue(StyleUtil.isBold("800"), "Expected '800' to return true");
        assertFalse(StyleUtil.isBold("600"), "Expected '600' to return false");
        assertFalse(StyleUtil.isBold("not-bold"), "Expected non-numeric input to return false");
        assertFalse(StyleUtil.isBold(null), "Expected null to return false");
        assertFalse(StyleUtil.isBold(" "), "Expected whitespace to return false");
    }
}
