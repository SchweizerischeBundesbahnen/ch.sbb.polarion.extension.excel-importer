<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">

<%! String bundleTimestamp = ch.sbb.polarion.extension.generic.util.VersionUtils.getVersion().getBundleBuildTimestampDigitsOnly(); %>

<head>
    <title>Import File</title>
    <link rel="stylesheet" href="../ui/generic/css/common.css?bundle=<%= bundleTimestamp %>">
    <script type="module" src="../js/modules/import_file.js?bundle=<%= bundleTimestamp %>"></script>
    <script type="text/javascript">
        // Show this topic in the Polarion app-header breadcrumb via the shared generic
        // BreadcrumbBridge (injected into the shell window); it stays out of the Administration area.
        (function () {
            try {
                var shell = window.top;
                var cfg = { marker: 'excel-importer', title: 'Excel Importer', icon: '/polarion/excel-importer-admin/ui/images/menu/30x30/_parent.svg' };
                if (shell.SbbBreadcrumbBridge) { shell.SbbBreadcrumbBridge.install(cfg); return; }
                var doc = shell.document;
                if (!doc || !doc.head) { return; }
                var old = doc.getElementById('sbb-breadcrumb-bridge-loader');
                if (old) { old.parentNode.removeChild(old); }
                var s = doc.createElement('script');
                s.id = 'sbb-breadcrumb-bridge-loader';
                s.type = 'text/javascript';
                s.src = window.location.pathname.replace(/\/pages\/[^/]*$/, '/ui/generic/js/modules/') + 'BreadcrumbBridge.js';
                s.setAttribute('data-marker', cfg.marker);
                s.setAttribute('data-title', cfg.title);
                if (cfg.icon) { s.setAttribute('data-icon', cfg.icon); }
                doc.head.appendChild(s);
            } catch (e) { /* no accessible shell window */ }
        })();
    </script>
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

        #no-mapping-note {
            display: none;
        }

        #import-progress {
            width: 20px;
            height: 20px;
            vertical-align: middle;
        }
    </style>
</head>

<body>
<div class="standard-admin-page excel-importer-admin">
    <h1>Import File</h1>

    <div class="input-container">
        <span id="no-mapping-note"><i>No mapping configurations found. Please create a configuration first.</i></span>
        <div id="import-panel" class="input-block wide">
            <div class="label-block"><label>Excel File</label></div>

            <div style="margin-top: 10px; margin-bottom: 10px; display: flex; align-items: center; gap: 8px;">
                <label for='mapping-select'>Mapping:</label>
                <select id="mapping-select"></select>
            </div>

            <div>
                <label id="file-xlsx-label" for="file-xlsx" class="toolbar-button styled-upload-button">Choose xlsx file</label>
                <input id="file-xlsx" name="file" type="file" accept=".xlsx" onchange="fileChosen()"/>
                <span id="file-name">No file chosen</span>
            </div>

            <div id="import-progress-container" style="display: none; margin-top: 10px;">
                <span id='import-progress' class='sbb-spinner' role='img' aria-label='Loading'></span>
                <span style="margin-left: 10px;">Import is in progress. Please wait...</span>
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
</body>
</html>
