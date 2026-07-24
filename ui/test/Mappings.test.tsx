import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { userEvent } from 'vitest/browser';
import App from '../src/App';
import { CONTENT, FIELDS, NAMES, REVISIONS, SCOPE, WORKITEM_TYPES, mappingsRoutes } from './fixtures/mappings';
import { type FetchMock, installFetchMock, jsonResponse } from './mockFetch';

// Behavior tests for the whole Mappings page, driven through the real App (feature router + Toaster).
// The extension's REST layer is mocked at the global fetch boundary (installFetchMock), so no Polarion
// is needed. Screenshot-free, so these run everywhere; the full-page look is covered in
// Mappings.visual.test.tsx.

const origUrl = window.location.pathname + window.location.search;
const setUrl = (search: string) => window.history.replaceState({}, '', search);

const triggers = () =>
  Array.from(document.querySelectorAll<HTMLInputElement>('.searchable-dropdown .sd-trigger')).map((t) => t.value);
const sbbButton = (label: string): HTMLButtonElement => {
  const b = Array.from(document.querySelectorAll<HTMLButtonElement>('.sbb-btn')).find(
    (x) => (x.textContent ?? '').trim() === label,
  );
  if (!b) throw new Error(`button "${label}" not found`);
  return b;
};
const sheetNameInput = () =>
  Array.from(document.querySelectorAll<HTMLInputElement>('input')).find((i) => i.value === 'Sheet1');

let fetchMock: FetchMock;

