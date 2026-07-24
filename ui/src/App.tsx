import { BreadcrumbInjector, Toaster } from '@grigoriev/react-sbb-polarion';
import { findFeature } from './features';
import Landing from './pages/Landing';

/**
 * Top-level feature router. There is a single index.html / bundle; the page to show is chosen
 * from the `feature` query parameter, e.g. `?feature=mappings`. No matching feature (including
 * the bare root `/`) renders the Landing stub, which lists links to every feature so the whole
 * app can be exercised in `vite dev` without a running Polarion.
 *
 * In Polarion, hivemodule.xml will point each admin extender at
 * `/polarion/excel-importer-app/ui/app/index.html?feature=<id>&scope=$scope$`.
 */
export default function App() {
  const feature = new URLSearchParams(window.location.search).get('feature');
  const match = findFeature(feature);
  const Page = match ? match.component : Landing;

  return (
    // `standard-admin-page` scopes the shared generic checkbox styling (checkboxes.css) to the app.
    <div className="app standard-admin-page">
      {/* Fixes the app-header breadcrumb when opened as a project-navigation topic (nav extender). */}
      <BreadcrumbInjector
        marker="excel-importer"
        title="Excel Importer"
        icon="/polarion/excel-importer-admin/ui/images/menu/30x30/_parent.svg"
      />
      {/* App-wide toast host: the shared react-sbb-polarion Toaster (top-center + richColors, so
          success toasts are green, errors red). Toasts are fired with `toast()` from sonner. */}
      <Toaster />
      <Page />
    </div>
  );
}
