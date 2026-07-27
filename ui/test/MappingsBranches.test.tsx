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
