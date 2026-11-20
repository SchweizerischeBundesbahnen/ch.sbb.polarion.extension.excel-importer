import type { ComponentType } from 'react';
import { DocPage } from '@sbb-polarion/react-sbb-polarion';
import { DOC_ORDER } from './docs/manifest';
import About from './pages/About';
import ImportFile from './pages/ImportFile';
import Mappings from './pages/Mappings';

/**
 * A single navigable page of the app. The `id` is what appears in the URL as `?feature=<id>` and is
 * also what `hivemodule.xml` points its admin extenders at, so the ids here and the `pageUrl`s there
 * must stay identical - a typo is a blank page in Polarion and no test catches it.
 */
export interface Feature {
  id: string;
  label: string;
  description: string;
  component: ComponentType;
}

// The documentation-site articles, derived from the shared manifest (docs.config.json) rather than listed by
// hand: each renders through the single DocPage bound to its manifest entry, in the manifest's reading order.
// Adding an article is a docs.config.json item (plus its markdown and the pom render) - nothing here changes.
// They carry no hivemodule.xml menu entry of their own: they are reached from the single `documentation` node
// and the articles' cross-document links (rewritten to ?feature=<id> by the interceptor in App.tsx); the ids
// equal the generated html basenames those links point at.
const DOC_FEATURES: Feature[] = DOC_ORDER.map((doc) => {
  const Component = () => <DocPage doc={doc} />;
  Component.displayName = `DocPage(${doc.id})`;
  return {
    id: doc.id,
    label: doc.title,
    description: `${doc.title}, generated from ${doc.source}.`,
    component: Component,
  };
});

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
  // The documentation site, in manifest reading order (Quick Start, User Guide, REST API, ...).
  ...DOC_FEATURES,
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
