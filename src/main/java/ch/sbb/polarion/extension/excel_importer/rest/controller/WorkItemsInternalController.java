package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.service.PolarionServiceExt;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.alm.tracker.model.ITypeOpt;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Set;

@Singleton
@Tag(name = "WorkItems")
@Hidden
@Path("/internal")
public class WorkItemsInternalController {

    protected final PolarionServiceExt polarionServiceExt = new PolarionServiceExt();

    @GET
    @Path("/projects/{projectId}/workitem_types/{workItemType}/fields")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all fields for requested project and workitem type",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved fields",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = FieldMetadata.class)
                            )
                    )
            }
    )
    public Set<FieldMetadata> getWorkItemFields(@PathParam("projectId") String projectId, @PathParam("workItemType") String workItemType) {
        return polarionServiceExt.getWorkItemsFields(projectId, workItemType);
    }

    @GET
    @Path("/projects/{projectId}/workitem_types")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get workitem types for project",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully retrieved work item types",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ITypeOpt.class)
                            )
                    )
            }
    )
    public List<ITypeOpt> getWorkItemTypes(@PathParam("projectId") String projectId) {
        return polarionServiceExt.getWorkItemTypes(projectId);
    }
}
