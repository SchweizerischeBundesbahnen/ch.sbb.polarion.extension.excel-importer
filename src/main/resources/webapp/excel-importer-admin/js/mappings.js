SbbCommon.init({
    extension: 'excel-importer',
    setting: 'mappings',
    scope: SbbCommon.getValueById('scope')
});
Configurations.init({
    setConfigurationContentCallback: parseAndSetSettings
});

function getColumnNames() {
    return ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z']
}

const cache = {
    fields: undefined,
    enumsMapping: {}
}

function createMappingRow({recreateAddButton, columnValue, fieldValue}) {
    if (recreateAddButton) {
        document.getElementById('add-button').remove();
    }
    const table = document.getElementById('mapping-table');
    const tableRow = document.createElement('tr');
    tableRow.classList.add('mapping-row');
    createRemoveButtonCell(tableRow);
    createColumnCell(tableRow, columnValue);
    createFieldCell(tableRow, fieldValue);
    table.appendChild(tableRow);
    if (recreateAddButton) {
        createAddButton();
    }
}

function createColumnCell(tableRow, columnValue) {
    const columnCell = document.createElement('td');
    const columnLabel = document.createElement('label');
    columnLabel.innerHTML = 'Column: ';
    columnCell.appendChild(columnLabel);
    const columnSelect = document.createElement('select');
    columnSelect.classList.add('fs-14', 'columns');
    columnCell.appendChild(columnSelect);
    columnSelect.addEventListener('change', () => {
        updateLinkColumnDropdown();
    });
    tableRow.appendChild(columnCell)
    populateColumnDropdown(tableRow, columnValue);
}

function populateColumnDropdown(container, selectedValue) {
    populateDropdown(container, 'columns', getColumnNames(), selectedValue);
}

function createFieldCell(tableRow, fieldValue) {
    const fieldCell = document.createElement('td');
    const fieldLabel = document.createElement('label');
    fieldLabel.innerHTML = ' Field Name: ';
    fieldCell.appendChild(fieldLabel);

    const fieldSelect = document.createElement('select');
    fieldSelect.classList.add('fs-14', 'fields');
    fieldCell.appendChild(fieldSelect);

    const mappingButton = document.createElement('button');
    mappingButton.classList.add('toolbar-button', 'options-mapping-button');
    mappingButton.textContent = 'Options mapping';
    mappingButton.style.display = 'none';
    mappingButton.addEventListener('click', () => {

        let popupBody = document.getElementById('modal-popup-content');
        popupBody.innerHTML = '';

        popupBody.appendChild(createHiddenInput('field-id', fieldSelect.value));

        const hint = document.createElement('span');
        hint.classList.add('option-mapping-hint');
        hint.innerHTML = 'Use <b>(empty)</b> keyword to map empty column value to some specific option.';
        const hintWrapper = document.createElement('span');
        hintWrapper.classList.add('option-mapping-hint-wrapper');
        hintWrapper.appendChild(hint);
        popupBody.appendChild(hintWrapper);

        const table = document.createElement('table');
        table.id = 'options-mapping-table';
        popupBody.appendChild(table);

        const options = getField(fieldSelect.value).options;
        for (const option of options) {
            const row = document.createElement('tr');
            row.classList.add('options-mapping-row');
            table.appendChild(row);

            let cell = document.createElement('td');
            row.appendChild(cell);

            const optionKey = document.createElement('span');
            optionKey.classList.add('fs-14', 'option-mapping-key');
            optionKey.title = option.key;
            optionKey.innerHTML = option.key;
            cell.classList.add('option-mapping-key-column');
            cell.appendChild(optionKey);

            cell = document.createElement('td');
            row.appendChild(cell);

            const input = document.createElement('input');
            input.type = "text";
            input.placeholder = option.name;
            input.title = `Comma-separated list of alternative values, which will be mapped to the option "${option.key}"`;
            input.value = getEnumMappingForField(fieldSelect.value, option.key);
            input.classList.add('fs-14', 'option-mapping-value');
            cell.appendChild(input);

            cell.appendChild(createHiddenInput('option-id', option.key));
        }

        MicroModal.show('modal-popup');
    });
    fieldCell.appendChild(mappingButton);

    fieldSelect.addEventListener('change', () => {
        updateOptionsComponents(fieldSelect.value, mappingButton);
    });

    tableRow.appendChild(fieldCell);
    populateFieldDropdown(tableRow, fieldValue);
}

function updateOptionsComponents(selectedField, mappingButton) {
    mappingButton.style.display = hasOptions(selectedField) ? 'inline-block' : 'none';
}

function hasOptions(fieldId) {
    const field = getField(fieldId);
    return field && field.options != null && field.options.length > 0;
}

function getField(fieldId) {
    for (const field of cache.fields) {
        if (field.id === fieldId) {
            return field;
        }
    }
    return null;
}

function getEnumMappingForField(fieldId, key) {
    let enumMapping = cache.enumsMapping[fieldId];
    let value = enumMapping ? enumMapping[key] : null;
    return value ? value : '';
}

