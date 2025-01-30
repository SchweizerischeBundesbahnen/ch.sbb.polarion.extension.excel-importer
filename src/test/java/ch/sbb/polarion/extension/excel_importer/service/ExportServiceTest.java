package ch.sbb.polarion.extension.excel_importer.service;

import org.intellij.lang.annotations.Language;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    @Language("HTML")
    private static final String VALID_TABLE_HTML = """
            <table>
                <tr>
                    <th>Header 1</th>
                    <th>Header 2</th>
                </tr>
                <tr>
                    <td>Data 1</td>
                    <td>Data 2</td>
                </tr>
            </table>
            """;

    private final ExportService exportService = new ExportService();

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

    @Test
    void testExtractSimpleText() {
        Element element = Jsoup.parseBodyFragment("Simple text").body();
        assertEquals("Simple text", exportService.extractTextWithLineBreaks(element));
    }

    @Test
    void testExtractSimpleTagText() {
        Element element = Jsoup.parseBodyFragment("<span>Simple text</span>").body().child(0);
        assertEquals("Simple text", exportService.extractTextWithLineBreaks(element));
    }

    @Test
    void testExtractTextWithBr() {
        Element element = Jsoup.parseBodyFragment("<span>Line 1<br>Line 2<br>Line 3</span>").body().child(0);
        assertEquals("Line 1\nLine 2\nLine 3", exportService.extractTextWithLineBreaks(element));
    }

    @Test
    void testExtractTextWithBrVariants() {
        Element element = Jsoup.parseBodyFragment("<span>First line<br/>Second line<br />Third line</span>").body().child(0);
        assertEquals("First line\nSecond line\nThird line", exportService.extractTextWithLineBreaks(element));
    }

    @Test
    void testExtractNestedElements() {
        Element element = Jsoup.parseBodyFragment("<span>Outer <b>Bold</b> Text<br>Next Line</span>").body().child(0);
        assertEquals("Outer Bold Text\nNext Line", exportService.extractTextWithLineBreaks(element));
    }

    @Test
    void testExtractDeeplyNestedElements() {
        Element element = Jsoup.parseBodyFragment("<div><div>Parent <span>Child <b>Bold</b></span></div><br>New Line</div>").body().child(0);
        assertEquals("Parent Child Bold\nNew Line", exportService.extractTextWithLineBreaks(element));
    }

    @Test
    void testExtractEmptyElement() {
        Element element = Jsoup.parseBodyFragment("<div></div>").body().child(0);
        assertEquals("", exportService.extractTextWithLineBreaks(element));
    }

    @Test
    void testExtractOnlyBrElements() {
        Element element = Jsoup.parseBodyFragment("<span><br><br><br></span>").body().child(0);
        assertEquals("", exportService.extractTextWithLineBreaks(element));
    }

    @Test
    void testExtractBrElementsInTheMiddle() {
        Element element = Jsoup.parseBodyFragment("<span>Some<br><br><br>text</span>").body().child(0);
        assertEquals("Some\n\n\ntext", exportService.extractTextWithLineBreaks(element));
    }

}
