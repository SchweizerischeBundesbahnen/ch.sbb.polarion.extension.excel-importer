import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import { page } from 'vitest/browser';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// Docker-only snapshot of the User Guide page (shared RSP UserGuide fed the /user-guide HTML, mocked).

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  window.top?.document.querySelectorAll('script[id$="-breadcrumb-bridge"]').forEach((s) => s.remove());
});

describe('User Guide page visual', () => {
  it('renders the help article', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/user-guide$/,
        // The build-generated article does not repeat the page title (PageLayout already renders it).
        respond: () =>
          new Response(
            '<h2>Getting started</h2><p>Create a mapping, then import an Excel file.</p><ul><li>Step one</li><li>Step two</li></ul>',
            { status: 200 },
          ),
      },
    ]);
    window.history.replaceState({}, '', '?feature=user-guide&embedded=true');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('article.user-guide-page')).not.toBeNull());
    const app = document.querySelector('.app') as HTMLElement;
    await page.viewport(1280, Math.ceil(app.scrollHeight) + 40);
    await expect(page.elementLocator(app)).toMatchScreenshot('user-guide-loaded');
  });
});
