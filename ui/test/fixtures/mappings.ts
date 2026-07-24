import type { FieldMetadata, MappingSettings, SettingName, WorkItemType } from '../../src/types';

// Canned REST payloads for the Mappings page. Kept small and project-scoped so the page renders a
// representative, note-free state (no "Default" / "inherited from global" notes).

export const SCOPE = 'project/elibrary/';
export const PROJECT_ID = 'elibrary';

// First entry (project scope == current scope) is auto-selected; no inherited-from-global note.
export const NAMES: SettingName[] = [
  { name: 'requirements-import', scope: 'project/elibrary/' },
  { name: 'Default', scope: '' },
];

export const CONTENT: MappingSettings = {
  sheetName: 'Sheet1',
  startFromRow: 2,
  overwriteWithEmpty: false,
  ignoreUnknown: true,
  unlinkExisting: false,
  columnsMapping: { A: 'title', B: 'description' },
  enumsMapping: {},
  stepsMapping: {},
  defaultWorkItemType: 'requirement',
  linkColumn: 'A',
};

export const WORKITEM_TYPES: WorkItemType[] = [
  { id: 'requirement', name: 'Requirement' },
  { id: 'testcase', name: 'Test Case' },
];

// Plain (non-enum, non-TestSteps) fields keep the snapshot simple and deterministic.
export const FIELDS: FieldMetadata[] = [
  { id: 'title', type: {} },
  { id: 'description', type: {} },
  { id: 'priority', type: {} },
];

export const REVISIONS = [
  { name: '3388', date: '2026-07-01', author: 'alice', baseline: '', description: 'Initial' },
  { name: '3401', date: '2026-07-05', author: 'bob', baseline: '', description: 'Add link column' },
];

/** The default route set that renders the Mappings page fully loaded. */
export function mappingsRoutes() {
  return [
    { method: 'GET', match: /\/settings\/mappings\/names\?/, json: NAMES },
    { method: 'GET', match: /\/settings\/mappings\/names\/[^/]+\/revisions/, json: REVISIONS },
    { method: 'GET', match: /\/settings\/mappings\/names\/[^/]+\/content/, json: CONTENT },
    { method: 'GET', match: /\/projects\/[^/]+\/workitem_types\/[^/]+\/fields/, json: FIELDS },
    { method: 'GET', match: /\/projects\/[^/]+\/workitem_types(\?|$)/, json: WORKITEM_TYPES },
    {
      method: 'PUT',
      match: /\/settings\/mappings\/names\/[^/]+\/content/,
      status: 204,
      respond: () => new Response(null, { status: 204 }),
    },
  ];
}
