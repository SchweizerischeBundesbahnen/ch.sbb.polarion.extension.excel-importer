package ch.sbb.polarion.extension.excel_importer.utils;

import com.polarion.alm.tracker.model.IExternallyLinkedWorkItemStruct;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LinkInfoTest {

    @Test
    void testFromString() {
        assertTrue(LinkInfo.fromString(null, mock(IWorkItem.class)).isEmpty());
        assertTrue(LinkInfo.fromString("", mock(IWorkItem.class)).isEmpty());
        assertTrue(LinkInfo.fromString("  ", mock(IWorkItem.class)).isEmpty());
        assertTrue(LinkInfo.fromString(", ", mock(IWorkItem.class)).isEmpty());

        IWorkItem workItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);
        when(workItem.getType()).thenReturn(mock(ITypeOpt.class));
        ILinkRoleOpt roleOpt = mock(ILinkRoleOpt.class);
        when(roleOpt.getId()).thenReturn("defaultRole");
        when(workItem.getProject().getWorkItemLinkRoleEnum().getDefaultOption(any(ITypeOpt.class))).thenReturn(roleOpt);
        when(workItem.getProjectId()).thenReturn("defaultProj");

        List<LinkInfo> result = LinkInfo.fromString("TEST-1,someProj/TEST-2,someRole:TEST-3,,https://some.host,anotherRole:anotherProj/TEST-4, TEST-5, TEST-6", workItem);
        assertEquals(7, result.size());
        assertEquals(new LinkInfo("defaultRole", "defaultProj", "TEST-1", false), result.get(0));
        assertEquals(new LinkInfo("defaultRole", "someProj", "TEST-2", false), result.get(1));
        assertEquals(new LinkInfo("someRole", "defaultProj", "TEST-3", false), result.get(2));
        assertEquals(new LinkInfo("defaultRole", null, "https://some.host", true), result.get(3));
        assertEquals(new LinkInfo("anotherRole", "anotherProj", "TEST-4", false), result.get(4));
        assertEquals(new LinkInfo("defaultRole", "defaultProj", "TEST-5", false), result.get(5));
        assertEquals(new LinkInfo("defaultRole", "defaultProj", "TEST-6", false), result.get(6));
    }

    @Test
    void testContainedIn() {
        IWorkItem workItem = mock(IWorkItem.class, RETURNS_DEEP_STUBS);

        IExternallyLinkedWorkItemStruct externalStruct = mock(IExternallyLinkedWorkItemStruct.class, RETURNS_DEEP_STUBS);
        when(externalStruct.getLinkRole().getId()).thenReturn("usedRole");
        when(externalStruct.getLinkedWorkItemURI().getURI().toString()).thenReturn("usedUri");
        when(workItem.getExternallyLinkedWorkItemsStructs()).thenReturn(List.of(externalStruct));

        ILinkedWorkItemStruct itemStruct = mock(ILinkedWorkItemStruct.class, RETURNS_DEEP_STUBS);
        when(itemStruct.getLinkRole().getId()).thenReturn("usedItemRole");
        when(itemStruct.getLinkedItem().getProjectId()).thenReturn("usedItemProj");
        when(itemStruct.getLinkedItem().getId()).thenReturn("TEST-42");
        when(workItem.getLinkedWorkItemsStructsDirect()).thenReturn(List.of(itemStruct));

        assertTrue(new LinkInfo("usedRole", null, "usedUri", true).containedIn(workItem));
        assertFalse(new LinkInfo("someRole", "someProj", "TEST-1", false).containedIn(workItem));
        assertFalse(new LinkInfo("usedItemRole", "someProj", "TEST-42", false).containedIn(workItem));
        assertFalse(new LinkInfo("usedItemRole", "someProj", "TEST-45", false).containedIn(workItem));
        assertTrue(new LinkInfo("usedItemRole", "usedItemProj", "TEST-42", false).containedIn(workItem));
    }

}
