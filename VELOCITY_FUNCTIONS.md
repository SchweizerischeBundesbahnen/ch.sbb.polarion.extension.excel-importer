# Velocity functions

The extension adds the tool `$excelTool` to the Velocity context of Polarion pages. It exports HTML tables to
Excel; the same functions are available as [REST endpoints](REST_API.md#table-export-through-the-api).

| Function                                                   | Purpose                                                              |
|------------------------------------------------------------|----------------------------------------------------------------------|
| `init()`                                                   | Puts the script on the page which the export buttons call.           |
| `exportHtmlTable(tableId [, sheetName [, fileName]])`      | Returns the `onclick` code which downloads a table of the page.      |
| `attachTable(tableHtml, object, fileName, fileTitle)`      | Converts a table and attaches the file to an object.                 |
| `getHTMLTable(rows)`                                       | Builds an HTML table from a list of rows.                            |
| `convertHtmlTableToXlsx(tableHtmlBase64, sheetName)`       | Converts a table (Base64-encoded HTML) and returns the file as bytes. |
| `getTableExportConfiguration(...)`                         | Creates the configuration of a Polarion table export.                |
| `waitForExport(export, timeoutSeconds)`                    | Waits for a Polarion export to finish.                               |

## Convert HTML table to XLSX
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

`$excelTool.init()` must be on the page once, before the first button. The sheet name and the file name are optional:
without them the sheet is `Sheet1` and the file `export.xlsx`. Give the file name without the extension, `.xlsx` is
always added. The table is taken as the browser shows it at the
moment of the click, so rows added or changed by a script are exported too.

How the table is converted:

- Every `tr` becomes a row, every `th` and `td` a cell. `colspan` and `rowspan` are not applied, and a table inside
  a cell is not supported.
- A `<br>` becomes a line break in the cell; text wraps.
- A cell with a link (`<a href>`) becomes a cell with a hyperlink.
- A cell with an image (`<img>`) gets the picture. The server downloads it from its `src`.
- Text cells are formatted as text, so that Excel leaves values like `001` as they are.
- `style` and `xlsx-cell-type` can be put on the table, a row or a cell; the closer one wins.
- A column without `xlsx-width` gets the width of the text in its `th`. Longer values in the rows below do not widen
  it, so set `xlsx-width` for such columns. A column without a `th` keeps the default width.
- Every `tr` must have at least one cell, and `xlsx-width` and `xlsx-height` must be whole numbers; otherwise the
  export fails.
- The text of nested elements is joined without a separator: `<p>a</p><p>b</p>` becomes `ab`. Use `<br>` for a line
  break.

Supported custom attributes:

| Attribute      | Description                                                                   |
|----------------|-------------------------------------------------------------------------------|
| xlsx-width     | Width for the column (approx. value in pixels). Can be put on 'th' tags only. |
| xlsx-height    | Height for the row (approx. value in pixels). Can be put on 'tr' tags only.   |
| xlsx-cell-type | Cell type. Supported values: 'auto' (default), 'text', 'link', 'image'.       |

Cell type details: cells default to the 'auto' type, letting the extension automatically identify the correct type. For complex content, like nested or multiple HTML tags, specifying the type explicitly is recommended.

Supported style properties:

| Property         | Description                                                                                                                                                                                        |
|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| color            | Font color. Supported formats: hex (#RRGGBB) or [CSS named colors](https://www.w3.org/TR/css-color-3/#svg-color) (e.g., 'red', 'blue', 'darkslategray'). Color names are case-insensitive.         |
| background-color | Cell background color. Supported formats: hex (#RRGGBB) or [CSS named colors](https://www.w3.org/TR/css-color-3/#svg-color) (e.g., 'red', 'blue', 'lightgreen'). Color names are case-insensitive. |
| font-weight      | Bold font when value = 'bold' or integer value >= 700                                                                                                                                              |

Color support details:
- All 147 [CSS Color Module Level 3](https://www.w3.org/TR/css-color-3/) named colors are supported
- Color names are case-insensitive (e.g., 'Red', 'RED', and 'red' are all valid)
- Both hex format (#RRGGBB) and named colors can be used interchangeably
- Includes all [HTML 4.01 color keywords](https://www.w3.org/TR/css-color-3/#html4) and [SVG color keywords](https://www.w3.org/TR/css-color-3/#svg-color)

## Attach a table to an entity
```velocity
$excelTool.attachTable($tableHtml, $workItem, 'attached_table.xlsx', 'Test table')
```
Converts the table in `$tableHtml` and attaches the file to the object, which can be anything with attachments: a
work item, a document, a test run or a rich page. The arguments after it are the file name (`.xlsx` is added if it is
missing) and the title of the attachment. The function returns `true` on success and `false` on any failure; the
reason is then in the Polarion log.

## Create html table from the given data
```velocity
#set($testData = [
    ["ID", "Title", "Status", "Priority", "Assignee"],
    ["WI-001", "Implement login feature", "In Progress", "High", "John Doe"],
    ["WI-002", "Fix navigation bug", "Open", "Critical", "Jane Smith"],
    ["WI-003", "Update documentation", "Done", "Low", "Bob Johnson"],
    ["WI-004", "Performance optimization", "In Progress", "Medium", "Alice Williams"],
    ["WI-005", "Add unit tests", "Open", "High", "Charlie Brown"]
])
$excelTool.getHTMLTable($testData)
```
The first row becomes the header of the table. The values are inserted as text, HTML in them is escaped. The result
can be put on the page as it is, or passed to `attachTable`.

## Creates the export configuration for a table
```velocity
#set($configuration = $excelTool.getTableExportConfiguration($workItems, $query, $charset, $fields, $template, $params, $contextId))
```

## Waits until IExport has status finished
```velocity
$excelTool.waitForExport($export, $timeoutSeconds)
```
Returns `true` as soon as the export is finished, and `false` if it is not after the given number of seconds. The
timeout cannot be longer than 3600 seconds.
