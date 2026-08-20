import { copyFileSync } from 'node:fs';
import { createRequire } from 'node:module';
import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

// react-sbb-polarion's BreadcrumbInjector loads breadcrumb-bridge.js from next to the running page. It
// runs in the Polarion shell window rather than in this app's frame, so it stays a classic script and
// cannot be bundled - it is copied next to the built app instead. See "Shell scripts" in the library's
// README.
function copyRspShellScripts() {
  return {
    name: 'copy-rsp-shell-scripts',
    writeBundle(options) {
      const require = createRequire(import.meta.url);
      copyFileSync(
        require.resolve('@sbb-polarion/react-sbb-polarion/breadcrumb-bridge.js'),
        `${options.dir}/breadcrumb-bridge.js`,
      );
    },
  };
}

export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), '');
  const polarionUrl = env.VITE_BASE_URL || 'http://localhost';

  // The shared @sbb-polarion/react-sbb-polarion (RSP) package is linked via a `file:` dependency, which npm
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
