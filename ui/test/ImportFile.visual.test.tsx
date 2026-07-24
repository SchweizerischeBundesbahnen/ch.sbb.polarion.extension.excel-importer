import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// Docker-only full-page snapshot of the Import File page (REST mocked). See Mappings.visual.test.tsx
// for the CSS-fidelity note (App.css + bundled RSP style.css; Polarion-served CSS is not included).

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'selected-configuration-mappings=; path=/; max-age=0';
  window.top?.document.querySelectorAll('script[id$="-breadcrumb-bridge"]').forEach((s) => s.remove());
});

describe('Import File page visual', () => {
  it('loaded (mapping picker + choose-file + import)', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/settings\/mappings\/names\?/,
        json: [{ name: 'requirements-import', scope: 'project/elibrary/' }],
      },
    ]);
    window.history.replaceState({}, '', '?feature=import-file&embedded=true&scope=project/elibrary/');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.import-panel')).not.toBeNull());
    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await expect(page.elementLocator(app)).toMatchScreenshot('import-file-loaded');
  });
});
