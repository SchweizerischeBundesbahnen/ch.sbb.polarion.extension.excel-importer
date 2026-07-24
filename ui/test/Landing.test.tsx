import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import App from '../src/App';
import { installFetchMock, jsonResponse } from './mockFetch';

// The dev Landing page (shown when no feature is selected): a project-scope picker + links to every
// feature. It loads projects from the platform REST API, mocked here.

const origUrl = window.location.pathname + window.location.search;

const PROJECTS = {
  data: [
    { id: 'elibrary', attributes: { name: 'E-Library' } },
    { id: 'drivepilot', attributes: { name: 'Drive Pilot' } },
  ],
};

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
  window.history.replaceState({}, '', origUrl);
  document.cookie = 'excel-importer-dev-scope=; path=/; max-age=0';
});

describe('Landing page', () => {
  it('renders the feature links and the loaded project scope options', async () => {
    installFetchMock([{ method: 'GET', match: /\/polarion\/rest\/v1\/projects/, json: PROJECTS }]);
    window.history.replaceState({}, '', '?'); // no feature -> Landing
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.landing')).not.toBeNull());
    // Every feature is linked.
    const links = Array.from(document.querySelectorAll<HTMLAnchorElement>('.feature-list a')).map((a) =>
      a.getAttribute('href'),
    );
    expect(links).toEqual(
      expect.arrayContaining(['?feature=about', '?feature=mappings', '?feature=user-guide', '?feature=import-file']),
    );
    // Projects loaded into the scope picker's options (global + the two projects).
    await vi.waitFor(() => {
      const options = Array.from(document.querySelectorAll('.landing-scope select option')).map((o) => o.textContent);
      expect(options).toContain('E-Library (elibrary)');
      expect(options).toContain('Drive Pilot (drivepilot)');
    });
  });

  it('pre-selects the scope from the ?scope query param', async () => {
    installFetchMock([{ method: 'GET', match: /\/polarion\/rest\/v1\/projects/, json: PROJECTS }]);
    window.history.replaceState({}, '', '?scope=project/elibrary'); // initialScope() reads the scope param
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.landing')).not.toBeNull());
    // The feature links carry the pre-selected scope.
    await vi.waitFor(() => {
      const hrefs = Array.from(document.querySelectorAll<HTMLAnchorElement>('.feature-list a')).map((a) =>
        a.getAttribute('href'),
      );
      expect(hrefs.some((h) => h?.includes('scope=project%2Felibrary%2F'))).toBe(true);
    });
  });

  it('shows an error when projects cannot be loaded', async () => {
    installFetchMock([
      { method: 'GET', match: /\/polarion\/rest\/v1\/projects/, respond: () => jsonResponse({}, 401) },
    ]);
    window.history.replaceState({}, '', '?');
    render(<App />);
    await vi.waitFor(() => expect(document.querySelector('.landing .alert-error')).not.toBeNull());
    expect(document.querySelector('.alert-error')!.textContent).toContain('Could not load projects');
  });
});
