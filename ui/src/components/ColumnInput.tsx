import { useEffect, useRef } from 'react';
import { type SearchableDropdownInstance, createEditableSelect } from '@grigoriev/react-sbb-polarion';

/** Only Latin letters, upper-cased — matches the legacy ColumnInput sanitisation. */
function sanitize(value: string): string {
  return value.replace(/[^A-Za-z]/g, '').toUpperCase();
}

interface ColumnInputProps {
  value: string;
  onChange: (value: string) => void;
  disabled?: boolean;
  placeholder?: string;
}

/**
 * Excel column-identifier picker: an editable (free-text) SearchableDropdown wrapping a text input,
 * built from the shared `createEditableSelect` bundled in react-sbb-polarion (no runtime fetch). The
 * user can type any identifier (A, B, …, AA) or pick a letter from the suggestions; input is sanitised
 * to upper-case Latin letters.
 *
 * The wrapped <input> is the source of truth. The dropdown mirrors the committed value back onto it
 * and dispatches a native `change` event — which is why we listen for `change` directly instead of
 * React's onChange (React maps a text input's onChange to the `input` event, which the dropdown does
 * not fire).
 */
export default function ColumnInput({ value, onChange, disabled = false, placeholder = '' }: ColumnInputProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const sdRef = useRef<SearchableDropdownInstance | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    const input = inputRef.current;
    if (!input) return;

    // Seed the initial value BEFORE wrapping: the editable dropdown copies the wrapped input's value
    // into its trigger at construction, so a value set afterwards would not appear.
    input.value = sanitize(value);

    const emit = () => {
      const sanitized = sanitize(input.value);
      if (sanitized !== input.value) {
        input.value = sanitized;
      }
      onChangeRef.current(sanitized);
    };
    input.addEventListener('change', emit);
    input.addEventListener('input', emit);

    const columns = Array.from({ length: 26 }, (_, i) => {
      const letter = String.fromCharCode('A'.charCodeAt(0) + i);
      return { value: letter, label: letter };
    });
    sdRef.current = createEditableSelect(input, { placeholder, inputFilter: sanitize, items: columns });

    return () => {
      input.removeEventListener('change', emit);
      input.removeEventListener('input', emit);
      if (sdRef.current) {
        sdRef.current.destroy();
        sdRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Reflect programmatic value changes (loading a config, clearing on Test Steps) onto both the
  // wrapped input and the dropdown's visible trigger — the editable dropdown does not observe
  // programmatic value changes.
  useEffect(() => {
    const input = inputRef.current;
    if (!input) return;
    const next = sanitize(value);
    if (input.value !== next) {
      input.value = next;
    }
    const trigger = sdRef.current?.trigger as HTMLInputElement | undefined;
    if (trigger) {
      trigger.value = next;
    }
  }, [value]);

  return (
    <input
      ref={inputRef}
      type="text"
      className="excel-column-input"
      maxLength={5}
      autoComplete="off"
      disabled={disabled}
      placeholder={placeholder}
    />
  );
}
