import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import App from '../src/App';
import { installFetchMock } from './mockFetch';

// The excel-importer User Guide page is a thin wrapper feeding the shared react-sbb-polarion UserGuide
// this app's sendRequest. Rendered through the feature router with the /user-guide endpoint mocked.

const origUrl = window.location.pathname + window.location.search;

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
});

describe('User Guide page (wrapper)', () => {
  it('renders the shared User Guide article from the /user-guide endpoint', async () => {
    installFetchMock([
      {
        method: 'GET',
        match: /\/user-guide$/,
        respond: () => new Response('<h2>Guide</h2><p>Body</p>', { status: 200 }),
      },
    ]);
    window.history.replaceState({}, '', '?feature=user-guide&embedded=true');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('article.user-guide-page')).not.toBeNull());
    expect(document.querySelector('article.user-guide-page')!.textContent).toContain('Body');
  });

  it('shows an HTTP error when the endpoint is not ok', async () => {
    installFetchMock([{ method: 'GET', match: /\/user-guide$/, respond: () => new Response('', { status: 404 }) }]);
    window.history.replaceState({}, '', '?feature=user-guide&embedded=true');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('HTTP 404');
  });
});
