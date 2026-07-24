package ch.sbb.polarion.extension.excel_importer;

import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExcelImporterNavigationExtenderTest {

    @Test
    void testExcelImporterNavigationExtender() {
        IContextId contextId = mock(IContextId.class);

        ExcelImporterNavigationExtender navigationExtender = new ExcelImporterNavigationExtender();
        assertEquals(ExcelImporterNavigationExtender.EXCEL_IMPORTER, navigationExtender.getId());
        assertEquals("Excel Importer", navigationExtender.getLabel());
        assertEquals("/polarion/excel-importer-admin/ui/images/menu/30x30/_parent.svg", navigationExtender.getIconUrl());
        assertFalse(navigationExtender.requiresToken());

        List<NavigationExtenderNode> rootNodes = navigationExtender.getRootNodes(contextId);
        assertTrue(rootNodes.isEmpty());
    }

    @Test
    void testGetPageUrlWithoutContextName() {
        IContextId contextId = mock(IContextId.class);
        when(contextId.getContextName()).thenReturn(null);

        String pageUrl = new ExcelImporterNavigationExtender().getPageUrl(contextId);
        assertEquals("/polarion/excel-importer-app/ui/app/index.html?feature=import-file&embedded=true&scope=", pageUrl);
    }

    @Test
    void testGetPageUrlWithContextName() {
        IContextId contextId = mock(IContextId.class);
        when(contextId.getContextName()).thenReturn("myProject");

        String pageUrl = new ExcelImporterNavigationExtender().getPageUrl(contextId);
        assertEquals("/polarion/excel-importer-app/ui/app/index.html?feature=import-file&embedded=true&scope=project/myProject/", pageUrl);
    }

}
