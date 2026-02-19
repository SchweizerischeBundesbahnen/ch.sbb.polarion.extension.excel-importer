package ch.sbb.polarion.extension.excel_importer.service;

import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettings;
import ch.sbb.polarion.extension.excel_importer.settings.ExcelSheetMappingSettingsModel;
import ch.sbb.polarion.extension.excel_importer.utils.LinkInfo;
import ch.sbb.polarion.extension.excel_importer.utils.ParseXlsRunnable;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.fields.model.Option;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.util.BundleJarsPrioritizingRunnable;
import ch.sbb.polarion.extension.generic.util.OptionsMappingUtils;
import com.polarion.alm.projects.model.IUniqueObject;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.model.IExternallyLinkedWorkItemStruct;
import com.polarion.alm.tracker.model.ILinkRoleOpt;
import com.polarion.alm.tracker.model.ILinkedWorkItemStruct;
import com.polarion.alm.tracker.model.ITestStep;
import com.polarion.alm.tracker.model.ITestSteps;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ITypeOpt;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.xml.HTMLHelper;
import com.polarion.platform.persistence.model.IStructure;
import com.polarion.subterra.base.data.model.IType;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ch.sbb.polarion.extension.excel_importer.utils.ParseXlsRunnable.*;

public class ImportService {

    private final PolarionServiceExt polarionServiceExt;

    public ImportService() {
        polarionServiceExt = new PolarionServiceExt();
    }

    public ImportService(PolarionServiceExt polarionServiceExt) {
        this.polarionServiceExt = polarionServiceExt;
    }

    @SuppressWarnings("unchecked")
    public ImportResult processFile(String projectId, String mappingName, byte[] fileContent) {
        ITrackerProject trackerProject = polarionServiceExt.findProject(projectId);
        ExcelSheetMappingSettingsModel settings = new ExcelSheetMappingSettings().load(projectId, SettingId.fromName(mappingName));
        ITypeOpt workItemType = polarionServiceExt.findWorkItemTypeInProject(trackerProject, settings.getDefaultWorkItemType());
        ImportContext context = new ImportContext(trackerProject, workItemType, settings);

        List<Map<String, Object>> xlsxData = (List<Map<String, Object>>) BundleJarsPrioritizingRunnable.executeCached(
                ParseXlsRunnable.class, Map.of(
                        PARAM_FILE_CONTENT, fileContent,
                        PARAM_SETTINGS, settings
                ), true).get(PARAM_RESULT);
        context.log("Xlsx file parsed successfully, found %d rows".formatted(xlsxData.size()));

        TransactionalExecutor.executeInWriteTransaction(transaction -> processData(xlsxData, context));
        context.log("Transaction completed");
        return context.toResult();
    }

    @SuppressWarnings("java:S3776") // ignore cognitive complexity complaint
    private Void processData(@NotNull List<Map<String, Object>> xlsxData, ImportContext context) {
        String identifierFieldId = context.settings.getColumnsMapping().get(context.settings.getLinkColumn());
        List<List<Map<String, Object>>> xlsxDataChunked = ListUtils.partition(xlsxData.stream().toList(), 100);
        for (List<Map<String, Object>> chunk : xlsxDataChunked) {
            Set<String> workItemIds = chunk.stream()
                    .map(dataRow -> getIdentifierValue(dataRow, context.settings.getLinkColumn(), identifierFieldId))
                    .collect(Collectors.toSet());
            if (workItemIds.isEmpty()) {
                throw new IllegalArgumentException(String.format("File must contain data in the '%s' column as it will be used for linking with the WorkItem.", context.settings.getLinkColumn()));
            }

            List<IWorkItem> foundWorkItems = polarionServiceExt.findWorkItemsById(context.project.getId(), identifierFieldId, workItemIds);
            for (Map<String, Object> columnMappingRecord : chunk) {
                Object idValue = columnMappingRecord.get(context.settings.getLinkColumn());
                String idString = String.valueOf(idValue);
                List<String> logEntries = new ArrayList<>();
                List<IWorkItem> workItems = isEmpty(idValue) ? List.of() : foundWorkItems.stream().filter(findWorkItemByFieldValue(identifierFieldId, idValue)).toList();
                if (workItems.isEmpty()) { //WorkItem not found - create a new one
                    if (IUniqueObject.KEY_ID.equals(identifierFieldId) && !isEmpty(idValue)) {
                        context.skippedIds.add(idString);
                        logEntries.add("no work item found by ID '%s'. Since the 'id' is used as the 'Link Column', new work item creation is impossible".formatted(idString));
                    } else {
                        IWorkItem createdWorkItem = polarionServiceExt.createWorkItem(context.project, context.workItemType);
                        fillWorkItemFields(createdWorkItem, columnMappingRecord, context.settings, identifierFieldId);
                        createdWorkItem.save();
                        logEntries.add("new work item '%s' is being created".formatted(createdWorkItem.getId()));
                        context.createdIds.add(createdWorkItem.getId());
                    }
                } else {
                    if (workItems.size() > 1) {
                        context.log("ATTENTION! Found multiple work items for '%s' with value '%s'. Will update all of them.".formatted(idString, idValue));
                    }
                    for (IWorkItem workItem : workItems) {
                        fillWorkItemFields(workItem, columnMappingRecord, context.settings, identifierFieldId);
                        if (!workItem.isModified()) {
                            logEntries.add("no changes were made to '%s'".formatted(workItem.getId()));
                            context.unchangedIds.add(workItem.getId());
                        } else {
                            logEntries.add("the data was updated for '%s'".formatted(workItem.getId()));
                            context.updatedIds.add(workItem.getId());
                        }
                        workItem.save();
                    }
                }
                context.log((isEmpty(idValue) ? "" : idString + ": ") + String.join(", ", logEntries));
            }
        }
        context.log("Work items processing completed");
        return null;
    }

