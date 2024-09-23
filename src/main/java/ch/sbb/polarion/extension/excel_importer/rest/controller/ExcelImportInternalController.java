package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.service.ImportResult;
import ch.sbb.polarion.extension.excel_importer.service.ImportService;
import ch.sbb.polarion.extension.excel_importer.service.PolarionServiceExt;
import ch.sbb.polarion.extension.excel_importer.service.parser.impl.XlsxParser;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettings;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettingsModel;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import com.polarion.alm.tracker.model.ITrackerProject;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Tag(name = "Excel Import")
@Hidden
@Path("/internal")
public class ExcelImportInternalController {

    protected final PolarionServiceExt polarionServiceExt = new PolarionServiceExt();

    @POST
    @Path("/projects/{projectId}/import")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Imports Excel sheet",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successful import",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ImportResult.class)
                            )
                    )
            }
    )
    public ImportResult importExcelSheet(@PathParam("projectId") String projectId,
                                         @DefaultValue(NamedSettings.DEFAULT_NAME) @FormDataParam("mappingName") String mappingName,
                                         @Parameter(schema = @Schema(type = "string", format = "binary")) @FormDataParam("file") InputStream inputStream) {
        final ITrackerProject trackerProject = polarionServiceExt.findProject(projectId);

        ExcelSheetMappingSettingsModel settings = new ExcelSheetMappingSettings().load(projectId, SettingId.fromName(mappingName));
        List<Map<String, Object>> xlsxData = new XlsxParser().parseFileStream(inputStream, settings);
        return new ImportService().processFile(trackerProject, xlsxData, settings);
    }

}
