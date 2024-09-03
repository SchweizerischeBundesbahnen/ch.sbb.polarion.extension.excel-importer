# Polarion ALM extension to process WorkItems by uploading xlsx-files

This Polarion extension provides possibility to update (or create) WorkItems using xlsx-file.
Column-to-Field mapping is manageable using mapping settings.

## Build

This extension can be produced using maven:

```bash
mvn clean package
```

## Installation to Polarion

To install the extension to Polarion, file `ch.sbb.polarion.extension.excel-importer-<version>.jar`
should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.excel-importer/eclipse/plugins`
It can be done manually or automated using maven build:

```bash
mvn clean install -P install-to-local-polarion
```

For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## Apache POI Polarion Bundle

Latest Polarion installations have relatively old version of Apache POI, so it is recommended to use Apache POI Polarion Bundle (for more information please check [ch.sbb.polarion.thirdparty.bundles](https://github.com/SchweizerischeBundesbahnen/ch.sbb.polarion.thirdparty.bundles)):
its artifact must be placed to `<polarion_home>/polarion/extensions/ch.sbb.polarion.thirdparty.bundles.org.apache.poi/eclipse/plugins/org.apache.poi-<version>.jar`

## Polarion configuration

### Import for non-admin users

1. Open a project where you wish Excel Importer's navigation element to be available
2. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
3. On the administration's navigation pane select Portal ➙ Topics and click on Edit button of desired View.
4. In opened Topics Configuration editor insert following new topic:
   ```xml
   …
   <topic id="excel-importer"/>
   …
   ```
5. Save changes by clicking 💾 Save

## REST API

This extension provides REST API. OpenAPI Specification can be obtained [here](docs/openapi.json).

## Known issues

All good so far.