    // temporary solution: should be clarified how to compare values of fields depending on field types
    @NotNull
    @VisibleForTesting
    static Predicate<IWorkItem> findWorkItemByFieldValue(String fieldId, Object value) {
        return workItem -> {
            Object workItemFieldValue = workItem.getValue(fieldId);
            boolean equals = Objects.equals(workItemFieldValue, value);
            if (!equals && workItemFieldValue != null && value != null) {
                equals = workItemFieldValue.toString().contains(value.toString());
            }
            return equals;
        };
    }

    private String getIdentifierValue(Map<String, Object> recordMap, String columnLetter, String identifierFieldId) {
        Object idValue = recordMap.get(columnLetter);
        if (IUniqueObject.KEY_ID.equals(identifierFieldId) && isEmpty(idValue)) {
            return null; // in case of 'id' column we allow empty values, so that a new work items can be created using standard way
        } else if (!(idValue instanceof String stringIdValue) || StringUtils.isEmptyTrimmed(stringIdValue)) {
            throw new IllegalArgumentException(String.format("Column '%s' contains empty or unsupported non-string type value", columnLetter));
        }
        return (String) idValue;
    }

    @VisibleForTesting
    void fillWorkItemFields(@NotNull IWorkItem workItem, Map<String, Object> mappingRecord, ExcelSheetMappingSettingsModel model, @NotNull String linkColumnId) {
        Set<FieldMetadata> fieldMetadataSet = polarionServiceExt.getWorkItemsFields(workItem.getProjectId(), workItem.getType() == null ? "" : workItem.getType().getId());
        // Work on a copy to avoid mutating the original map (which may be reused for other work items)
        Map<String, Object> dataMap = new HashMap<>(mappingRecord);
        constructTestStepsFields(dataMap, model, workItem, fieldMetadataSet);
        dataMap.forEach((columnId, value) -> {
            String fieldId = model.getColumnsMapping().get(columnId);
            if (fieldId == null && columnId.startsWith(ExcelSheetMappingSettingsModel.TEST_STEPS_COLUMN_FILLER_PREFIX)) {
                fieldId = columnId.substring(ExcelSheetMappingSettingsModel.TEST_STEPS_COLUMN_FILLER_PREFIX.length());
            }
            if (fieldId != null) {
                // we need to know possible mapped value asap because some types (at least boolean) need it to check value for modification
                String mappedOption = OptionsMappingUtils.getMappedOptionKey(fieldId, value, model.getEnumsMapping());
                value = mappedOption != null ? mappedOption : value;
                setFieldValue(value, getFieldMetadataForField(fieldMetadataSet, fieldId), workItem, model, linkColumnId);
            }
        });
    }

    /**
     * Data for TestSteps table scattered over several columns so we have to collect them into the proper structure
     */
    private void constructTestStepsFields(Map<String, Object> dataMap, ExcelSheetMappingSettingsModel model, @NotNull IWorkItem workItem, Set<FieldMetadata> fieldMetadataSet) {
        model.getStepsMapping().forEach((mappingKey, mappingValue) ->
                processStepMapping(mappingKey, mappingValue, dataMap, workItem, fieldMetadataSet));
    }

