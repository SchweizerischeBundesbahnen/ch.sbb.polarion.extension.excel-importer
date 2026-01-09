package ch.sbb.polarion.extension.excel_importer.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ImportJobsCleanerTest {

    @AfterEach
    void tearDown() {
        ImportJobsCleaner.stopCleaningJob();
    }

    @Test
    void testStartCleaningJob() {
        assertDoesNotThrow(ImportJobsCleaner::startCleaningJob);

        // Ensure that the method can be called multiple times without throwing an exception
        assertDoesNotThrow(ImportJobsCleaner::startCleaningJob);
    }

    @Test
    void testStopCleaningJob() {
        // Stop without start should not throw
        assertDoesNotThrow(ImportJobsCleaner::stopCleaningJob);

        // Start and stop should work
        ImportJobsCleaner.startCleaningJob();
        assertDoesNotThrow(ImportJobsCleaner::stopCleaningJob);

        // Stop again should not throw (already stopped)
        assertDoesNotThrow(ImportJobsCleaner::stopCleaningJob);
    }

    @Test
    void testRestartCleaningJob() {
        ImportJobsCleaner.startCleaningJob();
        ImportJobsCleaner.stopCleaningJob();

        // Should be able to start again after stopping
        assertDoesNotThrow(ImportJobsCleaner::startCleaningJob);
    }

}
