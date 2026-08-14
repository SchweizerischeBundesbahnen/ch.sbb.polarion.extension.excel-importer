import react from '@vitejs/plugin-react';
import { copyFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { defineConfig, loadEnv } from 'vite';

// react-sbb-polarion ships BreadcrumbBridge.js as a classic script that runs in the Polarion shell
// window, outside this app's frame, so it cannot be imported and has to be served as a file. Copying
// it next to the built app is what replaces fetching it from generic's webapp.
function copyRspShellScripts() {
  return {
    name: 'copy-rsp-shell-scripts',
    writeBundle(options) {
      const require = createRequire(import.meta.url);
      copyFileSync(
        require.resolve('@grigoriev/react-sbb-polarion/breadcrumb-bridge.js'),
        `${options.dir}/breadcrumb-bridge.js`,
      );
    },
  };
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const polarionUrl = env.VITE_BASE_URL || 'http://localhost';

  // The shared @grigoriev/react-sbb-polarion (RSP) package is linked via a `file:` dependency, which npm
  // symlinks into node_modules together with its own dev copy of React. Dedupe so the app and the
  // linked library resolve to this app's single React instance (avoids the dual-React "invalid hook
  // call"). Harmless once the package is consumed from a registry instead of a symlink.
  // sonner is deduped too: the app's `toast()` and RSP's `Toaster` host must share one sonner instance
  // (the file:-linked RSP has its own copy), or fired toasts never reach the host.
  const resolve = { dedupe: ['react', 'react-dom', 'sonner'] };

  if (command === 'serve') {
    return {
      plugins: [react()],
      resolve,
      server: {
        proxy: {
          // Generic UI toolkit (SearchableDropdown JS + its CSS) served by GenericUiServlet. Served
          // unauthenticated in Polarion (see the excel-importer-app web.xml), so the dev proxy can
          // fetch it without a session. Lets the shared component/styles render exactly as in prod.
          '/polarion/excel-importer-app/ui/generic': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/excel-importer/rest': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/rest': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/ria': {
            target: polarionUrl,
            changeOrigin: true,
          },
          '/polarion/icons': {
            target: polarionUrl,
            changeOrigin: true,
          },
        },
      },
    };
  }

  return {
    plugins: [react(), copyRspShellScripts()],
    resolve,
    // Never let a developer's personal access token reach a shipped bundle. VITE_BEARER_TOKEN is a
    // `vite dev` convenience (it switches useRemote to the token-authenticated /api endpoints); Vite
    // inlines import.meta.env.VITE_* at build time, so a local .env.local would otherwise be baked
    // into the bundle that `mvn -P install-to-local-polarion` deploys, readable by everyone the SPA is
    // served to. Forcing it undefined here keeps production on the session-authenticated /internal
    // endpoints, which is what Polarion provides anyway.
    define: { 'import.meta.env.VITE_BEARER_TOKEN': 'undefined' },
    base: '/polarion/excel-importer-app/ui/app/',
    build: {
      outDir: './dist/app',
      emptyOutDir: true,
    },
  };
});
