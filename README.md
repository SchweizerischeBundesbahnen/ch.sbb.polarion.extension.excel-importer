# Polarion ALM extension to process WorkItems by uploading xlsx-files

This Polarion extension provides possibility to update (or create) WorkItems using xlsx-file.
Column-to-Field mapping is manageable using mapping settings.

## Build

This extension can be produced using maven:
```bash
mvn clean package
```

## Installation to Polarion

!!! WARNING
Before using this extension you have to ensure that the current Polarion instance contains Apache POI extension v.5.2.x (`<polarion_home>/polarion/extensions/ch.sbb.polarion.thirdparty.bundles.org.apache.poi/eclipse/plugins/org.apache.poi-<version>.jar`).

To install the extension to Polarion, file `ch.sbb.polarion.extension.excel-importer-<version>.jar`
should be copied to `<polarion_home>/polarion/extensions/ch.sbb.polarion.extension.excel-importer/eclipse/plugins`
It can be done manually or automated using maven build:

```bash
mvn clean install -P install-to-local-polarion
```

For automated installation with maven env variable `POLARION_HOME` should be defined and point to folder where Polarion is installed.

Changes only take effect after restart of Polarion.

## Polarion configuration

### Import for non-admin users

1. Open a project where you wish Excel Importer's navigation element to be available
2. On the top of the project's navigation pane click ⚙ (Actions) ➙ 🔧 Administration. Project's administration page will be opened.
3. On the administration's navigation pane select Portal ➙ Topics and click on Edit button of desired View.
4. In opened Topics Configuration editor insert following new topic:
   ```
   …
   <topic id="excel-importer"/>
   …
   ```
5. Save changes by clicking 💾 Save

## Extension's REST API

### Settings

Settings may be changed using `/api/settings/mappings/names/{mappingName}` endpoint.
Request body example:

```json
{
  "sheetName": "Sheet 1",
  "startFromRow": 1,
  "columnsMapping": {
    "A": "title",
    "B": "docId",
    "C": "docName"
  },
  "defaultWorkItemType": "requirement",
  "linkColumn": "B"
}
```

List of workItem types for project may be accessible using `/api/projects/{projectId}/workitem_types` endpoint.
Request body example:

```json
[
  {
    "sequenceNumber": 1,
    "name": "User Story",
    "hidden": false,
    "properties": {
      "color": "#F1ED92",
      "description": "A functional requirement.",
      "iconURL": "/polarion/icons/default/enums/type_userstory.gif"
    },
    "id": "userstory",
    "default": true,
    "enumId": "work-item-type",
    "phantom": false
  },
  {
    "sequenceNumber": 2,
    "name": "Requirement",
    "hidden": false,
    "properties": {
      "color": "#A280A9",
      "description": "A nonfunctional requirement.",
      "iconURL": "/polarion/icons/default/enums/type_requirement.gif"
    },
    "id": "requirement",
    "default": false,
    "enumId": "work-item-type",
    "phantom": false
  }
]
```

List of workItem fields for project and workitem type may be accessible using `/api/projects/{projectId}/workitem_types/{workItemType}/fields` endpoint.
Request body example:

```json
[
  "approvals",
  "assignee",
  "attachments",
  "author",
  "categories",
  "comments"
]
```

### Import file

Import is accessible by posting a xlsx-file to the `/api/projects/{projectId}/import` endpoint.

Response example:

```json
{
  "updatedIds": [
    "updated_id1",
    "updated_id2"
  ],
  "createdIds": [
    "created_id1",
    "created_id2"
  ],
  "unchangedIds": [
    "unchanged_id1",
    "unchanged_id2"
  ]
}
```