async function mountLoaded(routes = mappingsRoutes()) {
  fetchMock = installFetchMock(routes);
  setUrl(`?feature=mappings&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
  render(<App />);
  // Settled state: work-item type + both mapped fields resolved and their dropdowns upgraded.
  await vi.waitFor(
    () => {
      const t = triggers();
      expect(t).toContain('Requirement');
      expect(t).toContain('title');
      expect(t).toContain('description');
    },
    { timeout: 5000 },
  );
}

beforeEach(() => setUrl(origUrl));

const mappingRows = () => document.querySelectorAll('tr.mapping-row').length;
// The mapping add-row button is icon-only (empty text); ConfigurationsPane's "Add new" shares the
// sbb-icon-table-plus glyph but carries a label, so filter to the icon-only one.
const addRowButton = () =>
  Array.from(document.querySelectorAll('.sbb-icon-table-plus'))
    .map((el) => el.closest('button'))
    .find((b): b is HTMLButtonElement => !!b && (b.textContent ?? '').trim() === '');
const removeRowButton = () =>
  document.querySelector('.sbb-icon-table-minus')?.closest('button') as HTMLButtonElement | null;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-mappings=; path=/; max-age=0';
  // Remove the breadcrumb script BreadcrumbInjector appends to the top window.
  window.top?.document.querySelectorAll('script[id$="-breadcrumb-bridge"]').forEach((s) => s.remove());
});

describe('Mappings page', () => {
  it('loads the selected configuration, its content, and the work-item fields', async () => {
    await mountLoaded();
    // General Settings populated from the mocked content.
    expect(sheetNameInput()).toBeTruthy();
    // Work item type + both column->field mappings rendered.
    expect(triggers()).toContain('Requirement');
    expect(triggers().filter((v) => v === 'title' || v === 'description')).toHaveLength(2);
    // The config selector shows the auto-selected project-scoped configuration.
    expect(triggers()).toContain('requirements-import');
    // REST was hit for names, content, types and fields.
    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls.some((u) => /\/settings\/mappings\/names\?/.test(u))).toBe(true);
    expect(urls.some((u) => /\/workitem_types\/requirement\/fields/.test(u))).toBe(true);
  });

  it('shows a validation error and does not save when the sheet name is empty', async () => {
    await mountLoaded();
    await userEvent.clear(sheetNameInput()!);
    sbbButton('Save').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Sheet name cannot be empty'));
    // No content PUT was issued.
    expect(fetchMock.mock.calls.some((c) => (c[1]?.method ?? 'GET').toUpperCase() === 'PUT')).toBe(false);
  });

  it('saves the configuration (PUT content) and reports success', async () => {
    await mountLoaded();
    sbbButton('Save').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Data successfully saved.'));
    expect(fetchMock.mock.calls.some((c) => (c[1]?.method ?? 'GET').toUpperCase() === 'PUT')).toBe(true);
  });

  it('toggles the revisions table on the Revisions button', async () => {
    await mountLoaded();
    expect(document.querySelector('.revisions-table')).toBeNull();
    sbbButton('Revisions').click();
    await vi.waitFor(() => expect(document.querySelector('.revisions-table')).not.toBeNull());
    // Two mocked revisions render.
    await vi.waitFor(() => expect(document.querySelectorAll('.revisions-table tbody tr').length).toBe(2));
  });

  it('reverts the form to a selected revision', async () => {
    await mountLoaded();
    sbbButton('Revisions').click();
    await vi.waitFor(() => expect(document.querySelectorAll('.revert-to-revision-button').length).toBe(2));
    document.querySelector<HTMLButtonElement>('.revert-to-revision-button')!.click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Reverted to revision'));
  });

  it('surfaces a load error when reverting to a revision fails to fetch content', async () => {
    let contentCalls = 0;
    await mountLoaded([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        respond: () => (++contentCalls <= 1 ? jsonResponse(CONTENT) : jsonResponse({ errorMessage: 'gone' }, 500)),
      },
      { method: 'GET', match: /\/settings\/mappings\/names\/[^/]+\/revisions/, json: REVISIONS },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: FIELDS },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    ]);
    sbbButton('Revisions').click();
    await vi.waitFor(() => expect(document.querySelector('.revert-to-revision-button')).not.toBeNull());
    document.querySelector<HTMLButtonElement>('.revert-to-revision-button')!.click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Error occurred loading the data'));
  });

  it('surfaces a load error if reverting/cancelling fails to fetch content', async () => {
    let contentCalls = 0;
    await mountLoaded([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        // First load succeeds; the reload triggered by Cancel fails.
        respond: () => (++contentCalls <= 1 ? jsonResponse(CONTENT) : jsonResponse({ errorMessage: 'gone' }, 500)),
      },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: FIELDS },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    ]);
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    sbbButton('Cancel').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Error occurred loading the data'));
  });

  it('deselects a field when it is chosen again in another row (no duplicate field)', async () => {
    await mountLoaded();
    const fieldTriggers = () =>
      Array.from(document.querySelectorAll<HTMLInputElement>('.mapping-row .sd-trigger')).map((t) => t.value);
    expect(fieldTriggers()).toEqual(expect.arrayContaining(['title', 'description']));
    // Open row A's field dropdown (shows 'title') and pick 'description' (already used by row B).
    const titleTrigger = Array.from(document.querySelectorAll<HTMLInputElement>('.mapping-row .sd-trigger')).find(
      (t) => t.value === 'title',
    )!;
    mousedown(titleTrigger);
    await vi.waitFor(() => expect(document.querySelector('.sd-portal .option')).not.toBeNull());
    mousedown(
      Array.from(document.querySelectorAll<HTMLElement>('.sd-portal .option')).find(
        (o) => (o.textContent ?? '').trim() === 'description',
      )!,
    );
    // 'description' now appears once (row B was cleared to avoid the duplicate).
    await vi.waitFor(() => expect(fieldTriggers().filter((v) => v === 'description')).toHaveLength(1));
  });

  it('adds and removes a column->field mapping row', async () => {
    await mountLoaded();
    expect(mappingRows()).toBe(2);
    addRowButton()!.click();
    await vi.waitFor(() => expect(mappingRows()).toBe(3));
    removeRowButton()!.click(); // remove the first row
    await vi.waitFor(() => expect(mappingRows()).toBe(2));
  });

  it('reloads the persisted content when Cancel is confirmed', async () => {
    await mountLoaded();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const contentCalls = () => fetchMock.mock.calls.filter((c) => /\/content/.test(String(c[0]))).length;
    const before = contentCalls();
    sbbButton('Cancel').click();
    await vi.waitFor(() => expect(contentCalls()).toBeGreaterThan(before));
  });

  it('validates an empty column name before saving', async () => {
    await mountLoaded();
    await setColumn(0, ''); // clear the first row's column
    sbbButton('Save').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Column name cannot be empty'));
    expect(fetchMock.mock.calls.some((c) => (c[1]?.method ?? 'GET').toUpperCase() === 'PUT')).toBe(false);
  });

  it('validates the start row must be a positive integer', async () => {
    await mountLoaded();
    await userEvent.fill(document.querySelector<HTMLInputElement>('input[type="number"]')!, '0');
    sbbButton('Save').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Row number must be a positive integer value'));
  });

  it('treats an empty (NaN) start row as invalid', async () => {
    await mountLoaded();
    await userEvent.clear(document.querySelector<HTMLInputElement>('input[type="number"]')!); // -> parseInt('') = NaN
    sbbButton('Save').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Row number must be a positive integer value'));
  });

  it('toggles the general-settings checkboxes', async () => {
    await mountLoaded();
    const overwrite = document.querySelector<HTMLInputElement>('#overwrite')!;
    const ignore = document.querySelector<HTMLInputElement>('#ignore-unknown')!;
    expect(overwrite.checked).toBe(false);
    expect(ignore.checked).toBe(true); // seeded from the fixture content
    overwrite.click();
    ignore.click();
    await vi.waitFor(() => expect(overwrite.checked).toBe(true));
    await vi.waitFor(() => expect(ignore.checked).toBe(false));
  });

  it('dims the form while the configuration-name editor is open', async () => {
    await mountLoaded();
    expect(document.querySelector('.mappings-form.dimmed')).toBeNull();
    // ConfigurationsPane "Add new" opens the name editor -> onEditingNameChange(true) -> form dimmed.
    Array.from(document.querySelectorAll<HTMLButtonElement>('.sbb-btn'))
      .find((b) => (b.textContent ?? '').trim() === 'Add new')!
      .click();
    await vi.waitFor(() => expect(document.querySelector('.mappings-form.dimmed')).not.toBeNull());
  });

  it('renders Test Steps sub-rows for a Test Steps field', async () => {
    const stepsField = {
      id: 'teststeps',
      type: { structTypeId: 'TestSteps' },
      options: [
        { key: 'action', name: 'Action' },
        { key: 'expected', name: 'Expected Result' },
      ],
    };
    installFetchMock([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        json: { ...CONTENT, columnsMapping: { A: 'title', C: 'teststeps' } },
      },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: [...FIELDS, stepsField] },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    ]);
    setUrl(`?feature=mappings&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
    render(<App />);
    // One read-only step field-name input per Test Steps option (the sub-rows).
    await vi.waitFor(() => expect(document.querySelectorAll('input.field-name[readonly]').length).toBe(2));
    const stepValues = Array.from(document.querySelectorAll<HTMLInputElement>('input.field-name[readonly]')).map(
      (i) => i.value,
    );
    expect(stepValues.every((v) => v.length > 0)).toBe(true);
    // Editing a sub-row column feeds setStepColumn (the step-column map).
    const cols = columnInputs();
    const subRowColumn = cols[cols.length - 1];
    subRowColumn.value = 'X';
    subRowColumn.dispatchEvent(new Event('change', { bubbles: true }));
    await new Promise((r) => setTimeout(r, 0));
    expect(subRowColumn.value).toBe('X');
  });

  // The backing input of each ColumnInput (one per row); the editable dropdown mirrors + dispatches
  // `change` on it, so setting its value and firing `change` drives the column value through onChange.
  const columnInputs = () => Array.from(document.querySelectorAll<HTMLInputElement>('.excel-column-input'));
  async function setColumn(index: number, value: string) {
    const input = columnInputs()[index];
    input.value = value;
    input.dispatchEvent(new Event('change', { bubbles: true }));
    await new Promise((r) => setTimeout(r, 0)); // let the resulting React state update flush
  }

  async function mountRaw(routes: Parameters<typeof installFetchMock>[0]) {
    fetchMock = installFetchMock(routes);
    setUrl(`?feature=mappings&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.mappings-form')).not.toBeNull());
  }

  const mousedown = (el: Element) =>
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, cancelable: true, composed: true }));

  it('reloads the field list when the work item type changes', async () => {
    await mountLoaded();
    // Open the "Import rows as" dropdown (its trigger shows the current type) and pick another type.
    const wiTrigger = Array.from(document.querySelectorAll<HTMLInputElement>('.searchable-dropdown .sd-trigger')).find(
      (t) => t.value === 'Requirement',
    )!;
    mousedown(wiTrigger);
    await vi.waitFor(() => expect(document.querySelector('.sd-portal .option')).not.toBeNull());
    const option = Array.from(document.querySelectorAll<HTMLElement>('.sd-portal .option')).find(
      (o) => (o.textContent ?? '').trim() === 'Test Case',
    )!;
    mousedown(option); // the shared dropdown selects on mousedown
    // The page reloads fields for the newly selected type.
    await vi.waitFor(() =>
      expect(fetchMock.mock.calls.some((c) => /\/workitem_types\/testcase\/fields/.test(String(c[0])))).toBe(true),
    );
  });

  it('guards Add-row when no work item type is selected', async () => {
    await mountRaw([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        json: { ...CONTENT, defaultWorkItemType: '', columnsMapping: {} },
      },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: FIELDS },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    ]);
    await vi.waitFor(() => expect(addRowButton()).toBeTruthy());
    addRowButton()!.click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Select workitem type first'));
  });

  it('rejects a duplicate column name', async () => {
    await mountLoaded();
    await setColumn(1, 'A'); // both rows now use column A
    sbbButton('Save').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain("Cannot have duplicate column name 'A'"));
  });

  it("rejects a mapped column with no field ('Mapping for column ... cannot be empty')", async () => {
    await mountLoaded();
    addRowButton()!.click();
    await vi.waitFor(() => expect(mappingRows()).toBe(3));
    await setColumn(2, 'Z'); // new row: column set, field left empty
    sbbButton('Save').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain("Mapping for column 'Z' cannot be empty"));
  });

  it('rejects an empty link column', async () => {
    await mountRaw([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      { method: 'GET', match: /\/settings\/mappings\/names\/[^/]+\/content/, json: { ...CONTENT, linkColumn: '' } },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: FIELDS },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    ]);
    await vi.waitFor(() => expect(triggers()).toContain('title'));
    sbbButton('Save').click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Link column cannot be empty'));
  });

  it('shows and toggles the Unlink existing checkbox for a linkedWorkItems field', async () => {
    await mountRaw([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        json: { ...CONTENT, columnsMapping: { A: 'linkedWorkItems' } },
      },
      {
        method: 'GET',
        match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/,
        json: [{ id: 'linkedWorkItems', type: {} }],
      },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    ]);
    const checkbox = () => document.querySelector<HTMLInputElement>('.unlink-existing-label input[type="checkbox"]');
    await vi.waitFor(() => expect(checkbox()).not.toBeNull());
    expect(checkbox()!.checked).toBe(false);
    checkbox()!.click();
    await vi.waitFor(() => expect(checkbox()!.checked).toBe(true));
  });

  it('saves Test Steps mappings into the payload (stepsMapping + testSteps column key)', async () => {
    const stepsField = {
      id: 'teststeps',
      type: { structTypeId: 'TestSteps' },
      options: [
        { key: 'action', name: 'Action' },
        { key: 'expected', name: 'Expected' },
      ],
    };
    await mountRaw([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        json: {
          ...CONTENT,
          columnsMapping: { A: 'title', 'testSteps|teststeps': 'teststeps' },
          stepsMapping: { teststeps: { action: 'SA', expected: 'SE' } },
          linkColumn: 'A',
        },
      },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: [...FIELDS, stepsField] },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
      { method: 'PUT', match: /\/content/, respond: () => new Response(null, { status: 204 }) },
    ]);
    await vi.waitFor(() => expect(document.querySelectorAll('input.field-name[readonly]').length).toBe(2));
    sbbButton('Save').click();
    await vi.waitFor(() => {
      const put = fetchMock.mock.calls.find((c) => (c[1]?.method ?? 'GET').toUpperCase() === 'PUT');
      expect(put).toBeTruthy();
      const body = String(put![1]?.body);
      expect(body).toContain('testSteps|teststeps');
      expect(body).toContain('SA');
    });
  });

  it('opens the Options mapping dialog for an enum field, edits a value and accepts', async () => {
    const enumField = {
      id: 'status',
      type: {},
      options: [
        { key: 'open', name: 'Open' },
        { key: 'done', name: 'Done' },
      ],
    };
    installFetchMock([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        json: { ...CONTENT, columnsMapping: { A: 'title', C: 'status' } },
      },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: [...FIELDS, enumField] },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    ]);
    setUrl(`?feature=mappings&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
    render(<App />);
    await vi.waitFor(() => {
      const t = triggers();
      expect(t).toContain('title');
      expect(t).toContain('status');
    });
    sbbButton('Options mapping').click();
    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
    // Per-option rows for the enum keys, plus the (empty) keyword hint.
    expect(document.querySelectorAll('.options-mapping-row').length).toBe(2);
    expect(document.querySelector('.rsp-modal')!.textContent).toContain('open');
    // Edit an option's alternative values and accept -> the dialog closes (mapping applied to state).
    await userEvent.fill(document.querySelector<HTMLInputElement>('.option-mapping-value')!, 'yes,y');
    sbbButton('Accept').click();
    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).toBeNull());
  });
});
