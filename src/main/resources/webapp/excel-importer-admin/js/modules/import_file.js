import ExtensionContext from '../../ui/generic/js/modules/ExtensionContext.js';

const SELECTED_CONFIGURATION_COOKIE = 'selected-configuration-';

const ctx = new ExtensionContext({
    extension: 'excel-importer',
    scopeFieldId: 'scope'
});

ctx.onChange(
    'file-xlsx', fileChosen
);

ctx.onClick(
    'import-button', importFile
);

function fileChosen() {
    const file = ctx.getElementById('file-xlsx').files[0];
    const label = ctx.getElementById('file-name');
    const importButton = ctx.getElementById('import-button');
    let missingFile = file === undefined;
    label.innerText = missingFile ? 'No file chosen' : file.name;
    importButton.disabled = missingFile;
    importButton.title = missingFile ? 'Please choose excel file using button above' : 'Initiate processing';
}

function importFile() {
    const file = ctx.getElementById('file-xlsx').files[0];
    if (file === undefined) {
        return;
    }
    ctx.hideActionAlerts();
    const formData = new FormData();
    formData.append('file', file);
    formData.append('mappingName', ctx.getValueById("mapping-select"));
    disableAllButtons(true);
    ctx.callAsync({
        method: 'POST',
        url: `/polarion/${ctx.extension}/rest/internal/projects/${getProjectIdFromScope()}/import`,
        body: formData,
        onOk: (response) => {
            const result = JSON.parse(response);
            const log = String(result.log);
            ctx.showActionAlert({
                containerId: 'action-success',
                message: `File successfully imported. Created: ${result.createdIds.length}, updated: ${result.updatedIds.length}, unchanged: ${result.unchangedIds.length}, skipped: ${result.skippedIds.length}.
                &nbsp;<a href="#" data-filename="${generateLogFileName()}" id="download-log-link">(log)</a>`,
                hideAlertByTimeout: false
            });

            ctx.onClick('download-log-link', () => {
                    ctx.downloadBlob(new Blob([log], {type: "text/plain"}), ctx.getElementById("download-log-link").dataset.filename);
                }
            );

            disableAllButtons(false);
        },
        onError: (status, responseText) => {
            ctx.showActionAlert({
                containerId: 'action-error',
                message: `Import error (${responseText}).`,
                hideAlertByTimeout: false
            });
            disableAllButtons(false);
        }
    });
}

function getProjectIdFromScope() {
    if (ctx.scope) {
        const regExp = "project/(.*)/"
        return ctx.scope.match(regExp)[1];
    }
    return '';
}

function generateLogFileName() {
    const importFileName = ctx.getElementById("file-name").textContent;
    const baseName = importFileName.replace(/\.[^/.]+$/, "") // remove file extension
        .replace(/[^a-zA-Z0-9-_]/g, "").replace(/\s+/g, "_"); // sanitize the name
    const now = new Date(); // append current date and time in the format YYYY_MM_DD_HH_MM_SS
    const formattedDate = now.getFullYear() + "_" +
        String(now.getMonth() + 1).padStart(2, "0") + "_" +
        String(now.getDate()).padStart(2, "0") + "_" +
        String(now.getHours()).padStart(2, "0") + "_" +
        String(now.getMinutes()).padStart(2, "0") + "_" +
        String(now.getSeconds()).padStart(2, "0");
    return `${baseName}_${formattedDate}.txt`;
}

function disableAllButtons(disable) {
    let chooseLabelClassList = ctx.getElementById('file-xlsx-label').classList;
    if (disable) {
        chooseLabelClassList.add("disabled");
    } else {
        chooseLabelClassList.remove("disabled");
    }
    ctx.getElementById('file-xlsx').disabled = disable;
    ctx.getElementById('import-button').disabled = disable;
}

function readMappingNames() {
    ctx.callAsync({
        method: 'GET',
        url: `/polarion/${ctx.extension}/rest/internal/settings/mappings/names?scope=${ctx.scope}`,
        contentType: 'application/json',
        onOk: (responseText) => {
            const previouslySelectedValue = ctx.getCookie(SELECTED_CONFIGURATION_COOKIE + 'mappings');
            const container = ctx.getElementById("mapping-select");
            for (const item of JSON.parse(responseText)) {
                const option = document.createElement('option');
                option.value = item.name;
                option.innerHTML = item.name;
                container.appendChild(option);
            }
            if (previouslySelectedValue) {
                container.value = previouslySelectedValue;
            }
        },
    });
}

readMappingNames();
