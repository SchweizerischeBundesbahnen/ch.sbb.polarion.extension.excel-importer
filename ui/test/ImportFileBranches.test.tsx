import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// Secondary branches of the Import File page: the remembered-mapping cookie, the error-detail
// fallbacks when a failing response has no errorMessage, a rejected (not just failing) names request,
// and clearing an already-chosen file. The happy paths live in ImportFile.test.tsx.

const origUrl = window.location.pathname + window.location.search;
const SCOPE = 'project/elibrary/';
const JOB = '/polarion/excel-importer/rest/internal/import/jobs/1';
const COOKIE = 'selected-configuration-mappings';

const NAMES = [
  { name: 'requirements-import', scope: SCOPE },
  { name: 'testcase-import', scope: SCOPE },
];

const sbbButton = (label: string) =>
  Array.from(document.querySelectorAll<HTMLButtonElement>('.sbb-btn')).find(
    (b) => (b.textContent ?? '').trim() === label,
  );

const fileInput = () => document.querySelector<HTMLInputElement>('input[type="file"]')!;

function setFile(name = 'data.xlsx') {
  const dt = new DataTransfer();
  dt.items.add(new File(['x'], name, { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }));
  fileInput().files = dt.files;
  fileInput().dispatchEvent(new Event('change', { bubbles: true }));
}

function mount(routes: Parameters<typeof installFetchMock>[0]) {
  installFetchMock(routes);
  window.history.replaceState({}, '', `?feature=import-file&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
  render(<App />);
}

const namesRoute = (json: unknown = NAMES) => ({ method: 'GET', match: /\/settings\/mappings\/names\?/, json });

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = `${COOKIE}=; path=/; max-age=0`;
  window.top?.document.querySelectorAll('script[id$="-breadcrumb-bridge"]').forEach((s) => s.remove());
});

describe('Import File mapping selection', () => {
  it('preselects the remembered configuration from the cookie', async () => {
    document.cookie = `${COOKIE}=testcase-import; path=/`;
    mount([namesRoute()]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    const select = document.querySelector<HTMLSelectElement>('.import-panel select')!;
    expect(select.value).toBe('testcase-import');
  });

  it('falls back to the first configuration when the remembered one is gone', async () => {
    document.cookie = `${COOKIE}=deleted-mapping; path=/`;
    mount([namesRoute()]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    const select = document.querySelector<HTMLSelectElement>('.import-panel select')!;
    expect(select.value).toBe('requirements-import');
  });
});

describe('Import File error-detail fallbacks', () => {
  it('uses the message field when the failing names response has no errorMessage', async () => {
    mount([
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\?/,
        respond: () =>
          new Response(JSON.stringify({ message: 'scope unknown' }), {
            status: 400,
            headers: { 'Content-Type': 'application/json' },
          }),
      },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('scope unknown');
  });

  it('falls back to the HTTP status when the failing names response is not JSON', async () => {
    mount([
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\?/,
        respond: () => new Response('<html>gateway</html>', { status: 502 }),
      },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('HTTP 502');
  });

  it('reports the thrown message when the names request is rejected outright', async () => {
    // useRemote turns a network failure into a 503 Response, so reject at the JSON-parsing step
    // instead: that is the path where the page's own try/catch is the only handler.
    installFetchMock([]);
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => ({
        ok: true,
        status: 200,
        json: async () => {
          throw new Error('stream closed');
        },
      })),
    );
    window.history.replaceState({}, '', `?feature=import-file&embedded=true&scope=${encodeURIComponent(SCOPE)}`);
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('stream closed');
  });

  it('falls back to a status message when the job POST error body carries no errorMessage', async () => {
    mount([
      namesRoute(),
      { method: 'POST', match: /\/import\/jobs$/, respond: () => new Response('{}', { status: 500 }) },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('Import failed with status 500');
  });

  it('falls back to a status message when the polled job fails without an errorMessage', async () => {
    mount([
      namesRoute(),
      {
        method: 'POST',
        match: /\/import\/jobs$/,
        respond: () => new Response(null, { status: 202, headers: { Location: JOB } }),
      },
      { method: 'GET', match: /\/import\/jobs\/1$/, respond: () => new Response('{}', { status: 500 }) },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull(), { timeout: 5000 });
    expect(document.querySelector('.alert-error')!.textContent).toContain('Import failed with status 500');
  });

  it('falls back to a status message when the result fetch fails without an errorMessage', async () => {
    mount([
      namesRoute(),
      {
        method: 'POST',
        match: /\/import\/jobs$/,
        respond: () => new Response(null, { status: 202, headers: { Location: JOB } }),
      },
      { method: 'GET', match: /\/import\/jobs\/1$/, respond: () => new Response(null, { status: 303 }) },
      { method: 'GET', match: /\/import\/jobs\/1\/result$/, respond: () => new Response('{}', { status: 500 }) },
    ]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile();
    await vi.waitFor(() => expect(sbbButton('Import')!.disabled).toBe(false));
    sbbButton('Import')!.click();
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull(), { timeout: 5000 });
    expect(document.querySelector('.alert-error')!.textContent).toContain('Import failed with status 500');
  });
});

describe('Import File file selection', () => {
  it('clears the chosen file when the picker is dismissed with no selection', async () => {
    mount([namesRoute()]);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    setFile('report.xlsx');
    await vi.waitFor(() => expect(document.body.textContent).toContain('report.xlsx'));

    fileInput().files = new DataTransfer().files; // empty selection
    fileInput().dispatchEvent(new Event('change', { bubbles: true }));
    await vi.waitFor(() => expect(document.body.textContent).toContain('No file chosen'));
    expect(sbbButton('Import')!.disabled).toBe(true);
  });
});
