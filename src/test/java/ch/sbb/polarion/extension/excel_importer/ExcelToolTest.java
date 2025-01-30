package ch.sbb.polarion.extension.excel_importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExcelToolTest {

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

}