    @SuppressWarnings("rawtypes")
    private void processStepMapping(String mappingKey, Map<String, String> mappingValue,
            Map<String, Object> dataMap, IWorkItem workItem, Set<FieldMetadata> fieldMetadataSet) {
        Map<String, Object> structureMap = new HashMap<>();

        FieldMetadata fieldMetadata = fieldMetadataSet.stream().filter(f -> f.getId().equals(mappingKey)).findFirst().orElse(null);
        if (fieldMetadata == null || !mappingValue.keySet().equals(fieldMetadata.getOptions().stream().map(Option::getKey).collect(Collectors.toSet()))) {
            throw new IllegalArgumentException("Test steps keys mismatch for the field '%s'. Check fields mapping.".formatted(mappingKey));
        }
        // Use options order from fieldMetadata to preserve the correct column order
        List<String> keys = fieldMetadata.getOptions().stream().map(Option::getKey).toList();
        structureMap.put(ITestSteps.KEY_KEYS, keys);

        List<Map<String, List<Text>>> steps = new ArrayList<>();
        // we take the number of rows in the table from the first column, assuming that all columns have the same number of rows as they are part of the same table
        Object firstKeyValue = dataMap.get(mappingValue.get(keys.getFirst()));
        // if the value is not a list, it means that the table has only one row
        int tableLength = firstKeyValue instanceof List list ? list.size() : 1;
        for (int i = 0; i < tableLength; i++) {
            final int index = i;
            List<Text> values = keys.stream().map(mappingValue::get).map(dataMap::get)
                    .map(columnValue -> columnValue instanceof List list ? list.get(index) : columnValue)
                    .map(nextRowValue -> Text.html(nextRowValue == null ? "" : String.valueOf(nextRowValue))).toList();
            steps.add(Map.of(ITestStep.KEY_VALUES, values));
        }
        structureMap.put(ITestSteps.KEY_STEPS, steps);

        // remove separate step items from mapping, from now we need only the structure we build
        keys.forEach(key -> dataMap.remove(mappingValue.get(key)));

        IStructure testSteps = polarionServiceExt.getTrackerService().getDataService().createStructureForTypeId(workItem, ITestSteps.STRUCTURE_ID, structureMap);
        dataMap.put(ExcelSheetMappingSettingsModel.TEST_STEPS_COLUMN_FILLER_PREFIX + mappingKey, testSteps);
    }

    private void setFieldValue(Object value, @NotNull FieldMetadata fieldMetadata, IWorkItem workItem, ExcelSheetMappingSettingsModel model, @NotNull String linkColumnId) {
        // The linkColumn field's value can't change, therefore it doesn't need to be overwritten.
        // However, it must be saved to the newly created work item otherwise sequential imports will produce several objects.
        String fieldId = fieldMetadata.getId();
        if (!IUniqueObject.KEY_ID.equals(fieldId) && (!linkColumnId.equals(fieldId) || !workItem.isPersisted()) &&
                (model.isOverwriteWithEmpty() || !isEmpty(value)) &&
                ensureValidValue(value, fieldMetadata) &&
                existingValueDiffers(workItem, fieldId, value, fieldMetadata, model.isUnlinkExisting())) {
            if (IWorkItem.KEY_LINKED_WORK_ITEMS.equals(fieldId)) {
                setLinkedWorkItems(workItem, value, model.isUnlinkExisting());
            } else {
                polarionServiceExt.setFieldValue(workItem, fieldId, prepareValue(value, fieldMetadata), model.getEnumsMapping());
            }
        } else if (IUniqueObject.KEY_ID.equals(fieldId) && !linkColumnId.equals(fieldId)) {
            // If the work item id is imported, it must be the Link Column. Its value also can't be set by imported data unlike other possible Link Column fields.
            throw new IllegalArgumentException("WorkItem id can only be imported if it is used as Link Column.");
        }
    }

    @VisibleForTesting
    void setLinkedWorkItems(IWorkItem workItem, Object value, boolean unlinkExisting) {
        List<LinkInfo> links = LinkInfo.fromString((String) value, workItem);
        List<LinkInfo> newLinks = links.stream().filter(link -> !link.containedIn(workItem)).toList();
        for (LinkInfo newLink : newLinks) {
            ILinkRoleOpt roleEnum = workItem.getProject().getWorkItemLinkRoleEnum().wrapOption(newLink.getRoleId(), workItem.getType());
            if (newLink.isExternal()) {
                workItem.addExternallyLinkedItem(URI.create(newLink.getWorkItemId()), roleEnum);
            } else {
                IWorkItem linkedWorkItem = polarionServiceExt.getWorkItem(newLink.getProjectId(), newLink.getWorkItemId());
                workItem.addLinkedItem(linkedWorkItem, roleEnum, null, false);
            }
        }

        if (unlinkExisting) {
            unlinkExistingExtraItems(workItem, links);
        }
    }

    private boolean hasExistingExtraItems(IWorkItem workItem, List<LinkInfo> linkInfos) {
        return workItem.getExternallyLinkedWorkItemsStructs().stream().anyMatch(l -> linkInfos.stream().filter(LinkInfo::isExternal).noneMatch(li -> Objects.equals(li.getRoleId(), l.getLinkRole().getId()) &&
                Objects.equals(li.getWorkItemId(), l.getLinkedWorkItemURI().getURI().toString()))) ||
                workItem.getLinkedWorkItemsStructsDirect().stream().anyMatch(s -> linkInfos.stream().filter(li -> !li.isExternal()).noneMatch(li -> Objects.equals(li.getRoleId(), s.getLinkRole().getId()) &&
                        Objects.equals(li.getProjectId(), s.getLinkedItem().getProjectId()) &&
                        Objects.equals(li.getWorkItemId(), s.getLinkedItem().getId())));
    }

