package ch.sbb.polarion.extension.excel_importer;

import com.polarion.alm.ui.server.navigation.NavigationExtender;
import com.polarion.alm.ui.server.navigation.NavigationExtenderNode;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ExcelImporterNavigationExtender extends NavigationExtender {

    public static final String EXCEL_IMPORTER = "excel-importer";

    @NotNull
    @Override
    public String getId() {
        return EXCEL_IMPORTER;
    }

    @NotNull
    @Override
    public String getLabel() {
        return "Excel Importer";
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return "/polarion/icons/default/topicIcons/documentsAndWiki.svg";
    }

    @Nullable
    @Override
    public String getPageUrl(@NotNull IContextId contextId) {
        String contextName = contextId.getContextName();
        String scope = contextName == null ? "" : "project/%s/".formatted(contextName);
        return "/polarion/excel-importer-admin/pages/import_file.jsp?scope=" + scope;
    }

    @Override
    public boolean requiresToken() {
        return false;
    }

    @NotNull
    @Override
    public List<NavigationExtenderNode> getRootNodes(@NotNull IContextId contextId) {
        return new ArrayList<>();
    }
}
