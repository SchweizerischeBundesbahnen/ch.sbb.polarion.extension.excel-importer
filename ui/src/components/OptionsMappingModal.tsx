import { useEffect, useState } from 'react';
import { Modal } from '@grigoriev/react-sbb-polarion';
import type { FieldMetadata } from '../types';

interface OptionsMappingModalProps {
  open: boolean;
  /** The enum field whose options are being mapped (null while closed). */
  field: FieldMetadata | null;
  /** Current mapping for this field: option key -> comma-separated alternative values. */
  mapping: Record<string, string>;
  onAccept: (mapping: Record<string, string>) => void;
  onCancel: () => void;
}

/**
 * The "Options mapping" dialog for an enum field: for each option, a text input holding a
 * comma-separated list of alternative Excel values that map to that option. Mirrors the legacy
 * micromodal popup, including the `(empty)` keyword hint.
 */
export default function OptionsMappingModal({ open, field, mapping, onAccept, onCancel }: OptionsMappingModalProps) {
  const [values, setValues] = useState<Record<string, string>>({});

  // Seed local input state from the stored mapping whenever the dialog opens for a field.
  useEffect(() => {
    if (open && field) {
      const seed: Record<string, string> = {};
      for (const option of field.options ?? []) {
        seed[option.key] = mapping[option.key] ?? '';
      }
      setValues(seed);
    }
  }, [open, field, mapping]);

  const handleAccept = () => {
    onAccept({ ...values });
  };

  return (
    <Modal
      open={open && !!field}
      title="Options mapping"
      okText="Accept"
      cancelText="Cancel"
      onOk={handleAccept}
      onCancel={onCancel}
    >
      <span className="option-mapping-hint-wrapper">
        <span className="option-mapping-hint">
          Use <b>(empty)</b> keyword to map empty column value to some specific option.
        </span>
      </span>
      <table className="options-mapping-table">
        <tbody>
          {(field?.options ?? []).map((option) => (
            <tr key={option.key} className="options-mapping-row">
              <td className="option-mapping-key-column">
                <span className="fs-14 option-mapping-key" title={option.key}>
                  {option.key}
                </span>
              </td>
              <td>
                <input
                  type="text"
                  className="fs-14 option-mapping-value"
                  placeholder={option.name}
                  title={`Comma-separated list of alternative values, which will be mapped to the option "${option.key}"`}
                  value={values[option.key] ?? ''}
                  onChange={(e) => setValues((prev) => ({ ...prev, [option.key]: e.target.value }))}
                />
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </Modal>
  );
}
