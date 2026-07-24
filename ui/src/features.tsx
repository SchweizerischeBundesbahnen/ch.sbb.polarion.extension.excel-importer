import type { ComponentType } from 'react';
import About from './pages/About';
import ImportFile from './pages/ImportFile';
import Mappings from './pages/Mappings';
import UserGuide from './pages/UserGuide';

/**
 * A single navigable page of the app. The `id` is what appears in the URL as `?feature=<id>`
 * and is also what `hivemodule.xml` will point its admin extenders at once we flip Polarion
 * over to this app. Keep the ids stable and aligned with the existing extender ids.
 */
export interface Feature {
  id: string;
  label: string;
  description: string;
  component: ComponentType;
}

export const FEATURES: Feature[] = [
  {
    id: 'about',
    label: 'About',
    description: 'Extension version and general information.',
    component: About,
  },
  {
    id: 'mappings',
    label: 'Mappings',
    description: 'Configure how Excel columns map to Polarion work item fields.',
    component: Mappings,
  },
  {
    id: 'user-guide',
    label: 'User Guide',
    description: 'How to configure mappings and import files.',
    component: UserGuide,
  },
  {
    id: 'import-file',
    label: 'Import File',
    description: 'Upload an Excel file and import its rows as work items.',
    component: ImportFile,
  },
];

export function findFeature(id: string | null): Feature | undefined {
  return FEATURES.find((f) => f.id === id);
}
