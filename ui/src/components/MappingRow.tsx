import { SearchableSelect, type SelectOption } from '@grigoriev/react-sbb-polarion';
import ColumnInput from './ColumnInput';

/** One row of the column-to-field mapping table. A root row maps an Excel column to a work item
 * field. A sub-row (present only under a Test Steps field) maps an Excel column to one fixed step
 * field; it has `parentUid` set and its field name is a disabled label. */
export interface MappingRowData {
  uid: string;
  column: string;
  fieldId: string;
  // Sub-row (Test Steps) fields:
  parentUid?: string;
  parentFieldId?: string;
  optionKey?: string;
  stepName?: string;
}

interface MappingRowProps {
  row: MappingRowData;
  /** Field options for the field-name dropdown (root rows only). */
  fieldOptions: SelectOption[];
  /** Whether the column input is disabled (Test Steps root rows clear + disable it). */
  columnDisabled: boolean;
  /** Show the "Options mapping" button (enum fields). */
  showOptionsButton: boolean;
  /** Show the "Unlink existing" checkbox (linkedWorkItems field). */
  showUnlink: boolean;
  unlinkExisting: boolean;
  onColumnChange: (uid: string, value: string) => void;
  onFieldChange: (uid: string, fieldId: string) => void;
  onRemove: (uid: string) => void;
  onOpenOptions: (uid: string) => void;
  onUnlinkChange: (checked: boolean) => void;
}

export default function MappingRow({
  row,
  fieldOptions,
  columnDisabled,
  showOptionsButton,
  showUnlink,
  unlinkExisting,
  onColumnChange,
  onFieldChange,
  onRemove,
  onOpenOptions,
  onUnlinkChange,
}: MappingRowProps) {
  const isSubRow = !!row.parentUid;

  return (
    <tr className="mapping-row">
      <td>
        {!isSubRow && (
          <button
            type="button"
            className="sbb-btn mapping-icon-button"
            title="Remove"
            aria-label="Remove"
            onClick={() => onRemove(row.uid)}
          >
            <span className="sbb-icon-table-minus" role="img" aria-label="Remove" />
          </button>
        )}
      </td>
      <td>
        <label>Column: </label>
        <div className="column-input-wrapper">
          <ColumnInput
            value={row.column}
            disabled={columnDisabled}
            onChange={(value) => onColumnChange(row.uid, value)}
          />
        </div>
      </td>
      <td>
        <label> Field Name: </label>
        {isSubRow ? (
          <input type="text" className="fs-14 field-name" value={row.stepName ?? ''} disabled readOnly />
        ) : (
          <>
            <SearchableSelect
              value={row.fieldId}
              onChange={(value) => onFieldChange(row.uid, value)}
              options={fieldOptions}
              allowEmpty
              placeholder="Select field name..."
            />
            {showOptionsButton && (
              <button
                type="button"
                className="sbb-btn sbb-btn--control options-mapping-button"
                onClick={() => onOpenOptions(row.uid)}
              >
                Options mapping
              </button>
            )}
            {showUnlink && (
              <label
                className="unlink-existing-label"
                title="If checked, existing links to work items not present in the imported data will be removed."
              >
                <input type="checkbox" checked={unlinkExisting} onChange={(e) => onUnlinkChange(e.target.checked)} />{' '}
                Unlink existing
              </label>
            )}
          </>
        )}
      </td>
    </tr>
  );
}
