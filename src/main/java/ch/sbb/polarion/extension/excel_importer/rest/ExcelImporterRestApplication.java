package ch.sbb.polarion.extension.excel_importer.rest;

import ch.sbb.polarion.extension.excel_importer.rest.controller.ExcelProcessingApiController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.ExcelProcessingInternalController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.ExcelToolApiController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.ExcelToolInternalController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.WorkItemsApiController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.WorkItemsInternalController;
import ch.sbb.polarion.extension.excel_importer.service.ImportJobsCleaner;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettings;
import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import com.polarion.core.util.logging.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class ExcelImporterRestApplication extends GenericRestApplication {

    private final Logger logger = Logger.getLogger(ExcelImporterRestApplication.class);

    public ExcelImporterRestApplication() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new ExcelSheetMappingSettings()));

        try {
            ImportJobsCleaner.startCleaningJob();
        } catch (Exception e) {
            logger.error("Error during starting of cleaning job", e);
        }
    }


    @Override
    protected @NotNull Set<Object> getExtensionControllerSingletons() {
        return Set.of(
                new ExcelProcessingApiController(),
                new ExcelProcessingInternalController(),
                new ExcelToolInternalController(),
                new ExcelToolApiController(),
                new WorkItemsApiController(),
                new WorkItemsInternalController()
        );
    }
}
