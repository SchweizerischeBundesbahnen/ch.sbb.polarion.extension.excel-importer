import { useEffect, useRef } from 'react';
import { type SearchableDropdownInstance, createEditableSelect } from '@sbb-polarion/react-sbb-polarion';

/** Only Latin letters, upper-cased — matches the legacy ColumnInput sanitisation. */
function sanitize(value: string): string {
  return value.replace(/[^A-Za-z]/g, '').toUpperCase();
}

interface ColumnInputProps {
  /** Id of the wrapped input. A `<label htmlFor>` pointing at it names the dropdown trigger. */
  id?: string;
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
 * The wrapped <input> is React-controlled. The dropdown commits a value through the native setter and
 * fires `input` and `change`, so React's onChange sees it like typed text.
 */
export default function ColumnInput({ id, value, onChange, disabled = false, placeholder = '' }: ColumnInputProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const sdRef = useRef<SearchableDropdownInstance | null>(null);
  const sanitized = sanitize(value);

  useEffect(() => {
    const input = inputRef.current;
    if (!input) return;

    const columns = Array.from({ length: 26 }, (_, i) => {
      const letter = String.fromCharCode('A'.charCodeAt(0) + i);
      return { value: letter, label: letter };
    });
    // The dropdown reads a <select>'s own labels, but not those of a wrapped <input>: pass the label in.
    sdRef.current = createEditableSelect(input, {
      placeholder,
      inputFilter: sanitize,
      items: columns,
      label: input.labels?.[0],
    });

    return () => {
      if (sdRef.current) {
        sdRef.current.destroy();
        sdRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // The editable dropdown does not observe its wrapped input, so copy programmatic value changes
  // (loading a config, clearing on Test Steps) onto the visible trigger.
  useEffect(() => {
    const trigger = sdRef.current?.trigger as HTMLInputElement | undefined;
    if (trigger) {
      trigger.value = sanitized;
    }
  }, [sanitized]);

  return (
    <input
      ref={inputRef}
      id={id}
      type="text"
      value={sanitized}
      onChange={(e) => onChange(sanitize(e.target.value))}
      className="excel-column-input"
      maxLength={5}
      autoComplete="off"
      disabled={disabled}
      placeholder={placeholder}
    />
  );
}
