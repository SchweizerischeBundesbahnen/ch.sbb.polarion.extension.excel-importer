package ch.sbb.polarion.extension.excel_importer;

import ch.sbb.polarion.extension.generic.test_extensions.BundleJarsPrioritizingRunnableMockExtension;
import com.polarion.alm.tracker.exporter.IExport;
import com.polarion.alm.tracker.exporter.IExportTemplate;
import com.polarion.alm.tracker.exporter.ITableExportConfiguration;
import com.polarion.alm.tracker.model.IAttachment;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.ITransactionService;
import com.polarion.platform.core.IPlatform;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.model.IPObject;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.spi.PObjectList;
import com.polarion.subterra.base.data.identification.IContextId;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, BundleJarsPrioritizingRunnableMockExtension.class})
class ExcelToolTest {

    public static final String TABLE_XLSX = "table.xlsx";
    public static final String SHEET_1 = "Sheet1";
    public static final String TABLE_HTML = "table.html";
    public static final String EXCEL_TABLE = "Excel Table";

    @Test
    void initReturnsSomething() {
        String result = ExcelTool.init();
        assertNotNull(result);
    }

    @Test
    void exportHtmlTableWithValidTableId() {
        String result = ExcelTool.exportHtmlTable("myTable");
        assertEquals("exportHtmlTable('myTable', '', '')", result);
    }

    @Test
    void exportHtmlTableWithNullTableId() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ExcelTool.exportHtmlTable(null)
        );
        assertEquals("Table ID required", exception.getMessage());
    }

    @Test
    void exportHtmlTableWithValidTableIdAndSheetName() {
        String result = ExcelTool.exportHtmlTable("myTable", "Sheet1");
        assertEquals("exportHtmlTable('myTable', 'Sheet1', '')", result);
    }

    @Test
    void exportHtmlTableWithNullSheetName() {
        String result = ExcelTool.exportHtmlTable("myTable", null);
        assertEquals("exportHtmlTable('myTable', '', '')", result);
    }

    @Test
    void exportHtmlTableWithNullTableIdAndValidSheetName() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ExcelTool.exportHtmlTable(null, "Sheet1")
        );
        assertEquals("Table ID required", exception.getMessage());
    }

    @Test
    void exportHtmlTableWithAllValidParameters() {
        String result = ExcelTool.exportHtmlTable("myTable", "Sheet1", "export.xlsx");
        assertEquals("exportHtmlTable('myTable', 'Sheet1', 'export.xlsx')", result);
    }

    @Test
    void exportHtmlTableWithNullOptionalParameters() {
        String result = ExcelTool.exportHtmlTable("myTable", null, null);
        assertEquals("exportHtmlTable('myTable', '', '')", result);
    }

    @Test
    void exportHtmlTableWithNullTableIdAndValidOptionalParameters() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> ExcelTool.exportHtmlTable(null, "Sheet1", "export.xlsx")
        );
        assertEquals("Table ID required", exception.getMessage());
    }

    @Test
    void exportHtmlTableWithMixedNullParameters() {
        String result = ExcelTool.exportHtmlTable("myTable", "Sheet1", null);
        assertEquals("exportHtmlTable('myTable', 'Sheet1', '')", result);

        result = ExcelTool.exportHtmlTable("myTable", null, "export.xlsx");
        assertEquals("exportHtmlTable('myTable', '', 'export.xlsx')", result);
    }

    @Test
    void testAttachTable() {
        IWorkItem workitem = mock(IWorkItem.class);
        IAttachment attachment = mock(IAttachment.class);
        when(workitem.createAttachment(eq(TABLE_XLSX), eq(EXCEL_TABLE), any())).thenReturn(attachment);

        try (MockedStatic<PlatformContext> platformContextMockedStatic = mockStatic(PlatformContext.class)) {
            IPlatform platform = mock(IPlatform.class);
            ITransactionService transactionService = mock(ITransactionService.class);
            platformContextMockedStatic.when(PlatformContext::getPlatform).thenReturn(platform);
            when(platform.lookupService(ITransactionService.class)).thenReturn(transactionService);

            try (InputStream inputStream = ExcelToolTest.class.getClassLoader().getResourceAsStream(TABLE_HTML)) {
                assertNotNull(inputStream);
                String htmlTable = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                boolean created = ExcelTool.attachTable(htmlTable, workitem, TABLE_XLSX, EXCEL_TABLE);

                assertTrue(created);
                verify(workitem, times(1)).createAttachment(eq(TABLE_XLSX), eq(EXCEL_TABLE), any());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Test
    void testConvertTableAndGetHTMLTable() {
        try (InputStream htmlInputStream = ExcelToolTest.class.getClassLoader().getResourceAsStream(TABLE_HTML)) {
            assertNotNull(htmlInputStream);
            final String htmlTable = new String(htmlInputStream.readAllBytes(), StandardCharsets.UTF_8);

            byte[] bytes = ExcelTool.convertHtmlTableToXlsx(Base64.getEncoder().encodeToString(htmlTable.getBytes(StandardCharsets.UTF_8)), null);
            assertTrue(bytes.length > 0);

            try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
                assertEquals(1, workbook.getNumberOfSheets());
                Sheet sheet = workbook.getSheetAt(0);
                assertEquals(SHEET_1, sheet.getSheetName());
                int physicalNumberOfRows = sheet.getPhysicalNumberOfRows();
                assertEquals(101, physicalNumberOfRows);

                // Convert sheet to list of lists for comparison
                List<List<String>> sheetData = new java.util.ArrayList<>();
                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        rowData.add(cell.toString());
                    }
                    sheetData.add(rowData);
                }

                String htmlTableFromSheetData = ExcelTool.getHTMLTable(sheetData);
                assertEquals(prepareForComparison(htmlTable), prepareForComparison(htmlTableFromSheetData));
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetTableExportConfiguration() {
        IWorkItem workItem1 = mock(IWorkItem.class);
        IWorkItem workItem2 = mock(IWorkItem.class);

        IPObjectList<IPObject> workItems = new PObjectList(null, List.of(workItem1, workItem2));

        IExportTemplate exportTemplate = mock(IExportTemplate.class);
        IContextId contextId = mock(IContextId.class);

        ITableExportConfiguration tableExportConfiguration = ExcelTool.getTableExportConfiguration(workItems, "", "utf-8", null, exportTemplate, null, contextId);
        assertNotNull(tableExportConfiguration);
    }

    @Test
    void testWaitForExport() {
        IExport export1 = mock(IExport.class);
        when(export1.isFinished()).thenReturn(false, false, false, true);

        boolean finished1 = ExcelTool.waitForExport(export1, 10);
        assertTrue(finished1);
        verify(export1, times(4)).isFinished();

        IExport export2 = mock(IExport.class);
        when(export2.isFinished()).thenReturn(false, false);

        boolean finished2 = ExcelTool.waitForExport(export2, 1);
        assertFalse(finished2);
        verify(export2, times(1)).isFinished();
    }

    private @NotNull String prepareForComparison(@NotNull String input) {
        return input.replaceAll("style\\s*=\\s*['\"]([^'\"]*?)['\"]", "")
                .replaceAll("class\\s*=\\s*['\"]([^'\"]*?)['\"]", "")
                .replaceAll("[\n\t]", "")
                .replaceAll("\\s{2,}", " ")
                .replaceAll(">\\s+", ">")
                .replaceAll("<\\s+", "<")
                .replaceAll(";\\s+", ";")
                .replaceAll("\\{\\s+", "{")
                .replaceAll("}\\s+", "}");
    }

}
