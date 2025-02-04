package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.htmltable.CellConfig;
import ch.sbb.polarion.extension.excel_importer.service.htmltable.CellData;
import ch.sbb.polarion.extension.excel_importer.service.htmltable.StyleUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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

    @Test
    void testAdjustColumnWidth() {
        CellData noAttrCellData = CellData.builder().attrs(new CellConfig(Map.of(), Map.of(), Map.of())).build();
        CellData attrExistsCellData = CellData.builder().attrs(new CellConfig(Map.of(), Map.of(), Map.of("xlsx-width", "100"))).build();
        Sheet sheetMock = mock(Sheet.class);
        StyleUtil.adjustColumnWidth(5, noAttrCellData, sheetMock);
        verify(sheetMock, times(0)).setColumnWidth(5, 3664);
        verify(sheetMock, times(1)).autoSizeColumn(5);
        StyleUtil.adjustColumnWidth(7, attrExistsCellData, sheetMock);
        verify(sheetMock, times(1)).setColumnWidth(7, 3664);
        verify(sheetMock, times(0)).autoSizeColumn(7);
    }

    @Test
    void testColumnWidth() {
        assertEquals(-1, StyleUtil.getColumnWidthForCell(CellData.builder().attrs(new CellConfig(Map.of(), Map.of(), Map.of())).build()));
        assertEquals(-1, StyleUtil.getColumnWidthForCell(CellData.builder().attrs(new CellConfig(Map.of("xlsx-width", "100"), Map.of(), Map.of())).build()));
        assertEquals(-1, StyleUtil.getColumnWidthForCell(CellData.builder().attrs(new CellConfig(Map.of(), Map.of("xlsx-width", "100"), Map.of())).build()));
        assertEquals(3664, StyleUtil.getColumnWidthForCell(CellData.builder().attrs(new CellConfig(Map.of(), Map.of(), Map.of("xlsx-width", "100"))).build()));

        CellData badCellData = CellData.builder().attrs(new CellConfig(Map.of(), Map.of(), Map.of("xlsx-width", "bad"))).build();
        NumberFormatException exception = assertThrows(NumberFormatException.class, () -> StyleUtil.getColumnWidthForCell(badCellData));
        assertEquals("For input string: \"bad\"", exception.getMessage());
    }

    @Test
    void testRowHeight() {
        assertEquals(-1, StyleUtil.getRowHeightForCell(CellData.builder().attrs(new CellConfig(Map.of(), Map.of(), Map.of())).build()));
        assertEquals(-1, StyleUtil.getRowHeightForCell(CellData.builder().attrs(new CellConfig(Map.of("xlsx-height", "100"), Map.of(), Map.of())).build()));
        assertEquals(1500, StyleUtil.getRowHeightForCell(CellData.builder().attrs(new CellConfig(Map.of(), Map.of("xlsx-height", "100"), Map.of())).build()));
        assertEquals(-1, StyleUtil.getRowHeightForCell(CellData.builder().attrs(new CellConfig(Map.of(), Map.of(), Map.of("xlsx-height", "100"))).build()));

        CellData badCellData = CellData.builder().attrs(new CellConfig(Map.of(), Map.of("xlsx-height", "bad"), Map.of())).build();
        NumberFormatException exception = assertThrows(NumberFormatException.class, () -> StyleUtil.getRowHeightForCell(badCellData));
        assertEquals("For input string: \"bad\"", exception.getMessage());
    }
}
