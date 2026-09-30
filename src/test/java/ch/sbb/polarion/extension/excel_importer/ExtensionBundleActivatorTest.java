package ch.sbb.polarion.extension.excel_importer;

import ch.sbb.polarion.extension.excel_importer.service.ImportJobsService;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.osgi.framework.BundleContext;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class ExtensionBundleActivatorTest {

    @Test
    void testNoFormExtensions() {
        assertTrue(new ExtensionBundleActivator().getExtensions().isEmpty());
    }

    /**
     * The import threads and the cleaner of finished imports stop with the bundle.
     */
    @Test
    void testTheImportJobsStopWithTheBundle() {
        try (MockedStatic<ImportJobsService> jobsService = mockStatic(ImportJobsService.class)) {
            new ExtensionBundleActivator().stop(mock(BundleContext.class));

            jobsService.verify(ImportJobsService::shutdown);
        }
    }

}
