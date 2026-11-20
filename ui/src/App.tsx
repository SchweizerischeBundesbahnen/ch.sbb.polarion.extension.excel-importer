import {
  BreadcrumbInjector,
  DocLinkInterceptor,
  DocsProvider,
  Toaster,
  buildDocsConfig,
} from '@sbb-polarion/react-sbb-polarion';
import { DOC_ORDER } from './docs/manifest';
import { SEARCH_INDEX } from './docs/searchIndex';
import { SOURCE_BASE_URL } from './docs/source';
import { findFeature } from './features';
import Landing from './pages/Landing';
import { switchToFeatureNode } from './services/adminNav';

/**
 * Documentation-site configuration for the shared components (@sbb-polarion/react-sbb-polarion): the article
 * manifest, the build-generated search index, and the admin-shell sync a cross-document link triggers. The
 * help articles cross-link with plain relative markdown links so they work as-is on GitHub; the build leaves
 * them relative, and DocLinkInterceptor resolves a click at runtime - a `.md` source through `mdLinkMap` to a
 * `?feature=` switch, and any other relative link (docs/openapi.json) to `sourceBaseUrl` on GitHub.
 * `onDocLinkNavigate` prefers switching the admin shell's node (so Polarion's breadcrumb and left menu
 * follow); it returns false when it cannot, and the interceptor then falls back to a plain in-frame navigation.
 */
const docsConfig = buildDocsConfig({
  docs: DOC_ORDER,
  searchIndex: SEARCH_INDEX,
  sourceBaseUrl: SOURCE_BASE_URL,
  // The README is not a doc-site article, but it is the About page: a link to it switches there.
  extraMdLinks: { 'README.md': 'about' },
  onDocLinkNavigate: switchToFeatureNode,
});

/**
 * Top-level feature router. There is a single index.html / bundle; the page to show is chosen
 * from the `feature` query parameter, e.g. `?feature=mappings`. No matching feature (including
 * the bare root `/`) renders the Landing stub, which lists links to every feature so the whole
 * app can be exercised in `vite dev` without a running Polarion.
 *
 * In Polarion, hivemodule.xml points each admin extender at
 * `/polarion/excel-importer-app/ui/app/index.html?feature=<id>&scope=$scope$`.
 */
export default function App() {
  const feature = new URLSearchParams(window.location.search).get('feature');
  const match = findFeature(feature);
  const Page = match ? match.component : Landing;

  return (
    <DocsProvider config={docsConfig}>
      {/* Delegates cross-document help-article links to in-app feature navigation (see docsConfig). */}
      <DocLinkInterceptor>
        {/* `standard-admin-page` scopes the shared generic checkbox styling (checkboxes.css) to the app. */}
        <div className="app standard-admin-page">
          {/* Fixes the app-header breadcrumb when opened as a project-navigation topic (nav extender). */}
          <BreadcrumbInjector
            marker="excel-importer"
            title="Excel Importer"
            icon="/polarion/excel-importer-app/ui/images/menu/30x30/_parent.svg"
          />
          {/* App-wide toast host: the shared react-sbb-polarion Toaster (top-center + richColors, so
              success toasts are green, errors red). Toasts are fired with `toast()` from sonner. */}
          <Toaster />
          <Page />
        </div>
      </DocLinkInterceptor>
    </DocsProvider>
  );
}
