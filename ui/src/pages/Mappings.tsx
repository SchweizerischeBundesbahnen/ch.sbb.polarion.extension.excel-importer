import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ConfigurationButtons,
  ConfigurationsPane,
  type ConfigurationsPaneHandle,
  PageLayout,
  RevisionsTable,
  SearchableSelect,
} from '@grigoriev/react-sbb-polarion';
import { toast } from 'sonner';
import MappingRow from '../components/MappingRow';
import type { MappingRowData } from '../components/MappingRow';
import OptionsMappingModal from '../components/OptionsMappingModal';
import { getProjectIdFromScope, getScope } from '../services/scope';
import useSettings from '../services/settings';
import type { FieldMetadata, MappingSettings, Revision, WorkItemType } from '../types';

const LINKED_WORK_ITEMS = 'linkedWorkItems';
const TEST_STEPS_PREFIX = 'testSteps|';

let rowCounter = 0;
function newUid(): string {
  rowCounter += 1;
  return `row-${rowCounter}`;
}

function isEnumField(field: FieldMetadata | null): boolean {
  return !!field && !!field.options && field.options.length > 0 && field.type?.structTypeId !== 'TestSteps';
}

function isTestStepsField(field: FieldMetadata | null): boolean {
  return !!field && !!field.options && field.options.length > 0 && field.type?.structTypeId === 'TestSteps';
}

