import { useCallback, useMemo } from 'react';
import type { FieldMetadata, MappingSettings, Revision, SettingName, WorkItemType } from '../types';
import useRemote from './useRemote';

/** The named-settings feature id (matches the JSP's `setting` and the backend `{feature}` path). */
const FEATURE = 'mappings';

/** Extract a human-readable error message from a failed Response, mirroring the generic
 * ExtensionContext.callAsync error handling (`{message}` JSON, falling back to raw text/status). */
async function errorMessage(response: Response): Promise<string> {
  const text = await response.text().catch(() => '');
  if (text) {
    try {
      const parsed = JSON.parse(text);
      if (parsed?.message) return parsed.message;
      if (parsed?.errorMessage) return parsed.errorMessage;
    } catch {
      return text;
    }
  }
  return `HTTP ${response.status}`;
}

async function jsonOrThrow<T>(response: Response): Promise<T> {
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
  return (await response.json()) as T;
}

async function okOrThrow(response: Response): Promise<void> {
  if (!response.ok) {
    throw new Error(await errorMessage(response));
  }
}

/**
 * REST helpers for the Mappings page: the generic named-settings endpoints (list / content /
 * default / rename / delete / revisions) plus this extension's work-item type & field endpoints.
 * Built on `useRemote`, so it uses the session `/internal` endpoints in Polarion and the token
 * `/api` endpoints in `vite dev`.
 */
export default function useSettings() {
  const { sendRequest } = useRemote();

  const settingsPath = useCallback((suffix: string): string => `/settings/${FEATURE}${suffix}`, []);

  const loadConfigurationNames = useCallback(
    (scope: string): Promise<SettingName[]> =>
      sendRequest({ method: 'GET', url: settingsPath(`/names?scope=${encodeURIComponent(scope)}`) }).then((r) =>
        jsonOrThrow<SettingName[]>(r),
      ),
    [sendRequest, settingsPath],
  );

  const loadContent = useCallback(
    (name: string, scope: string, revision?: string): Promise<MappingSettings> => {
      let url = settingsPath(`/names/${encodeURIComponent(name)}/content?scope=${encodeURIComponent(scope)}`);
      if (revision) {
        url += `&revision=${encodeURIComponent(revision)}`;
      }
      return sendRequest({ method: 'GET', url }).then((r) => jsonOrThrow<MappingSettings>(r));
    },
    [sendRequest, settingsPath],
  );

  const saveContent = useCallback(
    (name: string, scope: string, content: MappingSettings): Promise<void> =>
      sendRequest({
        method: 'PUT',
        url: settingsPath(`/names/${encodeURIComponent(name)}/content?scope=${encodeURIComponent(scope)}`),
        contentType: 'application/json',
        body: JSON.stringify(content),
      }).then(okOrThrow),
    [sendRequest, settingsPath],
  );

  /** Create a new named configuration. An empty body makes the backend seed default values. */
  const createConfiguration = useCallback(
    (name: string, scope: string): Promise<void> =>
      sendRequest({
        method: 'PUT',
        url: settingsPath(`/names/${encodeURIComponent(name)}/content?scope=${encodeURIComponent(scope)}`),
        contentType: 'application/json',
      }).then(okOrThrow),
    [sendRequest, settingsPath],
  );

  const renameConfiguration = useCallback(
    (name: string, scope: string, newName: string): Promise<void> =>
      sendRequest({
        method: 'POST',
        url: settingsPath(`/names/${encodeURIComponent(name)}?scope=${encodeURIComponent(scope)}`),
        contentType: 'application/json',
        body: newName,
      }).then(okOrThrow),
    [sendRequest, settingsPath],
  );

  const deleteConfiguration = useCallback(
    (name: string, scope: string): Promise<void> =>
      sendRequest({
        method: 'DELETE',
        url: settingsPath(`/names/${encodeURIComponent(name)}?scope=${encodeURIComponent(scope)}`),
      }).then(okOrThrow),
    [sendRequest, settingsPath],
  );

  const loadRevisions = useCallback(
    (name: string, scope: string): Promise<Revision[]> =>
      sendRequest({
        method: 'GET',
        url: settingsPath(`/names/${encodeURIComponent(name)}/revisions?scope=${encodeURIComponent(scope)}`),
      }).then((r) => jsonOrThrow<Revision[]>(r)),
    [sendRequest, settingsPath],
  );

  const loadWorkItemTypes = useCallback(
    (projectId: string): Promise<WorkItemType[]> =>
      sendRequest({
        method: 'GET',
        url: `/projects/${encodeURIComponent(projectId)}/workitem_types`,
      }).then((r) => jsonOrThrow<WorkItemType[]>(r)),
    [sendRequest],
  );

  const loadFields = useCallback(
    (projectId: string, workItemType: string): Promise<FieldMetadata[]> =>
      sendRequest({
        method: 'GET',
        url: `/projects/${encodeURIComponent(projectId)}/workitem_types/${encodeURIComponent(workItemType)}/fields`,
      }).then((r) => jsonOrThrow<FieldMetadata[]>(r)),
    [sendRequest],
  );

  return useMemo(
    () => ({
      loadConfigurationNames,
      loadContent,
      saveContent,
      createConfiguration,
      renameConfiguration,
      deleteConfiguration,
      loadRevisions,
      loadWorkItemTypes,
      loadFields,
    }),
    [
      loadConfigurationNames,
      loadContent,
      saveContent,
      createConfiguration,
      renameConfiguration,
      deleteConfiguration,
      loadRevisions,
      loadWorkItemTypes,
      loadFields,
    ],
  );
}
