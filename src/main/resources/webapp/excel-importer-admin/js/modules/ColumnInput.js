import { createEditableSelect } from '../../ui/generic/js/modules/searchableSelect.js';

// Excel column-identifier picker: an editable (free-text) SearchableDropdown wrapping a text input.
// The user can type any column identifier (A, B, …, AA, …) or pick a letter from the suggestion
// list; input is sanitised to Latin letters and upper-cased. Replaces the previous bespoke input +
// custom dropdown, so the column field is the same shared combobox as everything else.
export default class ColumnInput {

    constructor({
                    containerElement,
                    initialValue,
                    inputPlaceholder,
                    outputSpan,
                    onValueChange = () => {
                    },
                }) {
        this.containerElement = containerElement;
        this.containerElement.classList.add('column-input-wrapper');
        this.outputSpan = outputSpan;
        this.onValueChange = onValueChange;
        this.inputPlaceholder = inputPlaceholder;
        this.uniqueId = Math.random().toString(36).substring(2, 9);
        this.previousValue = '';

        this.render();

        // Seed the saved value BEFORE wrapping: the editable SearchableDropdown copies the wrapped
        // input's value into its trigger at construction, so a value set afterwards (e.g. on page
        // reload) would not appear in the trigger.
        if (initialValue) {
            this.excelColumnInput.value = initialValue.toUpperCase();
        }

        this.wrap();

        if (initialValue) {
            // Notify consumers (output span / onValueChange) of the restored value.
            this.excelColumnInput.dispatchEvent(new Event('change', {bubbles: true}));
        }
    }

    render() {
        this.excelColumnInput = document.createElement('input');
        this.excelColumnInput.setAttribute('type', 'text');
        this.excelColumnInput.setAttribute('id', `excelColumnIdentifier_${this.uniqueId}`);
        this.excelColumnInput.classList.add('excel-column-input');
        this.excelColumnInput.setAttribute('placeholder', this.inputPlaceholder ?? '');
        this.excelColumnInput.setAttribute('maxlength', '5');
        this.excelColumnInput.setAttribute('autocomplete', 'off');
        this.containerElement.appendChild(this.excelColumnInput);
    }

    wrap() {
        // A–Z column-letter suggestions; free entry (e.g. "AA") stays allowed via editable mode.
        const columns = Array.from({length: 26}, (_, i) => {
            const letter = String.fromCharCode('A'.charCodeAt(0) + i);
            return {value: letter, label: letter};
        });

        createEditableSelect(this.excelColumnInput, {
            placeholder: this.inputPlaceholder ?? '',
            // Only Latin letters, upper-cased (catches typing and paste).
            inputFilter: value => value.replace(/[^A-Za-z]/g, '').toUpperCase(),
            items: columns,
        });

        // SearchableDropdown mirrors the committed value onto the wrapped input and dispatches
        // `change`; forward that to the consumer callback and the optional preview span.
        this.excelColumnInput.addEventListener('change', () => {
            const value = this.excelColumnInput.value;
            if (value === this.previousValue) {
                return;
            }
            this.previousValue = value;
            if (this.outputSpan) {
                this.outputSpan.textContent = value;
            }
            this.onValueChange(value);
        });
    }
}
