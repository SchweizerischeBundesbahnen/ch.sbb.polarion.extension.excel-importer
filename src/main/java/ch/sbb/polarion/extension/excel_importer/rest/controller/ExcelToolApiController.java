package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.rest.model.AttachTableParams;
import ch.sbb.polarion.extension.excel_importer.service.ExportHtmlTableResult;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;

@Secured
@Path("/api/excel-tool")
public class ExcelToolApiController extends ExcelToolInternalController {

    @Override
    public boolean attachTable(String projectId, AttachTableParams params) {
        return polarionService.callPrivileged(() -> super.attachTable(projectId, params));
    }

    @Override
    public ExportHtmlTableResult exportHtmlTable(String sheetName, String tableHtml) {
        return polarionService.callPrivileged(() -> super.exportHtmlTable(sheetName, tableHtml));
    }

    @Override
    public boolean waitForExport(String exportId, int timeoutInSeconds) {
        return polarionService.callPrivileged(() -> super.waitForExport(exportId, timeoutInSeconds));
    }
}
