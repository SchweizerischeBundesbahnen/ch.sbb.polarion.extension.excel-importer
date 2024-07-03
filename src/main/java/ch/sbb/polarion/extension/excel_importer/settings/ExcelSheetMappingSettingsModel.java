package ch.sbb.polarion.extension.excel_importer.settings;

import ch.sbb.polarion.extension.excel_importer.service.parser.IParserSettings;
import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ExcelSheetMappingSettingsModel extends SettingsModel implements IParserSettings {
    public static final String SHEET_NAME = "SHEET_NAME";
    public static final String START_FROM_ROW = "START_FROM_ROW";
    public static final String OVERWRITE_WITH_EMPTY = "OVERWRITE_WITH_EMPTY";

    public static final String COLUMNS_MAPPING = "COLUMNS_MAPPING";
    public static final String ENUMS_MAPPING = "ENUMS_MAPPING";
    public static final String DEFAULT_WI_TYPE = "DEFAULT_WI_TYPE";
    public static final String LINK_COLUMN = "LINK_COLUMN";

    private String sheetName;
    private int startFromRow;
    private boolean overwriteWithEmpty;
    private Map<String, String> columnsMapping;
    private Map<String, Map<String, String>> enumsMapping;
    private String defaultWorkItemType;
    private String linkColumn;

    @JsonIgnore
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected String serializeModelData() {
        return serializeEntry(SHEET_NAME, sheetName) +
                serializeEntry(START_FROM_ROW, startFromRow) +
                serializeEntry(OVERWRITE_WITH_EMPTY, overwriteWithEmpty) +
                serializeEntry(COLUMNS_MAPPING, columnsMapping) +
                serializeEntry(ENUMS_MAPPING, enumsMapping) +
                serializeEntry(DEFAULT_WI_TYPE, defaultWorkItemType) +
                serializeEntry(LINK_COLUMN, linkColumn);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void deserializeModelData(String serializedString) {
        sheetName = deserializeEntry(SHEET_NAME, serializedString);
        Integer startFromRowValue = deserializeEntry(START_FROM_ROW, serializedString, Integer.class);
        startFromRow = startFromRowValue == null ? 0 : startFromRowValue;
        overwriteWithEmpty = !Objects.equals(Boolean.FALSE.toString(), deserializeEntry(OVERWRITE_WITH_EMPTY, serializedString));
        columnsMapping = deserializeEntry(COLUMNS_MAPPING, serializedString, Map.class);
        enumsMapping = deserializeEntry(ENUMS_MAPPING, serializedString, Map.class);
        defaultWorkItemType = deserializeEntry(DEFAULT_WI_TYPE, serializedString);
        linkColumn = deserializeEntry(LINK_COLUMN, serializedString);
    }

    @Override
    public Set<String> getUsedColumnsLetters() {
        return columnsMapping.keySet();
    }

}
