package ch.sbb.polarion.extension.excel_importer.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig("excel-importer")
class ExcelSheetMappingSettingsTest {

    @Test
    void testSettingDoesNotExist() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            ExcelSheetMappingSettings excelSheetMappingSettings = new ExcelSheetMappingSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(ScopeUtils::getDefaultLocation).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            ExcelSheetMappingSettingsModel model = excelSheetMappingSettings.defaultValues();
            model.setBundleTimestamp("default");

            assertThrows(ObjectNotFoundException.class, () -> {
                ExcelSheetMappingSettingsModel loadedModel = excelSheetMappingSettings.load(projectName, SettingId.fromName("Any setting name"));
            });
        }
    }

    @Test
    void testLoadCustomWhenSettingExists() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<ExcelSheetMappingSettingsModel> excelSheetMappingSettings = new ExcelSheetMappingSettings(mockedSettingsService);

            String projectName = "test_project";

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenCallRealMethod();
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);

            ExcelSheetMappingSettingsModel customSheetMappingSettingsModel = ExcelSheetMappingSettingsModel.builder()
                    .sheetName("customSheetName")
                    .defaultWorkItemType("defaultWorkItemType")
                    .enumsMapping(Map.of("mapping1", Map.of("mapping1", "mapping1")))
                    .columnsMapping(Map.of("mapping1", "mapping1"))
                    .startFromRow(1)
                    .build();
            customSheetMappingSettingsModel.setBundleTimestamp("custom");
            when(mockedSettingsService.read(eq(mockProjectLocation), any())).thenReturn(customSheetMappingSettingsModel.serialize());

            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("345");
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of("Any setting name"));

            ExcelSheetMappingSettingsModel loadedModel = excelSheetMappingSettings.load(projectName, SettingId.fromName("Any setting name"));
            assertEquals("customSheetName", loadedModel.getSheetName());
            assertEquals(Map.of("mapping1", Map.of("mapping1", "mapping1")), loadedModel.getEnumsMapping());
            assertEquals(Map.of("mapping1", "mapping1"), loadedModel.getColumnsMapping());
            assertEquals(1, loadedModel.getStartFromRow());
            assertEquals("custom", loadedModel.getBundleTimestamp());
        }
    }

    @Test
    void testNamedSettings() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService mockedSettingsService = mock(SettingsService.class);
            mockScopeUtils.when(() -> ScopeUtils.getFileContent(any())).thenCallRealMethod();

            GenericNamedSettings<ExcelSheetMappingSettingsModel> settings = new ExcelSheetMappingSettings(mockedSettingsService);

            String projectName = "test_project";
            String settingOne = "setting_one";
            String settingTwo = "setting_two";

            mockScopeUtils.when(() -> ScopeUtils.getScopeFromProject(projectName)).thenReturn("project/test_project/");

            ILocation mockDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            when(mockedSettingsService.getLastRevision(mockDefaultLocation)).thenReturn("some_revision");

            ILocation mockProjectLocation = mock(ILocation.class);
            when(mockedSettingsService.getPersistedSettingFileNames(mockProjectLocation)).thenReturn(List.of(settingOne, settingTwo));
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/test_project/")).thenReturn(mockProjectLocation);
            when(mockProjectLocation.append(contains("mappings"))).thenReturn(mockProjectLocation);
            ILocation settingOneLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingOne))).thenReturn(settingOneLocation);
            ILocation settingTwoLocation = mock(ILocation.class);
            when(mockProjectLocation.append(contains(settingTwo))).thenReturn(settingTwoLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocationByProject(projectName)).thenReturn(mockProjectLocation);
            when(mockedSettingsService.getLastRevision(mockProjectLocation)).thenReturn("some_revision");

            ExcelSheetMappingSettingsModel settingOneModel = ExcelSheetMappingSettingsModel.builder()
                    .sheetName("setting_oneCustomSheetName")
                    .defaultWorkItemType("setting_oneDefaultWorkItemType")
                    .enumsMapping(Map.of("setting_oneMapping1", Map.of("mapping1", "mapping1")))
                    .columnsMapping(Map.of("setting_oneMapping1", "mapping1"))
                    .startFromRow(1)
                    .build();
            settingOneModel.setBundleTimestamp("setting_one");
            when(mockedSettingsService.read(eq(settingOneLocation), any())).thenReturn(settingOneModel.serialize());

            ExcelSheetMappingSettingsModel settingTwoModel = ExcelSheetMappingSettingsModel.builder()
                    .sheetName("setting_twoCustomSheetName")
                    .defaultWorkItemType("setting_twoDefaultWorkItemType")
                    .enumsMapping(Map.of("setting_twoMapping1", Map.of("mapping1", "mapping1")))
                    .columnsMapping(Map.of("setting_twoMapping1", "mapping1"))
                    .startFromRow(1)
                    .build();
            settingTwoModel.setBundleTimestamp("setting_two");
            when(mockedSettingsService.read(eq(settingTwoLocation), any())).thenReturn(settingTwoModel.serialize());

            ExcelSheetMappingSettingsModel loadedOneModel = settings.load(projectName, SettingId.fromName(settingOne));
            assertEquals("setting_oneCustomSheetName", loadedOneModel.getSheetName());
            assertEquals("setting_oneDefaultWorkItemType", loadedOneModel.getDefaultWorkItemType());
            assertEquals(Map.of("setting_oneMapping1", Map.of("mapping1", "mapping1")), loadedOneModel.getEnumsMapping());
            assertEquals("setting_oneDefaultWorkItemType", loadedOneModel.getDefaultWorkItemType());
            assertEquals(1, loadedOneModel.getStartFromRow());
            assertEquals("setting_one", loadedOneModel.getBundleTimestamp());

            ExcelSheetMappingSettingsModel loadedTwoModel = settings.load(projectName, SettingId.fromName(settingTwo));
            assertEquals("setting_twoCustomSheetName", loadedTwoModel.getSheetName());
            assertEquals("setting_twoDefaultWorkItemType", loadedTwoModel.getDefaultWorkItemType());
            assertEquals(Map.of("setting_twoMapping1", Map.of("mapping1", "mapping1")), loadedTwoModel.getEnumsMapping());
            assertEquals("setting_twoDefaultWorkItemType", loadedTwoModel.getDefaultWorkItemType());
            assertEquals(1, loadedTwoModel.getStartFromRow());
            assertEquals("setting_two", loadedTwoModel.getBundleTimestamp());
        }
    }
}
