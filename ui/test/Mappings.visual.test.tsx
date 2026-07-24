import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import OptionsMappingModal from '../src/components/OptionsMappingModal';
import type { FieldMetadata } from '../src/types';
import { CONTENT, FIELDS, NAMES, SCOPE, WORKITEM_TYPES, mappingsRoutes } from './fixtures/mappings';
import { installFetchMock } from './mockFetch';

// Full-page visual snapshot of the Mappings page, rendered through the real App with the REST layer
// mocked at the fetch boundary. Docker-only, like the react-sbb-polarion visual tests (any
// toMatchScreenshot file diffs on non-Linux font antialiasing); references live in test/expected/ and
// MUST be generated with `npm run test:update:docker`.
//
// Fidelity note: the test loads App.css + the bundled react-sbb-polarion style.css (see test/setup.ts).
// The Polarion-served stylesheets linked in index.html (presentation.css / configurations.css /
// github-markdown-light.css) are not bundled and are not part of this snapshot; they are baseline
// chrome / help-article styling and do not materially affect the Mappings layout.

const origUrl = window.location.pathname + window.location.search;
const setUrl = (search: string) => window.history.replaceState({}, '', search);

const triggers = () =>
  Array.from(document.querySelectorAll<HTMLInputElement>('.searchable-dropdown .sd-trigger')).map((t) => t.value);

async function mountLoaded() {
  installFetchMock(mappingsRoutes());
  setUrl(`?feature=mappings&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
  render(<App />);
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

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-mappings=; path=/; max-age=0';
  window.top?.document.querySelectorAll('script[id$="-breadcrumb-bridge"]').forEach((s) => s.remove());
});

describe('Mappings page visual', () => {
  it('fully loaded (config pane, general settings, work-item type, column->field rows, link column, toolbar, quick help)', async () => {
    await mountLoaded();
    // The page is taller than the default 720px viewport; an element screenshot of a below-the-fold
    // region leaves it unpainted (white). Grow the viewport so the whole `.app` fits and paints, then
    // capture it in one shot.
    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await expect(page.elementLocator(app)).toMatchScreenshot('mappings-loaded');
  });

  it('Options mapping dialog (enum field)', async () => {
    const enumField: FieldMetadata = {
      id: 'status',
      options: [
        { key: 'open', name: 'Open' },
        { key: 'done', name: 'Done' },
      ],
    };
    // Render the dialog in isolation (not through the full Mappings page) and shoot the whole viewport,
    // so the snapshot is the "as seen on the page" look: the RSP overlay backdrop + the centered dialog
    // WITH its drop shadow. Capturing the `.rsp-modal` element instead would clip the shadow and drop the
    // backdrop, and rendering over <App/> would show the page dimmed through the semi-transparent overlay.
    // Wrapper mirrors the real App root (`app standard-admin-page`): `.app` gives the base font (App.css)
    // and `.standard-admin-page` carries the --sbb-* design tokens - without the latter the primary button
    // loses --sbb-btn-accent and renders invisible. The fixed overlay ignores the wrapper's padding.
    render(
      <div className="app standard-admin-page">
        <OptionsMappingModal open field={enumField} mapping={{}} onAccept={() => {}} onCancel={() => {}} />
      </div>,
    );
    await vi.waitFor(() => expect(document.querySelector('.rsp-modal')).not.toBeNull());
    await page.viewport(1280, 720);
    await expect(page.elementLocator(document.body)).toMatchScreenshot('mappings-options-modal');
  });

  // Renders the mapping table for a config whose second column maps to the given field, then captures
  // just the #mapping-table (focused on the row state).
  async function shotMappingTable(fieldId: string, extraFields: unknown[], name: string) {
    installFetchMock([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\/[^/]+\/content/,
        json: { ...CONTENT, columnsMapping: { A: 'title', B: fieldId } },
      },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: [...FIELDS, ...extraFields] },
      { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    ]);
    setUrl(`?feature=mappings&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
    render(<App />);
    await vi.waitFor(() => expect(triggers()).toContain(fieldId));
    await expect(page.elementLocator(document.querySelector('#mapping-table') as HTMLElement)).toMatchScreenshot(name);
  }

  it('mapping row: linkedWorkItems field selected (shows the Unlink existing checkbox)', async () => {
    await shotMappingTable('linkedWorkItems', [{ id: 'linkedWorkItems', type: {} }], 'mappings-row-linked-workitems');
  });

  it('mapping row: enumeration field selected (shows the Options mapping button)', async () => {
    await shotMappingTable(
      'status',
      [
        {
          id: 'status',
          type: {},
          options: [
            { key: 'open', name: 'Open' },
            { key: 'done', name: 'Done' },
          ],
        },
      ],
      'mappings-row-enum',
    );
  });
});