function createHiddenInput(className, value) {
    const idInput = document.createElement('input');
    idInput.classList.add(className);
    idInput.type = 'hidden';
    idInput.value = value;
    return idInput;
}

function saveOptionsMapping() {
    const fieldId = (document.getElementById('modal-popup').getElementsByClassName('field-id')[0]).value;
    cache.enumsMapping[fieldId] = Object.fromEntries(getOptionsMapping());
}

function populateFieldDropdown(container, selectedValue) {
    SbbCommon.callAsync({
        method: 'GET',
        url: `/polarion/${SbbCommon.extension}/rest/internal/projects/${getProjectIdFromScope()}/workitem_types/${SbbCommon.getValueById('wi-types')}/fields`,
        contentType: 'application/json',
        onOk: (responseText) => {
            cache.fields = JSON.parse(responseText);
            const ids = [];
            for (const field of cache.fields) {
                ids.push(field.id);
            }
            populateDropdown(container, 'fields', ids, selectedValue);

            const mappingButton = container.getElementsByClassName('options-mapping-button')[0];
            updateOptionsComponents(selectedValue, mappingButton);
        },
        onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
    });
}

function createRemoveButtonCell(tableRow) {
    const buttonCell = document.createElement('td');
    const removeButton = document.createElement('div');
    const image = document.createElement('img');
    image.setAttribute('src', '/polarion/ria/images/control/tableMinus.png');
    removeButton.appendChild(image);
    removeButton.addEventListener('click', function () {
        tableRow.remove();
        updateLinkColumnDropdown();
    })
    buttonCell.appendChild(removeButton);
    tableRow.appendChild(buttonCell);
}

function createAddButton() {
    const row = document.createElement('tr');
    row.id = 'add-button';
    const cell = document.createElement('td');
    cell.colSpan = 3;
    row.appendChild(cell);
    const div = document.createElement('div');
    div.title = 'Add';
    div.addEventListener('click', () => {
        if (SbbCommon.getValueById('wi-types').trim().length === 0) {
            SbbCommon.showActionAlert({ containerId: 'action-error', message: 'Select workitem type first to start creating a mapping'});
        } else {
            createMappingRow({recreateAddButton: true});
            updateLinkColumnDropdown();
        }
    })
    const image = document.createElement('img');
    image.src = '/polarion/ria/images/control/tablePlus.png';
    div.appendChild(image);
    cell.appendChild(div);
    document.getElementById('mapping-table').appendChild(row);
}

