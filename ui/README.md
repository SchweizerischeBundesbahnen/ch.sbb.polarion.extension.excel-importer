# Excel Importer UI (experimental)

A React + Vite single-page app that will progressively replace the legacy JSP admin pages
(`about`, `mappings`, `user-guide`, `import-file`).

## Feature routing

There is one `index.html` / bundle. The page to render is chosen from the `feature` query
parameter:

- `/` (no param) renders a development landing page listing every feature.
- `/?feature=about`, `/?feature=mappings`, `/?feature=user-guide`, `/?feature=import-file`
  render the corresponding page.

Features are declared in [`src/features.tsx`](src/features.tsx). Add a page component under
`src/pages/`, register it there, and it appears on the landing page automatically.

## Local development

No Polarion restart is needed to develop the UI:

```bash
cd ui
cp .env.local.template .env.local   # optional: set VITE_BASE_URL / VITE_BEARER_TOKEN for REST
npm install
npm run dev                          # http://localhost:5173/
```

REST calls (once pages use `useRemote`) are proxied to the Polarion instance in `VITE_BASE_URL`.

## Formatting & linting

Run from `ui/`. Prettier and ESLint match the shared `react-sbb-polarion` library (identical
`.prettierrc`; [`eslint.config.js`](eslint.config.js) mirrors its flat config).

```bash
cd ui
npm run format          # Prettier: format every file in place
npm run format:check    # Prettier: check only, no writes (what pre-commit / CI runs)
npm run lint            # ESLint: report problems
npm run lint:fix        # ESLint: auto-fix what it can
```

The repo's pre-commit hooks run `format:check` + `lint` (and the Dockerized Vitest suite) on any
change under `ui/`. They are **check-only** and never modify your files - fix locally with
`npm run format` / `npm run lint:fix` before committing.

## Testing

Vitest runs in real Chromium (browser mode). There are two layers: **behavior** tests (screenshot-free,
run anywhere) and **visual-regression** snapshots (`*.visual.test.tsx`). Visual references are committed
under `test/expected/` and are pixel-compared; because screenshot antialiasing depends on OS fonts,
they only match inside a pinned Playwright Docker image, so the authoritative runs are dockerized.

```bash
cd ui
npm test                    # behavior tests pass anywhere; visual tests will diff off Linux/Docker
npm run test:watch          # watch mode (behavior)
npm run test:coverage       # behavior-only + istanbul coverage (80% gate), no Docker
```

### Dockerized tests (authoritative)

These run inside the pinned `mcr.microsoft.com/playwright` image (via `scripts/docker-test.mjs`), so
the visual snapshots match their references. **Docker must be running.**

```bash
cd ui
npm run test:docker            # full suite (behavior + visual) in the pinned image
npm run test:coverage:docker   # full suite + the 80% coverage gate (what pre-commit runs)
npm run test:update:docker     # regenerate the committed visual reference PNGs (do this in Docker only)
```

Generate/refresh reference screenshots **only** via `test:update:docker` - a bare `npm run test -u`
on Windows/macOS would commit references with mismatching (non-Linux) pixels.

### As part of the Maven build

`mvn install` runs the dockerized JS tests (`npm run test:docker`) in the Maven `test` phase, alongside
the Java tests (so `ui/` also needs Docker during a full build). Skip just the JS tests with:

```bash
mvn install -DskipJsTests      # runs Java tests, skips the ui/ Vitest run (e.g. no Docker available)
```

## Production build

`npm run build` emits the bundle to `ui/dist/app` with base path
`/polarion/excel-importer-app/ui/app/`. The Maven build (frontend-maven-plugin +
maven-resources-plugin) runs this automatically and copies the bundle into
`src/main/resources/webapp/excel-importer-app/app`, where `ExcelImporterAppServlet` serves it at
`/polarion/excel-importer-app/ui/app/index.html`.

## Hooking into Polarion

The app is not yet wired into `hivemodule.xml`; the JSP pages remain in place. To flip a page
over, point its admin extender's `pageUrl` at
`/polarion/excel-importer-app/ui/app/index.html?feature=<id>&scope=$scope$`.
