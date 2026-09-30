package ch.sbb.polarion.extension.excel_importer.rest;

import ch.sbb.polarion.extension.excel_importer.service.ImportJobsService;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettings;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;

@SuppressWarnings("unused")
class ExcelImporterRestApplicationTest {

    @Test
    void testConstructor() {
        try (MockedStatic<ImportJobsService> configurationMockedStatic = mockStatic(ImportJobsService.class);
             MockedConstruction<ExcelSheetMappingSettings> excelSheetMappingSettingsMockedConstruction = mockConstruction(ExcelSheetMappingSettings.class)) {
            assertDoesNotThrow(ExcelImporterRestApplication::new);

            // check that an error when starting the jobs cleaner does not prevent application initialization
            configurationMockedStatic.when(ImportJobsService::startCleaner).thenThrow(new IllegalStateException());
            assertDoesNotThrow(ExcelImporterRestApplication::new);
        }
    }

}
