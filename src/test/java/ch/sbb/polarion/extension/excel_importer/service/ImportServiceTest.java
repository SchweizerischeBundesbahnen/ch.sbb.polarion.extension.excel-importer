package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.service.parser.impl.XlsxParser;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettings;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettingsModel;
import ch.sbb.polarion.extension.excel_importer.utils.LinkInfo;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.fields.model.Option;
import ch.sbb.polarion.extension.generic.test_extensions.BundleJarsPrioritizingRunnableMockExtension;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.shared.api.transaction.RunnableInWriteTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.internal.model.TestSteps;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.model.IStructure;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.model.ICustomField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("squid:S5778") //ignore assertThrows single invocation requirement
@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class, BundleJarsPrioritizingRunnableMockExtension.class})
class ImportServiceTest {

    private static final String TEST_PROJECT_ID = "test";

    private MockedConstruction<ExcelSheetMappingSettings> settingsMockedConstruction;
    private MockedConstruction<XlsxParser> xlsxParserMockedConstruction;
    private ExcelSheetMappingSettingsModel mockedSettings;
    private List<Map<String, Object>> parsedData;

    @BeforeEach
    void setUp() {
        mockedSettings = mock(ExcelSheetMappingSettingsModel.class);
        settingsMockedConstruction = mockConstruction(ExcelSheetMappingSettings.class,
                (mock, context) -> when(mock.load(nullable(String.class), any())).thenReturn(mockedSettings)
        );
        xlsxParserMockedConstruction = mockConstruction(XlsxParser.class,
                (mock, context) -> when(mock.parseFileStream(any(), any())).thenReturn(parsedData)
        );
        parsedData = new ArrayList<>();
    }

