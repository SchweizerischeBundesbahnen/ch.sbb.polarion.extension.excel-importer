package ch.sbb.polarion.extension.excel_importer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ExcelImporterAppServletTest {

    @Test
    void testConstruction() {
        assertDoesNotThrow(ExcelImporterAppServlet::new);
    }

}
