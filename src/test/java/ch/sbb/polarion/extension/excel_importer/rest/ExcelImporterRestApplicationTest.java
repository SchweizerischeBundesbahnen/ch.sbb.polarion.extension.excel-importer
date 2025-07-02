package ch.sbb.polarion.extension.excel_importer.rest;

import ch.sbb.polarion.extension.excel_importer.service.ImportJobsCleaner;
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
        try (MockedStatic<ImportJobsCleaner> configurationMockedStatic = mockStatic(ImportJobsCleaner.class);
             MockedConstruction<ExcelSheetMappingSettings> excelSheetMappingSettingsMockedConstruction = mockConstruction(ExcelSheetMappingSettings.class)) {
            assertDoesNotThrow(ExcelImporterRestApplication::new);

            // check that error in ImportJobsCleaner does not prevent application initialization
            configurationMockedStatic.when(ImportJobsCleaner::startCleaningJob).thenThrow(new IllegalStateException());
            assertDoesNotThrow(ExcelImporterRestApplication::new);
        }
    }

}
