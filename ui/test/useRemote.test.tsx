import { afterEach, describe, expect, it, vi } from 'vitest';
import { cleanup, render } from 'vitest-browser-react';
import useRemote from '../src/services/useRemote';

// useRemote is exercised end-to-end through the pages; this covers its error branch directly: a fetch
// rejection is turned into a 503 "network error" Response rather than throwing.

let api: ReturnType<typeof useRemote>;
function Capture() {
  api = useRemote();
  return null;
}

afterEach(() => {
  cleanup();
  vi.unstubAllGlobals();
});

describe('useRemote', () => {
  it('returns a 503 network-error response when fetch rejects', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new Error('down');
      }),
    );
    render(<Capture />);
    await vi.waitFor(() => expect(api).toBeTruthy());
    const res = await api.sendRequest({ method: 'GET', url: '/version' });
    expect(res.status).toBe(503);
    expect((await res.json()).errorMessage).toContain('Network error');
  });

  it('surfaces a redirect from followUrl as an opaqueredirect (manual redirect)', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn(async () => {
        throw new Error('down');
      }),
    );
    render(<Capture />);
    await vi.waitFor(() => expect(api).toBeTruthy());
    const res = await api.followUrl('GET', 'http://localhost/polarion/x/import/jobs/1');
    expect(res.status).toBe(503); // fetch rejected -> network-error response
  });
});
