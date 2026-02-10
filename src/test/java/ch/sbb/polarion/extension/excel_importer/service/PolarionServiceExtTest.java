package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.ITestStepKeyOpt;
import com.polarion.alm.tracker.model.ITestSteps;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWithAttachments;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import com.polarion.subterra.base.data.identification.IContextId;
import com.polarion.subterra.base.data.model.IStructType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class PolarionServiceExtTest {

    @CustomExtensionMock
    @SuppressWarnings("unused")
    private ITrackerService trackerService;

    @CustomExtensionMock
    @SuppressWarnings("unused")
    private MockedStatic<PlatformContext> platformContextMockedStatic;

    @Test
    void testGetWorkItemsFields() throws Exception {
        PolarionServiceExt service = spy(new PolarionServiceExt());

        // Extract the ITestManagementService mock injected by PlatformContextMockExtension
        Field tmField = PolarionServiceExt.class.getDeclaredField("testManagementService");
        tmField.setAccessible(true);
        ITestManagementService testManagementService = (ITestManagementService) tmField.get(service);

        ITrackerProject project = mock(ITrackerProject.class);
        IContextId contextId = mock(IContextId.class);
        when(project.getContextId()).thenReturn(contextId);

        ITypeOpt typeOpt = mock(ITypeOpt.class);
        when(typeOpt.getId()).thenReturn("testType");

        doReturn(project).when(service).findProject("testProject");
        doReturn(typeOpt).when(service).findWorkItemTypeInProject(project, "testType");

        // String field - should have no options after processing
        FieldMetadata stringField = FieldMetadata.builder().id("title").type(FieldType.STRING.getType()).build();

        // Boolean field - should get boolean option mappings
        FieldMetadata booleanField = FieldMetadata.builder().id("isActive").type(FieldType.BOOLEAN.getType()).build();

        // Test steps struct field - should get test step key options
        IStructType testStepsStructType = mock(IStructType.class);
        when(testStepsStructType.getStructTypeId()).thenReturn(ITestSteps.STRUCTURE_ID);
        FieldMetadata testStepsField = FieldMetadata.builder().id("testSteps").type(testStepsStructType).build();

        // Non-test-steps struct field - should NOT get options
        IStructType otherStructType = mock(IStructType.class);
        when(otherStructType.getStructTypeId()).thenReturn("someOtherStructure");
        FieldMetadata otherStructField = FieldMetadata.builder().id("otherStruct").type(otherStructType).build();

        doReturn(new TreeSet<>(Set.of(stringField, testStepsField))).when(service).getGeneralFields(eq(IWorkItem.PROTO), any());
        doReturn(new TreeSet<>(Set.of(otherStructField))).when(service).getCustomFields(eq(IWorkItem.PROTO), any(), isNull());
        doReturn(new TreeSet<>(Set.of(booleanField))).when(service).getCustomFields(eq(IWorkItem.PROTO), any(), eq("testType"));

        // Setup test step keys returned by ITestManagementService
        ITestStepKeyOpt key1 = mock(ITestStepKeyOpt.class);
        when(key1.getId()).thenReturn("step");
        when(key1.getName()).thenReturn("Step");
        ITestStepKeyOpt key2 = mock(ITestStepKeyOpt.class);
        when(key2.getId()).thenReturn("expectedResult");
        when(key2.getName()).thenReturn("Expected Result");
        when(testManagementService.getTestStepsKeys("testProject", "testType")).thenReturn(List.of(key1, key2));

        Set<FieldMetadata> result = service.getWorkItemsFields("testProject", "testType");

        assertEquals(4, result.size());

        // Test steps field should have options from test step keys
        FieldMetadata resultTestSteps = result.stream().filter(f -> "testSteps".equals(f.getId())).findFirst().orElseThrow();
        assertNotNull(resultTestSteps.getOptions());
        assertEquals(2, resultTestSteps.getOptions().size());
        assertTrue(resultTestSteps.getOptions().stream().anyMatch(o -> "step".equals(o.getKey()) && "Step".equals(o.getName())));
        assertTrue(resultTestSteps.getOptions().stream().anyMatch(o -> "expectedResult".equals(o.getKey()) && "Expected Result".equals(o.getName())));

        // Boolean field should have True/False options
        FieldMetadata resultBoolean = result.stream().filter(f -> "isActive".equals(f.getId())).findFirst().orElseThrow();
        assertNotNull(resultBoolean.getOptions());
        assertEquals(2, resultBoolean.getOptions().size());

        // String field should have no options
        FieldMetadata resultString = result.stream().filter(f -> "title".equals(f.getId())).findFirst().orElseThrow();
        assertNull(resultString.getOptions());

        // Non-test-steps struct field should have no options
        FieldMetadata resultOtherStruct = result.stream().filter(f -> "otherStruct".equals(f.getId())).findFirst().orElseThrow();
        assertNull(resultOtherStruct.getOptions());
    }

    @Test
    void testFindWorkItemsById() {
        IWorkItem testWorkItem = mock(IWorkItem.class);
        when(trackerService.queryWorkItems(eq("project.id:testProjectId AND (fieldId:T-123 OR fieldId:T-456)"), anyString())).thenReturn(new PObjectListStub<>(List.of(testWorkItem)));
        List<IWorkItem> workItems = new PolarionServiceExt().findWorkItemsById("testProjectId", "fieldId", List.of("T-123", "T-456"));
        assertEquals(1, workItems.size());
        assertSame(testWorkItem, workItems.get(0));
    }

    @Test
    @SuppressWarnings("rawtypes")
    void testGetObjectAttachments() throws Exception {
        PolarionServiceExt service = spy(new PolarionServiceExt());

        IModule module = mock(IModule.class);
        doReturn(module).when(service).getModule("testProjectId", "testSpace", "testObjectId");
        IWithAttachments result = service.getObjectAttachments("testProjectId", "MODULE", "testSpace/testObjectId");
        assertEquals(module, result);

        assertThrows(IllegalArgumentException.class, () -> service.getObjectAttachments("testProjectId", "MODULE", "invalidObjectId"));

        IRichPage richPage = mock(IRichPage.class);
        when(trackerService.getRichPageManager().getRichPage().path("testSpace/testObjectId")).thenReturn(richPage);
        result = service.getObjectAttachments("testProjectId", "RICHPAGE", "testSpace/testObjectId");
        assertEquals(richPage, result);

        ITestRun testRun = mock(ITestRun.class);
        Field tmField = PolarionServiceExt.class.getDeclaredField("testManagementService");
        tmField.setAccessible(true);
        ITestManagementService testManagementService = (ITestManagementService) tmField.get(service);
        when(testManagementService.getTestRun("testProjectId", "testObjectId")).thenReturn(testRun);
        result = service.getObjectAttachments("testProjectId", "TESTRUN", "testObjectId");
        assertEquals(testRun, result);

        IWorkItem workItem = mock(IWorkItem.class);
        doReturn(workItem).when(service).getWorkItem("testProjectId", "testObjectId");
        result = service.getObjectAttachments("testProjectId", "WORKITEM", "testObjectId");
        assertEquals(workItem, result);

        assertThrows(IllegalArgumentException.class, () -> service.getObjectAttachments("testProjectId", "UNKNOWN_TYPE", "testObjectId"));
    }
}
