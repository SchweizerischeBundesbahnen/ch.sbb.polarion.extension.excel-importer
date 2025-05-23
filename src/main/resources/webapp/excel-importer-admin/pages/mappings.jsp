<%@ page import="java.lang.String" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>Mappings</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/custom-select.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/configurations.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/mappings.js?bundle=<%= bundleTimestamp %>"></script>
    <style>
        #mapping-table {
            min-height: 1px;
        }

        .mapping-row {
            padding: 5px 0;
        }

        .fs-14 {
            font-size: 14px;
        }

        .fs-14 option {
            font-size: 14px;
        }

        select {
            margin-right: 10px;
        }

        .option-mapping-value {
            width: 400px;
        }

        .option-mapping-key {
            display: table-cell;
            max-width: 200px;
            overflow: hidden;
            text-overflow: ellipsis;
        }

        .option-mapping-hint-wrapper {
            display: flex;
            justify-content: center;
            align-items: center;
        }

        .option-mapping-hint {
            font-size: 11px;
            color: #999999;
            width: 450px;
            padding-bottom: 15px;
        }

        #default-toolbar-button {
            display: none;
        }

        #default-cannot-be-deleted-note {
            display: none;
        }

        .column-input-wrapper {
            position: relative;
            width: 60px;
            display: inline-block;
            align-items: center;
        }

        .excel-column-input {
            width: 100%;
            box-sizing: border-box;
        }

        .dropdown-menu {
            position: absolute;
            background-color: white;
            border: 1px solid #e5e7eb;
            border-radius: 0.375rem;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
            max-height: 200px;
            overflow-y: auto;
            z-index: 10;

            top: 100%; /* Position right below the input field */
        }

        .dropdown-item {
            padding: 0.5rem 0.75rem;
        }

        .dropdown-item:hover {
            background-color: #1967D2;
            color: white;
        }

        .hidden {
            display: none;
        }
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>Mappings</h1>

    <jsp:include page='/common/jsp/notifications.jsp'/>

    <jsp:include page='/common/jsp/configurations.jsp'/>

    <h2>General Settings</h2>
    <table>
        <tr>
            <td><label for="sheet-name">Import rows from sheet:</label></td>
            <td><input id="sheet-name" type="text" value="Sheet1"></td>
        </tr>
        <tr>
            <td><label for="start-from-row">Start import from row:</label></td>
            <td><input id="start-from-row" type="number" min="1" step="1"></td>
        </tr>
        <tr>
            <td colspan="2"><input id="overwrite" type="checkbox"><label for="overwrite" title="Determines whether or not an empty value from the imported excel file should overwrite (and thus delete) an existing value of the work item field.">Overwrite with empty values</label></td>
        </tr>
    </table>
    <h2 class="align-left">Workitem Type To Create</h2>
    <div id="workitem-types-container">
        <label for="wi-types">Import rows as: </label><select class="fs-14" id="wi-types"></select>
    </div>
    <h2 class="align-left">Column Name To Workitem Field Mapping</h2>
    <table id="mapping-table">
    </table>
    <h2 class="align-left">Link Column</h2>
    <div id="link-column-container">
        <label for="link-column">Column name to link Excel row with Polarion workitem: </label><select id="link-column" class="fs-14 columns"></select>
    </div>
    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<jsp:include page='/common/jsp/buttons.jsp'/>

<div class="standard-admin-page">
    <h2 class="align-left">Quick Help</h2>

    <div class="quick-help-text">
        <div>
            <h3>General Settings</h3>
            <p>This section contains inputs with the Excel sheet name (<code>Sheet1</code> by default) and the row number (1 by default) to import data from.</p>

            <h3>Workitem Type To Create</h3>
            <p>This section contains a combobox with workitem types available for the current project.</p>
            <p>In case if there is no workitem found by given identifier - it will be created using this type. This means that the extension can update any type in the current project (not only the one selected in this combobox).</p>

            <h3>Column Name To Workitem Field Mapping</h3>
            <p>This section contains comboboxes for mapping Excel column names to Polarion workitem fields.</p>
            <p><strong>Note:</strong> If a mandatory Polarion field is not mapped, an error will occur during the import process.</p>
            <p>If a mapped column contains an empty value, this will result in an error unless explicitly allowed in the mapping configuration using <strong>(empty)</strong> meta value.</p>

            <h3>Link Column</h3>
            <p>This section contains a combobox for linking Excel rows to existing Polarion workitems for data updates.</p>

            <h3>Date and Time Formats</h3>
            <p>If date, time, or date-time values are stored in text format, the importer will attempt to parse them using supported ISO date format patterns:</p>
            <ul>
                <li>Date-time: <code>yyyy-MM-ddTHH:mm:ss</code></li>
                <li>Date only: <code>yyyy-MM-dd</code></li>
                <li>Time only: <code>HH:mm:ss</code> or <code>HH:mm</code></li>
            </ul>

            <h3>Duration Fields</h3>
            <p>For duration-type fields only days and hours are supported due to Polarion limitations.</p>
            <p>Examples (d=days, h=hours): <code>1d</code>, <code>3d</code>, <code>1/2h</code>, <code>2 1/2h</code>, <code>3d 1h</code>.</p>
        </div>
    </div>
</div>

<jsp:include page='modal.jsp'>
    <jsp:param name="titleText" value="Options mapping"/>
    <jsp:param name="okText" value="Accept"/>
    <jsp:param name="cancelText" value="Cancel"/>
</jsp:include>

</body>
</html>