function createWITypesDropdown() {
    SbbCommon.setLoadingErrorNotificationVisible(false);

    return new Promise((resolve, reject) => {
        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/projects/${getProjectIdFromScope()}/workitem_types`,
            contentType: 'application/json',
            onOk: (responseText) => {
                const workItemTypes = JSON.parse(responseText);
                const select = document.getElementById('wi-types');
                select.innerHTML = '';
                workItemTypes.forEach((value) => {
                    const option = document.createElement('option');
                    option.value = value.id;
                    option.innerHTML = value.name;
                    select.appendChild(option);
                });
                select.addEventListener('change', () => {
                    updateFieldDropdowns();
                });
                resolve();
            },
            onError: () => {
                SbbCommon.setLoadingErrorNotificationVisible(true);
                reject();
            }
        })
    });
}

function updateFieldDropdowns() {
    const table = document.getElementById('mapping-table');
    const tableRows = table.getElementsByClassName('mapping-row');
    if (tableRows !== undefined && tableRows !== null) {
        Array.from(tableRows).forEach((row) => {
            const fields = row.getElementsByClassName('fields')[0];
            populateFieldDropdown(row, fields.value);
        })
    }
}

function populateDropdown(container, className, data, selectedValue) {
    const select = container.getElementsByClassName(className)[0];
    select.innerHTML = '';
    data.forEach((value) => {
        const option = document.createElement('option');
        option.value = value;
        option.innerHTML = value;
        select.appendChild(option);
    });
    if (selectedValue !== undefined) {
        select.value = selectedValue;
    }
}

function getColumnToFieldMapping() {
    const container = document.getElementById('mapping-table');
    const rows = container.getElementsByClassName('mapping-row');
    const map = new Map();
    Array.from(rows).forEach(row => {
        const columns = row.getElementsByClassName('columns')[0];
        const columnValue = columns.value;
        const fields = row.getElementsByClassName('fields')[0];
        const fieldValue = fields.value;
        map.set(columnValue, fieldValue);
    });
    return map;
}

function getOptionsMapping() {
    const container = document.getElementById('options-mapping-table');
    const rows = container.getElementsByClassName('options-mapping-row');
    const map = new Map();
    Array.from(rows).forEach(row => {
        map.set((row.getElementsByClassName('option-id')[0]).value, (row.getElementsByClassName('option-mapping-value')[0]).value);
    });
    return map;
}

function updateLinkColumnDropdown() {
    const table = document.getElementById('mapping-table');
    const columns = table.getElementsByClassName('columns');
    const names = new Set();
    Array.from(columns).forEach((option) => {
        names.add(option.value);
    });
    const linkColumnContainer = document.getElementById('link-column-container');
    const linkColumnDropdown = document.getElementById('link-column');
    const value = linkColumnDropdown.value;
    linkColumnDropdown.innerHTML = '';
    populateDropdown(linkColumnContainer, 'columns', names, value);
}

function parseAndSetSettings(text) {
    const settings = JSON.parse(text);
    SbbCommon.setValueById('sheet-name', settings.sheetName);
    SbbCommon.setValueById('start-from-row', settings.startFromRow);
    SbbCommon.setCheckboxValueById('overwrite', settings.overwriteWithEmpty);
    SbbCommon.setValueById('wi-types', settings.defaultWorkItemType);
    document.getElementById('mapping-table').innerHTML = '';
    Object.entries(settings.columnsMapping).forEach(entry => {
        createMappingRow({columnValue: entry[0], fieldValue: entry[1]});
    })
    cache.enumsMapping = settings.enumsMapping == null ? {} : settings.enumsMapping;
    updateLinkColumnDropdown();
    SbbCommon.setValueById('link-column', settings.linkColumn);
    createAddButton();

    if (settings.bundleTimestamp !== SbbCommon.getValueById('bundle-timestamp')) {
        SbbCommon.setNewerVersionNotificationVisible(true);
    }
}

function getProjectIdFromScope() {
    if (SbbCommon.scope) {
        const regExp = 'project/(.*)/'
        return SbbCommon.scope.match(regExp)[1];
    }
    return '';
}

function saveSettings() {
    if (!validateBeforeSave()) {
        return;
    }

    SbbCommon.hideActionAlerts();

    SbbCommon.callAsync({
        method: 'PUT',
        url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/names/${Configurations.getSelectedConfiguration()}/content?scope=${SbbCommon.scope}`,
        contentType: 'application/json',
        body: JSON.stringify({
            'sheetName': SbbCommon.getValueById('sheet-name'),
            'startFromRow': SbbCommon.getValueById('start-from-row'),
            'overwriteWithEmpty': SbbCommon.getCheckboxValueById('overwrite'),
            'columnsMapping': Object.fromEntries(getColumnToFieldMapping()),
            'enumsMapping': cache.enumsMapping,
            'defaultWorkItemType': SbbCommon.getValueById('wi-types'),
            'linkColumn': SbbCommon.getValueById('link-column')
        }),
        onOk: () => {
            SbbCommon.showSaveSuccessAlert();
            SbbCommon.setNewerVersionNotificationVisible(false);
            Configurations.loadConfigurationNames();
        },
        onError: () => SbbCommon.showSaveErrorAlert()
    });
}

function validateBeforeSave() {
    const sheetNameIsEmpty = checkIfEmptyValue(document.getElementById('sheet-name'));
    if (sheetNameIsEmpty) {
        showErrorMessage('Sheet name cannot be empty');
        return false;
    }
    const startFromRowValue = SbbCommon.getValueById('start-from-row');
    if (startFromRowValue === undefined || startFromRowValue === null || startFromRowValue < 1) {
        showErrorMessage('Row number must be a positive integer value');
        return false;
    }
    const validationResult = validateMapping();
    if (!validationResult.valid) {
        showErrorMessage(`Mapping for column ${validationResult.columnName} cannot be empty`);
        return false;
    }
    const linkColumnIsEmpty = checkIfEmptyValue(document.getElementById('link-column'));
    if (linkColumnIsEmpty) {
        showErrorMessage('Link column cannot be empty');
        return false;
    }
    return true;
}

function validateMapping() {
    const mappingTable = document.getElementById('mapping-table');
    let mappingRows = mappingTable.getElementsByClassName('mapping-row');
    let validationResult = {valid: true};
    Array.from(mappingRows).forEach((row) => {
        const fieldName = row.getElementsByClassName('fields')[0].value;
        const columnName = row.getElementsByClassName('columns')[0].value;
        if (fieldName.trim().length === 0) {
            validationResult = {columnName: columnName, valid: false};
        }
    })
    return validationResult;
}

function checkIfEmptyValue(element) {
    const value = element.value;
    return value === undefined || value === null || value.trim().length === 0;
}

function showErrorMessage(errorMessage) {
    SbbCommon.showActionAlert({ containerId: 'action-error', message: errorMessage});
}

function revertToDefault() {
    if (confirm('Are you sure you want to return the default values?')) {
        SbbCommon.setLoadingErrorNotificationVisible(false);
        SbbCommon.hideActionAlerts();

        SbbCommon.callAsync({
            method: 'GET',
            url: `/polarion/${SbbCommon.extension}/rest/internal/settings/${SbbCommon.setting}/default-content`,
            contentType: 'application/json',
            onOk: (responseText) => {
                parseAndSetSettings(responseText);
                SbbCommon.showRevertedToDefaultAlert();
            },
            onError: () => SbbCommon.setLoadingErrorNotificationVisible(true)
        });
    }
}

Promise.all([
    createWITypesDropdown()
]).then(() => {
    Configurations.loadConfigurationNames();
});
