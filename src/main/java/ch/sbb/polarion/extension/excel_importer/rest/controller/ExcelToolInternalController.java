package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.ExcelTool;
import ch.sbb.polarion.extension.excel_importer.rest.model.AttachTableParams;
import ch.sbb.polarion.extension.excel_importer.service.ExportHtmlTableResult;
import ch.sbb.polarion.extension.excel_importer.service.PolarionServiceExt;
import com.polarion.alm.tracker.exporter.IExport;
import com.polarion.alm.tracker.exporter.IExportManager;
import com.polarion.alm.tracker.model.IAttachmentBase;
import com.polarion.alm.tracker.model.IWithAttachments;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.Base64;
import java.util.List;

@Singleton
@Hidden
@Path("/internal/excel-tool")
@Tag(name = "Excel Tool")
public class ExcelToolInternalController {

    protected final PolarionServiceExt polarionService = new PolarionServiceExt();

    @POST
    @Path("projects/{projectId}/attach-table")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Attach table to project")
    @SuppressWarnings("unchecked")
    public boolean attachTable(
            @Parameter(description = "Project ID", required = true) @PathParam("projectId") String projectId,
            @Parameter(description = "Parameters to attach table", required = true) AttachTableParams params) {
        IWithAttachments<? extends IAttachmentBase> objectAttachments = polarionService.getObjectAttachments(projectId, params.getObjectType(), params.getObjectId());
        return ExcelTool.attachTable(params.getHtmlTable(), objectAttachments, params.getFileName(), params.getFileTitle());
    }

    @POST
    @Path("/export-html-table")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Export html table as excel sheet",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful export",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ExportHtmlTableResult.class)
                            )
                    )
            }
    )
    public ExportHtmlTableResult exportHtmlTable(
            @Parameter(schema = @Schema(type = "string")) @FormDataParam("sheetName") String sheetName,
            @Parameter(schema = @Schema(type = "string", format = "byte")) @FormDataParam("tableHtml") String tableHtmlBase64Encoded) {
        return new ExportHtmlTableResult(Base64.getEncoder().encodeToString(ExcelTool.convertHtmlTableToXlsx(tableHtmlBase64Encoded, sheetName)));
    }

    @POST
    @Path("html-table-from-list")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_HTML)
    @Operation(summary = "Generate HTML table", responses = @ApiResponse(responseCode = "200", description = "HTML content"))
    public String getHTMLTable(@Parameter(description = "List of data to be converted into HTML table", required = true) List<List<String>> dataList) {
        return ExcelTool.getHTMLTable(dataList);
    }

    @GET
    @Path("/exports/{exportId}/wait")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Wait for export completion")
    public boolean waitForExport(@Parameter(description = "ID of the export", required = true) @PathParam("exportId") String exportId,
                                 @Parameter(description = "Timeout in seconds", required = true) @QueryParam("timeout") int timeoutInSeconds) {
        IExportManager exportManager = polarionService.getTrackerService().getExportManager();
        IExport export = exportManager.getExport(exportId);
        return ExcelTool.waitForExport(export, timeoutInSeconds);
    }

}
