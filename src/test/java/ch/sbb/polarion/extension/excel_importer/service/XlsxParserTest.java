package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.parser.IParserSettings;
import ch.sbb.polarion.extension.excel_importer.service.parser.impl.XlsxParser;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class XlsxParserTest {

    private static final String SHEET_FIRST = "first";
    private static final String SHEET_EMPTY = "empty";
    private static final String SHEET_BAD = "bad";

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
    void testFormulaError() {
        XlsxParser xlsxParser = new XlsxParser();
        InputStream fileAttempt1 = getClass().getClassLoader().getResourceAsStream("error.xlsx");
        IParserSettings settingsAttempt1 = generateSettings(SHEET_FIRST, 1);

        // one of the columns has error
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> xlsxParser.parseFileStream(fileAttempt1, settingsAttempt1));
        assertEquals("E1 contains bad/error value", exception.getMessage());

        // narrow columns mapping usage - this time column with error is ignored
        InputStream fileAttempt2 = getClass().getClassLoader().getResourceAsStream("error.xlsx");
        IParserSettings settingsAttempt2 = generateSettings(SHEET_FIRST, 1, "A", "B", "C", "D");
        assertEquals(
                List.of(Map.of("A", "id1", "B", 1.5, "C", 3.5, "D", 5.0),
                        Map.of("A", "id2", "B", 0.0, "C", 12.0, "D", 12.0),
                        Map.of("A", "id3", "B", 7.0, "C", 2.0, "D", 9.0)),
                cleanNulls(xlsxParser.parseFileStream(fileAttempt2, settingsAttempt2)));
    }

    private List<Map<String, Object>> cleanNulls(List<Map<String, Object>> list) {
        list.forEach(map -> map.entrySet().removeIf(entry -> entry.getValue() == null));
        return list;
    }

    @Test
    void testEmptyFile() {
        assertEquals(List.of(), new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(SHEET_FIRST, 4)));
        assertEquals(List.of(), new XlsxParser().parseFileStream(getClass().getClassLoader().getResourceAsStream("test.xlsx"), generateSettings(SHEET_EMPTY, 1)));
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
