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
            ctx.showActionAlert({
                containerId: 'action-success',
                message: `File successfully imported. Created: ${result.createdIds.length}, updated: ${result.updatedIds.length}, unchanged: ${result.unchangedIds.length}.`,
                hideAlertByTimeout: false
            });
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
