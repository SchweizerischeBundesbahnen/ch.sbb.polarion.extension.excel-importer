<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>Mappings</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/custom-select.css?bundle=<%= bundleTimestamp %>">
    <link rel="stylesheet" href="../ui/generic/css/configurations.css?bundle=<%= bundleTimestamp %>">
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
    </style>
</head>

<body>
<div class="standard-admin-page">
    <h1>Mappings</h1>

    <jsp:include page='/common/jsp/notifications.jsp' />

    <jsp:include page='/common/jsp/configurations.jsp' />

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
    <h2 class="align-left">Workitem Type</h2>
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

<jsp:include page='/common/jsp/buttons.jsp'>
    <jsp:param name="saveFunction" value="saveSettings()"/>
    <jsp:param name="cancelFunction" value="SbbCommon.cancelEdit()"/>
    <jsp:param name="defaultFunction" value="revertToDefault()"/>
</jsp:include>

<div class="standard-admin-page">
    <h2 class="align-left">Quick Help</h2>

    <div class="quick-help-text">
        <h3>How-to configure excel column to Workitem field mapping</h3>
        <p>First section contains inputs with Excel sheet name (standard Excel sheet name 'Sheet1' by default) and row number (1 by default) to import data from.</p>
        <p>Second section contains combobox with workitem types available for current project.</p>
        <p>Third section contains comboboxes with Excel column name to Polarion workitem field mapping.</p>
        <p>Next section contains combobox with Excel column name to link Excel row with Polarion workitem to import data to.</p>
        <p>If date, time, or date-time will be stored in the text format cells, there will be an attempt to parse stored information from ISO date format.</p>
        <p>For date-time pattern 'yyyy-MM-ddTHH:mm:ss' supported.</p>
        <p>For date only pattern 'yyyy-MM-dd' supported.</p>
        <p>For time only both 'HH:mm:ss' and 'HH:mm' patterns are supported.</p>
        <p>For duration type field only days and hours are supported due to polarion limitations.</p>
        <p>Examples: (d=days, h=hours): 1d, 3d, 1/2h, 2 1/2h, 3d 1h</p>
    </div>
</div>

<jsp:include page='modal.jsp'>
    <jsp:param name="titleText" value="Enum mapping"/>
    <jsp:param name="okText" value="Accept"/>
    <jsp:param name="cancelText" value="Cancel"/>
    <jsp:param name="okClickFunction" value="saveEnumMapping()"/>
</jsp:include>

<script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/custom-select.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../ui/generic/js/configurations.js?bundle=<%= bundleTimestamp %>"></script>
<script type="text/javascript" src="../js/mappings.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>