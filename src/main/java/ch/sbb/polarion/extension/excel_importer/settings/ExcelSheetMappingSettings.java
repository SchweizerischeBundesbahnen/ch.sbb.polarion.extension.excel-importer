package ch.sbb.polarion.extension.excel_importer.settings;

import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsService;
import com.polarion.core.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Objects;

public class ExcelSheetMappingSettings extends GenericNamedSettings<ExcelSheetMappingSettingsModel> {

    private static final String FEATURE_NAME = "mappings";

    public ExcelSheetMappingSettings() {
        super(FEATURE_NAME);
    }

    public ExcelSheetMappingSettings(SettingsService settingsService) {
        super(FEATURE_NAME, settingsService);
    }

    @Override
    public Collection<SettingName> readNames(@NotNull String scope) {
        Collection<SettingName> names = super.readNames(scope);
        return names.stream().filter(name -> Objects.equals(name.getScope(), scope)).toList();
    }

    @Override
    public @NotNull ExcelSheetMappingSettingsModel save(@NotNull String scope, @NotNull SettingId id, @NotNull ExcelSheetMappingSettingsModel what) {
        if (StringUtils.isEmpty(scope)) {
            throw new IllegalArgumentException("scope value is required");
        }
        return super.save(scope, id, what);
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
