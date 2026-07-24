import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// Docker-only full-page snapshot of the About page (shared RSP About component fed this app's
// endpoints, mocked). Covers the extension-info / properties / status tables and the README article.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  window.top?.document.querySelectorAll('script[id$="-breadcrumb-bridge"]').forEach((s) => s.remove());
});

describe('About page visual', () => {
  it('loaded (info + properties + status tables, README article)', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/version$/,
        json: {
          bundleName: 'Excel Importer',
          bundleVendor: 'SBB',
          supportEmail: 'support@example.com',
          automaticModuleName: 'ch.sbb.polarion.extension.excel_importer',
          bundleVersion: '6.1.2',
          bundleBuildTimestamp: '2026-07-01 10:00',
        },
      },
      {
        method: 'GET',
        match: /\/configuration-properties$/,
        json: {
          properties: [
            { key: 'ch.sbb.excel.debug', value: 'false', defaultValue: 'false', description: 'Debug logging' },
          ],
          obsoleteProperties: [],
        },
      },
      {
        method: 'GET',
        match: /\/configuration-status/,
        json: [{ name: 'Apache POI', status: 'OK', details: 'v5.2.3' }],
      },
      {
        method: 'GET',
        match: /\/readme$/,
        respond: () => new Response('<h1>Excel Importer</h1><p>Imports Excel rows as work items.</p>', { status: 200 }),
      },
    ]);
    window.history.replaceState({}, '', '?feature=about&embedded=true');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('article.markdown-body')).not.toBeNull());
    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await expect(page.elementLocator(app)).toMatchScreenshot('about-loaded');
  });
});
