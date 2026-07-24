import { useCallback, useEffect, useRef, useState } from 'react';
import { PageLayout } from '@grigoriev/react-sbb-polarion';
import { SearchableSelect } from '@grigoriev/react-sbb-polarion';
import { getProjectIdFromScope, getScope } from '../services/scope';
import useRemote from '../services/useRemote';
import type { ImportResult, MappingName } from '../types';

const SELECTED_MAPPING_COOKIE = 'selected-configuration-mappings';
const POLL_INTERVAL_MS = 1000;

function getCookie(name: string): string | null {
  const prefix = `${name}=`;
  for (const part of document.cookie.split('; ')) {
    if (part.startsWith(prefix)) return decodeURIComponent(part.substring(prefix.length));
  }
  return null;
}

function setCookie(name: string, value: string): void {
  document.cookie = `${name}=${encodeURIComponent(value)}; path=/; max-age=${60 * 60 * 24 * 365}; SameSite=Lax`;
}

/** `<basename>_YYYY_MM_DD_HH_MM_SS.txt`, sanitized, mirroring the legacy log file name. */
function generateLogFileName(importFileName: string): string {
  const baseName = importFileName
    .replace(/\.[^/.]+$/, '')
    .replace(/[^a-zA-Z0-9-_]/g, '')
    .replace(/\s+/g, '_');
  const now = new Date();
  const p = (n: number) => String(n).padStart(2, '0');
  const stamp = `${now.getFullYear()}_${p(now.getMonth() + 1)}_${p(now.getDate())}_${p(now.getHours())}_${p(now.getMinutes())}_${p(now.getSeconds())}`;
  return `${baseName}_${stamp}.txt`;
}

function downloadText(text: string, filename: string): void {
  const url = URL.createObjectURL(new Blob([text], { type: 'text/plain' }));
  const link = document.createElement('a');
  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export default function ImportFile() {
  const { sendRequest, followUrl } = useRemote();
  const scope = getScope();
  const projectId = getProjectIdFromScope(scope);

  const [mappings, setMappings] = useState<string[]>([]);
  const [mappingsLoaded, setMappingsLoaded] = useState(false);
  const [selectedMapping, setSelectedMapping] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const [importing, setImporting] = useState(false);
  const [result, setResult] = useState<ImportResult | null>(null);
  const [logFileName, setLogFileName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load available mapping configurations for the current scope.
  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const response = await sendRequest({
          method: 'GET',
          url: `/settings/mappings/names?scope=${encodeURIComponent(scope)}`,
        });
        if (cancelled) return;
        if (!response.ok) {
          const data = await response.json().catch(() => null);
          const detail = data?.errorMessage || data?.message || `HTTP ${response.status}`;
          setLoadError(`Failed to load mapping configurations (${detail}).`);
          setMappingsLoaded(true);
          return;
        }
        const items: MappingName[] = await response.json();
        if (cancelled) return;
        const names = items.map((i) => i.name);
        setMappings(names);
        const remembered = getCookie(SELECTED_MAPPING_COOKIE);
        setSelectedMapping(remembered && names.includes(remembered) ? remembered : names[0] || '');
        setMappingsLoaded(true);
      } catch (e) {
        if (cancelled) return;
        setLoadError(`Failed to load mapping configurations (${(e as Error).message}).`);
        setMappingsLoaded(true);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [sendRequest, scope]);

  useEffect(() => {
    if (selectedMapping) setCookie(SELECTED_MAPPING_COOKIE, selectedMapping);
  }, [selectedMapping]);

  // Poll the async import job until it finishes. The status endpoint returns 202 while running and
  // a 303 redirect to `.../result` when done. We do NOT let fetch auto-follow that redirect (its
  // Location is an absolute Polarion URL, so a cross-origin follow would drop the token and 401);
  // instead we detect the redirect and fetch `<jobUrl>/result` ourselves, same-origin and authed.
  const pollJob = useCallback(
    async (location: string): Promise<ImportResult> => {
      for (;;) {
        await new Promise((resolve) => window.setTimeout(resolve, POLL_INTERVAL_MS));
        const response = await followUrl('GET', location);
        if (response.type === 'opaqueredirect' || response.status === 303) {
          const resultResponse = await followUrl('GET', `${location.replace(/\/$/, '')}/result`);
          if (!resultResponse.ok) {
            const data = await resultResponse.json().catch(() => null);
            throw new Error(data?.errorMessage || `Import failed with status ${resultResponse.status}`);
          }
          return (await resultResponse.json()) as ImportResult;
        }
        if (response.status === 202) {
          continue;
        }
        const data = await response.json().catch(() => null);
        throw new Error(data?.errorMessage || `Import failed with status ${response.status}`);
      }
    },
    [followUrl],
  );

  const handleImport = async () => {
    if (!file || importing) return;
    setError(null);
    setResult(null);
    setImporting(true);
    const importedName = file.name;

    try {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('mappingName', selectedMapping);
      formData.append('projectId', projectId);

      const response = await sendRequest({ method: 'POST', url: '/import/jobs', body: formData });
      if (response.status !== 202) {
        const data = await response.json().catch(() => null);
        throw new Error(data?.errorMessage || `Import failed with status ${response.status}`);
      }
      const location = response.headers.get('Location');
      if (!location) {
        throw new Error('Import job did not return a status location.');
      }

      const importResult = await pollJob(location);
      setResult(importResult);
      setLogFileName(generateLogFileName(importedName));
      setFile(null);
      if (fileInputRef.current) fileInputRef.current.value = '';
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setImporting(false);
    }
  };

  return (
    <PageLayout title="Import File">
      {!mappingsLoaded ? (
        <p>Loading...</p>
      ) : loadError ? (
        <div className="alert alert-error">{loadError}</div>
      ) : mappings.length === 0 ? (
        <p className="no-mapping-note">
          <i>No mapping configurations found. Please create a configuration first.</i>
        </p>
      ) : (
        <div className="import-panel">
          <div className="import-row">
            <label>Mapping:</label>
            <SearchableSelect
              value={selectedMapping}
              onChange={setSelectedMapping}
              options={mappings.map((name) => ({ id: name, name }))}
              disabled={importing}
            />
          </div>

          <div className="import-row">
            <label className={`sbb-btn sbb-btn--control upload-button${importing ? ' -disabled' : ''}`}>
              Choose xlsx file
              <input
                ref={fileInputRef}
                type="file"
                accept=".xlsx"
                disabled={importing}
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              />
            </label>
            <span className="file-name">{file ? file.name : 'No file chosen'}</span>
          </div>

          {importing && (
            <div className="import-progress">
              <span className="spinner" />
              <span>Import is in progress. Please wait...</span>
            </div>
          )}

          <div className="import-row">
            <button
              className="sbb-btn sbb-btn--control"
              disabled={!file || importing}
              title={!file ? 'Please choose excel file using button above' : 'Initiate processing'}
              onClick={handleImport}
            >
              Import
            </button>
          </div>

          {error && <div className="alert alert-error">Import error ({error})</div>}

          {result && (
            <div className="alert alert-success">
              File successfully imported. Created: {result.createdIds.length}, updated: {result.updatedIds.length},
              unchanged: {result.unchangedIds.length}, skipped: {result.skippedIds.length}.{' '}
              <a
                href="#"
                onClick={(e) => {
                  e.preventDefault();
                  downloadText(result.log, logFileName);
                }}
              >
                (log)
              </a>
            </div>
          )}
        </div>
      )}
    </PageLayout>
  );
}
