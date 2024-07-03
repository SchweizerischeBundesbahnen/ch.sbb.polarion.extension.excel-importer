package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import com.polarion.alm.tracker.model.ITypeOpt;

import javax.ws.rs.Path;
import java.util.List;
import java.util.Set;

@Secured
@Path("/api")
public class WorkItemsApiController extends WorkItemsInternalController {

    @Override
    public Set<FieldMetadata> getWorkItemFields(String projectId, String workItemType) {
        return polarionServiceExt.callPrivileged(() -> super.getWorkItemFields(projectId, workItemType));
    }

    @Override
    public List<ITypeOpt> getWorkItemTypes(String projectId) {
        return polarionServiceExt.callPrivileged(() -> super.getWorkItemTypes(projectId));
    }
}
