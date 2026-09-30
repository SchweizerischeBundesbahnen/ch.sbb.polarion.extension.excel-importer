package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.generic.jobs.AsyncJobsService;
import ch.sbb.polarion.extension.generic.jobs.JobsProperties;
import ch.sbb.polarion.extension.generic.jobs.JobsRegistry;
import ch.sbb.polarion.extension.generic.jobs.TimeoutPolicy;
import com.polarion.platform.security.ISecurityService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Objects;

/**
 * Runs Excel imports in the background. The job mechanics are generic's {@link AsyncJobsService}.
 * <p>
 * An import writes work items, so it is never declared over from the outside ({@link TimeoutPolicy#COOPERATIVE}):
 * at its timeout it is asked to stop, and {@link ImportService} stops before the next row, rolling back what it wrote.
 * The caller is never told that nothing was imported while the import still writes.
 * <p>
 * The job keeps no payload: the uploaded file is not needed once the import has read it.
 */
public class ImportJobsService extends AsyncJobsService<Void, ImportResult> {

    public static final String JOBS_PROPERTIES_FILE = "/import-jobs.properties";

    // Static, so that the jobs survive the controller instance which started them
    private static final JobsRegistry<Void, ImportResult> REGISTRY = registryBuilder().build();

    private final ImportService importService;

    public ImportJobsService(@NotNull ImportService importService, @NotNull ISecurityService securityService) {
        this(importService, securityService, REGISTRY);
    }

    @VisibleForTesting
    ImportJobsService(@NotNull ImportService importService, @NotNull ISecurityService securityService,
                      @NotNull JobsRegistry<Void, ImportResult> registry) {
        super(registry, securityService);
        this.importService = importService;
    }

    /**
     * @return the job timeouts of this extension
     */
    public static @NotNull JobsProperties jobsProperties() {
        return new JobsProperties(ImportJobsService.class, JOBS_PROPERTIES_FILE);
    }

    /**
     * Starts dropping finished imports once they are older than the finished job timeout.
     */
    public static void startCleaner() {
        REGISTRY.startCleaner(jobsProperties().getFinishedJobTimeout());
    }

    /**
     * Stops the cleaner and the import threads.
     */
    public static void shutdown() {
        REGISTRY.shutdown();
    }

    /**
     * Starts an import with the in-progress timeout of this extension.
     */
    public @NotNull String startJob(@NotNull ImportJobParams jobParams) {
        return startJob(jobParams, jobsProperties().getInProgressJobTimeout());
    }

    public @NotNull String startJob(@NotNull ImportJobParams jobParams, int timeoutInMinutes) {
        return startJob(null, timeoutInMinutes, control -> importService.processFile(
                jobParams.getProjectId(), jobParams.getMappingName(), jobParams.getFileContent(), control::isAbortRequested));
    }

    /**
     * @return the message of the innermost cause, or its class where it carries no message: an import fails deep
     * inside Polarion or the Excel parser, and the wrappers around that say little
     */
    @Override
    protected @NotNull String describeFailure(@NotNull Throwable thrown) {
        // older commons-lang3 versions answer null for a throwable without a cause
        Throwable rootCause = Objects.requireNonNullElse(ExceptionUtils.getRootCause(thrown), thrown);
        String message = rootCause.getMessage();
        return message == null || message.isBlank() ? rootCause.getClass().getName() : message;
    }

    @VisibleForTesting
    static @NotNull JobsRegistry.Builder<Void, ImportResult> registryBuilder() {
        return JobsRegistry.<Void, ImportResult>builder("Import")
                .timeoutPolicy(TimeoutPolicy.COOPERATIVE);
    }
}
