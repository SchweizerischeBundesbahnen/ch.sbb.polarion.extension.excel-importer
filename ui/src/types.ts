/** One saved mapping configuration name (generic settings framework: SettingName). */
export interface MappingName {
  name: string;
  scope: string;
}

/** Result of an Excel import (service.ImportResult). */
export interface ImportResult {
  createdIds: string[];
  updatedIds: string[];
  unchangedIds: string[];
  skippedIds: string[];
  log: string;
}

/** A saved named setting (generic settings.SettingName). `scope` differs from the current scope
 * when the configuration is inherited from a broader (e.g. global) scope. */
export interface SettingName {
  name: string;
  scope: string;
}

// The Revision type is owned by react-sbb-polarion (used by its shared RevisionsTable); re-exported
// here so this app's local imports (settings.ts, Mappings.tsx) keep a single `../types` source.
export type { Revision } from '@grigoriev/react-sbb-polarion';

/** One selectable option of an enum / test-steps field (generic fields.model.Option). */
export interface FieldOption {
  key: string;
  name: string;
  iconUrl?: string;
}

/** Metadata of a work item field (generic fields.model.FieldMetadata). `type.structTypeId`
 * distinguishes enums from Test Steps; `options` is present for enum/test-steps fields. */
export interface FieldMetadata {
  id: string;
  label?: string;
  type?: { structTypeId?: string } | null;
  custom?: boolean;
  required?: boolean;
  readOnly?: boolean;
  multi?: boolean;
  options?: FieldOption[] | null;
}

/** A work item type option (Polarion ITypeOpt), as returned by the workitem_types endpoint. */
export interface WorkItemType {
  id: string;
  name: string;
}

/** The Mappings settings content (excel_importer settings.ExcelSheetMappingSettingsModel).
 * Mirrors the JSON the content endpoints exchange. Optional maps may be omitted (NON_NULL). */
export interface MappingSettings {
  sheetName: string;
  startFromRow: number;
  overwriteWithEmpty: boolean;
  ignoreUnknown: boolean;
  unlinkExisting: boolean;
  columnsMapping: Record<string, string>;
  enumsMapping?: Record<string, Record<string, string>>;
  stepsMapping?: Record<string, Record<string, string>>;
  defaultWorkItemType: string;
  linkColumn: string;
}
