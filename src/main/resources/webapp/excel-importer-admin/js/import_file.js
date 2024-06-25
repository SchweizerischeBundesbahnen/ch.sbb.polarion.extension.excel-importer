const SELECTED_CONFIGURATION_COOKIE = 'selected-configuration-';
SbbCommon.init({
    extension: 'excel-importer',
    scope: SbbCommon.getValueById('scope')
});

function fileChosen() {
    const file = document.getElementById('file-xlsx').files[0];
    const label = document.getElementById('file-name');
    const importButton = document.getElementById('import-button');
    let missingFile = file === undefined;
    label.innerText = missingFile ? 'No file chosen' : file.name;
    importButton.disabled = missingFile;
    importButton.title = missingFile ? 'Please choose excel file using button above' : 'Initiate processing';
}

function importFile() {
    const file = document.getElementById('file-xlsx').files[0];
    if (file === undefined) {
        return;
    }
    SbbCommon.hideActionAlerts();
    const formData = new FormData();
    formData.append('file', file);
    formData.append('mappingName', SbbCommon.getValueById("mapping-select"));
    disableAllButtons(true);
    SbbCommon.callAsync({
        method: 'POST',
        url: `/polarion/${SbbCommon.extension}/rest/internal/projects/${getProjectIdFromScope()}/import`,
        body: formData,
        onOk: (response) => {
            const result = JSON.parse(response);
            SbbCommon.showActionAlert({
                containerId: 'action-success',
                message: `File successfully imported. Created: ${result.createdIds.length}, updated: ${result.updatedIds.length}, unchanged: ${result.unchangedIds.length}.`,
                hideAlertByTimeout: false
            });
            disableAllButtons(false);
        },
        onError: (status, responseText) => {
            SbbCommon.showActionAlert({
                containerId: 'action-error',
                message: `Import error (${responseText}).`,
                hideAlertByTimeout: false
            });
            disableAllButtons(false);
        }
    });
}

function getProjectIdFromScope() {
    if (SbbCommon.scope) {
        const regExp = "project/(.*)/"
        return SbbCommon.scope.match(regExp)[1];
    }
    return '';
}

function disableAllButtons(disable) {
    let chooseLabelClassList = document.getElementById('file-xlsx-label').classList;
    if (disable) {
        chooseLabelClassList.add("disabled");
    } else {
        chooseLabelClassList.remove("disabled");
    }
    document.getElementById('file-xlsx').disabled = disable;
    document.getElementById('import-button').disabled = disable;
}

function readMappingNames() {
    SbbCommon.callAsync({
        method: 'GET',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/mappings/names?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        onOk: (responseText) => {
            const previouslySelectedValue = SbbCommon.getCookie(SELECTED_CONFIGURATION_COOKIE + 'mappings');
            const container = document.getElementById("mapping-select");
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