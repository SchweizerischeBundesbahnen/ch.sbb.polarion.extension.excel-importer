package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.utils.PolarionUtils;
import org.apache.commons.io.IOUtils;
import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;

class ExportServiceTest {

    @Language("HTML")
    private static final String VALID_TABLE_HTML = """
            <table>
                <tr>
                    <th>Header 1</th>
                    <th>Header 2</th>
                    <th>Header 3</th>
                    <th>Header 4</th>
                </tr>
                <tr>
                    <td>Data 1</td>
                    <td>Data 2</td>
                    <td><a href="https://polarion.url/polarion/#/project/elibrary/workitem?id=EL-12345">EL-12345 - Mega WorkItem</a></td>
                    <td><img src="https://polarion.url/polarion/image.png" alt=''></td>
                </tr>
            </table>
            """;

    private final ExportService exportService = new ExportService();


    MockedStatic<PolarionUtils> polarionUtilsMockedStatic;
    MockedStatic<IOUtils> ioUtilsMockedStatic;

    @BeforeEach
    void setUp() throws IOException {
        polarionUtilsMockedStatic = mockStatic(PolarionUtils.class);
        ioUtilsMockedStatic = mockStatic(IOUtils.class);

        URL imageURL = mock(URL.class);
        when(imageURL.openStream()).thenReturn(mock(InputStream.class));
        polarionUtilsMockedStatic.when(() -> PolarionUtils.getAbsoluteUrl(anyString())).thenReturn(imageURL);
        ioUtilsMockedStatic.when(() -> IOUtils.toByteArray(any(InputStream.class))).thenReturn("image_content".getBytes(StandardCharsets.UTF_8));
    }

    @AfterEach
    void tearDown() {
        polarionUtilsMockedStatic.close();
        ioUtilsMockedStatic.close();
    }

    @Test
    void exportHtmlTableValidInputTest() {
        String sheetName = "TestSheet";
        String encodedHtml = Base64.getEncoder().encodeToString(VALID_TABLE_HTML.getBytes(StandardCharsets.UTF_8));

        String result = exportService.exportHtmlTable(sheetName, encodedHtml);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertDoesNotThrow(() -> Base64.getDecoder().decode(result));
    }

    @Test
    void exportHtmlTableEmptySheetTest() {
        String encodedHtml = Base64.getEncoder().encodeToString(VALID_TABLE_HTML.getBytes(StandardCharsets.UTF_8));
        String result = exportService.exportHtmlTable("", encodedHtml);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void exportHtmlTableEmptyContentTest() {
        String sheetName = "TestSheet";
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exportService.exportHtmlTable(sheetName, "")
        );
        assertEquals("html table content is empty", exception.getMessage());
    }

    @Test
    void exportHtmlTableInvalidBase64ContentTest() {
        String sheetName = "TestSheet";
        String invalidBase64 = "!@#$%^&*";

        assertThrows(
                IllegalArgumentException.class,
                () -> exportService.exportHtmlTable(sheetName, invalidBase64)
        );
    }

    @Test
    void exportHtmlTableNoTableInHtmlTest() {
        String sheetName = "TestSheet";
        String htmlWithoutTable = "<div>No table here</div>";
        String encodedHtml = Base64.getEncoder().encodeToString(htmlWithoutTable.getBytes(StandardCharsets.UTF_8));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> exportService.exportHtmlTable(sheetName, encodedHtml)
        );
        assertEquals("html table not found in the provided html", exception.getMessage());
    }

    @Test
    void exportHtmlTableComplexTableTest() {
        @Language("HTML")
        String complexTable = """
                <table>
                    <tr>
                        <th>Header 1</th>
                        <th>Header 2</th>
                        <th>Header 3</th>
                    </tr>
                    <tr>
                        <td>Data 1</td>
                        <td>Data 2</td>
                        <td>Data 3</td>
                    </tr>
                    <tr>
                        <td>More Data 1</td>
                        <td>More Data 2</td>
                        <td>More Data 3</td>
                    </tr>
                </table>
                """;
        String encodedHtml = Base64.getEncoder().encodeToString(complexTable.getBytes(StandardCharsets.UTF_8));

        String result = exportService.exportHtmlTable("ComplexSheet", encodedHtml);
        assertNotNull(result);
        assertFalse(result.isEmpty());
        byte[] decodedBytes = Base64.getDecoder().decode(result);
        assertTrue(decodedBytes.length > 0);
    }

    @Test
    void exportHtmlTableNullSheetNameTest() {
        String encodedHtml = Base64.getEncoder().encodeToString(VALID_TABLE_HTML.getBytes(StandardCharsets.UTF_8));

        String result = exportService.exportHtmlTable(null, encodedHtml);
        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void exportHtmlTableNullContentTest() {
        assertThrows(
                IllegalArgumentException.class,
                () -> exportService.exportHtmlTable("TestSheet", null)
        );
    }

}
