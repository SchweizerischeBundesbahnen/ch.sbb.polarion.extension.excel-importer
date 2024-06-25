package ch.sbb.polarion.extension.excel_importer.rest;

import ch.sbb.polarion.extension.excel_importer.rest.controller.ExcelImportApiController;
import ch.sbb.polarion.extension.excel_importer.rest.controller.ExcelImportInternalController;
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
    @NotNull
    protected Set<Class<?>> getControllerClasses() {
        final Set<Class<?>> controllerClasses = super.getControllerClasses();
        controllerClasses.addAll(Set.of(
                ExcelImportApiController.class,
                ExcelImportInternalController.class,
                WorkItemsApiController.class,
                WorkItemsInternalController.class
        ));
        return controllerClasses;
    }
}
