package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.utils.PropertiesUtility;
import com.polarion.platform.security.ISecurityService;
import lombok.SneakyThrows;
import org.awaitility.Durations;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.security.auth.Subject;
import java.lang.reflect.Field;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class ImportJobsServiceTest {

    @Mock
    private ImportService importService;

    @Mock
    private ISecurityService securityService;

    @Mock
    private Subject mockSubject;

    private MockedConstruction<PropertiesUtility> propertiesMockedConstruction;

    private ImportJobsService importJobsService;

    @BeforeEach
    @SneakyThrows
    void setUp() {
        importJobsService = new ImportJobsService(importService, securityService);

        propertiesMockedConstruction = mockConstruction(PropertiesUtility.class,
                (mock, context) -> when(mock.getInProgressJobTimeout()).thenReturn(30)
        );

        lenient().when(securityService.getCurrentSubject()).thenReturn(mockSubject);

        Field jobsField = ImportJobsService.class.getDeclaredField("jobs");
        jobsField.setAccessible(true);
        ((Map) jobsField.get(null)).clear();

        Field failedJobsReasonsField = ImportJobsService.class.getDeclaredField("failedJobsReasons");
        failedJobsReasonsField.setAccessible(true);
        ((Map) failedJobsReasonsField.get(null)).clear();
    }

    @AfterEach
    void tearDown() {
        propertiesMockedConstruction.close();
    }

    @Test
    void testStartJob() {
        ImportJobParams jobParams = new ImportJobParams("projectId", "mapping", "fileContent".getBytes());
        ImportResult expectedResult = new ImportResult(List.of(), List.of(), List.of(), List.of(), "");

        doAnswer(invocation -> {
            PrivilegedAction<ImportResult> action = invocation.getArgument(1);
            return action.run();
        }).when(securityService).doAsUser(eq(mockSubject), any(PrivilegedAction.class));

        when(importService.processFile("projectId", "mapping", "fileContent".getBytes())).thenReturn(expectedResult);

        String jobId = importJobsService.startJob(jobParams);

        assertNotNull(jobId);
        verify(securityService).getCurrentSubject();
    }

    @Test
    void testGetJobState_unknownJob() {
        String nonExistingJobId = "non-existing-job";

        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobState(nonExistingJobId));
    }

    @Test
    void testGetJobParams_unknownJob() {
        String nonExistingJobId = "non-existing-job";

        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobParams(nonExistingJobId));
    }

    @Test
    void testGetJobResult_unknownJob() {
        String nonExistingJobId = "non-existing-job";

        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobResult(nonExistingJobId));
    }

    @Test
    void testGetAllJobsStates_emptyWhenNoJobs() {
        Map<String, ImportJobsService.JobState> states = importJobsService.getAllJobsStates();

        assertNotNull(states);
        assertTrue(states.isEmpty());
    }

    @Test
    void testSuccessfulJobExecution() {
        ImportJobParams jobParams = new ImportJobParams("projectId", "mapping", "fileContent".getBytes());
        ImportResult expectedResult = new ImportResult(List.of(), List.of(), List.of(), List.of(), ""); // Create appropriate result

        doAnswer(invocation -> {
            PrivilegedAction<ImportResult> action = invocation.getArgument(1);
            return action.run();
        }).when(securityService).doAsUser(eq(mockSubject), any(PrivilegedAction.class));

        when(importService.processFile("projectId", "mapping", "fileContent".getBytes())).thenReturn(expectedResult);

        String jobId = importJobsService.startJob(jobParams);

        await().atMost(Durations.ONE_SECOND)
                .until(() -> importJobsService.getJobState(jobId).isDone());

        ImportJobsService.JobState jobState = importJobsService.getJobState(jobId);
        assertTrue(jobState.isDone());
        assertFalse(jobState.isCompletedExceptionally());

        Optional<ImportResult> result = importJobsService.getJobResult(jobId);
        assertTrue(result.isPresent());
        assertEquals(expectedResult, result.get());
    }

    @Test
    void testFailedJobExecution() {
        ImportJobParams jobParams = new ImportJobParams("projectId", "mapping", "fileContent".getBytes());
        RuntimeException expectedException = new RuntimeException("Import failed");

        doAnswer(invocation -> {
            PrivilegedAction<ImportResult> action = invocation.getArgument(1);
            return action.run();
        }).when(securityService).doAsUser(eq(mockSubject), any(PrivilegedAction.class));

        when(importService.processFile("projectId", "mapping", "fileContent".getBytes())).thenThrow(expectedException);

        String jobId = importJobsService.startJob(jobParams);

        await().atMost(Durations.ONE_SECOND)
                .until(() -> importJobsService.getJobState(jobId).isDone());

        ImportJobsService.JobState jobState = importJobsService.getJobState(jobId);
        assertTrue(jobState.isDone());
        assertTrue(jobState.isCompletedExceptionally());
        assertEquals("Import failed", jobState.errorMessage());

        assertThrows(IllegalStateException.class, () -> importJobsService.getJobResult(jobId));
    }

    @Test
    void testCleanupExpiredJobs() {
        assertDoesNotThrow(() -> ImportJobsService.cleanupExpiredJobs(5));
    }

    @Test
    void testCancelJobsAndCleanMap() {
        ImportJobParams jobParams = new ImportJobParams("projectId", "mapping", "fileContent".getBytes());

        lenient().doAnswer(invocation -> {
            PrivilegedAction<ImportResult> action = invocation.getArgument(1);
            return action.run();
        }).when(securityService).doAsUser(eq(mockSubject), any(PrivilegedAction.class));

        lenient().when(importService.processFile(anyString(), anyString(), any()))
                .thenReturn(new ImportResult(List.of(), List.of(), List.of(), List.of(), ""));

        String jobId = importJobsService.startJob(jobParams);
        importJobsService.cancelJobsAndCleanMap();

        Map<String, ImportJobsService.JobState> states = importJobsService.getAllJobsStates();
        assertTrue(states.isEmpty());

        assertThrows(NoSuchElementException.class, () -> importJobsService.getJobState(jobId));
    }
}
