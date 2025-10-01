package ch.sbb.polarion.extension.excel_importer.utils;

import ch.sbb.polarion.extension.excel_importer.service.ExportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static ch.sbb.polarion.extension.excel_importer.utils.ExportXlsRunnable.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportXlsRunnableTest {

    @Test
    void testRun() {
        // Given
        String sheetName = "TestSheet";
        String content = "<table><tr><td>Test Content</td></tr></table>";
        String expectedResult = "exported-excel-content";

        Map<String, Object> params = Map.of(
                PARAM_SHEET_NAME, sheetName,
                PARAM_CONTENT, content
        );

        try (MockedConstruction<ExportService> exportServiceMock = mockConstruction(ExportService.class,
                (mock, context) -> when(mock.exportHtmlTable(anyString(), anyString())).thenReturn(expectedResult))) {

            // When
            ExportXlsRunnable runnable = new ExportXlsRunnable();
            Map<String, Object> result = runnable.run(params);

            // Then
            assertEquals(expectedResult, result.get(PARAM_RESULT));
            assertEquals(1, exportServiceMock.constructed().size());
            ExportService constructedService = exportServiceMock.constructed().get(0);
            verify(constructedService).exportHtmlTable(sheetName, content);
        }
    }
}
