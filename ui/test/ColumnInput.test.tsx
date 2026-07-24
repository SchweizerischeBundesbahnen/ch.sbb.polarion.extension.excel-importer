import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import ColumnInput from '../src/components/ColumnInput';

// ColumnInput builds an editable (free-text) combobox from react-sbb-polarion's bundled
// createEditableSelect (no runtime fetch), so it upgrades in the test env too. It sanitizes input to
// upper-case Latin letters and mirrors programmatic value changes onto the dropdown trigger.

function Host({
  initial = '',
  disabled = false,
  onValue,
}: {
  initial?: string;
  disabled?: boolean;
  onValue?: (v: string) => void;
}) {
  const [v, setV] = useState(initial);
  return (
    <div className="sbb-ui">
      <button data-testid="set" onClick={() => setV('xy9')}>
        set
      </button>
      <ColumnInput
        value={v}
        disabled={disabled}
        onChange={(val) => {
          onValue?.(val);
          setV(val);
        }}
      />
    </div>
  );
}

const backing = () => document.querySelector<HTMLInputElement>('.excel-column-input')!;
const trigger = () => document.querySelector<HTMLInputElement>('.searchable-dropdown .sd-trigger')!;

afterEach(cleanup);

describe('ColumnInput', () => {
  it('upgrades the input into an editable dropdown seeded with the value', async () => {
    render(<Host initial="A" />);
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());
    expect(document.querySelector('.searchable-dropdown')!.classList.contains('editable')).toBe(true);
    expect(trigger().value).toBe('A');
  });

  it('sanitizes typed input to upper-case Latin letters and emits it', async () => {
    const onValue = vi.fn();
    render(<Host onValue={onValue} />);
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());
    // Commit a value on the backing input (the dropdown mirrors and dispatches `change` here).
    backing().value = 'ab3c';
    backing().dispatchEvent(new Event('change', { bubbles: true }));
    await vi.waitFor(() => expect(onValue).toHaveBeenCalledWith('ABC'));
    expect(backing().value).toBe('ABC');
  });

  it('reflects a programmatic value change onto the dropdown trigger', async () => {
    render(<Host initial="" />);
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());
    document.querySelector<HTMLButtonElement>('[data-testid="set"]')!.click(); // sets value to "xy9" -> "XY"
    await vi.waitFor(() => expect(trigger().value).toBe('XY'));
  });

  it('disables the input when disabled', async () => {
    render(<Host disabled />);
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());
    expect(backing().disabled).toBe(true);
  });

  it('uses default props and emits an already-sanitized value unchanged', async () => {
    const onValue = vi.fn();
    // Rendered directly (no disabled / placeholder) to exercise the default parameters.
    render(
      <div className="sbb-ui">
        <ColumnInput value="" onChange={onValue} />
      </div>,
    );
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());
    backing().value = 'AB'; // already sanitized -> emit takes the "unchanged" branch
    backing().dispatchEvent(new Event('change', { bubbles: true }));
    await vi.waitFor(() => expect(onValue).toHaveBeenCalledWith('AB'));
    expect(backing().disabled).toBe(false);
  });

  it('honors a provided placeholder', async () => {
    render(
      <div className="sbb-ui">
        <ColumnInput value="" placeholder="col" onChange={() => {}} />
      </div>,
    );
    await vi.waitFor(() => expect(document.querySelector('.searchable-dropdown')).not.toBeNull());
    expect(backing().getAttribute('placeholder')).toBe('col');
  });
});