export default function Mappings() {
  const settings = useSettings();
  const scope = getScope();
  const projectId = getProjectIdFromScope(scope);
  const paneRef = useRef<ConfigurationsPaneHandle>(null);

  // Data loaded from the server.
  const [workItemTypes, setWorkItemTypes] = useState<WorkItemType[]>([]);
  const [wiTypesLoaded, setWiTypesLoaded] = useState(false);
  const [fields, setFields] = useState<FieldMetadata[]>([]);

  // Form state (the mapping configuration content).
  const [sheetName, setSheetName] = useState('Sheet1');
  const [startFromRow, setStartFromRow] = useState<number>(1);
  const [overwriteWithEmpty, setOverwriteWithEmpty] = useState(false);
  const [ignoreUnknown, setIgnoreUnknown] = useState(false);
  const [selectedWiType, setSelectedWiType] = useState('');
  const [rootRows, setRootRows] = useState<MappingRowData[]>([]);
  // Test-steps sub-row columns: fieldId -> optionKey -> column value.
  const [stepColumns, setStepColumns] = useState<Record<string, Record<string, string>>>({});
  const [enumsMapping, setEnumsMapping] = useState<Record<string, Record<string, string>>>({});
  const [unlinkExisting, setUnlinkExisting] = useState(false);
  const [linkColumn, setLinkColumn] = useState('');

  // UI state.
  const [selectedConfig, setSelectedConfig] = useState<string | null>(null);
  const [editingName, setEditingName] = useState(false);
  const [showRevisions, setShowRevisions] = useState(false);
  const [revisionsToken, setRevisionsToken] = useState(0);
  const [optionsFieldId, setOptionsFieldId] = useState<string | null>(null);
  const [loadingError, setLoadingError] = useState(false);

  const getField = useCallback(
    (fieldId: string): FieldMetadata | null => fields.find((f) => f.id === fieldId) ?? null,
    [fields],
  );

  // Load work item types first (mirrors createWITypesDropdown); the ConfigurationsPane mounts only
  // afterwards so a loaded configuration's fields resolve against a known type.
  useEffect(() => {
    let cancelled = false;
    setLoadingError(false);
    settings
      .loadWorkItemTypes(projectId)
      .then((types) => {
        if (cancelled) return;
        setWorkItemTypes(types);
        setWiTypesLoaded(true);
      })
      .catch(() => {
        if (cancelled) return;
        setLoadingError(true);
        setWiTypesLoaded(true);
      });
    return () => {
      cancelled = true;
    };
  }, [settings, projectId]);

  // (Re)load the fields whenever the selected work item type changes, then drop any row field
  // selection that is no longer valid for the new type (mirrors updateFieldDropdowns).
  useEffect(() => {
    let cancelled = false;
    if (!selectedWiType) {
      setFields([]);
      return;
    }
    settings
      .loadFields(projectId, selectedWiType)
      .then((list) => {
        if (cancelled) return;
        setFields(list);
        const validIds = new Set(list.map((f) => f.id));
        setRootRows((rows) => rows.map((r) => (validIds.has(r.fieldId) ? r : { ...r, fieldId: '' })));
      })
      .catch(() => {
        if (!cancelled) setLoadingError(true);
      });
    return () => {
      cancelled = true;
    };
  }, [settings, projectId, selectedWiType]);

  // Apply a loaded configuration's content to the form (mirrors parseAndSetSettings).
  const applySettings = useCallback((model: MappingSettings) => {
    setSheetName(model.sheetName ?? '');
    setStartFromRow(model.startFromRow ?? 1);
    setOverwriteWithEmpty(!!model.overwriteWithEmpty);
    setIgnoreUnknown(!!model.ignoreUnknown);
    setSelectedWiType(model.defaultWorkItemType ?? '');
    setEnumsMapping(model.enumsMapping ?? {});
    setStepColumns(model.stepsMapping ?? {});
    setUnlinkExisting(!!model.unlinkExisting);
    const columns = model.columnsMapping ?? {};
    setRootRows(
      Object.entries(columns).map(([key, fieldId]) => ({
        uid: newUid(),
        // Test-steps filler keys carry no real column; the column becomes disabled/empty anyway.
        column: key.startsWith(TEST_STEPS_PREFIX) ? '' : key,
        fieldId,
      })),
    );
    setLinkColumn(model.linkColumn ?? '');
  }, []);

  const handleContentLoaded = useCallback(
    (model: MappingSettings) => {
      toast.dismiss();
      applySettings(model);
    },
    [applySettings],
  );

  const handleSelectedChange = useCallback((name: string | null) => {
    setSelectedConfig(name);
    setRevisionsToken((t) => t + 1);
  }, []);

  // --- Mapping row operations ---

  const addRow = () => {
    if (!selectedWiType.trim()) {
      toast.error('Select workitem type first to start creating a mapping');
      return;
    }
    setRootRows((rows) => [...rows, { uid: newUid(), column: '', fieldId: '' }]);
  };

  const removeRow = (uid: string) => {
    setRootRows((rows) => rows.filter((r) => r.uid !== uid));
  };

  const setRowColumn = (uid: string, value: string) => {
    setRootRows((rows) => rows.map((r) => (r.uid === uid ? { ...r, column: value } : r)));
  };

  const setRowField = (uid: string, fieldId: string) => {
    setRootRows((rows) =>
      rows.map((r) => {
        if (r.uid === uid) {
          return { ...r, fieldId };
        }
        // Deselect the same field in other rows to avoid duplicate selection.
        if (fieldId && r.fieldId === fieldId) {
          return { ...r, fieldId: '' };
        }
        return r;
      }),
    );
  };

  const setStepColumn = (fieldId: string, optionKey: string, value: string) => {
    setStepColumns((prev) => ({ ...prev, [fieldId]: { ...(prev[fieldId] ?? {}), [optionKey]: value } }));
  };

  // --- Link column options: every non-empty column value in the table (root + step columns) ---
  const linkColumnOptions = useMemo(() => {
    const names = new Set<string>();
    for (const row of rootRows) {
      if (row.column && row.column.trim()) names.add(row.column);
    }
    for (const perField of Object.values(stepColumns)) {
      for (const value of Object.values(perField)) {
        if (value && value.trim()) names.add(value);
      }
    }
    return Array.from(names).map((name) => ({ id: name, name }));
  }, [rootRows, stepColumns]);

  // --- Options mapping modal ---
  const openOptions = (uid: string) => {
    const row = rootRows.find((r) => r.uid === uid);
    if (row) setOptionsFieldId(row.fieldId);
  };
  const acceptOptions = (mapping: Record<string, string>) => {
    if (optionsFieldId) {
      setEnumsMapping((prev) => ({ ...prev, [optionsFieldId]: mapping }));
    }
    setOptionsFieldId(null);
  };

  // --- Validation (mirrors validateBeforeSave / validateMapping) ---
  const validate = (): string | null => {
    if (!sheetName || sheetName.trim().length === 0) {
      return 'Sheet name cannot be empty';
    }
    if (startFromRow === undefined || startFromRow === null || Number.isNaN(startFromRow) || startFromRow < 1) {
      return 'Row number must be a positive integer value';
    }
    const seenColumns: string[] = [];
    for (const row of rootRows) {
      const field = getField(row.fieldId);
      const columnDisabled = isTestStepsField(field);
      if (!columnDisabled) {
        if (!row.column) {
          return 'Column name cannot be empty';
        }
        if (seenColumns.includes(row.column)) {
          return `Cannot have duplicate column name '${row.column}'`;
        }
        seenColumns.push(row.column);
        if (!row.fieldId) {
          return `Mapping for column '${row.column}' cannot be empty`;
        }
      }
      // Test-steps sub-rows: their column inputs are editable, so validate them too.
      if (isTestStepsField(field)) {
        for (const option of field?.options ?? []) {
          const value = stepColumns[row.fieldId]?.[option.key] ?? '';
          if (!value) {
            return 'Column name cannot be empty';
          }
          if (seenColumns.includes(value)) {
            return `Cannot have duplicate column name '${value}'`;
          }
          seenColumns.push(value);
        }
      }
    }
    if (!linkColumn || linkColumn.trim().length === 0) {
      return 'Link column cannot be empty';
    }
    return null;
  };

  // --- Build the save payload (mirrors getColumnToFieldMapping / getTestStepsMapping) ---
  const buildPayload = (): MappingSettings => {
    const columnsMapping: Record<string, string> = {};
    const stepsMapping: Record<string, Record<string, string>> = {};
    for (const row of rootRows) {
      if (!row.fieldId) continue;
      const field = getField(row.fieldId);
      if (isTestStepsField(field)) {
        const key = `${TEST_STEPS_PREFIX}${row.fieldId}`;
        columnsMapping[key] = row.fieldId;
        const perStep: Record<string, string> = {};
        for (const option of field?.options ?? []) {
          perStep[option.key] = stepColumns[row.fieldId]?.[option.key] ?? '';
        }
        stepsMapping[row.fieldId] = perStep;
      } else {
        columnsMapping[row.column || `${TEST_STEPS_PREFIX}${row.fieldId}`] = row.fieldId;
      }
    }
    return {
      sheetName,
      startFromRow,
      overwriteWithEmpty,
      ignoreUnknown,
      columnsMapping,
      stepsMapping,
      enumsMapping,
      defaultWorkItemType: selectedWiType,
      linkColumn,
      unlinkExisting,
    };
  };

  // --- Toolbar actions ---
  const handleSave = async () => {
    const error = validate();
    if (error) {
      toast.error(error);
      return;
    }
    if (!selectedConfig) {
      toast.error('No configuration selected to save');
      return;
    }
    toast.dismiss();
    try {
      await settings.saveContent(selectedConfig, scope, buildPayload());
      // Reload first: it re-selects the config and reloads its content, which fires
      // handleContentLoaded -> toast.dismiss(). Showing the success toast AFTER the reload keeps it
      // from being dismissed by that reload.
      await paneRef.current?.reloadNames(selectedConfig);
      setRevisionsToken((t) => t + 1);
      toast.success('Data successfully saved.');
    } catch (e) {
      toast.error((e as Error).message || 'Error occurred during saving the data.');
    }
  };

  const handleCancel = async () => {
    if (!selectedConfig) return;
    if (!window.confirm('Cancel editing and revert to the last persisted state?')) return;
    try {
      const model = await settings.loadContent(selectedConfig, scope);
      applySettings(model);
      toast.dismiss();
    } catch {
      setLoadingError(true);
    }
  };

  const handleRevertToRevision = async (revision: Revision) => {
    if (!selectedConfig) return;
    try {
      const model = await settings.loadContent(selectedConfig, scope, revision.name);
      applySettings(model);
      toast.success(`Reverted to revision ${revision.name}. Don't forget to save the configuration.`);
    } catch {
      setLoadingError(true);
    }
  };

  const fieldOptions = useMemo(() => fields.map((f) => ({ id: f.id, name: f.id })), [fields]);
  const wiTypeOptions = useMemo(() => workItemTypes.map((t) => ({ id: t.id, name: t.name })), [workItemTypes]);
  const optionsField = optionsFieldId ? getField(optionsFieldId) : null;

  if (!wiTypesLoaded) {
    return (
      <PageLayout title="Mappings">
        <p>Loading...</p>
      </PageLayout>
    );
  }

  return (
    <PageLayout title="Mappings">
      {loadingError && (
        <div className="alert alert-error">
          Error occurred loading the data. Be sure Polarion is started and accessible, and that the scope is a project.
        </div>
      )}

      <ConfigurationsPane
        ref={paneRef}
        scope={scope}
        service={settings}
        cookieKey="selected-configuration-mappings"
        label="mapping"
        onContentLoaded={handleContentLoaded}
        onSelectedChange={handleSelectedChange}
        onEditingNameChange={setEditingName}
      />

      <div className={editingName ? 'mappings-form dimmed' : 'mappings-form'}>
        <h2>General Settings</h2>
        <table className="general-settings">
          <tbody>
            <tr>
              <td>
                <label htmlFor="sheet-name">Import rows from sheet:</label>
              </td>
              <td>
                <input id="sheet-name" type="text" value={sheetName} onChange={(e) => setSheetName(e.target.value)} />
              </td>
            </tr>
            <tr>
              <td>
                <label htmlFor="start-from-row">Start import from row:</label>
              </td>
              <td>
                <input
                  id="start-from-row"
                  type="number"
                  min={1}
                  step={1}
                  value={Number.isNaN(startFromRow) ? '' : startFromRow}
                  onChange={(e) => setStartFromRow(parseInt(e.target.value, 10))}
                />
              </td>
            </tr>
            <tr>
              <td colSpan={2}>
                <input
                  id="overwrite"
                  type="checkbox"
                  checked={overwriteWithEmpty}
                  onChange={(e) => setOverwriteWithEmpty(e.target.checked)}
                />
                <label
                  htmlFor="overwrite"
                  title="Determines whether or not an empty value from the imported excel file should overwrite (and thus delete) an existing value of the work item field."
                >
                  Overwrite with empty values
                </label>
              </td>
            </tr>
            <tr>
              <td colSpan={2}>
                <input
                  id="ignore-unknown"
                  type="checkbox"
                  checked={ignoreUnknown}
                  onChange={(e) => setIgnoreUnknown(e.target.checked)}
                />
                <label
                  htmlFor="ignore-unknown"
                  title="Silently skip unresolvable items (e.g. missing linked work items) instead of failing the import."
                >
                  Ignore unknown/nonexistent values
                </label>
              </td>
            </tr>
          </tbody>
        </table>

        <h2 className="align-left">Workitem Type To Create</h2>
        <div id="workitem-types-container" className="wi-types-row">
          <label>Import rows as: </label>
          <SearchableSelect
            value={selectedWiType}
            onChange={setSelectedWiType}
            options={wiTypeOptions}
            allowEmpty
            placeholder=""
          />
        </div>

        <h2 className="align-left">Column Name To Workitem Field Mapping</h2>
        <table id="mapping-table">
          <tbody>
            {rootRows.map((row) => {
              const field = getField(row.fieldId);
              const testSteps = isTestStepsField(field);
              const rowEls = [
                <MappingRow
                  key={row.uid}
                  row={row}
                  fieldOptions={fieldOptions}
                  columnDisabled={testSteps}
                  showOptionsButton={isEnumField(field)}
                  showUnlink={row.fieldId === LINKED_WORK_ITEMS}
                  unlinkExisting={unlinkExisting}
                  onColumnChange={setRowColumn}
                  onFieldChange={setRowField}
                  onRemove={removeRow}
                  onOpenOptions={openOptions}
                  onUnlinkChange={setUnlinkExisting}
                />,
              ];
              if (testSteps) {
                for (const option of field?.options ?? []) {
                  const subUid = `${row.uid}:${option.key}`;
                  rowEls.push(
                    <MappingRow
                      key={subUid}
                      row={{
                        uid: subUid,
                        column: stepColumns[row.fieldId]?.[option.key] ?? '',
                        fieldId: '',
                        parentUid: row.uid,
                        parentFieldId: row.fieldId,
                        optionKey: option.key,
                        stepName: option.name,
                      }}
                      fieldOptions={fieldOptions}
                      columnDisabled={false}
                      showOptionsButton={false}
                      showUnlink={false}
                      unlinkExisting={unlinkExisting}
                      onColumnChange={(_uid, value) => setStepColumn(row.fieldId, option.key, value)}
                      onFieldChange={() => {}}
                      onRemove={() => {}}
                      onOpenOptions={() => {}}
                      onUnlinkChange={() => {}}
                    />,
                  );
                }
              }
              return rowEls;
            })}
            <tr id="add-button">
              <td colSpan={3}>
                <button
                  type="button"
                  className="sbb-btn mapping-icon-button"
                  title="Add"
                  aria-label="Add"
                  onClick={addRow}
                >
                  <span className="sbb-icon-table-plus" role="img" aria-label="Add" />
                </button>
              </td>
            </tr>
          </tbody>
        </table>

        <h2 className="align-left">Link Column</h2>
        <div id="link-column-container" className="link-column-row">
          <label>Column name to link Excel row with Polarion workitem: </label>
          <SearchableSelect
            value={linkColumn}
            onChange={setLinkColumn}
            options={linkColumnOptions}
            allowEmpty
            placeholder=""
          />
        </div>

        {/* No onRevertToDefault: the Mappings page hides the Default button (there is no default). */}
        <ConfigurationButtons
          onSave={handleSave}
          onCancel={handleCancel}
          onToggleRevisions={() => setShowRevisions((v) => !v)}
          revisionsShown={showRevisions}
        />

        {showRevisions && selectedConfig && (
          <RevisionsTable
            name={selectedConfig}
            scope={scope}
            reloadToken={revisionsToken}
            loadRevisions={settings.loadRevisions}
            onRevert={handleRevertToRevision}
          />
        )}
      </div>

      <div className="quick-help">
        <h2 className="align-left">Quick Help</h2>
        <div className="quick-help-text">
          <h3>General Settings</h3>
          <p>
            This section contains inputs with the Excel sheet name (<code>Sheet1</code> by default) and the row number
            (1 by default) to import data from.
          </p>
          <h3>Workitem Type To Create</h3>
          <p>This section contains a combobox with workitem types available for the current project.</p>
          <p>
            In case if there is no workitem found by given identifier - it will be created using this type. This means
            that the extension can update any type in the current project (not only the one selected in this combobox).
          </p>
          <h3>Column Name To Workitem Field Mapping</h3>
          <p>This section contains comboboxes for mapping Excel column names to Polarion workitem fields.</p>
          <p>
            <strong>Note:</strong> If a mandatory Polarion field is not mapped, an error will occur during the import
            process.
          </p>
          <p>
            If a mapped column contains an empty value, this will result in an error unless explicitly allowed in the
            mapping configuration using <strong>(empty)</strong> meta value.
          </p>
          <h3>Link Column</h3>
          <p>
            This section contains a combobox for linking Excel rows to existing Polarion workitems for data updates.
          </p>
          <h3>Date and Time Formats</h3>
          <p>
            If date, time, or date-time values are stored in text format, the importer will attempt to parse them using
            supported ISO date format patterns:
          </p>
          <ul>
            <li>
              Date-time: <code>yyyy-MM-ddTHH:mm:ss</code>
            </li>
            <li>
              Date only: <code>yyyy-MM-dd</code>
            </li>
            <li>
              Time only: <code>HH:mm:ss</code> or <code>HH:mm</code>
            </li>
          </ul>
          <h3>Duration Fields</h3>
          <p>For duration-type fields only days and hours are supported due to Polarion limitations.</p>
          <p>
            Examples (d=days, h=hours): <code>1d</code>, <code>3d</code>, <code>1/2h</code>, <code>2 1/2h</code>,{' '}
            <code>3d 1h</code>.
          </p>
        </div>
      </div>

      <OptionsMappingModal
        open={optionsFieldId !== null}
        field={optionsField}
        mapping={optionsFieldId ? (enumsMapping[optionsFieldId] ?? {}) : {}}
        onAccept={acceptOptions}
        onCancel={() => setOptionsFieldId(null)}
      />
    </PageLayout>
  );
}
