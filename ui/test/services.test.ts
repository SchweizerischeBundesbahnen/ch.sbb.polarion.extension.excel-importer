import { afterEach, describe, expect, it, vi } from 'vitest';
import { getCookie, setCookie } from '../src/services/cookies';
import { fetchProjects } from '../src/services/projects';
import { getProjectIdFromScope, getScope } from '../src/services/scope';

// Unit tests for the app's local services. cookies/scope are pure DOM/URL logic; fetchProjects hits the
// platform REST API, mocked here at the fetch boundary.

const origUrl = window.location.pathname + window.location.search;
const setSearch = (s: string) => window.history.replaceState({}, '', s || window.location.pathname);

function clearCookies() {
  for (const part of document.cookie.split('; ')) {
    const name = part.split('=')[0];
    if (name) document.cookie = `${name}=; path=/; max-age=0`;
  }
}

afterEach(() => {
  window.history.replaceState({}, '', origUrl);
  clearCookies();
  vi.unstubAllGlobals();
});

describe('cookies', () => {
  it('round-trips a value and returns null for a missing one', () => {
    setCookie('sel', 'abc');
    expect(getCookie('sel')).toBe('abc');
    expect(getCookie('nope')).toBeNull();
  });

  it('encodes and decodes special characters', () => {
    setCookie('k', 'a b;c=d');
    expect(getCookie('k')).toBe('a b;c=d');
  });
});

describe('getScope', () => {
  it('is empty without the param and normalizes the trailing slash', () => {
    setSearch('?feature=mappings');
    expect(getScope()).toBe('');
    setSearch('?scope=project/elibrary');
    expect(getScope()).toBe('project/elibrary/');
    setSearch('?scope=project/elibrary/');
    expect(getScope()).toBe('project/elibrary/');
  });
});

describe('getProjectIdFromScope', () => {
  it('extracts the id or returns empty for non-project scopes', () => {
    expect(getProjectIdFromScope('project/elibrary/')).toBe('elibrary');
    expect(getProjectIdFromScope('project/elibrary')).toBe('elibrary');
    expect(getProjectIdFromScope('')).toBe('');
    expect(getProjectIdFromScope('repository/')).toBe('');
  });
});

describe('fetchProjects', () => {
  it('maps, filters and sorts the JSON:API project list', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () =>
          new Response(
            JSON.stringify({
              data: [
                { id: 'zeta', attributes: { name: 'Zeta' } },
                { id: 'alpha', attributes: { name: 'Alpha' } },
                { attributes: { name: 'no-id' } }, // dropped: no id
              ],
            }),
            { status: 200, headers: { 'Content-Type': 'application/json' } },
          ),
      ),
    );
    const projects = await fetchProjects();
    expect(projects.map((p) => p.id)).toEqual(['alpha', 'zeta']); // sorted by name, id-less dropped
    expect(projects[0]).toEqual({ id: 'alpha', name: 'Alpha' });
  });

  it('falls back to attributes.id for the id and to the id for a missing name', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () =>
          new Response(JSON.stringify({ data: [{ attributes: { id: 'p2' } }] }), {
            status: 200,
            headers: { 'Content-Type': 'application/json' },
          }),
      ),
    );
    const projects = await fetchProjects();
    expect(projects).toEqual([{ id: 'p2', name: '' }]);
  });

  it('returns an empty list when the response has no data array', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(
        async () => new Response(JSON.stringify({}), { status: 200, headers: { 'Content-Type': 'application/json' } }),
      ),
    );
    expect(await fetchProjects()).toEqual([]);
  });

  it('throws on a non-ok response', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => new Response('', { status: 500 })),
    );
    await expect(fetchProjects()).rejects.toThrow('HTTP 500');
  });
});
