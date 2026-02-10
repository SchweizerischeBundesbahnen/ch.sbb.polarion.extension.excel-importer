package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.parser.IParserSettings;
import ch.sbb.polarion.extension.excel_importer.service.parser.impl.XlsxParser;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.apache.poi.ss.util.CellRangeAddress;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class XlsxParserTest {

    private static final String SHEET_FIRST = "first";
    private static final String SHEET_EMPTY = "empty";
    private static final String SHEET_WIDE = "wide";
    private static final String SHEET_BAD = "bad";
    private static final String SHEET_MERGED_FAIL = "merged_fail";
    private static final String SHEET_MERGED_OK = "merged_ok";
    private static final String SHEET_MERGED_MIXED = "merged_mixed";

    @Test
    void testSuccessfulParse() {
        assertEquals(List.of(
                Map.of("A", "a1", "B", "b1", "C", "c1"),
                Map.of("A", "a2", "C", "c2"),
                Map.of("A", "a3", "B", "b3", "D", "d4")
        ), cleanNulls(new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(SHEET_FIRST, 1))));
        assertEquals(List.of(
                Map.of("A", "a2", "C", "c2"),
                Map.of("A", "a3", "B", "b3", "D", "d4")
        ), cleanNulls(new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(SHEET_FIRST, 2))));
        assertEquals(List.of(
                Map.of("A", "a2", "C", "c2"),
                Map.of("A", "a3", "B", "b3", "D", "d4")
        ), cleanNulls(new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(null, 2))));
    }

    @Test
    @SneakyThrows
    void testFormulaError() {
        XlsxParser xlsxParser = new XlsxParser();
        IllegalArgumentException exception;
        try (InputStream fileAttempt1 = getClass().getClassLoader().getResourceAsStream("error.xlsx")) {
            IParserSettings settingsAttempt1 = generateSettings(SHEET_FIRST, 1);

            // one of the columns has error
            exception = assertThrows(IllegalArgumentException.class,
                    () -> xlsxParser.parseFileStream(fileAttempt1, settingsAttempt1));
        }
        assertEquals("E1 contains bad/error value", exception.getMessage());

        // narrow columns mapping usage - this time column with error is ignored
        InputStream fileAttempt2 = getClass().getClassLoader().getResourceAsStream("error.xlsx");
        IParserSettings settingsAttempt2 = generateSettings(SHEET_FIRST, 1, "A", "B", "C", "D");
        assertEquals(
                List.of(Map.of("A", "id1", "B", "1.5", "C", "3.5", "D", "5"),
                        Map.of("A", "id2", "B", "0", "C", "12", "D", "12"),
                        Map.of("A", "id3", "B", "7", "C", "2", "D", "9")),
                cleanNulls(xlsxParser.parseFileStream(fileAttempt2, settingsAttempt2)));
    }

    private List<Map<String, Object>> cleanNulls(List<Map<String, Object>> list) {
        list.forEach(map -> map.entrySet().removeIf(entry -> entry.getValue() == null));
        return list;
    }

    @Test
    void testEmptyRows() {
        assertEquals(List.of(), new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(SHEET_FIRST, 4)));
        assertEquals(List.of(), new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(SHEET_EMPTY, 1)));
    }

    @Test
    void testWideTable() {
        assertEquals(List.of(
                Map.of("A", "a1", "AA", "aa1", "AAA", "aaa1")
        ), cleanNulls(new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(SHEET_WIDE, 1, "A", "AA", "AAA"))));
    }

    @Test
    @SneakyThrows
    void testOverlappingRowsOk() {
        try (InputStream fileStream = getClass().getClassLoader().getResourceAsStream("test.xlsx")) {
            IParserSettings settings = generateSettings(SHEET_MERGED_OK, 6, "A", "B", "C");
            assertDoesNotThrow(() -> new XlsxParser().parseFileStream(fileStream, settings));
        }
    }

    @Test
    @SneakyThrows
    void testOverlappingRowsFail() {
        try (InputStream fileStream = getClass().getClassLoader().getResourceAsStream("test.xlsx")) {
            IParserSettings settings = generateSettings(SHEET_MERGED_FAIL, 2, "A", "B", "C");
            XlsxParser xlsxParser = new XlsxParser();
            IllegalArgumentException exception =  assertThrows(IllegalArgumentException.class, () -> xlsxParser.parseFileStream(fileStream, settings));
            assertEquals("Merged regions A2:A4 and B3:B4 have partially overlapping row ranges which is not allowed", exception.getMessage());
        }
    }

    @Test
    void testMixed() {
        List<Map<String, Object>> result = new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(SHEET_MERGED_MIXED, 1, "A", "B", "C", "D"));
        assertEquals(List.of(
                Map.of("A", "A1-3", "B", List.of("B11", "B12", "B13"), "C", "C1-3+D1-3", "D", "C1-3+D1-3"),
                Map.of("A", "A4", "B", "B4", "C", "C4", "D", "D4"),
                Map.of("A", "A5-6", "B", "B5-6", "C", Arrays.asList("C5", null), "D", Arrays.asList(null, null))
        ), result);
    }

    @Test
    @SneakyThrows
    void testUnknownSheetName() {
        XlsxParser xlsxParser = new XlsxParser();
        IParserSettings parserSettings = generateSettings(SHEET_BAD, 1);
        try (InputStream xlsxInputStream = getClass().getClassLoader().getResourceAsStream("test.xlsx")) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> xlsxParser.parseFileStream(xlsxInputStream, parserSettings),
                    "Expected IllegalArgumentException thrown, but it didn't");
            assertEquals(String.format("File doesn't contain sheet '%s'", SHEET_BAD), exception.getMessage());
        }
    }

    @Test
    @SneakyThrows
    void testBadStream() {
        XlsxParser xlsxParser = new XlsxParser();
        IParserSettings parserSettings = generateSettings(SHEET_BAD, 1);
        try (InputStream xlsxInputStream = getClass().getClassLoader().getResourceAsStream("bad.xlsx")) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> xlsxParser.parseFileStream(xlsxInputStream, parserSettings),
                    "Expected IllegalArgumentException thrown, but it didn't");
            assertTrue(exception.getMessage().startsWith("File isn't an xlsx"));
        }
    }

    @Test
    void testValidateMergedRegionsConditions() throws Exception {
        XlsxParser parser = new XlsxParser();
        Method method = XlsxParser.class.getDeclaredMethod("validateMergedRegions", List.class, int.class);
        method.setAccessible(true);

        // Non-overlapping: region1 entirely before region2 (first condition true, second false)
        List<CellRangeAddress> beforeRegions = List.of(new CellRangeAddress(1, 2, 0, 0), new CellRangeAddress(4, 5, 1, 1));
        assertDoesNotThrow(() -> method.invoke(parser, beforeRegions, 1));

        // Non-overlapping: region1 entirely after region2 (first condition false)
        List<CellRangeAddress> afterRegions = List.of(new CellRangeAddress(4, 5, 0, 0), new CellRangeAddress(1, 2, 1, 1));
        assertDoesNotThrow(() -> method.invoke(parser, afterRegions, 1));

        // Overlapping with same row range → no exception
        List<CellRangeAddress> sameRangeRegions = List.of(new CellRangeAddress(1, 3, 0, 0), new CellRangeAddress(1, 3, 1, 1));
        assertDoesNotThrow(() -> method.invoke(parser, sameRangeRegions, 1));

        // Same first row, different last row → sameRowRange second condition is false → exception
        List<CellRangeAddress> sameFirstRowRegions = List.of(new CellRangeAddress(1, 3, 0, 0), new CellRangeAddress(1, 4, 1, 1));
        InvocationTargetException ex1 = assertThrows(InvocationTargetException.class, () -> method.invoke(parser, sameFirstRowRegions, 1));
        assertInstanceOf(IllegalArgumentException.class, ex1.getCause());

        // Different first row, same last row → sameRowRange first condition is false → exception
        List<CellRangeAddress> sameLastRowRegions = List.of(new CellRangeAddress(1, 4, 0, 0), new CellRangeAddress(2, 4, 1, 1));
        InvocationTargetException ex2 = assertThrows(InvocationTargetException.class, () -> method.invoke(parser, sameLastRowRegions, 1));
        assertInstanceOf(IllegalArgumentException.class, ex2.getCause());
    }

    private IParserSettings generateSettings(String sheetName, int startingRow, String... customUsedColumnsLetters) {
        return new IParserSettings() {
            @Override
            public String getSheetName() {
                return sheetName;
            }

            @Override
            public int getStartFromRow() {
                return startingRow;
            }

            @Override
            public Set<String> getUsedColumnsLetters() {
                return customUsedColumnsLetters.length > 0 ? Set.of(customUsedColumnsLetters) : Set.of("A", "B", "C", "D", "E");
            }
        };
    }
}
