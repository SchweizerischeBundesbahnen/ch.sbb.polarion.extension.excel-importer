package ch.sbb.polarion.extension.excel_importer.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class ExcelSheetMappingSettings extends GenericNamedSettings<ExcelSheetMappingSettingsModel> {

    private static final String FEATURE_NAME = "mappings";

    public ExcelSheetMappingSettings() {
        super(FEATURE_NAME);
    }

    public ExcelSheetMappingSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public @NotNull ExcelSheetMappingSettingsModel defaultValues() {
        ExcelSheetMappingSettingsModel settingsModel = new ExcelSheetMappingSettingsModel();
        settingsModel.setSheetName("Sheet1");
        settingsModel.setStartFromRow(1);
        settingsModel.setOverwriteWithEmpty(true);
        settingsModel.setDefaultWorkItemType("");
        settingsModel.setLinkColumn("");
        settingsModel.setColumnsMapping(new HashMap<>());
        return settingsModel;
    }
}
