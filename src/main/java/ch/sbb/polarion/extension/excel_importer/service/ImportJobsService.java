package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.utils.PropertiesUtility;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.ISecurityService;
import lombok.Builder;
import org.jetbrains.annotations.VisibleForTesting;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.security.auth.Subject;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ImportJobsService {
    private final Logger logger = Logger.getLogger(ImportJobsService.class);
    // Static maps are necessary for per-request scoped InternalController and ApiController. In case of singletons static can be removed
    private static final Map<String, JobDetails> jobs = new ConcurrentHashMap<>();
    private static final Map<String, String> failedJobsReasons = new ConcurrentHashMap<>();
    private static final String UNKNOWN_JOB_MESSAGE = "Importer Job is unknown: %s";

    private final ImportService importService;
    private final ISecurityService securityService;
    private final PropertiesUtility propertiesUtility = new PropertiesUtility();

    public ImportJobsService(ImportService importService, ISecurityService securityService) {
        this.importService = importService;
        this.securityService = securityService;
    }

    public String startJob(ImportJobParams jobParams) {
        String jobId = UUID.randomUUID().toString();
        Subject userSubject = securityService.getCurrentSubject();
        boolean isJobLogoutRequired = isJobLogoutRequired();
        long timeoutInMinutes = propertiesUtility.getInProgressJobTimeout();

        CompletableFuture<ImportResult> asyncImportJob = CompletableFuture.supplyAsync(() -> {
            try {
                return securityService.doAsUser(userSubject, (PrivilegedAction<ImportResult>) () -> importService.processFile(jobParams.getProjectId(), jobParams.getMappingName(), jobParams.getFileContent()));
            } catch (Exception e) {
                String errorMessage = String.format("Import job '%s' is failed with error: %s", jobId, e.getMessage());
                logger.error(errorMessage, e);
                failedJobsReasons.put(jobId, e.getMessage());
                throw e;
            } finally {
                if ((userSubject != null) && isJobLogoutRequired) {
                    securityService.logout(userSubject);
                }
                logger.info("Import job '%s' is finished".formatted(jobId));
            }
        }, Executors.newSingleThreadExecutor());
        asyncImportJob
                .orTimeout(timeoutInMinutes, TimeUnit.MINUTES)
                .exceptionally(e -> {
                    String failedReason = e.getMessage();
                    if (e instanceof TimeoutException) {
                        failedReason = String.format("Timeout after %d min", timeoutInMinutes);
                    } else if (e instanceof CompletionException ce && ce.getCause() != null) {
                        failedReason = ce.getCause().getMessage();
                    }
                    failedJobsReasons.put(jobId, failedReason);
                    logger.error(String.format("Import job '%s' is failed with error: %s", jobId, failedReason), e);
                    asyncImportJob.completeExceptionally(e);
                    return null;
                });
        JobDetails jobDetails = JobDetails.builder()
                .future(asyncImportJob)
                .jobParams(jobParams)
                .startingTime(Instant.now()).build();
        jobs.put(jobId, jobDetails);
        return jobId;
    }

    public JobState getJobState(String jobId) {
        JobDetails jobDetails = jobs.get(jobId);
        if (jobDetails == null) {
            throw new NoSuchElementException(String.format(UNKNOWN_JOB_MESSAGE, jobId));
        }
        CompletableFuture<ImportResult> future = jobDetails.future();
        return JobState.builder()
                .isDone(future.isDone())
                .isCompletedExceptionally(future.isCompletedExceptionally())
                .isCancelled(future.isCancelled())
                .errorMessage(failedJobsReasons.get(jobId)).build();
    }

    public Optional<ImportResult> getJobResult(String jobId) {
        JobDetails jobDetails = jobs.get(jobId);
        if (jobDetails == null) {
            throw new NoSuchElementException(String.format(UNKNOWN_JOB_MESSAGE, jobId));
        }
        CompletableFuture<ImportResult> future = jobDetails.future();
        if (!future.isDone()) {
            return Optional.empty();
        }
        if (future.isCancelled() || future.isCompletedExceptionally()) {
            throw new IllegalStateException("Job was cancelled or failed: " + failedJobsReasons.get(jobId));
        }
        try {
            return Optional.of(future.get());
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Cannot extract result for job " + jobId + " :" + e.getMessage(), e);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot extract result for job " + jobId + " :" + e.getMessage(), e);
        }
    }

    public ImportJobParams getJobParams(String jobId) {
        JobDetails jobDetails = jobs.get(jobId);
        if (jobDetails == null) {
            throw new NoSuchElementException(String.format(UNKNOWN_JOB_MESSAGE, jobId));
        }
        return jobDetails.jobParams;
    }

    public Map<String, JobState> getAllJobsStates() {
        return jobs.keySet().stream()
                .collect(Collectors.toMap(Function.identity(), this::getJobState));
    }

    public static void cleanupExpiredJobs(int timeout) {
        Instant currentTime = Instant.now();

        jobs.entrySet().stream()
                .filter(entry -> entry.getValue().future.isDone()
                        && entry.getValue().startingTime.plus(timeout, ChronoUnit.MINUTES).isBefore(currentTime))
                .map(Map.Entry::getKey)
                .forEach(ImportJobsService::removeKeyFromJobMaps);
    }

    private static void removeKeyFromJobMaps(String id) {
        jobs.remove(id);
        failedJobsReasons.remove(id);
    }

    @VisibleForTesting
    void cancelJobsAndCleanMap() {
        jobs.values().forEach(j -> j.future().cancel(true));
        jobs.clear();
    }

    @Builder
    public record JobDetails(
            CompletableFuture<ImportResult> future,
            ImportJobParams jobParams,
            Instant startingTime) {
    }

    @Builder
    public record JobState(
            boolean isDone,
            boolean isCompletedExceptionally,
            boolean isCancelled,
            String errorMessage) {
    }

    private boolean isJobLogoutRequired() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes != null) {
            if (requestAttributes.getAttribute(LogoutFilter.XSRF_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE) {
                return false;
            }
            return requestAttributes.getAttribute(LogoutFilter.ASYNC_SKIP_LOGOUT, RequestAttributes.SCOPE_REQUEST) == Boolean.TRUE;
        }
        return false;
    }
}
