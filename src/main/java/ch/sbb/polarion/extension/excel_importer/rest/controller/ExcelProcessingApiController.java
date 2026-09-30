package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.service.ImportResult;
import ch.sbb.polarion.extension.generic.rest.filter.Secured;
import ch.sbb.polarion.extension.generic.util.RequestContextUtil;

import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;

@Singleton
@Secured
@Path("/api")
public class ExcelProcessingApiController extends ExcelProcessingInternalController {

    @Override
    public ImportResult importExcelSheet(String projectId, String mappingName, InputStream inputStream) {
        return polarionServiceExt.callPrivileged(() -> super.importExcelSheet(projectId, mappingName, inputStream));
    }

    @Override
    public Response startImportJob(String projectId, String mappingName, InputStream inputStream) {
        // Async case: the background job uses the current subject after this request returns, so the
        // response-filter logout must be deactivated here, or the token principal is destroyed before the
        // job runs (DestroyedPrincipalException). The job itself logs out once it finishes; a start which
        // fails gives the session back.
        RequestContextUtil.keepSessionAlive();
        try {
            return polarionServiceExt.callPrivileged(() -> super.startImportJob(projectId, mappingName, inputStream));
        } catch (RuntimeException e) {
            RequestContextUtil.releaseSession();
            throw e;
        }
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

}
