package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.service.ExportHtmlTableResult;
import ch.sbb.polarion.extension.excel_importer.service.ImportResult;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Secured
@Path("/api")
public class ExcelProcessingApiController extends ExcelProcessingInternalController {

    @Override
    public ImportResult importExcelSheet(String projectId, String mappingName, InputStream inputStream) {
        return polarionServiceExt.callPrivileged(() -> super.importExcelSheet(projectId, mappingName, inputStream));
    }

    @Override
    public Response startImportJob(String projectId, String mappingName, InputStream inputStream) {
        return polarionServiceExt.callPrivileged(() -> super.startImportJob(projectId, mappingName, inputStream));
    }

    @Override
    public Response getImportJobStatus(String jobId) {
        return polarionServiceExt.callPrivileged(() -> super.getImportJobStatus(jobId));
    }

    @Override
    public Response getImportJobResult(String jobId) {
        return polarionServiceExt.callPrivileged(() -> super.getImportJobResult(jobId));
    }

    @Override
    public Response getAllImporterJobs() {
        return polarionServiceExt.callPrivileged(super::getAllImporterJobs);
    }

    @Override
    public ExportHtmlTableResult exportHtmlTable(String sheetName, String tableHtml) {
        return polarionServiceExt.callPrivileged(() -> super.exportHtmlTable(sheetName, tableHtml));
    }
}