    private void unlinkExistingExtraItems(IWorkItem workItem, List<LinkInfo> keepLinks) {
        List<IExternallyLinkedWorkItemStruct> externalToRemove = workItem.getExternallyLinkedWorkItemsStructs().stream()
                .filter(s -> keepLinks.stream().filter(LinkInfo::isExternal).noneMatch(li ->
                        Objects.equals(li.getRoleId(), s.getLinkRole().getId()) &&
                                Objects.equals(li.getWorkItemId(), s.getLinkedWorkItemURI().getURI().toString())))
                .toList();
        externalToRemove.forEach(s -> workItem.removeExternallyLinkedItem(s.getLinkedWorkItemURI().getURI(), s.getLinkRole()));

        List<ILinkedWorkItemStruct> structsToRemove = workItem.getLinkedWorkItemsStructsDirect().stream()
                .filter(s -> keepLinks.stream().filter(li -> !li.isExternal()).noneMatch(li ->
                        Objects.equals(li.getRoleId(), s.getLinkRole().getId()) &&
                                Objects.equals(li.getProjectId(), s.getLinkedItem().getProjectId()) &&
                                Objects.equals(li.getWorkItemId(), s.getLinkedItem().getId())))
                .toList();
        structsToRemove.forEach(s -> workItem.removeLinkedItem(s.getLinkedItem(), s.getLinkRole()));
    }

    @VisibleForTesting
    Object prepareValue(Object value, @NotNull FieldMetadata fieldMetadata) {
        if (FieldType.RICH.getType().equals(fieldMetadata.getType()) && value instanceof String richTextString) {
            return HTMLHelper.convertPlainToHTML(richTextString);
        }
        return value;
    }

    @VisibleForTesting
    boolean ensureValidValue(Object value, FieldMetadata fieldMetadata) {
        if (FieldType.BOOLEAN.getType().equals(fieldMetadata.getType()) &&
                (!(value instanceof String stringValue) || !("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)))) {
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
    boolean existingValueDiffers(IWorkItem workItem, String fieldId, Object newValue, FieldMetadata fieldMetadata, boolean unlinkExisting) {
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
        } else if (IWorkItem.KEY_LINKED_WORK_ITEMS.equals(fieldId)) {
            if (newValue instanceof String || newValue == null) {
                List<LinkInfo> linksToInsert = newValue == null ? List.of() : LinkInfo.fromString((String) newValue, workItem);
                return linksToInsert.stream().anyMatch(linkInfo -> !linkInfo.containedIn(workItem)) ||
                        unlinkExisting && hasExistingExtraItems(workItem, linksToInsert);
            } else {
                throw new IllegalArgumentException(IWorkItem.KEY_LINKED_WORK_ITEMS + " can be set using string value only");
            }
        } else {
            return !Objects.equals(newValue, existingValue);
        }
    }

    @NotNull
    @VisibleForTesting
    FieldMetadata getFieldMetadataForField(Set<FieldMetadata> fieldMetadataSet, String fieldId) {
        return fieldMetadataSet.stream()
                .filter(m -> m.getId().equals(fieldId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Cannot find field metadata for ID '%s'".formatted(fieldId)));
    }

    private boolean isEmpty(Object value) {
        return value == null || (value instanceof String stringValue && StringUtils.isEmptyTrimmed(stringValue));
    }

    private static class ImportContext {
        ITrackerProject project;
        ITypeOpt workItemType;
        ExcelSheetMappingSettingsModel settings;
        List<String> logs = new ArrayList<>();
        List<String> updatedIds = new ArrayList<>();
        List<String> createdIds = new ArrayList<>();
        List<String> unchangedIds = new ArrayList<>();
        List<String> skippedIds = new ArrayList<>();
        StopWatch stopWatch = StopWatch.createStarted();

        public ImportContext(ITrackerProject project, ITypeOpt workItemType, ExcelSheetMappingSettingsModel settings) {
            this.project = project;
            this.workItemType = workItemType;
            this.settings = settings;
        }

        void log(String message) {
            logs.add(stopWatch.formatTime() + " " + message);
        }

        ImportResult toResult() {
            return ImportResult.builder()
                    .updatedIds(updatedIds)
                    .createdIds(createdIds)
                    .unchangedIds(unchangedIds)
                    .skippedIds(skippedIds)
                    .log(logs.stream().collect(Collectors.joining(System.lineSeparator())))
                    .build();
        }
    }
}
