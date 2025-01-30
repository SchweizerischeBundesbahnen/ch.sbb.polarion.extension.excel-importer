package ch.sbb.polarion.extension.excel_importer.rest;

import ch.sbb.polarion.extension.excel_importer.rest.controller.ExcelProcessingApiController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.ExcelProcessingInternalController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.WorkItemsApiController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.WorkItemsInternalController;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettings;
import ch.sbb.polarion.extension.generic.rest.GenericRestApplication;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class ExcelImporterRestApplication extends GenericRestApplication {

    public ExcelImporterRestApplication() {
        NamedSettingsRegistry.INSTANCE.register(List.of(new ExcelSheetMappingSettings()));
    }


    @Override
    protected @NotNull Set<Object> getExtensionControllerSingletons() {
        return Set.of(
                new ExcelProcessingApiController(),
                new ExcelProcessingInternalController(),
                new WorkItemsApiController(),
                new WorkItemsInternalController()
        );
    }
}
