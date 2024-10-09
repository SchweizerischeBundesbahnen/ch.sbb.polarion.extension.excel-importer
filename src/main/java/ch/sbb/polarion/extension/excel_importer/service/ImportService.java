package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettingsModel;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.util.OptionsMappingUtils;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.StringUtils;
import com.polarion.subterra.base.data.model.IType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ImportService {

    private final PolarionServiceExt polarionServiceExt;

    public ImportService() {
        polarionServiceExt = new PolarionServiceExt();
    }

    public ImportService(PolarionServiceExt polarionServiceExt) {
        this.polarionServiceExt = polarionServiceExt;
    }

    public ImportResult processFile(@NotNull ITrackerProject trackerProject, @NotNull List<Map<String, Object>> xlsxData, @NotNull ExcelSheetMappingSettingsModel settings) {
        //get & check WorkItem type existence
        ITypeOpt workItemType = polarionServiceExt.findWorkItemTypeInProject(trackerProject, settings.getDefaultWorkItemType());

        //extract WorkItem IDs from each row
        Set<String> workItemIds = xlsxData.stream()
                .map(dataRow -> getIdentifierValue(dataRow, settings.getLinkColumn()))
                .collect(Collectors.toSet());
        if (workItemIds.isEmpty()) {
            throw new IllegalArgumentException(String.format("File must contain data in the '%s' column as it will be used for linking with the WorkItem.", settings.getLinkColumn()));
        }

        //find & process WorkItems
        return TransactionalExecutor.executeInWriteTransaction(transaction -> processWorkItemIds(workItemIds, trackerProject, workItemType, xlsxData, settings));
    }

    private ImportResult processWorkItemIds(Set<String> workItemIds, ITrackerProject project, ITypeOpt workItemType, @NotNull List<Map<String, Object>> xlsxData, ExcelSheetMappingSettingsModel settings) {
        List<String> updatedIds = new ArrayList<>();
        List<String> createdIds = new ArrayList<>();
        List<String> unchangedIds = new ArrayList<>();

        String identifierFieldId = settings.getColumnsMapping().get(settings.getLinkColumn());
        List<IWorkItem> foundWorkItems = polarionServiceExt.findWorkItemsById(project.getId(), settings.getDefaultWorkItemType(), identifierFieldId, workItemIds);

        for (Map<String, Object> columnMappingRecord : xlsxData) {
            Object idValue = columnMappingRecord.get(settings.getLinkColumn());
            IWorkItem workItem = foundWorkItems.stream()
                    .filter(findWorkItemByFieldValue(identifierFieldId, idValue))
                    .findFirst()
                    .orElse(null);
            boolean createNew = workItem == null;
            if (createNew) { //WorkItem not found - create a new one
                if (!identifierFieldId.equals("id")) {
                    workItem = polarionServiceExt.createWorkItem(project, workItemType);
                } else {
                    throw new IllegalArgumentException("If id is used as Link Column, no new Work Items can be created via import.");
                }
            }
            fillWorkItemFields(workItem, columnMappingRecord, settings, identifierFieldId); //set fields values
            boolean isWorkItemUnchanged = !workItem.isModified(); // maybe isModified doesn't work on a new WI before save?
            workItem.save();
            if (createNew) {
                createdIds.add(workItem.getId());
            } else if (isWorkItemUnchanged) {
                unchangedIds.add(workItem.getId());
            } else {
                updatedIds.add(workItem.getId());
            }
        }

        return ImportResult.builder()
                .updatedIds(updatedIds)
                .createdIds(createdIds)
                .unchangedIds(unchangedIds)
                .build();
    }

    // temporary solution: should be clarified how to compare values of fields depending on field types
    @NotNull
    private static Predicate<IWorkItem> findWorkItemByFieldValue(String fieldId, Object value) {
        return workItem -> {
            Object workItemFieldValue = workItem.getValue(fieldId);
            boolean equals = Objects.equals(workItemFieldValue, value);
            if (!equals) {
                equals = workItemFieldValue.toString().contains(value.toString());
            }
            return equals;
        };
    }

    private String getIdentifierValue(Map<String, Object> recordMap, String columnLetter) {
        Object idValue = recordMap.get(columnLetter);
        if (!(idValue instanceof String stringIdValue) || StringUtils.isEmptyTrimmed(stringIdValue)) {
            throw new IllegalArgumentException(String.format("Column '%s' contains empty or unsupported non-string type value", columnLetter));
        }
        return stringIdValue;
    }

    private void fillWorkItemFields(IWorkItem workItem, Map<String, Object> mappingRecord, ExcelSheetMappingSettingsModel model, String linkColumnId) {
        mappingRecord.forEach((columnId, value) -> {
            String fieldId = model.getColumnsMapping().get(columnId);
            // we need to know possible mapped value asap because some types (at least boolean) need it to check value for modification
            String mappedOption = OptionsMappingUtils.getMappedOptionKey(fieldId, value, model.getEnumsMapping());
            value = mappedOption != null ? mappedOption : value;
            Set<FieldMetadata> fieldMetadataSet = polarionServiceExt.getWorkItemsFields(workItem.getProjectId(), workItem.getType() == null ? "" : workItem.getType().getId());
            // The linkColumn field's value can't change, therefore it doesn't need to be overwritten.
            // However, it must be saved to the newly created work item otherwise sequential imports will produce several objects.
            if (fieldId != null && !fieldId.equals(IUniqueObject.KEY_ID) && (!fieldId.equals(linkColumnId) || !workItem.isPersisted()) &&
                    (model.isOverwriteWithEmpty() || !isEmpty(value)) &&
                    ensureValidValue(fieldId, value, fieldMetadataSet) &&
                    existingValueDiffers(workItem, fieldId, value, fieldMetadataSet)) {
                polarionServiceExt.setFieldValue(workItem, fieldId, value, model.getEnumsMapping());
            } else if (fieldId != null && fieldId.equals(IUniqueObject.KEY_ID) && !fieldId.equals(linkColumnId)) {
                // If the work item id is imported, it must be the Link Column. Its value also can't be set by imported data unlike other possible Link Column fields.
                throw new IllegalArgumentException("WorkItem id can only be imported if it is used as Link Column.");
            }
        });
    }

    @VisibleForTesting
    boolean ensureValidValue(String fieldId, Object value, Set<FieldMetadata> fieldMetadataSet) {
        FieldMetadata fieldMetadata = getFieldMetadataForField(fieldMetadataSet, fieldId);
        if (FieldType.BOOLEAN.getType().equals(fieldMetadata.getType()) &&
                (!(value instanceof String) || !("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value)))) {
            throw new IllegalArgumentException(String.format("'%s' isn't a valid boolean value", value == null ? "" : value));
        }
        return true;
    }

    /**
     * Here wy try to make a preliminary check whether the value differs with the existing one.
     * This can be useful coz Polarion sometimes makes update in case when it isn't needed (e.g. if you try to
     * set false to the boolean field which already has this value Polarion will rewrite the value and increment revision).
     */
    @SuppressWarnings("java:S1125") //will be improved later
    @VisibleForTesting
    boolean existingValueDiffers(IWorkItem workItem, String fieldId, Object newValue, Set<FieldMetadata> fieldMetadataSet) {
        FieldMetadata fieldMetadata = getFieldMetadataForField(fieldMetadataSet, fieldId);
        Object existingValue = polarionServiceExt.getFieldValue(workItem, fieldId);
        if (existingValue == null && newValue == null) {
            return false;
        }

        IType fieldType = fieldMetadata.getType();
        if (FieldType.BOOLEAN.getType().equals(fieldType)) {
            newValue = Boolean.valueOf((String) newValue); // validator that had been running before must ensure that the new value is a proper string
            return !Objects.equals(existingValue == null ? false : existingValue, newValue);
        } else if (FieldType.FLOAT.getType().equals(fieldType)) {
            //WORKAROUND: converting to string helps to find same values even between different types (Float, Double etc.)
            return !Objects.equals(String.valueOf(newValue), String.valueOf(existingValue));
        } else {
            return !Objects.equals(newValue, existingValue);
        }
    }

    private FieldMetadata getFieldMetadataForField(Set<FieldMetadata> fieldMetadataSet, String fieldId) {
        return fieldMetadataSet.stream()
                .filter(m -> m.getId().equals(fieldId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find field metadata for ID '%s'".formatted(fieldId)));
    }

    private boolean isEmpty(Object value) {
        return value == null || (value instanceof String stringValue && StringUtils.isEmptyTrimmed(stringValue));
    }
}
