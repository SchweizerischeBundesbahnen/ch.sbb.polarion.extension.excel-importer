<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>Import File</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <script type="text/javascript" src="../ui/generic/js/common.js?bundle=<%= bundleTimestamp %>"></script>
    <style type="text/css">
        input[type="file"] {
            display: none;
        }

        .styled-upload-button {
            background-color: buttonface;
        }

        .styled-upload-button:disabled, .styled-upload-button.disabled {
            color: #999999;
        }
    </style>
</head>

<body>
<div class="standard-admin-page excel-importer-admin">
    <h1>Import File</h1>

    <div class="input-container">
        <div class="input-block wide">
            <div class="label-block"><label>Excel File</label></div>

            <div style="margin-top: 10px; margin-bottom: 10px;">
                <label for='mapping-select'>Mapping:</label>
                <select id="mapping-select"></select>
            </div>

            <div>
                <label id="file-xlsx-label" for="file-xlsx" class="toolbar-button styled-upload-button">Choose xlsx file</label>
                <input id="file-xlsx" name="file" type="file" accept=".xlsx" onchange="fileChosen()"/>
                <span id="file-name">No file chosen</span>
            </div>

            <div class="actions-pane" style="margin-top: 20px;">
                <div class="action-buttons inline-flex">
                    <button id="import-button" class="toolbar-button styled-upload-button" disabled title="Please choose excel file using button above" onclick="importFile()">Import</button>
                </div>
                <div class="action-alerts inline-flex">
                    <div id="action-error" class="alert alert-error" style="display: none"></div>
                    <div id="action-success" class="alert alert-success" style="display: none"></div>
                </div>
            </div>

        </div>
    </div>

    <input id="scope" type="hidden" value="<%= request.getParameter("scope")%>"/>
    <input id="bundle-timestamp" type="hidden" value="<%= ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestamp() %>"/>
</div>

<script type="text/javascript" src="../js/import_file.js?bundle=<%= bundleTimestamp %>"></script>
</body>
</html>