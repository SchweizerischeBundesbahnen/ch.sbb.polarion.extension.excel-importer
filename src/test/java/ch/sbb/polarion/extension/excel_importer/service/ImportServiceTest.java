package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettingsModel;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.shared.api.transaction.RunnableInWriteTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.shared.api.transaction.WriteTransaction;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.IPlatformService;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.persistence.ICustomFieldsService;
import com.polarion.platform.persistence.IDataService;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.platform.persistence.model.IPObjectList;
import com.polarion.platform.persistence.model.IPrototype;
import com.polarion.platform.security.ISecurityService;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.data.model.ICustomField;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings("squid:S5778") //ignore assertThrows single invocation requirement
@ExtendWith(MockitoExtension.class)
class ImportServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MockedStatic<PlatformContext> mockPlatformContext;

    @AfterEach
    void cleanup() {
        mockPlatformContext.close();
    }

    private static final String TEST_PROJECT_ID = "test";

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

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new ImportService(polarionServiceExt).processFile(project, List.of(Map.of("A", "a1", "B", "b1", "C", "c1")), generateSettings(true)),
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
            map.put("C", "c1");
            map.put("D", null);

            //test using disabled 'overwriteWithEmpty'
            assertEquals(new ImportResult(List.of(), List.of("testId"), List.of()),
                    new ImportService(polarionServiceExt).processFile(project, List.of(map), generateSettings(false)));
            verify(workItem, times(0)).setCustomField(eq("nullPossible"), eq(null));

            //'overwriteWithEmpty' enabled but existing value is null therefore is no update expected
            assertEquals(new ImportResult(List.of(), List.of("testId"), List.of()),
                    new ImportService(polarionServiceExt).processFile(project, List.of(map), generateSettings(true)));
            verify(workItem, times(0)).setCustomField(eq("nullPossible"), eq(null));

            //'overwriteWithEmpty' enabled and existing value differs
            lenient().when(workItem.getCustomField(eq("nullPossible"))).thenReturn("someExistingValue");
            assertEquals(new ImportResult(List.of(), List.of("testId"), List.of()),
                    new ImportService(polarionServiceExt).processFile(project, List.of(map), generateSettings(true)));
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

            // test importing wi with id but not using id as link column
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new ImportService(polarionServiceExt).processFile(project, List.of(mapOne), generateSettingsForIdImport(false)),
                    "Expected IllegalArgumentException thrown, but it didn't");
            assertEquals("WorkItem id can only be imported if it is used as Link Column.", exception.getMessage());

            // test importing wi with id as link column but imported wi has id that does not exist
            exception = assertThrows(IllegalArgumentException.class,
                    () -> new ImportService(polarionServiceExt).processFile(project, List.of(mapOne), generateSettingsForIdImport(true)),
                    "Expected IllegalArgumentException thrown, but it didn't");
            assertEquals("If id is used as Link Column, no new Work Items can be created via import.", exception.getMessage());


            Map<String, Object> mapTwo = new HashMap<>(); // imported id cell is empty
            mapTwo.put("A", "");
            mapTwo.put("B", "b1");

            // test importing wi with id as link column but imported wi has empty id field
            exception = assertThrows(IllegalArgumentException.class,
                    () -> new ImportService(polarionServiceExt).processFile(project, List.of(mapTwo), generateSettingsForIdImport(true)),
                    "Expected IllegalArgumentException thrown, but it didn't");
            assertEquals(String.format("Column '%s' contains empty or unsupported non-string type value", "A"), exception.getMessage());


            Map<String, Object> mapThree = new HashMap<>(); // imported id cell has valid value (and title cell has new value)
            mapThree.put("A", "testId");
            mapThree.put("B", "newTitle");

            // test importing wi with id as link column and imported wi has same id as existing wi
            assertEquals(new ImportResult(List.of(), List.of(), List.of("testId")),
                    new ImportService(polarionServiceExt).processFile(project, List.of(mapThree), generateSettingsForIdImport(true)));

        }
    }

    private ExcelSheetMappingSettingsModel generateSettings(boolean overwriteWithEmpty) {
        ExcelSheetMappingSettingsModel model = new ExcelSheetMappingSettingsModel();
        model.setLinkColumn("B");
        model.setDefaultWorkItemType("requirement");
        model.setColumnsMapping(Map.of("B", "excelId", "C", "excelData", "D", "nullPossible"));
        model.setOverwriteWithEmpty(overwriteWithEmpty);
        return model;
    }

    private ExcelSheetMappingSettingsModel generateSettingsForIdImport(boolean useIdAsLinkColumn) {
        ExcelSheetMappingSettingsModel model = new ExcelSheetMappingSettingsModel();
        String linkColumn = (useIdAsLinkColumn) ? "A" : "B";
        model.setLinkColumn(linkColumn);
        model.setDefaultWorkItemType("requirement");
        model.setColumnsMapping(Map.of("A", "id", "B", "title"));
        return model;
    }
}