    @AfterEach
    void cleanup() {
        settingsMockedConstruction.close();
        xlsxParserMockedConstruction.close();
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void testUnknownType() {
        ITrackerService trackerService = mock(ITrackerService.class);
        PolarionServiceExt polarionServiceExt = new PolarionServiceExt(trackerService, mock(IProjectService.class), mock(ISecurityService.class), mock(IPlatformService.class), mock(IRepositoryService.class));
        ITrackerProject project = mock(ITrackerProject.class);
        lenient().when(trackerService.getTrackerProject(anyString())).thenReturn(project);
        lenient().when(project.isUnresolvable()).thenReturn(false);

        IEnumeration typesEnumeration = mock(IEnumeration.class);
        lenient().when(project.getId()).thenReturn(TEST_PROJECT_ID);
        when(project.getWorkItemTypeEnum()).thenReturn(typesEnumeration);

        ITypeOpt typeOption = mock(ITypeOpt.class);
        lenient().when(typeOption.getId()).thenReturn("otherType");
        when(typesEnumeration.wrapOption(anyString())).thenReturn(typeOption);
        when(typeOption.isPhantom()).thenReturn(true);

        when(mockedSettings.getDefaultWorkItemType()).thenReturn("requirement");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]),
                "Expected IllegalArgumentException thrown, but it didn't");
        assertEquals(String.format("Cannot find WorkItem type '%s' in scope of the project '%s'", "requirement", TEST_PROJECT_ID), exception.getMessage());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked", "java:S6068"})
    void testSuccessfulImport() {
        ITrackerService trackerService = mock(ITrackerService.class);
        PolarionServiceExt polarionServiceExt = new PolarionServiceExt(trackerService, mock(IProjectService.class), mock(ISecurityService.class), mock(IPlatformService.class), mock(IRepositoryService.class));
        ITrackerProject project = mock(ITrackerProject.class);
        lenient().when(trackerService.getTrackerProject(anyString())).thenReturn(project);
        lenient().when(project.isUnresolvable()).thenReturn(false);

        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);
        ICustomFieldsService customFieldsService = mock(ICustomFieldsService.class);
        when(dataService.getCustomFieldsService()).thenReturn(customFieldsService);

        ICustomField excelIdField = mock(ICustomField.class);
        when(excelIdField.getId()).thenReturn("excelId");
        when(excelIdField.getType()).thenReturn(FieldType.STRING.getType());
        ICustomField excelDataField = mock(ICustomField.class);
        when(excelDataField.getId()).thenReturn("excelData");
        when(excelDataField.getType()).thenReturn(FieldType.BOOLEAN.getType());
        ICustomField nullPossibleField = mock(ICustomField.class);
        when(nullPossibleField.getId()).thenReturn("nullPossible");
        when(nullPossibleField.getType()).thenReturn(FieldType.FLOAT.getType());

        when(customFieldsService.getCustomFields(anyString(), any(), anyString())).thenReturn(Arrays.asList(excelIdField, excelDataField, nullPossibleField));
        when(customFieldsService.getCustomFields(anyString(), any(), eq(null))).thenReturn(Collections.EMPTY_LIST);

        IEnumeration typesEnumeration = mock(IEnumeration.class);
        lenient().when(project.getId()).thenReturn(TEST_PROJECT_ID);
        when(project.getWorkItemTypeEnum()).thenReturn(typesEnumeration);

        ITypeOpt typeOption = mock(ITypeOpt.class);
        lenient().when(typeOption.getId()).thenReturn("requirement");
        when(typesEnumeration.wrapOption(anyString())).thenReturn(typeOption);
        when(typeOption.isPhantom()).thenReturn(false);

        try (final MockedStatic<TransactionalExecutor> executor = mockStatic(TransactionalExecutor.class)) {
            executor.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(invocation -> {
                RunnableInWriteTransaction runnable = invocation.getArgument(0);
                return runnable.run(mock(WriteTransaction.class));
            });

            IPObjectList queryResult = mock(IPObjectList.class);
            when(queryResult.stream()).thenAnswer(invocationOnMock -> Stream.of());
            when(trackerService.queryWorkItems(anyString(), anyString())).thenReturn(queryResult);

            IWorkItem workItem = mock(IWorkItem.class);
            when(workItem.getId()).thenReturn("testId");
            IPrototype prototype = mock(IPrototype.class);
            when(prototype.isKeyDefined(any())).thenReturn(false);
            when(workItem.getPrototype()).thenReturn(prototype);
            ICustomField customField = mock(ICustomField.class);
            when(workItem.getCustomFieldPrototype(any())).thenReturn(customField);
            when(trackerService.createWorkItem(any())).thenReturn(workItem);
            when(workItem.getProjectId()).thenReturn("testId");

            lenient().when(workItem.getCustomField(eq("excelId"))).thenReturn("1");
            lenient().when(workItem.getCustomField(eq("excelData"))).thenReturn("someData");
            lenient().when(workItem.getCustomField(eq("nullPossible"))).thenReturn(null);

            lenient().when(dataService.getPrototype(anyString())).thenReturn(prototype);

            Map<String, Object> map = new HashMap<>();
            map.put("A", "a1");
            map.put("B", "b1");
            map.put("C", "false");
            map.put("D", null);
            parsedData.add(map);

            //test using disabled 'overwriteWithEmpty'
            mockSettings(false);
            ImportResult result = new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]);
            assertTrue(result.getUpdatedIds().isEmpty());
            assertEquals(List.of("testId"), result.getCreatedIds());
            assertTrue(result.getUnchangedIds().isEmpty());
            assertTrue(result.getSkippedIds().isEmpty());
            assertTrue(result.getLog().contains("b1: new work item 'testId' is being created"));
            verify(workItem, times(0)).setCustomField(eq("nullPossible"), eq(null));

            //'overwriteWithEmpty' enabled but existing value is null therefore is no update expected
            mockSettings(true);
            new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]);
            verify(workItem, times(0)).setCustomField(eq("nullPossible"), eq(null));

            //'overwriteWithEmpty' enabled and existing value differs
            when(workItem.getCustomField(eq("nullPossible"))).thenReturn("someExistingValue");
            new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]);
            verify(workItem, times(1)).setCustomField(eq("nullPossible"), eq(null));
        }
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void testImportViaId() {
        ITrackerService trackerService = mock(ITrackerService.class);
        PolarionServiceExt polarionServiceExt = new PolarionServiceExt(trackerService, mock(IProjectService.class), mock(ISecurityService.class), mock(IPlatformService.class), mock(IRepositoryService.class));
        ITrackerProject project = mock(ITrackerProject.class);
        lenient().when(trackerService.getTrackerProject(anyString())).thenReturn(project);
        lenient().when(project.isUnresolvable()).thenReturn(false);

        IDataService dataService = mock(IDataService.class);
        when(trackerService.getDataService()).thenReturn(dataService);
        ICustomFieldsService customFieldsService = mock(ICustomFieldsService.class);
        when(dataService.getCustomFieldsService()).thenReturn(customFieldsService);

        ICustomField excelIdField = mock(ICustomField.class);
        when(excelIdField.getId()).thenReturn("id");
        when(excelIdField.getType()).thenReturn(FieldType.STRING.getType());
        ICustomField excelTitleField = mock(ICustomField.class);
        when(excelTitleField.getId()).thenReturn("title");
        when(excelTitleField.getType()).thenReturn(FieldType.STRING.getType());

        when(customFieldsService.getCustomFields(anyString(), any(), anyString())).thenReturn(Arrays.asList(excelIdField, excelTitleField));
        when(customFieldsService.getCustomFields(anyString(), any(), eq(null))).thenReturn(Collections.EMPTY_LIST);

        IEnumeration typesEnumeration = mock(IEnumeration.class);
        lenient().when(project.getId()).thenReturn(TEST_PROJECT_ID);
        when(project.getWorkItemTypeEnum()).thenReturn(typesEnumeration);

        ITypeOpt typeOption = mock(ITypeOpt.class);
        lenient().when(typeOption.getId()).thenReturn("requirement");
        when(typesEnumeration.wrapOption(anyString())).thenReturn(typeOption);
        when(typeOption.isPhantom()).thenReturn(false);

        try (final MockedStatic<TransactionalExecutor> executor = mockStatic(TransactionalExecutor.class)) {
            executor.when(() -> TransactionalExecutor.executeInWriteTransaction(any())).thenAnswer(invocation -> {
                RunnableInWriteTransaction runnable = invocation.getArgument(0);
                return runnable.run(mock(WriteTransaction.class));
            });

            IWorkItem workItem = mock(IWorkItem.class);
            when(workItem.getId()).thenReturn("testId");
            when(workItem.getValue("id")).thenReturn("testId");
            when(workItem.getValue("title")).thenReturn("b1");
            when(workItem.getProjectId()).thenReturn("testId");

            IPObjectList queryResult = mock(IPObjectList.class);
            when(queryResult.stream()).thenAnswer(invocationOnMock -> Stream.of(workItem));
            when(trackerService.queryWorkItems(anyString(), anyString())).thenReturn(queryResult);

            IPrototype prototype = mock(IPrototype.class);
            when(prototype.isKeyDefined(any())).thenReturn(true);
            when(workItem.getPrototype()).thenReturn(prototype);

            lenient().when(dataService.getPrototype(anyString())).thenReturn(prototype);

            Map<String, Object> mapOne = new HashMap<>(); // imported id cell has wrong value
            mapOne.put("A", "a1");
            mapOne.put("B", "b1");
            parsedData.add(mapOne);

            IWorkItem createdWorkItem = mock(IWorkItem.class);
            when(createdWorkItem.getProjectId()).thenReturn("test");
            when(trackerService.createWorkItem(any())).thenReturn(createdWorkItem);

            when(createdWorkItem.getId()).thenReturn("testId");
            when(createdWorkItem.getPrototype()).thenReturn(prototype);
            ICustomField customField = mock(ICustomField.class);
            lenient().when(createdWorkItem.getCustomFieldPrototype(any())).thenReturn(customField);

            // test importing wi with id but not using id as link column
            mockSettingsForIdImport(false);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]),
                    "Expected IllegalArgumentException thrown, but it didn't");
            assertEquals("WorkItem id can only be imported if it is used as Link Column.", exception.getMessage());

            mockSettingsForIdImport(true);
            ImportResult result = new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]);
            assertTrue(result.getUpdatedIds().isEmpty());
            assertTrue(result.getCreatedIds().isEmpty());
            assertTrue(result.getUnchangedIds().isEmpty());
            assertEquals(List.of("a1"), result.getSkippedIds());
            assertTrue(result.getLog().contains("a1: no work item found by ID 'a1'. Since the 'id' is used as the 'Link Column', new work item creation is impossible"));

            Map<String, Object> mapTwo = new HashMap<>(); // imported id cell is empty
            mapTwo.put("A", "");
            mapTwo.put("B", "b1");
            parsedData.clear();
            parsedData.add(mapTwo);

            // test importing wi with id as link column but imported wi has empty id field
            result = new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]);
            assertTrue(result.getUpdatedIds().isEmpty());
            assertTrue(result.getSkippedIds().isEmpty());
            assertTrue(result.getUnchangedIds().isEmpty());
            assertEquals(List.of("testId"), result.getCreatedIds());
            assertTrue(result.getLog().contains("new work item 'testId' is being created"));

            // same using null
            mapTwo.put("A", null);
            result = new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]);
            assertTrue(result.getLog().contains("new work item 'testId' is being created"));

            // or empty trimmed string
            mapTwo.put("A", "     ");
            result = new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]);
            assertTrue(result.getLog().contains("new work item 'testId' is being created"));

            Map<String, Object> mapThree = new HashMap<>(); // imported id cell has valid value (and title cell has new value)
            mapThree.put("A", "testId");
            mapThree.put("B", "newTitle");
            parsedData.clear();
            parsedData.add(mapThree);

            // test importing wi with id as link column and imported wi has same id as existing wi
            result = new ImportService(polarionServiceExt).processFile(TEST_PROJECT_ID, "testMapping", new byte[0]);
            assertTrue(result.getUpdatedIds().isEmpty());
            assertTrue(result.getCreatedIds().isEmpty());
            assertEquals(List.of("testId"), result.getUnchangedIds());
            assertTrue(result.getSkippedIds().isEmpty());
            assertTrue(result.getLog().contains("testId: no changes were made to 'testId'"));
        }
    }

    @Test
    void testPrepareValue() {
        ImportService service = new ImportService(mock(PolarionServiceExt.class));

        FieldMetadata stringMetadata = FieldMetadata.builder().id("fieldId").type(FieldType.STRING.getType()).build();
        assertEquals("aaa\nbbb", service.prepareValue("aaa\nbbb", stringMetadata));

        // in case of rich text some characters must be converted to their HTML equivalents
        FieldMetadata richMetadata = FieldMetadata.builder().id("fieldId").type(FieldType.RICH.getType()).build();
        assertEquals("aaa<br/>bbb", service.prepareValue("aaa\nbbb", richMetadata));
        assertEquals("aaa&nbsp;&nbsp;&nbsp;&nbsp;bbb", service.prepareValue("aaa\tbbb", richMetadata));
        assertEquals("&lt;tag&gt;&amp;", service.prepareValue("<tag>&", richMetadata));
    }

    @Test
    void testEnsureValidValue() {
        ImportService service = new ImportService(mock(PolarionServiceExt.class));

        FieldMetadata metadata = mock(FieldMetadata.class);
        when(metadata.getType()).thenReturn(FieldType.BOOLEAN.getType());

        assertTrue(service.ensureValidValue("true", metadata));
        assertTrue(service.ensureValidValue("True", metadata));
        assertTrue(service.ensureValidValue("FALSE", metadata));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.ensureValidValue("FALSE ", metadata));
        assertEquals("'FALSE ' isn't a valid boolean value", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> service.ensureValidValue(42, metadata));
        assertEquals("'42' isn't a valid boolean value", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class, () -> service.ensureValidValue(null, metadata));
        assertEquals("'' isn't a valid boolean value", exception.getMessage());
    }

    @Test
    void testGetFieldMetadataForField() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ImportService service = new ImportService(polarionService);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getFieldMetadataForField(Set.of(), "unknownFieldId"));
        assertEquals("Cannot find field metadata for ID 'unknownFieldId'", exception.getMessage());
    }

    @Test
    void testFillWorkItemFields() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ImportService service = new ImportService(polarionService);

        FieldMetadata titleField = FieldMetadata.builder().id("title").type(FieldType.STRING.getType()).build();
        FieldMetadata priorityField = FieldMetadata.builder().id("priority").type(FieldType.STRING.getType()).build();
        when(polarionService.getWorkItemsFields("projectId", "requirement")).thenReturn(Set.of(titleField, priorityField));

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");
        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("requirement");
        when(workItem.getType()).thenReturn(typeOpt);

        ExcelSheetMappingSettingsModel model = ExcelSheetMappingSettingsModel.builder()
                .columnsMapping(Map.of("A", "title", "B", "priority"))
                .stepsMapping(Map.of())
                .enumsMapping(Map.of("priority", Map.of("high", "High")))
                .overwriteWithEmpty(true)
                .build();

        Map<String, Object> mappingRecord = new HashMap<>();
        mappingRecord.put("A", "Test Title");
        mappingRecord.put("B", "High");
        mappingRecord.put("C", "unmapped");

        service.fillWorkItemFields(workItem, mappingRecord, model, "linkField");

        // title: normal mapping, no option mapping → value unchanged
        verify(polarionService).setFieldValue(eq(workItem), eq("title"), eq("Test Title"), any());
        // priority: option mapped from "High" to "high"
        verify(polarionService).setFieldValue(eq(workItem), eq("priority"), eq("high"), any());
        // column C has no mapping and no prefix → skipped, so exactly 2 calls total
        verify(polarionService, times(2)).setFieldValue(any(IWorkItem.class), anyString(), any(), any());
    }

    @Test
    void testFillWorkItemFieldsNullTypeAndTestStepsPrefix() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ImportService service = new ImportService(polarionService);

        FieldMetadata stepsField = FieldMetadata.builder().id("mySteps").type(FieldType.STRING.getType()).build();
        when(polarionService.getWorkItemsFields("projectId", "")).thenReturn(Set.of(stepsField));

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");
        // workItem.getType() returns null by default → getWorkItemsFields called with ""

        ExcelSheetMappingSettingsModel model = ExcelSheetMappingSettingsModel.builder()
                .columnsMapping(Map.of())
                .stepsMapping(Map.of())
                .overwriteWithEmpty(true)
                .build();

        // Key with testSteps| prefix, not present in columnsMapping → fieldId extracted from prefix
        Map<String, Object> mappingRecord = new HashMap<>();
        mappingRecord.put("testSteps|mySteps", "stepsData");

        service.fillWorkItemFields(workItem, mappingRecord, model, "linkField");

        verify(polarionService).getWorkItemsFields("projectId", "");
        verify(polarionService).setFieldValue(eq(workItem), eq("mySteps"), eq("stepsData"), any());
    }

    @Test
    void testFillWorkItemFieldsWithConstructTestSteps() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ITrackerService trackerService = mock(ITrackerService.class, RETURNS_DEEP_STUBS);
        when(polarionService.getTrackerService()).thenReturn(trackerService);
        ImportService service = new ImportService(polarionService);

        FieldMetadata titleField = FieldMetadata.builder().id("title").type(FieldType.STRING.getType()).build();
        // stepsField must have options matching the mapping keys so validation passes
        FieldMetadata stepsField = FieldMetadata.builder().id("testSteps").type(FieldType.STRING.getType())
                .options(Set.of(new Option("step", "Step", null), new Option("expectedResult", "Expected Result", null)))
                .build();
        when(polarionService.getWorkItemsFields("projectId", "requirement")).thenReturn(Set.of(titleField, stepsField));

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");
        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("requirement");
        when(workItem.getType()).thenReturn(typeOpt);

        IStructure mockStructure = mock(IStructure.class);
        when(trackerService.getDataService().createStructureForTypeId(any(), eq(TestSteps.STRUCTURE_ID), any())).thenReturn(mockStructure);

        Map<String, Map<String, String>> stepsMapping = new HashMap<>();
        Map<String, String> stepFields = new HashMap<>();
        stepFields.put("step", "B");
        stepFields.put("expectedResult", "C");
        stepsMapping.put("testSteps", stepFields);

        ExcelSheetMappingSettingsModel model = ExcelSheetMappingSettingsModel.builder()
                .columnsMapping(Map.of("A", "title"))
                .stepsMapping(stepsMapping)
                .overwriteWithEmpty(true)
                .build();

        // dataMap: columns B and C contain lists of step data
        Map<String, Object> mappingRecord = new HashMap<>();
        mappingRecord.put("A", "Test Title");
        mappingRecord.put("B", List.of("step1", "step2"));
        mappingRecord.put("C", List.of("result1", "result2"));

        service.fillWorkItemFields(workItem, mappingRecord, model, "linkField");

        // constructTestStepsFields should have called createStructureForTypeId
        verify(trackerService.getDataService()).createStructureForTypeId(eq(workItem), eq(TestSteps.STRUCTURE_ID), any());
        // title field should still be set via normal mapping
        verify(polarionService).setFieldValue(eq(workItem), eq("title"), eq("Test Title"), any());
        // The constructed structure is placed with key "testSteps|testSteps" which gets resolved to fieldId "testSteps"
        verify(polarionService).setFieldValue(eq(workItem), eq("testSteps"), eq(mockStructure), any());
    }

    @Test
    void testFillWorkItemFieldsWithConstructTestStepsSingleRow() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ITrackerService trackerService = mock(ITrackerService.class, RETURNS_DEEP_STUBS);
        when(polarionService.getTrackerService()).thenReturn(trackerService);
        ImportService service = new ImportService(polarionService);

        FieldMetadata titleField = FieldMetadata.builder().id("title").type(FieldType.STRING.getType()).build();
        FieldMetadata stepsField = FieldMetadata.builder().id("testSteps").type(FieldType.STRING.getType())
                .options(Set.of(new Option("step", "Step", null), new Option("expectedResult", "Expected Result", null)))
                .build();
        when(polarionService.getWorkItemsFields("projectId", "requirement")).thenReturn(Set.of(titleField, stepsField));

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");
        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("requirement");
        when(workItem.getType()).thenReturn(typeOpt);

        IStructure mockStructure = mock(IStructure.class);
        when(trackerService.getDataService().createStructureForTypeId(any(), eq(TestSteps.STRUCTURE_ID), any())).thenReturn(mockStructure);

        Map<String, Map<String, String>> stepsMapping = new HashMap<>();
        Map<String, String> stepFields = new HashMap<>();
        stepFields.put("step", "B");
        stepFields.put("expectedResult", "C");
        stepsMapping.put("testSteps", stepFields);

        ExcelSheetMappingSettingsModel model = ExcelSheetMappingSettingsModel.builder()
                .columnsMapping(Map.of("A", "title"))
                .stepsMapping(stepsMapping)
                .overwriteWithEmpty(true)
                .build();

        // Single-row table: values are plain strings, not lists
        Map<String, Object> mappingRecord = new HashMap<>();
        mappingRecord.put("A", "Test Title");
        mappingRecord.put("B", "singleStep");
        mappingRecord.put("C", "singleResult");

        service.fillWorkItemFields(workItem, mappingRecord, model, "linkField");

        verify(trackerService.getDataService()).createStructureForTypeId(eq(workItem), eq(TestSteps.STRUCTURE_ID), any());
        verify(polarionService).setFieldValue(eq(workItem), eq("testSteps"), eq(mockStructure), any());
    }

    @Test
    void testConstructTestStepsFieldsThrowsOnUnknownField() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ImportService service = new ImportService(polarionService);

        FieldMetadata titleField = FieldMetadata.builder().id("title").type(FieldType.STRING.getType()).build();
        // No "testSteps" field in metadata → constructTestStepsFields should throw
        when(polarionService.getWorkItemsFields("projectId", "req")).thenReturn(Set.of(titleField));

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");
        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("req");
        when(workItem.getType()).thenReturn(typeOpt);

        Map<String, Map<String, String>> stepsMapping = new HashMap<>();
        stepsMapping.put("testSteps", Map.of("step", "B"));

        ExcelSheetMappingSettingsModel model = ExcelSheetMappingSettingsModel.builder()
                .columnsMapping(Map.of("A", "title"))
                .stepsMapping(stepsMapping)
                .overwriteWithEmpty(true)
                .build();

        Map<String, Object> mappingRecord = new HashMap<>();
        mappingRecord.put("A", "Test Title");
        mappingRecord.put("B", List.of("step1"));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.fillWorkItemFields(workItem, mappingRecord, model, "linkField"));
        assertTrue(exception.getMessage().contains("Test steps keys mismatch"));
    }

    @Test
    void testConstructTestStepsFieldsKeysMismatchFewerKeys() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ImportService service = new ImportService(polarionService);

        // Field options require "step" and "expectedResult", but mapping only provides "step"
        FieldMetadata stepsField = FieldMetadata.builder().id("testSteps").type(FieldType.STRING.getType())
                .options(Set.of(new Option("step", "Step", null), new Option("expectedResult", "Expected Result", null)))
                .build();
        when(polarionService.getWorkItemsFields("projectId", "req")).thenReturn(Set.of(stepsField));

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");
        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("req");
        when(workItem.getType()).thenReturn(typeOpt);

        Map<String, Map<String, String>> stepsMapping = new HashMap<>();
        stepsMapping.put("testSteps", Map.of("step", "B"));

        ExcelSheetMappingSettingsModel model = ExcelSheetMappingSettingsModel.builder()
                .columnsMapping(Map.of())
                .stepsMapping(stepsMapping)
                .overwriteWithEmpty(true)
                .build();

        Map<String, Object> mappingRecord = new HashMap<>();
        mappingRecord.put("B", List.of("step1"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.fillWorkItemFields(workItem, mappingRecord, model, "linkField"));
        assertTrue(ex.getMessage().contains("testSteps"));
    }

    @Test
    void testConstructTestStepsFieldsKeysMismatchExtraKeys() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ImportService service = new ImportService(polarionService);

        // Field options only have "step", but mapping provides "step" and "extra"
        FieldMetadata stepsField = FieldMetadata.builder().id("testSteps").type(FieldType.STRING.getType())
                .options(Set.of(new Option("step", "Step", null)))
                .build();
        when(polarionService.getWorkItemsFields("projectId", "req")).thenReturn(Set.of(stepsField));

        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getProjectId()).thenReturn("projectId");
        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("req");
        when(workItem.getType()).thenReturn(typeOpt);

        Map<String, Map<String, String>> stepsMapping = new HashMap<>();
        Map<String, String> stepFields = new HashMap<>();
        stepFields.put("step", "B");
        stepFields.put("extra", "C");
        stepsMapping.put("testSteps", stepFields);

        ExcelSheetMappingSettingsModel model = ExcelSheetMappingSettingsModel.builder()
                .columnsMapping(Map.of())
                .stepsMapping(stepsMapping)
                .overwriteWithEmpty(true)
                .build();

        Map<String, Object> mappingRecord = new HashMap<>();
        mappingRecord.put("B", List.of("step1"));
        mappingRecord.put("C", List.of("extra1"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.fillWorkItemFields(workItem, mappingRecord, model, "linkField"));
        assertTrue(ex.getMessage().contains("testSteps"));
    }

    @Test
    void testSetLinkedWorkItems() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ImportService service = new ImportService(polarionService);

        LinkInfo regularLink = new LinkInfo("relates_to", "elibrary", "EL-123", false);
        LinkInfo externalLink = new LinkInfo("relates_to", null, "someExternalUrl", true);

        try (MockedStatic<LinkInfo> linkInfoMockedStatic = mockStatic(LinkInfo.class)) {
            linkInfoMockedStatic.when(() -> LinkInfo.fromString(eq("expectedItems"), any(IWorkItem.class))).thenReturn(List.of(regularLink, externalLink));

            IWorkItem workItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);
            service.setLinkedWorkItems(workItem, "expectedItems");

            verify(workItem, times(1)).addExternallyLinkedItem(any(), any());
            verify(workItem, times(1)).addLinkedItem(any(), any(), isNull(), anyBoolean());
        }
    }

    @Test
    void testExistingValueDiffers() {
        PolarionServiceExt polarionService = mock(PolarionServiceExt.class);
        ImportService service = new ImportService(polarionService);
        IWorkItem workItem = mock(IWorkItem.class);

        FieldMetadata stringMetadata = FieldMetadata.builder().id("fieldId").type(FieldType.STRING.getType()).build();

        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn(null);
        assertFalse(service.existingValueDiffers(workItem, "fieldId", null, stringMetadata));
        assertTrue(service.existingValueDiffers(workItem, "fieldId", "someValue", stringMetadata));
        assertTrue(service.existingValueDiffers(workItem, "fieldId", "", stringMetadata));

        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn("");
        assertTrue(service.existingValueDiffers(workItem, "fieldId", null, stringMetadata));

        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn("someValue");
        assertFalse(service.existingValueDiffers(workItem, "fieldId", "someValue", stringMetadata));

        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn("someValue");
        assertTrue(service.existingValueDiffers(workItem, "fieldId", "someValue ", stringMetadata));

        FieldMetadata booleanMetadata = FieldMetadata.builder().id("fieldId").type(FieldType.BOOLEAN.getType()).build();
        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn(null);
        assertFalse(service.existingValueDiffers(workItem, "fieldId", null, booleanMetadata));
        assertFalse(service.existingValueDiffers(workItem, "fieldId", "someValue", booleanMetadata)); // unrealistic scenario
        assertFalse(service.existingValueDiffers(workItem, "fieldId", "", booleanMetadata)); // unrealistic scenario

        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn(false);
        assertTrue(service.existingValueDiffers(workItem, "fieldId", "true", booleanMetadata));
        assertFalse(service.existingValueDiffers(workItem, "fieldId", null, booleanMetadata));
        assertFalse(service.existingValueDiffers(workItem, "fieldId", "FALSE", booleanMetadata));

        // boolean fields can hold null values, they are treated as having 'false' value
        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn(null);
        assertTrue(service.existingValueDiffers(workItem, "fieldId", "true", booleanMetadata));
        assertFalse(service.existingValueDiffers(workItem, "fieldId", "false", booleanMetadata));

        FieldMetadata floatMetadata = FieldMetadata.builder().id("fieldId").type(FieldType.FLOAT.getType()).build();
        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn(null);
        assertFalse(service.existingValueDiffers(workItem, "fieldId", null, floatMetadata));
        assertTrue(service.existingValueDiffers(workItem, "fieldId", 0f, floatMetadata));

        when(polarionService.getFieldValue(workItem, "fieldId")).thenReturn(42f);
        assertFalse(service.existingValueDiffers(workItem, "fieldId", "42.0", floatMetadata));
        assertTrue(service.existingValueDiffers(workItem, "fieldId", "42", floatMetadata));
        assertTrue(service.existingValueDiffers(workItem, "fieldId", null, floatMetadata));

        FieldMetadata linkedMetadata = FieldMetadata.builder().id("linkedWorkItems").type(FieldType.LIST.getType()).build();
        LinkInfo link1 = mock(LinkInfo.class);
        when(link1.containedIn(workItem)).thenReturn(true);
        LinkInfo link2 = mock(LinkInfo.class);
        when(link2.containedIn(workItem)).thenReturn(false);
        try (MockedStatic<LinkInfo> linkInfoMockedStatic = mockStatic(LinkInfo.class)) {
            linkInfoMockedStatic.when(() -> LinkInfo.fromString(eq("EL-1"), any(IWorkItem.class))).thenReturn(List.of(link1));
            linkInfoMockedStatic.when(() -> LinkInfo.fromString(eq("EL-2"), any(IWorkItem.class))).thenReturn(List.of(link2));
            linkInfoMockedStatic.when(() -> LinkInfo.fromString(eq("EL-1,EL-2"), any(IWorkItem.class))).thenReturn(List.of(link1, link2));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> service.existingValueDiffers(workItem, "linkedWorkItems", 42, linkedMetadata));
            assertEquals("linkedWorkItems can be set using string value only", exception.getMessage());

            assertFalse(service.existingValueDiffers(workItem, "linkedWorkItems", "EL-1", linkedMetadata));
            assertTrue(service.existingValueDiffers(workItem, "linkedWorkItems", "EL-2", linkedMetadata));
            assertTrue(service.existingValueDiffers(workItem, "linkedWorkItems", "EL-1,EL-2", linkedMetadata));
        }
    }

    @Test
    void testFindWorkItemByFieldValue() {
        Predicate<IWorkItem> predicate = ImportService.findWorkItemByFieldValue("someFieldId", "someValue");
        IWorkItem workItem = mock(IWorkItem.class);
        when(workItem.getValue("someFieldId")).thenReturn("someValue");
        assertTrue(predicate.test(workItem));
        when(workItem.getValue("someFieldId")).thenReturn("");
        assertFalse(predicate.test(workItem));
        when(workItem.getValue("someFieldId")).thenReturn(null);
        assertFalse(predicate.test(workItem));

        predicate = ImportService.findWorkItemByFieldValue("someFieldId", null);
        when(workItem.getValue("someFieldId")).thenReturn("someValue");
        assertFalse(predicate.test(workItem));
        when(workItem.getValue("someFieldId")).thenReturn("");
        assertFalse(predicate.test(workItem));
        when(workItem.getValue("someFieldId")).thenReturn(null);
        assertTrue(predicate.test(workItem));
    }

    private void mockSettings(boolean overwriteWithEmpty) {
        when(mockedSettings.getLinkColumn()).thenReturn("B");
        when(mockedSettings.getDefaultWorkItemType()).thenReturn("requirement");
        when(mockedSettings.getColumnsMapping()).thenReturn(Map.of("B", "excelId", "C", "excelData", "D", "nullPossible"));
        when(mockedSettings.isOverwriteWithEmpty()).thenReturn(overwriteWithEmpty);
    }

    private void mockSettingsForIdImport(boolean useIdAsLinkColumn) {
        when(mockedSettings.getLinkColumn()).thenReturn((useIdAsLinkColumn) ? "A" : "B");
        when(mockedSettings.getDefaultWorkItemType()).thenReturn("requirement");
        when(mockedSettings.getColumnsMapping()).thenReturn(Map.of("A", "id", "B", "title"));
    }
}
