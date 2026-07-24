import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import App from '../src/App';
import { installFetchMock, jsonResponse } from './mockFetch';

// The excel-importer About page is a thin wrapper feeding the shared react-sbb-polarion About component
// this app's sendRequest / appIcon / restApiUrl. We render it through the feature router with the
// generic About endpoints mocked, and assert the shared page rendered (RSP owns the deeper coverage).

const origUrl = window.location.pathname + window.location.search;

const aboutRoutes = () => [
  {
    method: 'GET',
    match: /\/version$/,
    json: { bundleName: 'Excel Importer', bundleVendor: 'SBB', bundleVersion: '6.1.2' },
  },
  { method: 'GET', match: /\/configuration-properties$/, json: { properties: [], obsoleteProperties: [] } },
  { method: 'GET', match: /\/configuration-status/, json: [] },
  { method: 'GET', match: /\/readme$/, respond: () => new Response('<h1>Readme</h1>', { status: 200 }) },
];

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe('About page (wrapper)', () => {
  it('renders the shared About page with the extension info', async () => {
    installFetchMock(aboutRoutes());
    window.history.replaceState({}, '', '?feature=about&embedded=true');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.about-table')).not.toBeNull());
    expect(document.body.textContent).toContain('Excel Importer');
    // The app icon is passed through and rendered.
    expect(document.querySelector('.about-page .app-icon')).not.toBeNull();
  });

  it('shows an error alert when a required endpoint fails', async () => {
    installFetchMock([
      { method: 'GET', match: /\/version$/, respond: () => jsonResponse({ errorMessage: 'boom' }, 500) },
      { method: 'GET', match: /\/configuration-properties$/, json: { properties: [], obsoleteProperties: [] } },
      { method: 'GET', match: /\/configuration-status/, json: [] },
      { method: 'GET', match: /\/readme$/, respond: () => new Response('', { status: 404 }) },
    ]);
    window.history.replaceState({}, '', '?feature=about&embedded=true');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('boom');
  });
});
