import { useState } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import ColumnInput from '../src/components/ColumnInput';
import MappingRow from '../src/components/MappingRow';
import type { MappingRowData } from '../src/components/MappingRow';
import OptionsMappingModal from '../src/components/OptionsMappingModal';
import { fetchProjects } from '../src/services/projects';
import useRemote from '../src/services/useRemote';
import type { WorkItemField } from '../src/types';

// Branches the page-level tests never reach: the VITE_BEARER_TOKEN path (only ever set in a developer's
// .env.local), and the secondary states of the mapping components - a Test Steps sub-row, an option
// dialog for a field without options, and ColumnInput reacting to a programmatic value change.

// Reset between tests: `render` commits asynchronously, so waiting only for the hook to be truthy
// would otherwise pass instantly on the previous test's value.
let api: ReturnType<typeof useRemote> | undefined;
function Capture() {
  api = useRemote();
  return null;
}

afterEach(() => {
  cleanup();
  api = undefined;
  vi.unstubAllGlobals();
  vi.unstubAllEnvs();
  vi.restoreAllMocks();
});

describe('bearer-token authentication', () => {
  it('useRemote switches to the /api base and sends the bearer header', async () => {
    vi.stubEnv('VITE_BEARER_TOKEN', 'tok-123');
    const fetchMock = vi.fn(async () => new Response('{}', { status: 200 }));
    vi.stubGlobal('fetch', fetchMock);
    render(<Capture />);
    await vi.waitFor(() => expect(api).toBeTruthy());
    await api!.sendRequest({ method: 'GET', url: '/version' });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(String(url)).toContain('/rest/api/version');
    expect((init.headers as Record<string, string>)['Authorization']).toBe('Bearer tok-123');
  });

  it('fetchProjects sends the Authorization header only with a token', async () => {
    const okEmpty = () =>
      vi.fn(
        async () =>
          new Response(JSON.stringify({ data: [] }), { status: 200, headers: { 'Content-Type': 'application/json' } }),
      );
    const authHeader = (m: ReturnType<typeof okEmpty>) =>
      (m.mock.calls[0][1] as RequestInit | undefined)?.headers?.['Authorization' as never];

    let fetchMock = okEmpty();
    vi.stubGlobal('fetch', fetchMock);
    await fetchProjects();
    expect(authHeader(fetchMock)).toBeUndefined();

    vi.stubEnv('VITE_BEARER_TOKEN', 'tok-123');
    fetchMock = okEmpty();
    vi.stubGlobal('fetch', fetchMock);
    await fetchProjects();
    expect(authHeader(fetchMock)).toBe('Bearer tok-123');
  });
});

describe('MappingRow sub-rows', () => {
  // A sub-row is identified by parentUid: it maps a column to one fixed Test Steps step field.
  const row: MappingRowData = {
    uid: 's1',
    column: 'A',
    fieldId: 'expectedResult',
    parentUid: 'r1',
    parentFieldId: 'testSteps',
    stepName: 'Expected Result',
  };

  it('renders a Test Steps sub-row with a read-only field name', async () => {
    render(
      <table>
        <tbody>
          <MappingRow
            row={row}
            fieldOptions={[]}
            columnDisabled={false}
            showOptionsButton={false}
            showUnlink={false}
            unlinkExisting={false}
            onColumnChange={() => {}}
            onFieldChange={() => {}}
            onRemove={() => {}}
            onOpenOptions={() => {}}
            onUnlinkChange={() => {}}
          />
        </tbody>
      </table>,
    );
    const input = (await vi.waitFor(() => {
      const el = document.querySelector<HTMLInputElement>('input.field-name');
      expect(el).not.toBeNull();
      return el!;
    }))!;
    // A sub-row's field name is dictated by the step definition, so it is shown but not editable.
    expect(input.value).toBe('Expected Result');
    expect(input.readOnly).toBe(true);
    expect(input.disabled).toBe(true);
  });
});

describe('OptionsMappingModal', () => {
  it('opens with an empty form for a field that declares no options', async () => {
    const field = { id: 'status', name: 'Status' } as WorkItemField;
    render(<OptionsMappingModal open field={field} mapping={{}} onSave={() => {}} onCancel={() => {}} />);
    await vi.waitFor(() => expect(document.querySelector('.options-mapping-table')).not.toBeNull());
    expect(document.querySelectorAll('.options-mapping-table input').length).toBe(0);
  });
});

describe('ColumnInput', () => {
  it('reflects a programmatic value change onto the input', async () => {
    // Loading another configuration replaces the value from the outside; the wrapped input must follow
    // (the editable dropdown does not observe programmatic changes on its own). Driven through a
    // stateful harness because the browser renderer has no rerender handle.
    function Harness() {
      const [value, setValue] = useState('A');
      return (
        <>
          <ColumnInput value={value} onChange={() => {}} />
          <button onClick={() => setValue('bc')}>load</button>
        </>
      );
    }
    render(<Harness />);
    const input = (await vi.waitFor(() => {
      const el = document.querySelector<HTMLInputElement>('input.excel-column-input');
      expect(el).not.toBeNull();
      return el!;
    }))!;
    expect(input.value).toBe('A');
    document.querySelector<HTMLButtonElement>('button')!.click();
    await vi.waitFor(() => expect(input.value).toBe('BC'));
  });

  it('sanitizes typed input to upper-case letters', async () => {
    const onChange = vi.fn();
    render(<ColumnInput value="" onChange={onChange} />);
    const input = (await vi.waitFor(() => {
      const el = document.querySelector<HTMLInputElement>('input.excel-column-input');
      expect(el).not.toBeNull();
      return el!;
    }))!;
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!.call(input, 'a1b!');
    input.dispatchEvent(new Event('change', { bubbles: true }));
    // Digits and punctuation are stripped and the rest upper-cased, both in the field and in the
    // reported value.
    expect(input.value).toBe('AB');
    expect(onChange).toHaveBeenLastCalledWith('AB');
  });
});
