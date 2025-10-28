package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.tracker.ITestManagementService;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IAttachmentBase;
import com.polarion.alm.tracker.model.IModule;
import com.polarion.alm.tracker.model.IRichPage;
import com.polarion.alm.tracker.model.ITestRun;
import com.polarion.alm.tracker.model.IWithAttachments;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.core.PlatformContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    void testFindWorkItemsById() {
        IWorkItem testWorkItem = mock(IWorkItem.class);
        when(trackerService.queryWorkItems(eq("project.id:testProjectId AND (fieldId:T-123 OR fieldId:T-456)"), anyString())).thenReturn(new PObjectListStub<>(List.of(testWorkItem)));
        List<IWorkItem> workItems = new PolarionServiceExt().findWorkItemsById("testProjectId", "fieldId", List.of("T-123", "T-456"));
        assertEquals(1, workItems.size());
        assertSame(testWorkItem, workItems.get(0));
    }

    @Test
    void testGetObjectAttachments() {
        PolarionServiceExt service = spy(new PolarionServiceExt());

        IModule module = mock(IModule.class);
        doReturn(module).when(service).getModule("testProjectId", "testSpace", "testObjectId");
        IWithAttachments<? extends IAttachmentBase> result = service.getObjectAttachments("testProjectId", "MODULE", "testSpace/testObjectId");
        assertEquals(module, result);

        IRichPage richPage = mock(IRichPage.class);
        when(trackerService.getRichPageManager().getRichPage().path("testSpace/testObjectId")).thenReturn(richPage);
        result = service.getObjectAttachments("testProjectId", "RICHPAGE", "testSpace/testObjectId");
        assertEquals(richPage, result);

        ITestRun testRun = mock(ITestRun.class);
        ITestManagementService testManagementService = mock(ITestManagementService.class);
        when(PlatformContext.getPlatform().lookupService(ITestManagementService.class)).thenReturn(testManagementService);
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
