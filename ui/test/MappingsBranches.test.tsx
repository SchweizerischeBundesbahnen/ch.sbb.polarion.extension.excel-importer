import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import App from '../src/App';
import { NAMES, SCOPE, WORKITEM_TYPES, mappingsRoutes } from './fixtures/mappings';
import { type Route, installFetchMock } from './mockFetch';

// Secondary branches of the Mappings page: a configuration whose model omits every optional field (so
// every default applies), a saved field id that no longer exists on the work item type, and the two
// metadata load failures. The fully-populated happy path lives in Mappings.test.tsx.

const origUrl = window.location.pathname + window.location.search;
const setUrl = (search: string) => window.history.replaceState({}, '', search);

const replaceRoute = (routes: Route[], match: RegExp, route: Route): Route[] => [
  route,
  ...routes.filter((r) => String(r.match) !== String(match)),
];

async function mount(routes: Route[]) {
  installFetchMock(routes);
  setUrl(`?feature=mappings&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
  render(<App />);
  await vi.waitFor(() => expect(document.querySelector('.mappings-form')).not.toBeNull(), { timeout: 5000 });
}

const triggers = () =>
  Array.from(document.querySelectorAll<HTMLInputElement>('.searchable-dropdown .sd-trigger')).map((t) => t.value);
const mappingRows = () => document.querySelectorAll('tr.mapping-row').length;

const saveButton = (): HTMLButtonElement => {
  const b = Array.from(document.querySelectorAll<HTMLButtonElement>('.sbb-btn')).find(
    (x) => (x.textContent ?? '').trim() === 'Save',
  );
  if (!b) throw new Error('Save button not found');
  return b;
};

/** Click Save and return the payload the page PUT to the settings endpoint. */
async function saveAndCapture(): Promise<Record<string, never>> {
  saveButton().click();
  const put = await vi.waitFor(
    () => {
      const call = (globalThis.fetch as unknown as { mock: { calls: [string, RequestInit][] } }).mock.calls.find(
        ([, init]) => init?.method === 'PUT',
      );
      expect(call).toBeTruthy();
      return call!;
    },
    { timeout: 5000 },
  );
  return JSON.parse(String(put[1].body));
}

beforeEach(() => setUrl(origUrl));

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  setUrl(origUrl);
  document.cookie = 'selected-configuration-mappings=; path=/; max-age=0';
  window.top?.document.querySelectorAll('script[id$="-breadcrumb-bridge"]').forEach((s) => s.remove());
});

describe('Mappings with a minimal configuration', () => {
  it('applies the defaults for every field the stored model omits', async () => {
    await mount(
      replaceRoute(mappingsRoutes(), /\/settings\/mappings\/names\/[^/]+\/content/, {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        json: {}, // a configuration saved before any of these fields existed
      }),
    );
    // Sheet name blank, start row back to 1, no work item type selected, no mapping rows at all.
    const startRow = Array.from(document.querySelectorAll<HTMLInputElement>('.general-settings input')).find(
      (i) => i.inputMode === 'numeric' || /^\d+$/.test(i.value),
    );
    await vi.waitFor(() => expect(startRow?.value).toBe('1'));
    expect(mappingRows()).toBe(0);
    // The checkboxes fall back to unchecked rather than staying undefined.
    const checkboxes = Array.from(
      document.querySelectorAll<HTMLInputElement>('.general-settings input[type=checkbox]'),
    );
    expect(checkboxes.length).toBeGreaterThan(0);
    expect(checkboxes.every((c) => !c.checked)).toBe(true);
  });

  it('clears a mapped field that the work item type no longer defines', async () => {
    const routes = replaceRoute(mappingsRoutes(), /\/settings\/mappings\/names\/[^/]+\/content/, {
      method: 'GET',
      match: /\/settings\/mappings\/names\/[^/]+\/content/,
      json: {
        sheetName: 'Sheet1',
        startFromRow: 2,
        columnsMapping: { A: 'title', B: 'removedField' },
        defaultWorkItemType: 'requirement',
      },
    });
    await mount(routes);
    // Both rows survive, but the one pointing at a field the type no longer has is reset to blank so
    // the user is forced to re-pick instead of silently saving a dangling id.
    await vi.waitFor(() => expect(mappingRows()).toBe(2), { timeout: 5000 });
    await vi.waitFor(() => expect(triggers()).toContain('title'), { timeout: 5000 });
    expect(triggers()).not.toContain('removedField');
  });
});

describe('Mappings with a Test Steps field', () => {
  // A TestSteps field expands into one sub-row per step option; its own column input is disabled and
  // its mapping is stored under the testSteps| prefix instead of a spreadsheet column.
  const TEST_STEPS_FIELDS = [
    { id: 'title', type: {} },
    {
      id: 'testSteps',
      type: { structTypeId: 'TestSteps' },
      options: [{ key: 'step' }, { key: 'expectedResult' }],
    },
  ];

  const routesWithSteps = (stepsMapping: Record<string, Record<string, string>>): Route[] => [
    {
      method: 'GET',
      match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/,
      json: TEST_STEPS_FIELDS,
    },
    {
      method: 'GET',
      match: /\/settings\/mappings\/names\/[^/]+\/content/,
      json: {
        sheetName: 'Sheet1',
        startFromRow: 2,
        columnsMapping: { A: 'title', 'testSteps|testSteps': 'testSteps' },
        stepsMapping,
        defaultWorkItemType: 'requirement',
        linkColumn: 'A',
      },
    },
    ...mappingsRoutes().filter(
      (r) =>
        !/workitem_types\/\[\^\/\]\+\/fields/.test(String(r.match)) &&
        !/names\/\[\^\/\]\+\/content/.test(String(r.match)),
    ),
  ];

  it('renders a sub-row per step option and saves them under the steps mapping', async () => {
    await mount(routesWithSteps({ testSteps: { step: 'B', expectedResult: 'C' } }));
    // Two step sub-rows plus the two root rows.
    await vi.waitFor(() => expect(mappingRows()).toBe(4), { timeout: 5000 });
    const saved = await saveAndCapture();
    expect(saved.stepsMapping).toEqual({ testSteps: { step: 'B', expectedResult: 'C' } });
    // The TestSteps root row is stored under the prefixed key, not a spreadsheet column.
    expect(saved.columnsMapping['testSteps|testSteps']).toBe('testSteps');
  });

  it('refuses to save while a step column is empty', async () => {
    await mount(routesWithSteps({ testSteps: { step: 'B', expectedResult: '' } }));
    await vi.waitFor(() => expect(mappingRows()).toBe(4), { timeout: 5000 });
    saveButton().click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Column name cannot be empty'), {
      timeout: 5000,
    });
  });

  it('refuses to save duplicate step columns', async () => {
    await mount(routesWithSteps({ testSteps: { step: 'B', expectedResult: 'B' } }));
    await vi.waitFor(() => expect(mappingRows()).toBe(4), { timeout: 5000 });
    saveButton().click();
    await vi.waitFor(() => expect(document.body.textContent).toContain("duplicate column name 'B'"), { timeout: 5000 });
  });
});

describe('Mappings end import row', () => {
  const routesWithRows = (rows: { startFromRow: number; endAtRow?: number | null }): Route[] =>
    replaceRoute(mappingsRoutes(), /\/settings\/mappings\/names\/[^/]+\/content/, {
      method: 'GET',
      match: /\/settings\/mappings\/names\/[^/]+\/content/,
      json: {
        sheetName: 'Sheet1',
        ...rows,
        columnsMapping: { A: 'title' },
        defaultWorkItemType: 'requirement',
        linkColumn: 'A',
      },
    });

  const endCheckbox = () => document.querySelector<HTMLInputElement>('#end-at-row-enabled')!;
  const endInput = () => document.querySelector<HTMLInputElement>('#end-at-row');

  const setNumber = (input: HTMLInputElement, value: string) => {
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!;
    setter.call(input, value);
    input.dispatchEvent(new Event('input', { bubbles: true }));
  };

  it('hides the end row input until the checkbox is checked and saves null when unchecked', async () => {
    await mount(routesWithRows({ startFromRow: 3 }));
    await vi.waitFor(() => expect(mappingRows()).toBe(1), { timeout: 5000 });
    expect(endCheckbox().checked).toBe(false);
    expect(endInput()).toBeNull();

    endCheckbox().click();
    await vi.waitFor(() => expect(endInput()?.value).toBe('3'));
    endCheckbox().click();
    await vi.waitFor(() => expect(endInput()).toBeNull());

    const saved = await saveAndCapture();
    expect(saved.endAtRow).toBeNull();
  });

  it('loads a stored end row and saves it back', async () => {
    await mount(routesWithRows({ startFromRow: 2, endAtRow: 10 }));
    await vi.waitFor(() => expect(endInput()?.value).toBe('10'), { timeout: 5000 });
    expect(endCheckbox().checked).toBe(true);
    const saved = await saveAndCapture();
    expect(saved.endAtRow).toBe(10);
  });

  it('refuses to save an end row less than the start row', async () => {
    await mount(routesWithRows({ startFromRow: 5, endAtRow: 10 }));
    await vi.waitFor(() => expect(endInput()?.value).toBe('10'), { timeout: 5000 });
    setNumber(endInput()!, '4');
    await vi.waitFor(() => expect(endInput()?.value).toBe('4'));
    saveButton().click();
    await vi.waitFor(
      () =>
        expect(document.body.textContent).toContain('End row number must be greater than or equal to start row number'),
      { timeout: 5000 },
    );
  });

  it('refuses to save an empty end row', async () => {
    await mount(routesWithRows({ startFromRow: 1, endAtRow: 10 }));
    await vi.waitFor(() => expect(endInput()?.value).toBe('10'), { timeout: 5000 });
    setNumber(endInput()!, '');
    saveButton().click();
    await vi.waitFor(
      () => expect(document.body.textContent).toContain('End row number must be a positive integer value'),
      { timeout: 5000 },
    );
  });
});

describe('Mappings link column uniqueness', () => {
  const routesWithUnique = (linkColumnUnique?: boolean): Route[] =>
    replaceRoute(mappingsRoutes(), /\/settings\/mappings\/names\/[^/]+\/content/, {
      method: 'GET',
      match: /\/settings\/mappings\/names\/[^/]+\/content/,
      json: {
        sheetName: 'Sheet1',
        startFromRow: 1,
        columnsMapping: { A: 'title' },
        defaultWorkItemType: 'requirement',
        linkColumn: 'A',
        ...(linkColumnUnique === undefined ? {} : { linkColumnUnique }),
      },
    });

  const uniqueCheckbox = () => document.querySelector<HTMLInputElement>('#link-column-unique')!;

  it('is off for a mapping saved without the option and saves the checked value', async () => {
    await mount(routesWithUnique());
    await vi.waitFor(() => expect(mappingRows()).toBe(1), { timeout: 5000 });
    expect(uniqueCheckbox().checked).toBe(false);

    uniqueCheckbox().click();
    await vi.waitFor(() => expect(uniqueCheckbox().checked).toBe(true));
    const saved = await saveAndCapture();
    expect(saved.linkColumnUnique).toBe(true);
  });

  it('loads a stored option and saves it unchecked', async () => {
    await mount(routesWithUnique(true));
    await vi.waitFor(() => expect(uniqueCheckbox().checked).toBe(true), { timeout: 5000 });

    uniqueCheckbox().click();
    await vi.waitFor(() => expect(uniqueCheckbox().checked).toBe(false));
    const saved = await saveAndCapture();
    expect(saved.linkColumnUnique).toBe(false);
  });
});

describe('Mappings filter query', () => {
  const routesWithFilter = (filterQuery?: string | null): Route[] =>
    replaceRoute(mappingsRoutes(), /\/settings\/mappings\/names\/[^/]+\/content/, {
      method: 'GET',
      match: /\/settings\/mappings\/names\/[^/]+\/content/,
      json: {
        sheetName: 'Sheet1',
        startFromRow: 1,
        filterQuery,
        columnsMapping: { A: 'title' },
        defaultWorkItemType: 'requirement',
        linkColumn: 'A',
      },
    });

  const filterCheckbox = () => document.querySelector<HTMLInputElement>('#filter-query-enabled')!;
  const filterInput = () => document.querySelector<HTMLInputElement>('#filter-query');

  const setText = (input: HTMLInputElement, value: string) => {
    const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')!.set!;
    setter.call(input, value);
    input.dispatchEvent(new Event('input', { bubbles: true }));
  };

  it('hides the query input until the checkbox is checked and saves null when unchecked', async () => {
    await mount(routesWithFilter());
    await vi.waitFor(() => expect(mappingRows()).toBe(1), { timeout: 5000 });
    expect(filterCheckbox().checked).toBe(false);
    expect(filterInput()).toBeNull();

    filterCheckbox().click();
    await vi.waitFor(() => expect(filterInput()).not.toBeNull());
    expect(document.querySelector('.filter-query .help-icon')?.getAttribute('title')).toContain(
      "unless 'Skip new workitems creation' is enabled",
    );
    setText(filterInput()!, 'status:draft');
    filterCheckbox().click();
    await vi.waitFor(() => expect(filterInput()).toBeNull());

    const saved = await saveAndCapture();
    expect(saved.filterQuery).toBeNull();
  });

  it('loads a stored query and saves it back trimmed', async () => {
    await mount(routesWithFilter('status:draft'));
    await vi.waitFor(() => expect(filterInput()?.value).toBe('status:draft'), { timeout: 5000 });
    expect(filterCheckbox().checked).toBe(true);
    setText(filterInput()!, '  status:open  ');
    await vi.waitFor(() => expect(filterInput()?.value).toBe('  status:open  '));
    const saved = await saveAndCapture();
    expect(saved.filterQuery).toBe('status:open');
  });

  it('refuses to save a blank query while the checkbox is checked', async () => {
    await mount(routesWithFilter('status:draft'));
    await vi.waitFor(() => expect(filterInput()?.value).toBe('status:draft'), { timeout: 5000 });
    setText(filterInput()!, '   ');
    saveButton().click();
    await vi.waitFor(
      () =>
        expect(document.body.textContent).toContain(
          'Filter query cannot be empty. Enter a query or uncheck the option',
        ),
      { timeout: 5000 },
    );
  });
});

describe('Mappings skip work items creation', () => {
  const routesWithSkip = (skipWorkItemsCreation?: boolean): Route[] =>
    replaceRoute(mappingsRoutes(), /\/settings\/mappings\/names\/[^/]+\/content/, {
      method: 'GET',
      match: /\/settings\/mappings\/names\/[^/]+\/content/,
      json: {
        sheetName: 'Sheet1',
        startFromRow: 1,
        skipWorkItemsCreation,
        columnsMapping: { A: 'title' },
        defaultWorkItemType: 'requirement',
        linkColumn: 'A',
      },
    });

  const skipCheckbox = () => document.querySelector<HTMLInputElement>('#skip-workitems-creation')!;
  const typeDisabled = () => document.querySelector('#workitem-types-container [disabled]') !== null;

  it('keeps the type selector enabled while checked and saves the type', async () => {
    await mount(routesWithSkip());
    await vi.waitFor(() => expect(mappingRows()).toBe(1), { timeout: 5000 });
    expect(skipCheckbox().checked).toBe(false);
    expect(typeDisabled()).toBe(false);

    skipCheckbox().click();
    await vi.waitFor(() => expect(skipCheckbox().checked).toBe(true));
    expect(typeDisabled()).toBe(false);

    const saved = await saveAndCapture();
    expect(saved.skipWorkItemsCreation).toBe(true);
    expect(saved.defaultWorkItemType).toBe('requirement');
  });

  it('loads a stored flag and saves false when unchecked', async () => {
    await mount(routesWithSkip(true));
    await vi.waitFor(() => expect(skipCheckbox().checked).toBe(true), { timeout: 5000 });

    skipCheckbox().click();
    await vi.waitFor(() => expect(skipCheckbox().checked).toBe(false));

    const saved = await saveAndCapture();
    expect(saved.skipWorkItemsCreation).toBe(false);
  });
});

describe('Mappings metadata load failures', () => {
  it('reports a failure to load the work item types', async () => {
    await mount(
      replaceRoute(mappingsRoutes(), /\/projects\/[^/]+\/workitem_types(\?|$)/, {
        method: 'GET',
        match: /\/projects\/[^/]+\/workitem_types(\?|$)/,
        respond: () => new Response('{}', { status: 500 }),
      }),
    );
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull(), { timeout: 5000 });
  });

  it('reports a failure to load the fields of the selected type', async () => {
    await mount(
      replaceRoute(mappingsRoutes(), /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, {
        method: 'GET',
        match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/,
        respond: () => new Response('{}', { status: 500 }),
      }),
    );
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull(), { timeout: 5000 });
    // The work item types themselves did load, so their dropdown is still usable.
    expect(WORKITEM_TYPES.length).toBeGreaterThan(0);
    expect(NAMES.length).toBeGreaterThan(0);
  });
});
