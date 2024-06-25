package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.service.ImportResult;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;

import javax.ws.rs.Path;
import java.io.InputStream;

@Secured
@Path("/api")
public class ExcelImportApiController extends ExcelImportInternalController {

    @Override
    public ImportResult importExcelSheet(String projectId, String mappingName, InputStream inputStream) {
        return polarionServiceExt.callPrivileged(() -> super.importExcelSheet(projectId, mappingName, inputStream));
    }
}
