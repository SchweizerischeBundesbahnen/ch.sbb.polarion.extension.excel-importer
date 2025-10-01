package ch.sbb.polarion.extension.excel_importer.utils;

import ch.sbb.polarion.extension.excel_importer.service.parser.impl.XlsxParser;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettingsModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static ch.sbb.polarion.extension.excel_importer.utils.ParseXlsRunnable.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParseXlsRunnableTest {

    @Test
    void testRun() {
        // Given
        byte[] fileContent = new byte[]{1, 2, 3};
        ExcelSheetMappingSettingsModel settings = mock(ExcelSheetMappingSettingsModel.class);
        List<Map<String, Object>> expectedResult = List.of(
                Map.of("A", "value1", "B", "value2"),
                Map.of("A", "value3", "B", "value4")
        );

        Map<String, Object> params = Map.of(
                PARAM_FILE_CONTENT, fileContent,
                PARAM_SETTINGS, settings
        );

        try (MockedConstruction<XlsxParser> xlsxParserMock = mockConstruction(XlsxParser.class,
                (mock, context) -> when(mock.parseFileStream(any(InputStream.class), any())).thenReturn(expectedResult))) {

            // When
            ParseXlsRunnable runnable = new ParseXlsRunnable();
            Map<String, Object> result = runnable.run(params);

            // Then
            assertEquals(expectedResult, result.get(PARAM_RESULT));
            assertEquals(1, xlsxParserMock.constructed().size());
            XlsxParser constructedParser = xlsxParserMock.constructed().get(0);
            verify(constructedParser).parseFileStream(any(InputStream.class), eq(settings));
        }
    }
}
