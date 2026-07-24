import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import App from '../src/App';
import { type FetchMock, installFetchMock } from './mockFetch';

// Behavior tests for the Import File page (mapping picker + xlsx upload + async import job polling),
// with the REST layer mocked at the fetch boundary. Rendered through the feature router.

const origUrl = window.location.pathname + window.location.search;
const SCOPE = 'project/elibrary/';
const JOB = '/polarion/excel-importer/rest/internal/import/jobs/1';

const NAMES = [{ name: 'requirements-import', scope: 'project/elibrary/' }];
const RESULT = { createdIds: ['R-1'], updatedIds: [], unchangedIds: [], skippedIds: [], log: 'import log' };

const sbbButton = (label: string) =>
  Array.from(document.querySelectorAll<HTMLButtonElement>('.sbb-btn')).find(
    (b) => (b.textContent ?? '').trim() === label,
  );

function setFile(name = 'data.xlsx') {
  const input = document.querySelector<HTMLInputElement>('input[type="file"]')!;
  const dt = new DataTransfer();
  dt.items.add(new File(['x'], name, { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }));
  input.files = dt.files;
  input.dispatchEvent(new Event('change', { bubbles: true }));
}

let fetchMock: FetchMock;

function mount(routes: Parameters<typeof installFetchMock>[0]) {
  fetchMock = installFetchMock(routes);
  window.history.replaceState({}, '', `?feature=import-file&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
  render(<App />);
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-mappings=; path=/; max-age=0';
  window.top?.document.querySelectorAll('script[id$="-breadcrumb-bridge"]').forEach((s) => s.remove());
});

describe('Import File page', () => {
  it('loads mapping configurations and shows the import panel', async () => {
    mount([{ method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES }]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    expect(document.body.textContent).toContain('No file chosen');
    // Import is disabled until a file is chosen.
    expect(sbbButton('Import')!.disabled).toBe(true);
  });

  it('shows the empty-state note when there are no configurations', async () => {
    mount([{ method: 'GET', match: /\/settings\/mappings\/names\?/, json: [] }]);
    await vi.waitFor(() => expect(document.querySelector('.no-mapping-note')).not.toBeNull());
    expect(document.body.textContent).toContain('No mapping configurations found');
  });

  it('shows a load-error alert when the names request fails', async () => {
    mount([
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\?/,
        respond: () => new Response(JSON.stringify({ errorMessage: 'nope' }), { status: 500 }),
      },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('Failed to load mapping configurations');
  });

  it('imports a chosen file: POST job, poll to completion, show the success summary', async () => {
    mount([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      { method: 'GET', match: /\/import\/jobs\/1\/result$/, json: RESULT },
      { method: 'GET', match: /\/import\/jobs\/1$/, respond: () => new Response(null, { status: 303 }) },
      {
        method: 'POST',
        match: /\/import\/jobs$/,
        respond: () => new Response(null, { status: 202, headers: { Location: JOB } }),
      },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.querySelector('.alert-success')).not.toBeNull(), { timeout: 6000 });
    const text = document.querySelector('.alert-success')!.textContent ?? '';
    expect(text).toContain('File successfully imported');
    expect(text).toContain('Created: 1');
    expect(text).toContain('(log)');
    expect(fetchMock.mock.calls.some((c) => (c[1]?.method ?? 'GET').toUpperCase() === 'POST')).toBe(true);
  });

  it('polls while the job runs (202) then completes, and downloads the log', async () => {
    let statusCalls = 0;
    mount([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      { method: 'GET', match: /\/import\/jobs\/1\/result$/, json: RESULT },
      {
        method: 'GET',
        match: /\/import\/jobs\/1$/,
        respond: () => new Response(null, { status: ++statusCalls < 2 ? 202 : 303 }), // 202 once, then 303
      },
      {
        method: 'POST',
        match: /\/import\/jobs$/,
        respond: () => new Response(null, { status: 202, headers: { Location: JOB } }),
      },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.querySelector('.alert-success')).not.toBeNull(), { timeout: 8000 });
    expect(statusCalls).toBeGreaterThanOrEqual(2); // polled at least twice (202 then 303)
    // The (log) link downloads the log; clicking it must not throw.
    const logLink = document.querySelector<HTMLAnchorElement>('.alert-success a')!;
    logLink.click();
    expect(document.querySelector('.alert-success')).not.toBeNull();
  });

  it('shows an error when the job POST returns 202 without a Location header', async () => {
    mount([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      { method: 'POST', match: /\/import\/jobs$/, respond: () => new Response(null, { status: 202 }) }, // no Location
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('did not return a status location'));
  });

  it('shows an error when the job fails during polling', async () => {
    mount([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/import\/jobs\/1$/,
        respond: () => new Response(JSON.stringify({ errorMessage: 'job failed' }), { status: 409 }),
      },
      {
        method: 'POST',
        match: /\/import\/jobs$/,
        respond: () => new Response(null, { status: 202, headers: { Location: JOB } }),
      },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Import error (job failed)'), { timeout: 6000 });
  });

  it('shows an error when the result fetch fails after the job redirects', async () => {
    mount([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'GET',
        match: /\/import\/jobs\/1\/result$/,
        respond: () => new Response(JSON.stringify({ errorMessage: 'no result' }), { status: 500 }),
      },
      { method: 'GET', match: /\/import\/jobs\/1$/, respond: () => new Response(null, { status: 303 }) },
      {
        method: 'POST',
        match: /\/import\/jobs$/,
        respond: () => new Response(null, { status: 202, headers: { Location: JOB } }),
      },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Import error (no result)'), { timeout: 6000 });
  });

  it('shows an import error when the job POST is rejected', async () => {
    mount([
      { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
      {
        method: 'POST',
        match: /\/import\/jobs$/,
        respond: () => new Response(JSON.stringify({ errorMessage: 'bad file' }), { status: 409 }),
      },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.body.textContent).toContain('Import error (bad file)'));
  });
});
