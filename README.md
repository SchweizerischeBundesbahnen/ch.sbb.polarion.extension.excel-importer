[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Bugs](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=bugs)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Code Smells](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=code_smells)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=coverage)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Duplicated Lines (%)](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=duplicated_lines_density)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Reliability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=reliability_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Security Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=security_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Maintainability Rating](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=sqale_rating)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)
[![Vulnerabilities](https://sonarcloud.io/api/project_badges/measure?project=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer&metric=vulnerabilities)](https://sonarcloud.io/summary/new_code?id=SchweizerischeBundesbahnen_ch.sbb.polarion.extension.excel-importer)

# Polarion ALM extension to process WorkItems by uploading xlsx-files

This Polarion extension provides possibility to update (or create) WorkItems using xlsx-file.
Column-to-Field mapping is manageable using mapping settings.

> [!IMPORTANT]
> Starting from version 3.0.0 only latest version of Polarion is supported.
> Right now it is Polarion 2410.

## Quick start

The latest version of the extension can be downloaded from the [releases page](../../releases/latest) and installed to Polarion instance without necessity to be compiled from the sources.
The extension should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.excel-importer/eclipse/plugins` and changes will take effect after Polarion restart.
> [!IMPORTANT]
> Don't forget to clear `<polarion_home>/data/workspace/.config` folder after extension installation/update to make it work properly.

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

## Velocity functions

### Convert HTML table to XLSX
It is possible to generate xlsx document from existing html table in a LiveDoc or Wiki by adding code like this:
```html
$excelTool.init()
<p>
<input type="button" onclick="$excelTool.exportHtmlTable('users-table', 'Users', 'UsersData')" value="Export to Excel">
</p>
<table id="users-table">
   <tr>
      <th style="font-weight: bold">Name</th>
      <th xlsx-width="300">Phone number</th>
   </tr>
   <tr style="color: #22CC66" xlsx-height="200">
      <td>John Doe</td>
      <td>555-12-34</td>
   </tr>
   <tr>
      <td>Ann Smith</td>
      <td>555-09-87</td>
   </tr>
</table>

```
In the above case extension generates the file `UsersData.xlsx` with the data from table `users-table` on the sheet `Users`.

Supported custom attributes:

| Attribute   | Description                                                                   |
|-------------|-------------------------------------------------------------------------------|
| xlsx-width  | Width for the column (approx. value in pixels). Can be put on 'th' tags only. |
| xlsx-height | Height for the row (approx. value in pixels). Can be put on 'tr' tags only.   |

Supported style properties:

| Property         | Description                                           |
|------------------|-------------------------------------------------------|
| color            | Font color (supported format: #RRGGBB)                |
| background-color | Cell background color (supported format: #RRGGBB)     |
| font-weight      | Bold font when value = 'bold' or integer value >= 700 |

## Known issues

All good so far.
