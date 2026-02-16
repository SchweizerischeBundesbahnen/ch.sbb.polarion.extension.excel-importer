package ch.sbb.polarion.extension.excel_importer.rest.controller;

import ch.sbb.polarion.extension.excel_importer.rest.model.jobs.ImportJobDetails;
import ch.sbb.polarion.extension.excel_importer.rest.model.jobs.ImportJobStatus;
import ch.sbb.polarion.extension.excel_importer.service.ImportJobParams;
import ch.sbb.polarion.extension.excel_importer.service.ImportResult;
import ch.sbb.polarion.extension.excel_importer.service.ImportService;
import ch.sbb.polarion.extension.excel_importer.service.ImportJobsService;
import ch.sbb.polarion.extension.excel_importer.service.PolarionServiceExt;
import ch.sbb.polarion.extension.generic.settings.NamedSettings;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.security.ISecurityService;
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
import org.springframework.http.HttpStatus;

import javax.inject.Singleton;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Singleton
@Tag(name = "Excel Processing")
@Hidden
@Path("/internal")
public class ExcelProcessingInternalController {

    protected final PolarionServiceExt polarionServiceExt = new PolarionServiceExt();
    protected final ImportJobsService jobsService;

    @Context
    private UriInfo uriInfo;

    public ExcelProcessingInternalController() {
        ISecurityService securityService = PlatformContext.getPlatform().lookupService(ISecurityService.class);
        this.jobsService = new ImportJobsService(new ImportService(polarionServiceExt), securityService);
    }

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
        return new ImportService().processFile(projectId, mappingName, IOUtils.toByteArray(inputStream));
    }

    @POST
    @Path("import/jobs")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Operation(summary = "Starts asynchronous import job",
            responses = {
                    @ApiResponse(responseCode = "202",
                            description = "Import process is started, job URI is returned in Location header"
                    )
            })
    @SneakyThrows
    public Response startImportJob(@FormDataParam("projectId") String projectId,
                                         @DefaultValue(NamedSettings.DEFAULT_NAME) @FormDataParam("mappingName") String mappingName,
                                         @Parameter(schema = @Schema(type = "string", format = "binary")) @FormDataParam("file") InputStream inputStream) {
        ImportJobParams jobParams = new ImportJobParams(projectId, mappingName, IOUtils.toByteArray(inputStream));
        String jobId = jobsService.startJob(jobParams);

        URI jobUri = UriBuilder.fromUri(uriInfo.getRequestUri().getPath()).path(jobId).build();
        return Response.accepted().location(jobUri).build();
    }

    @GET
    @Path("/import/jobs/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Returns import job status",
            responses = {
                    // OpenAPI response MediaTypes for 303 and 202 response codes are generic to satisfy automatic redirect in SwaggerUI
                    @ApiResponse(responseCode = "303",
                            description = "Import job is finished successfully, Location header contains result URL",
                            content = {@Content(mediaType = "application/*", schema = @Schema(implementation = ImportJobDetails.class))}
                    ),
                    @ApiResponse(responseCode = "202",
                            description = "Import job is still in progress",
                            content = {@Content(mediaType = "application/*", schema = @Schema(implementation = ImportJobDetails.class))}
                    ),
                    @ApiResponse(responseCode = "409",
                            description = "Import job is failed or cancelled"
                    ),
                    @ApiResponse(responseCode = "404",
                            description = "Import job id is unknown"
                    )
            })
    public Response getImportJobStatus(@PathParam("id") String jobId) {
        ImportJobsService.JobState jobState = jobsService.getJobState(jobId);

        ImportJobStatus jobStatus = convertToJobStatus(jobState);
        ImportJobDetails jobDetails = ImportJobDetails.builder()
                .status(jobStatus)
                .errorMessage(jobState.errorMessage()).build();

        Response.ResponseBuilder responseBuilder;
        switch (jobStatus) {
            case IN_PROGRESS -> responseBuilder = Response.accepted();
            case SUCCESSFULLY_FINISHED -> {
                URI jobUri = UriBuilder.fromUri(uriInfo.getRequestUri().getPath()).path("result").build();
                responseBuilder = Response.status(HttpStatus.SEE_OTHER.value()).location(jobUri);
            }
            default -> responseBuilder = Response.status(HttpStatus.CONFLICT.value());
        }
        return responseBuilder.entity(jobDetails).build();
    }

    @GET
    @Path("/import/jobs/{id}/result")
    @Produces("application/json")
    @Operation(summary = "Returns import job result",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Import result is ready",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ImportResult.class)
                            )
                    ),
                    @ApiResponse(responseCode = "204",
                            description = "Import job is still in progress"
                    ),
                    @ApiResponse(responseCode = "409",
                            description = "Import job is failed, cancelled or result is unreachable"
                    ),
                    @ApiResponse(responseCode = "404",
                            description = "Import job id is unknown"
                    )
            })
    public Response getImportJobResult(@PathParam("id") String jobId) {
        Optional<ImportResult> result = jobsService.getJobResult(jobId);
        if (result.isEmpty()) {
            return Response.status(HttpStatus.NO_CONTENT.value()).build();
        }
        return Response.ok(result.get()).build();
    }

    @GET
    @Path("/import/jobs")
    @Produces("application/json")
    @Operation(summary = "Returns all active import jobs statuses",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Import jobs statuses",
                            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = Map.class))}
                    )
            })
    public Response getAllImporterJobs() {
        Map<String, ImportJobsService.JobState> jobsStates = jobsService.getAllJobsStates();
        Map<String, ImportJobDetails> jobsDetails = jobsStates.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry ->
                        ImportJobDetails.builder()
                                .status(convertToJobStatus(entry.getValue()))
                                .errorMessage(entry.getValue().errorMessage())
                                .build()));
        return Response.ok(jobsDetails).build();
    }

    private ImportJobStatus convertToJobStatus(ImportJobsService.JobState jobState) {
        if (!jobState.isDone()) {
            return ImportJobStatus.IN_PROGRESS;
        } else if (!jobState.isCancelled() && !jobState.isCompletedExceptionally()) {
            return ImportJobStatus.SUCCESSFULLY_FINISHED;
        } else if (jobState.isCancelled()) {
            return ImportJobStatus.CANCELLED;
        } else {
            return ImportJobStatus.FAILED;
        }
    }

}
