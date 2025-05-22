package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.generic.test_extensions.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.util.PObjectListStub;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class PolarionServiceExtTest {

    @CustomExtensionMock
    @SuppressWarnings("unused")
    private ITrackerService trackerService;

    @Test
    void testFindWorkItemsById() {
        IWorkItem testWorkItem = mock(IWorkItem.class);
        when(trackerService.queryWorkItems(eq("project.id:testProjectId AND (fieldId:T-123 OR fieldId:T-456)"), anyString())).thenReturn(new PObjectListStub<>(List.of(testWorkItem)));
        List<IWorkItem> workItems = new PolarionServiceExt().findWorkItemsById("testProjectId", "fieldId", List.of("T-123", "T-456"));
        assertEquals(1, workItems.size());
        assertSame(testWorkItem, workItems.get(0));
    }

}
