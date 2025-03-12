package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.service.ExportHtmlTableResult;
import ch.sbb.polarion.extension.excel_importer.service.ExportService;
import ch.sbb.polarion.extension.excel_importer.service.ImportResult;
import ch.sbb.polarion.extension.excel_importer.service.ImportService;
import ch.sbb.polarion.extension.excel_importer.service.PolarionServiceExt;
import ch.sbb.polarion.extension.excel_importer.service.parser.IParser;
import ch.sbb.polarion.extension.excel_importer.service.parser.impl.XlsxParser;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettings;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettingsModel;
import ch.sbb.polarion.extension.excel_importer.utils.ClassLoaderUtils;
import ch.sbb.polarion.extension.excel_importer.utils.IsolatedClassLoader;
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
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
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

@Tag(name = "Excel Processing")
@Hidden
@Path("/internal")
public class ExcelProcessingInternalController {

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
    @SneakyThrows
    public ImportResult importExcelSheet(@PathParam("projectId") String projectId,
                                         @DefaultValue(NamedSettings.DEFAULT_NAME) @FormDataParam("mappingName") String mappingName,
                                         @Parameter(schema = @Schema(type = "string", format = "binary")) @FormDataParam("file") InputStream inputStream) {
        final ITrackerProject trackerProject = polarionServiceExt.findProject(projectId);

        ExcelSheetMappingSettingsModel settings = new ExcelSheetMappingSettings().load(projectId, SettingId.fromName(mappingName));

        String commonsIoVersion1 = IOUtils.class.getPackage().getImplementationVersion();
        String commonsIoVersion2 = ClassLoaderUtils.runWithClassLoader(() -> IOUtils.class.getPackage().getImplementationVersion());
        String commonsIoVersion3 = ClassLoaderUtils.runWithClassLoader(IsolatedClassLoader.createForCurrentBundle(), () -> IOUtils.class.getPackage().getImplementationVersion());
        IsolatedClassLoader isolatedClassLoader = IsolatedClassLoader.createForCurrentBundle();
        Class<?> ioUtilsClass = isolatedClassLoader.loadClass("org.apache.commons.io.IOUtils");
        String commonsIoVersion4 = ioUtilsClass.getPackage().getImplementationVersion();

        Class<? extends IOUtils> ioUtilsClass2 = ClassLoaderUtils.loadClass(IOUtils.class, isolatedClassLoader);
        String commonsIoVersion5 = ioUtilsClass2.getPackage().getImplementationVersion();

        IParser xlsxParser = ClassLoaderUtils.createInstance(XlsxParser.class, isolatedClassLoader);

//        XlsxParser xlsxParser = new XlsxParser();
        List<Map<String, Object>> xlsxData = xlsxParser.parseFileStream(inputStream, settings);
        return new ImportService().processFile(trackerProject, xlsxData, settings);
    }

    @POST
    @Path("/exportHtmlTable")
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
            @Parameter(schema = @Schema(type = "string", format = "")) @FormDataParam("sheetName") String sheetName,
            @Parameter(schema = @Schema(type = "string", format = "byte")) @FormDataParam("tableHtml") String tableHtml) {
        return new ExportHtmlTableResult(new ExportService().exportHtmlTable(sheetName, tableHtml));
    }

}
