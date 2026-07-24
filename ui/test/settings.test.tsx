import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import useSettings from '../src/services/settings';
import type { MappingSettings } from '../src/types';
import { type FetchMock, installFetchMock, jsonResponse } from './mockFetch';

// Unit tests for the useSettings REST hook, driven via a tiny capture component so we can call each
// function directly with mocked fetch and assert the request (method / url / body) and the error paths.

let api: ReturnType<typeof useSettings>;
function Capture() {
  api = useSettings();
  return null;
}

let fetchMock: FetchMock;
async function mountApi(routes: Parameters<typeof installFetchMock>[0] = []) {
  fetchMock = installFetchMock(routes);
  render(<Capture />);
  await vi.waitFor(() => expect(api).toBeTruthy());
}

const lastCall = () => fetchMock.mock.calls[fetchMock.mock.calls.length - 1];

beforeEach(() => {
  api = undefined as unknown as ReturnType<typeof useSettings>;
});
afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

const CONTENT: MappingSettings = {
  sheetName: 'S',
  startFromRow: 1,
  overwriteWithEmpty: false,
  ignoreUnknown: false,
  unlinkExisting: false,
  columnsMapping: { A: 'title' },
  defaultWorkItemType: 'requirement',
  linkColumn: 'A',
};

describe('useSettings requests', () => {
  it('GET names / content (with revision) / revisions / workitem types / fields', async () => {
    await mountApi([
      { method: 'GET', match: /\/names\?/, json: [{ name: 'x', scope: '' }] },
      { method: 'GET', match: /\/content\?/, json: CONTENT },
      { method: 'GET', match: /\/revisions\?/, json: [{ name: '1' }] },
      { method: 'GET', match: /\/workitem_types\/[^/]+\/fields/, json: [{ id: 'title' }] },
      { method: 'GET', match: /\/workitem_types(\?|$)/, json: [{ id: 'requirement', name: 'Req' }] },
    ]);
    expect(await api.loadConfigurationNames('project/x/')).toEqual([{ name: 'x', scope: '' }]);
    expect(String(lastCall()[0])).toContain('/settings/mappings/names?scope=project%2Fx%2F');

    await api.loadContent('cfg', 'project/x/', '42');
    expect(String(lastCall()[0])).toContain('/names/cfg/content?scope=project%2Fx%2F&revision=42');

    expect(await api.loadRevisions('cfg', '')).toEqual([{ name: '1' }]);
    expect(await api.loadWorkItemTypes('elibrary')).toEqual([{ id: 'requirement', name: 'Req' }]);
    expect(await api.loadFields('elibrary', 'requirement')).toEqual([{ id: 'title' }]);
  });

  it('PUT saveContent (JSON body) and createConfiguration (no body)', async () => {
    await mountApi([{ method: 'PUT', match: /\/content\?/, respond: () => new Response(null, { status: 204 }) }]);

    await api.saveContent('cfg', '', CONTENT);
    let call = lastCall();
    expect((call[1]?.method ?? '').toUpperCase()).toBe('PUT');
    expect(call[1]?.body).toBe(JSON.stringify(CONTENT));

    await api.createConfiguration('new-cfg', 'project/x/');
    call = lastCall();
    expect((call[1]?.method ?? '').toUpperCase()).toBe('PUT');
    expect(String(call[0])).toContain('/names/new-cfg/content?scope=project%2Fx%2F');
    expect(call[1]?.body).toBeUndefined(); // empty body seeds defaults
  });

  it('POST rename (body = new name) and DELETE', async () => {
    await mountApi([
      { method: 'POST', match: /\/names\/[^/]+\?/, respond: () => new Response(null, { status: 204 }) },
      { method: 'DELETE', match: /\/names\/[^/]+\?/, respond: () => new Response(null, { status: 204 }) },
    ]);
    await api.renameConfiguration('old', '', 'new');
    let call = lastCall();
    expect((call[1]?.method ?? '').toUpperCase()).toBe('POST');
    expect(call[1]?.body).toBe('new');

    await api.deleteConfiguration('old', 'project/x/');
    call = lastCall();
    expect((call[1]?.method ?? '').toUpperCase()).toBe('DELETE');
    expect(String(call[0])).toContain('/names/old?scope=project%2Fx%2F');
  });

  it('rejects with the server errorMessage / message / raw text / HTTP status', async () => {
    await mountApi([{ method: 'GET', match: /\/names\?/, respond: () => jsonResponse({ errorMessage: 'boom' }, 500) }]);
    await expect(api.loadConfigurationNames('')).rejects.toThrow('boom');

    installFetchMock([{ method: 'GET', match: /\/names\?/, respond: () => jsonResponse({ message: 'msg' }, 400) }]);
    await expect(api.loadConfigurationNames('')).rejects.toThrow('msg');

    installFetchMock([
      { method: 'GET', match: /\/names\?/, respond: () => new Response('plain text error', { status: 400 }) },
    ]);
    await expect(api.loadConfigurationNames('')).rejects.toThrow('plain text error');

    installFetchMock([{ method: 'GET', match: /\/names\?/, respond: () => new Response('', { status: 503 }) }]);
    await expect(api.loadConfigurationNames('')).rejects.toThrow('HTTP 503');
  });

  it('rejects a failed save (okOrThrow)', async () => {
    await mountApi([
      { method: 'PUT', match: /\/content\?/, respond: () => jsonResponse({ errorMessage: 'denied' }, 409) },
    ]);
    await expect(api.saveContent('cfg', '', CONTENT)).rejects.toThrow('denied');
  });
});
