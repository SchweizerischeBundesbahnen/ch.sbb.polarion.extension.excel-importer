# REST API

Everything the pages of the extension do can be done by a script: import a file, manage the
[mappings](USER_GUIDE.md#mappings) and export tables. The `REST API` page under `Excel Importer` is a Swagger UI with
all endpoints, where they can be tried out.

| What           | Value                                                                                |
|----------------|--------------------------------------------------------------------------------------|
| Base URL       | `https://<polarion-host>/polarion/excel-importer/rest/api`                           |
| Authentication | A Polarion personal access token in the header `Authorization: Bearer <token>`       |
| Errors         | A JSON body with the reason in `message` (or `errorMessage` for a failed import job) |

The examples below use `curl` with these two variables:

```bash
API=https://polarion.example.com/polarion/excel-importer/rest/api
TOKEN=<personal access token>
```

## Importing a file through the API
There are two ways to import. Both take a `multipart/form-data` request with the file in the part `file` and the name
of the mapping in `mappingName`. Always send it: without it a mapping named `Default` is looked up, which a project
has only if somebody created one with this name.

**In one request.** The request returns when the import is done. This suits small files: a large one can run into
the timeout of a proxy between the client and Polarion, and the client then does not learn the outcome. The import
itself has no time limit here; the 60 minutes apply to jobs only.

An error which is found while the file is read - it is not an xlsx file, the sheet does not exist, a cell holds an
error value - is answered with `500` and an error ID instead of the reason. The same import as a job reports the
reason in `errorMessage`.

```bash
curl -H "Authorization: Bearer $TOKEN" \
     -F "file=@requirements.xlsx" -F "mappingName=Requirements" \
     "$API/projects/elibrary/import"
```

**As a background job.** This is what the `Import File` page does.

1. Start the job. The project is a form field here. The response is `202 Accepted`, and its `Location` header is the
   address of the job.

   ```bash
   curl -i -H "Authorization: Bearer $TOKEN" \
        -F "file=@requirements.xlsx" -F "mappingName=Requirements" -F "projectId=elibrary" \
        "$API/import/jobs"
   ```

2. Ask for the status of the job until it is finished: `GET $API/import/jobs/<job id>`.

   | Response | Meaning                                                                                  |
   |----------|------------------------------------------------------------------------------------------|
   | `202`    | The import is still running.                                                             |
   | `303`    | The import is done, `Location` points to its result.                                     |
   | `409`    | The import failed or was cancelled. Nothing was imported; `errorMessage` says why.       |
   | `500`    | There is no such job: the ID is wrong, the job belongs to another user or it has expired. |

3. Fetch the result: `GET $API/import/jobs/<job id>/result`. It answers `204` while the import is still running and
   `409` for an import which failed or was cancelled.

A finished job and its result are kept for 30 minutes. `GET $API/import/jobs` lists the jobs of the calling user.

Both ways return the same result:

```json
{
  "createdIds": ["EL-1201", "EL-1202"],
  "updatedIds": ["EL-100"],
  "unchangedIds": ["EL-101", "EL-102"],
  "skippedIds": ["EL-999"],
  "log": "00:00:00.412 Xlsx file parsed successfully, found 6 rows\n..."
}
```

The lists hold work item IDs, with one exception: a row which was skipped before its work item existed is listed in
`skippedIds` by its value of the link column, or as `row <number>` if that is empty. `log` is the text which the
`(log)` link of the page downloads.

## Managing mappings through the API
Mappings are the settings feature `mappings`. They exist per project, so every request needs the scope of the project
in the form `project/<project id>/`.

| Request                                                          | Purpose                                                                                                                                   |
|------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|
| `GET /settings/mappings/names?scope=project/elibrary/`           | Names of the mappings of the project                                                                                                      |
| `GET /settings/mappings/names/{name}/content?scope=...`          | A mapping; `&revision=...` returns an earlier state                                                                                       |
| `PUT /settings/mappings/names/{name}/content?scope=...`          | Creates or replaces a mapping Without a body the mapping gets the default settings. A name may hold letters, digits, spaces, `-` and `_`. |
| `POST /settings/mappings/names/{name}?scope=...`                 | Renames a mapping The new name is the request body.                                                                                       |
| `DELETE /settings/mappings/names/{name}?scope=...`               | Deletes a mapping                                                                                                                         |
| `GET /settings/mappings/names/{name}/revisions?scope=...`        | History of a mapping                                                                                                                      |
| `GET /projects/{projectId}/workitem_types`                       | Work item types of the project                                                                                                            |
| `GET /projects/{projectId}/workitem_types/{workItemType}/fields` | Fields which can be mapped for a type, with their options                                                                                 |

A mapping is a JSON object with the settings of the `Mappings` page:

```json
{
  "sheetName": "Sheet1",
  "startFromRow": 2,
  "endAtRow": 500,
  "filterQuery": "status:draft AND type:requirement",
  "skipWorkItemsCreation": false,
  "overwriteWithEmpty": false,
  "ignoreUnknown": false,
  "disableEnumDependenciesValidation": false,
  "skipWorkflowOnStatusChange": false,
  "defaultWorkItemType": "requirement",
  "columnsMapping": {
    "A": "externalId",
    "B": "title",
    "C": "severity",
    "D": "linkedWorkItems",
    "testSteps|testSteps": "testSteps"
  },
  "enumsMapping": {
    "severity": { "must_have": "must, (empty)", "nice_to_have": "nice" }
  },
  "stepsMapping": {
    "testSteps": { "step": "E", "expectedResult": "F" }
  },
  "linkColumn": "A",
  "linkColumnUnique": true,
  "unlinkExisting": false
}
```

| Property                | Content                                                                                                                          |
|-------------------------|----------------------------------------------------------------------------------------------------------------------------------|
| `endAtRow`              | Leave it out to import up to the last row.                                                                                       |
| `filterQuery`           | A Lucene query which the work items to update must match. Leave it out, or send `null` or a blank string, for no filter.         |
| `skipWorkItemsCreation` | `true` for an update-only import: rows without a work item are skipped. `defaultWorkItemType` is then not looked up.             |
| `columnsMapping`        | Column letters to field IDs. A test steps field has no column of its own, its key is `testSteps\|<field ID>`.                     |
| `enumsMapping`          | Per field ID: option ID to the comma-separated cell values which are read as this option.                                        |
| `stepsMapping`          | Per test steps field ID: ID of the step column to the letter of the Excel column. All step columns of the field must be present. |
| `linkColumn`            | A column letter which is a key of `columnsMapping`.                                                                              |

The page validates a mapping before it saves it; the API stores what it gets. A mapping with a gap - no link column,
a field which the type does not have - fails later, at the import.

## Table export through the API
The functions behind the [Velocity functions](VELOCITY_FUNCTIONS.md) are endpoints too, under `/excel-tool`:

| Request                                              | Body                                                                                      | Answer                                         |
|------------------------------------------------------|-------------------------------------------------------------------------------------------|------------------------------------------------|
| `POST /excel-tool/export-html-table`                 | Form parts `tableHtml` (the HTML of the table, Base64-encoded) and optionally `sheetName` | `{"content": "<Base64 of the xlsx file>"}`     |
| `POST /excel-tool/html-table-from-list`              | JSON list of rows, each a list of strings; the first row is the header                    | The HTML of the table                          |
| `POST /excel-tool/projects/{projectId}/attach-table` | JSON with `htmlTable`, `fileName`, `fileTitle`, `objectType` and `objectId`               | `true` if the file was attached                |
| `GET /excel-tool/exports/{exportId}/wait?timeout=60` | -                                                                                         | `true` if the Polarion export finished in time |

`objectType` and `objectId` of `attach-table` name the object which gets the attachment:

| `objectType` | `objectId`                                            |
|--------------|-------------------------------------------------------|
| `WORKITEM`   | Work item ID, e.g. `EL-100`                           |
| `MODULE`     | Space and document name: `Specification/Requirements` |
| `TESTRUN`    | Test run ID                                           |
| `RICHPAGE`   | Path of the page                                      |
