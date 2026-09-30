package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.generic.jobs.JobState;
import ch.sbb.polarion.extension.generic.jobs.JobsRegistry;
import ch.sbb.polarion.extension.generic.rest.model.jobs.JobStatus;
import com.polarion.platform.security.ISecurityService;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.Subject;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportJobsServiceTest {

    private static final String TEST_USER = "testUser";
    private static final byte[] FILE_CONTENT = "fileContent".getBytes();
    private static final ImportJobParams JOB_PARAMS = new ImportJobParams("projectId", "mapping", FILE_CONTENT);

    @Mock
    private ImportService importService;

    @Mock
    private ISecurityService securityService;

    @Mock
    private Subject mockSubject;

    private JobsRegistry<Void, ImportResult> registry;
    private ImportJobsService importJobsService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        importJobsService = jobsService(TimeUnit.MINUTES);
        lenient().when(securityService.getCurrentUser()).thenReturn(TEST_USER);
        lenient().when(securityService.getCurrentSubject()).thenReturn(mockSubject);
        lenient().when(securityService.doAsUser(eq(mockSubject), any(PrivilegedAction.class)))
                .thenAnswer(invocation -> ((PrivilegedAction<?>) invocation.getArgument(1)).run());
    }

    @AfterEach
    void tearDown() {
        registry.clear();
        registry.shutdown();
    }

    /**
     * A service over a registry of its own, set up as the one of the extension, so that no test leaves jobs behind
     * for the next one. A timeout unit shorter than minutes lets a test see an import run out of time.
     */
    private ImportJobsService jobsService(TimeUnit timeoutUnit) {
        if (registry != null) {
            registry.shutdown();
        }
        registry = ImportJobsService.registryBuilder().timeoutUnit(timeoutUnit).build();
        return new ImportJobsService(importService, securityService, registry);
    }

    @Test
    void testStartJobWithTimeoutOfExtension() {
        when(importService.processFile(eq("projectId"), eq("mapping"), eq(FILE_CONTENT), any())).thenReturn(emptyResult());

        String jobId = importJobsService.startJob(JOB_PARAMS);

        assertNotNull(jobId);
        waitToFinishJob(jobId);
        assertEquals(60, ImportJobsService.jobsProperties().getInProgressJobTimeout());
    }

    @Test
    void testGetJobState_unknownJob() {
        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobState("non-existing-job"));
    }

    @Test
    void testGetJobResult_unknownJob() {
        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobResult("non-existing-job"));
    }

    @Test
    void testGetAllJobsStates_emptyWhenNoJobs() {
        Map<String, JobState> states = importJobsService.getAllJobsStates();

        assertNotNull(states);
        assertTrue(states.isEmpty());
    }

    @Test
    void testSuccessfulJobExecution() {
        ImportResult expectedResult = emptyResult();
        when(importService.processFile(eq("projectId"), eq("mapping"), eq(FILE_CONTENT), any())).thenReturn(expectedResult);

        String jobId = importJobsService.startJob(JOB_PARAMS, 30);

        waitToFinishJob(jobId);
        verify(securityService).doAsUser(eq(mockSubject), any(PrivilegedAction.class));
        assertEquals(1, importJobsService.getAllJobsStates().size());
        assertEquals(JobStatus.SUCCESSFULLY_FINISHED, importJobsService.getJobState(jobId).status());

        Optional<ImportResult> result = importJobsService.getJobResult(jobId);
        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());

        // the uploaded file is not kept with the job
        assertNull(importJobsService.getJobPayload(jobId));

        // check unknown job ID
        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobResult("unknownJobId"));

        // check job is not accessible for other users
        when(securityService.getCurrentUser()).thenReturn("other_" + TEST_USER);
        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobResult(jobId));
        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobState(jobId));
        assertTrue(importJobsService.getAllJobsStates().isEmpty());

        // double check that job is still accessible for the user who started it
        when(securityService.getCurrentUser()).thenReturn(TEST_USER);
        assertDoesNotThrow(() -> importJobsService.getJobResult(jobId));
        assertDoesNotThrow(() -> importJobsService.getJobState(jobId));
        assertEquals(1, importJobsService.getAllJobsStates().size());
    }

    @Test
    void testFailedJobExecution() {
        when(importService.processFile(eq("projectId"), eq("mapping"), eq(FILE_CONTENT), any())).thenThrow(new RuntimeException("Import failed"));

        String jobId = importJobsService.startJob(JOB_PARAMS, 30);

        waitToFinishJob(jobId);
        JobState jobState = importJobsService.getJobState(jobId);
        assertEquals(JobStatus.FAILED, jobState.status());
        assertEquals("Import failed", jobState.errorMessage());
        assertThrows(IllegalStateException.class, () -> importJobsService.getJobResult(jobId));
    }

    /**
     * An import fails deep inside Polarion or the Excel parser, and the message worth showing is the innermost one.
     */
    @Test
    void testFailureIsDescribedByItsRootCause() {
        when(importService.processFile(eq("projectId"), eq("mapping"), eq(FILE_CONTENT), any()))
                .thenThrow(new IllegalStateException("wrapper", new InvocationTargetException(new IllegalArgumentException("Column 'A' is empty"))));

        String jobId = importJobsService.startJob(JOB_PARAMS, 30);

        waitToFinishJob(jobId);
        assertEquals("Column 'A' is empty", importJobsService.getJobState(jobId).errorMessage());
    }

    @Test
    void testFailureWithoutMessageIsDescribedByItsClass() {
        when(importService.processFile(eq("projectId"), eq("mapping"), eq(FILE_CONTENT), any())).thenThrow(new IllegalStateException());

        String jobId = importJobsService.startJob(JOB_PARAMS, 30);

        waitToFinishJob(jobId);
        assertEquals(IllegalStateException.class.getName(), importJobsService.getJobState(jobId).errorMessage());
    }

    /**
     * An import writes work items, so at its timeout it is asked to stop and stays a running job until it has. It is
     * then reported failed, with the timeout as the reason.
     */
    @Test
    void testImportWhichRunsTooLongIsAskedToStop() throws InterruptedException {
        importJobsService = jobsService(TimeUnit.MILLISECONDS);
        CountDownLatch askedToStop = new CountDownLatch(1);
        CountDownLatch releaseImport = new CountDownLatch(1);
        when(importService.processFile(eq("projectId"), eq("mapping"), eq(FILE_CONTENT), any())).thenAnswer(invocation -> {
            BooleanSupplier abortRequested = invocation.getArgument(3);
            await().atMost(Durations.FIVE_SECONDS).until(abortRequested::getAsBoolean);
            askedToStop.countDown();
            releaseImport.await(5, TimeUnit.SECONDS);
            throw new CancellationException("Import was asked to stop before it imported all rows, nothing was imported");
        });

        String jobId = importJobsService.startJob(JOB_PARAMS, 50);

        try {
            assertTrue(askedToStop.await(5, TimeUnit.SECONDS));
            // still writing, or rolling back: not over yet
            assertEquals(JobStatus.IN_PROGRESS, importJobsService.getJobState(jobId).status());
        } finally {
            releaseImport.countDown();
        }
        waitToFinishJob(jobId);
        JobState jobState = importJobsService.getJobState(jobId);
        assertEquals(JobStatus.FAILED, jobState.status());
        assertEquals("Timeout after 50 min", jobState.errorMessage());
    }

    @Test
    void testCleanupExpiredJobs() {
        when(importService.processFile(eq("projectId"), eq("mapping"), eq(FILE_CONTENT), any())).thenReturn(emptyResult());
        String jobId = importJobsService.startJob(JOB_PARAMS, 30);
        waitToFinishJob(jobId);

        registry.cleanupExpiredJobs(5);
        assertDoesNotThrow(() -> importJobsService.getJobState(jobId));

        // a finished job expires once it is older than the timeout, which takes a moment even for a timeout of 0
        await().atMost(Durations.FIVE_SECONDS).untilAsserted(() -> {
            registry.cleanupExpiredJobs(0);
            assertThrows(NoSuchElementException.class, () -> importJobsService.getJobState(jobId));
        });
    }

    private void waitToFinishJob(String jobId) {
        await().atMost(Durations.FIVE_SECONDS).until(() -> importJobsService.getJobState(jobId).isDone());
    }

    private static ImportResult emptyResult() {
        return new ImportResult(List.of(), List.of(), List.of(), List.of(), "");
    }
}
