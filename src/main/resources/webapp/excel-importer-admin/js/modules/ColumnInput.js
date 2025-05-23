export default class ColumnInput {

    constructor({
                    containerElement,
                    initialValue,
                    inputPlaceholder,
                    outputSpan,
                    errorSpan,
                    onValueChange = () => {
                    },
                }) {
        this.containerElement = containerElement;
        this.containerElement.classList.add('column-input-wrapper');
        this.outputSpan = outputSpan;
        this.errorSpan = errorSpan;
        this.uniqueId = Math.random().toString(36).substring(2, 9); // Generate a unique ID suffix

        this.onValueChange = onValueChange;
        this.inputPlaceholder = inputPlaceholder;
        this.excelColumnInput = null;
        this.columnDropdown = null;

        this.render();
        this.populateDropdown();
        this.addEventListeners();

        if (initialValue) {
            this.excelColumnInput.value = initialValue.toUpperCase(); // Ensure initial value is uppercase
            this.handleInput();
        }
    }

    render() {
        // Create Input field
        this.excelColumnInput = document.createElement('input');
        this.excelColumnInput.setAttribute('type', 'text');
        this.excelColumnInput.setAttribute('id', `excelColumnIdentifier_${this.uniqueId}`);
        this.excelColumnInput.classList.add('excel-column-input');
        this.excelColumnInput.setAttribute('placeholder', this.inputPlaceholder ?? '');
        this.excelColumnInput.setAttribute('maxlength', '5');
        this.excelColumnInput.setAttribute('autocomplete', 'off');
        this.containerElement.appendChild(this.excelColumnInput);

        // Create Dropdown menu container
        this.columnDropdown = document.createElement('div');
        this.columnDropdown.setAttribute('id', `columnDropdown_${this.uniqueId}`);
        this.columnDropdown.classList.add('dropdown-menu', 'hidden');
        this.containerElement.appendChild(this.columnDropdown);
    }

    populateDropdown() {
        for (let i = 0; i < 26; i++) {
            const charCode = 'A'.charCodeAt(0) + i;
            const letter = String.fromCharCode(charCode);

            const dropdownItem = document.createElement('div');
            dropdownItem.classList.add('dropdown-item');
            dropdownItem.textContent = letter;

            // Use an arrow function to maintain 'this' context
            dropdownItem.addEventListener('click', () => {
                this.excelColumnInput.value = letter;
                this.excelColumnInput.dispatchEvent(new Event('input', {bubbles: true}));
                this.columnDropdown.classList.add('hidden');
                this.excelColumnInput.focus();
            });

            this.columnDropdown.appendChild(dropdownItem);
        }
    }

    adjustDropdownPosition() {
        const inputRect = this.excelColumnInput.getBoundingClientRect();
        const parentRect = this.excelColumnInput.parentElement.getBoundingClientRect();

        const relativeLeft = inputRect.left - parentRect.left;

        this.columnDropdown.style.width = `${inputRect.width}px`;
        this.columnDropdown.style.left = `${relativeLeft}px`;
    }

    handleInput() {
        let inputValue = this.excelColumnInput.value;
        let sanitizedValue = '';
        let hasInvalidChar = false;

        for (let i = 0; i < inputValue.length; i++) {
            const char = inputValue[i];
            if ((char >= 'a' && char <= 'z') || (char >= 'A' && char <= 'Z')) {
                sanitizedValue += char.toUpperCase();
            } else {
                hasInvalidChar = true;
            }
        }

        this.excelColumnInput.value = sanitizedValue;

        if (this.errorSpan) {
            if (hasInvalidChar) {
                this.errorSpan.textContent = 'Only Latin letters (A-Z) are allowed.';
            } else {
                this.errorSpan.textContent = '';
            }
        }

        const currentValue = this.excelColumnInput.value;
        if (currentValue !== this.previousValue) {
            if (this.outputSpan) {
                this.outputSpan.textContent = currentValue;
            }
            this.onValueChange(currentValue);
            this.previousValue = currentValue;
        }

        if (this.excelColumnInput.value.length === 0) {
            this.adjustDropdownPosition(); // Ensure position is correct before showing
            this.columnDropdown.classList.remove('hidden');
        } else {
            this.columnDropdown.classList.add('hidden');
        }
    }

    /**
     * Handles the 'focus' event, showing the dropdown if input is empty.
     */
    handleFocus() {
        if (this.excelColumnInput.value === '') {
            this.adjustDropdownPosition();
            this.columnDropdown.classList.remove('hidden');
        }
    }

    /**
     * Handles the 'blur' event, hiding the dropdown after a short delay.
     */
    handleBlur() {
        setTimeout(() => {
            this.columnDropdown.classList.add('hidden');
        }, 150);

        // Ensure uppercasing on blur as a safeguard
        const currentValueOnBlur = this.excelColumnInput.value.toUpperCase();
        this.excelColumnInput.value = currentValueOnBlur;

        // Check if the value has truly changed before updating output and calling callback
        if (currentValueOnBlur !== this.previousValue) {
            if (this.outputSpan) {
                this.outputSpan.textContent = currentValueOnBlur;
            }
            this.onValueChange(currentValueOnBlur);
            this.previousValue = currentValueOnBlur;
        }
    }

    /**
     * Handles window resize event to readjust dropdown position.
     */
    handleResize() {
        if (!this.columnDropdown.classList.contains('hidden')) {
            this.adjustDropdownPosition();
        }
    }

    /**
     * Adds all necessary event listeners to the component elements.
     */
    addEventListeners() {
        this.excelColumnInput.addEventListener('input', this.handleInput.bind(this));
        this.excelColumnInput.addEventListener('focus', this.handleFocus.bind(this));
        this.excelColumnInput.addEventListener('blur', this.handleBlur.bind(this));
        window.addEventListener('resize', this.handleResize.bind(this));
    }
}
