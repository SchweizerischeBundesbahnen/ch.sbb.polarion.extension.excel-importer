import { useCallback, useMemo } from 'react';

const REST_PATH = '/polarion/excel-importer/rest';

interface RequestParams {
  method: string;
  url: string;
  body?: BodyInit;
  contentType?: string;
}

/** A Location header may come back as an absolute URL; reduce it to a same-origin path so the
 * Vite dev proxy handles it (and so it stays same-origin in Polarion). */
function toLocalPath(url: string): string {
  if (/^https?:\/\//i.test(url)) {
    const parsed = new URL(url);
    return parsed.pathname + parsed.search;
  }
  return url;
}

/**
 * Thin wrapper over fetch for the extension's REST API. In `vite dev` requests are proxied to a
 * real Polarion (see vite.config.js); a personal access token in VITE_BEARER_TOKEN switches to
 * the token-authenticated `/api` endpoints, otherwise the session-based `/internal` endpoints
 * are used.
 */
export default function useRemote() {
  const token = import.meta.env.VITE_BEARER_TOKEN;
  const restBase = useMemo(() => `${REST_PATH}${token ? '/api' : '/internal'}`, [token]);

  const authHeaders = useCallback(
    (extra?: Record<string, string>): Record<string, string> => {
      const headers: Record<string, string> = { ...extra };
      if (token) {
        headers['Authorization'] = `Bearer ${token}`;
      }
      return headers;
    },
    [token],
  );

  const networkErrorResponse = () =>
    new Response(
      JSON.stringify({ errorMessage: 'Network error occurred. Be sure Polarion is started and accessible.' }),
      {
        status: 503,
        headers: { 'Content-Type': 'application/json' },
      },
    );

  /** Call an endpoint relative to the REST base (e.g. `/import/jobs`, `/settings/mappings/names`). */
  const sendRequest = useCallback(
    ({ method, url, body, contentType }: RequestParams): Promise<Response> => {
      // For FormData bodies do NOT set Content-Type; the browser adds the multipart boundary.
      const headers = authHeaders(contentType ? { 'Content-Type': contentType } : undefined);
      return fetch(`${restBase}${url}`, { method, mode: 'cors', cache: 'no-cache', headers, body }).catch(
        networkErrorResponse,
      );
    },
    [restBase, authHeaders],
  );

  /**
   * Call an absolute-or-relative URL as returned by the server (e.g. an async job Location).
   * Uses `redirect: 'manual'` so a server 3xx is surfaced (as an `opaqueredirect` response) instead
   * of being auto-followed: an auto-followed absolute redirect to the Polarion origin would drop the
   * Authorization header (cross-origin) and 401. Callers resolve the redirect target themselves via
   * this same helper, keeping it same-origin and authenticated.
   */
  const followUrl = useCallback(
    (method: string, url: string): Promise<Response> =>
      fetch(toLocalPath(url), {
        method,
        mode: 'cors',
        cache: 'no-cache',
        headers: authHeaders(),
        redirect: 'manual',
      }).catch(networkErrorResponse),
    [authHeaders],
  );

  return { sendRequest, followUrl };
}
