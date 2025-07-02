package ch.sbb.polarion.extension.excel_importer.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ImportJobsCleanerTest {

    @Test
    void testStartCleaningJob() {
        assertDoesNotThrow(ImportJobsCleaner::startCleaningJob);
    }

}
