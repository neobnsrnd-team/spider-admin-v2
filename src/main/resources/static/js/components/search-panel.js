/**
 * SpiderSearchPanel — Carbon Grid 기반 검색 패널 컴포넌트
 *
 * 검색 조건 필드를 N-column 그리드 레이아웃으로 배치하고,
 * 검색/초기화 버튼을 제공한다.
 *
 * Field types: text, select, dateRange, checkbox
 */
(function () {
    'use strict';

    /**
     * Translate a key if it looks like an i18n key (contains '.').
     * Falls back to the key itself if SpiderI18n is not available.
     */
    function t(key) {
        if (!key) return '';
        if (key.indexOf('.') === -1) return key;
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    /**
     * Create a text input field.
     */
    function createTextField(field) {
        const input = document.createElement('input');
        input.type = 'text';
        input.name = field.name;
        input.className = 'input-field';
        if (field.placeholder) {
            input.placeholder = t(field.placeholder);
        }
        if (field.maxLength) {
            input.maxLength = field.maxLength;
        }
        return input;
    }

    /**
     * Create a select dropdown field.
     */
    function createSelectField(field) {
        const select = document.createElement('select');
        select.name = field.name;
        select.className = 'input-field';

        // "All" placeholder option
        if (field.allOption !== false) {
            const allOpt = document.createElement('option');
            allOpt.value = '';
            allOpt.textContent = t('common.all') || '-- All --';
            select.appendChild(allOpt);
        }

        const options = field.options || [];
        for (let i = 0; i < options.length; i++) {
            const opt = document.createElement('option');
            opt.value = options[i].value;
            opt.textContent = options[i].label;
            select.appendChild(opt);
        }

        return select;
    }

    /**
     * Create a date range field (two date inputs).
     * Uses .spider-search-date-group / .spider-search-date-separator CSS classes.
     */
    function createDateRangeField(field) {
        const wrapper = document.createElement('div');
        wrapper.className = 'spider-search-date-group';

        const startInput = document.createElement('input');
        startInput.type = 'date';
        startInput.name = field.name + 'Start';
        startInput.className = 'input-field';
        startInput.title = t('date.startDate');

        const separator = document.createElement('span');
        separator.className = 'spider-search-date-separator';
        separator.textContent = '~';

        const endInput = document.createElement('input');
        endInput.type = 'date';
        endInput.name = field.name + 'End';
        endInput.className = 'input-field';
        endInput.title = t('date.endDate');

        wrapper.appendChild(startInput);
        wrapper.appendChild(separator);
        wrapper.appendChild(endInput);

        // Initialize defaults if screen type is provided
        if (field.screen && window.SpiderDateDefaults) {
            SpiderDateDefaults.init(
                field.screen,
                '[name="' + field.name + 'Start"]',
                '[name="' + field.name + 'End"]'
            );
        }

        return wrapper;
    }

    /**
     * Create a checkbox field.
     */
    function createCheckboxField(field) {
        const wrapper = document.createElement('label');
        Object.assign(wrapper.style, {
            display: 'inline-flex',
            alignItems: 'center',
            gap: 'var(--cds-spacing-03)',
            cursor: 'pointer',
            height: '2.5rem',
        });

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.name = field.name;
        checkbox.value = field.value || 'Y';
        Object.assign(checkbox.style, {
            width: '16px',
            height: '16px',
            accentColor: 'var(--cds-interactive)',
            cursor: 'pointer',
        });

        const text = document.createElement('span');
        text.textContent = field.checkLabel ? t(field.checkLabel) : '';
        text.style.color = 'var(--cds-text-primary)';
        text.style.fontSize = 'var(--cds-body-01-size)';

        wrapper.appendChild(checkbox);
        wrapper.appendChild(text);

        return wrapper;
    }

    /**
     * Create a single field wrapper (label + control).
     */
    function createFieldWrapper(field) {
        const wrapper = document.createElement('div');
        wrapper.className = 'spider-search-field';

        // Label
        const label = document.createElement('label');
        label.className = 'spider-search-label';
        label.textContent = t(field.label);
        if (field.name) {
            label.setAttribute('for', 'search-' + field.name);
        }
        wrapper.appendChild(label);

        // Control
        let control;
        switch (field.type) {
            case 'select':
                control = createSelectField(field);
                break;
            case 'dateRange':
                control = createDateRangeField(field);
                break;
            case 'checkbox':
                control = createCheckboxField(field);
                break;
            default:
                control = createTextField(field);
                break;
        }

        // Set id for label association (skip dateRange/checkbox which use wrappers)
        if (field.type !== 'dateRange' && field.type !== 'checkbox' && field.name) {
            control.id = 'search-' + field.name;
        }

        wrapper.appendChild(control);
        return wrapper;
    }

    /**
     * Build the action area (Search + Reset buttons).
     */
    function createActions(onSearch, onReset) {
        const actions = document.createElement('div');
        actions.className = 'spider-search-actions';

        const resetBtn = document.createElement('button');
        resetBtn.type = 'button';
        resetBtn.className = 'btn btn-secondary spider-search-btn-reset';
        resetBtn.textContent = t('common.reset');
        resetBtn.addEventListener('click', function () {
            if (typeof onReset === 'function') onReset();
        });

        const searchBtn = document.createElement('button');
        searchBtn.type = 'button';
        searchBtn.className = 'btn btn-primary spider-search-btn-search';
        searchBtn.textContent = t('common.search');
        searchBtn.addEventListener('click', function () {
            if (typeof onSearch === 'function') onSearch();
        });

        actions.appendChild(resetBtn);
        actions.appendChild(searchBtn);
        return actions;
    }

    /**
     * Extract values from all fields within the panel.
     */
    function getValues(panel) {
        const values = {};

        // Text inputs and selects
        const inputs = qsa('input[type="text"], input[type="date"], select', panel);
        for (let i = 0; i < inputs.length; i++) {
            const input = inputs[i];
            if (input.name) {
                values[input.name] = input.value;
            }
        }

        // Checkboxes
        const checkboxes = qsa('input[type="checkbox"]', panel);
        for (let i = 0; i < checkboxes.length; i++) {
            const cb = checkboxes[i];
            if (cb.name) {
                values[cb.name] = cb.checked ? cb.value : '';
            }
        }

        return values;
    }

    /**
     * Reset all fields within the panel to their default state.
     */
    function resetFields(panel) {
        const textInputs = qsa('input[type="text"], input[type="date"]', panel);
        for (let i = 0; i < textInputs.length; i++) {
            textInputs[i].value = '';
        }

        const selects = qsa('select', panel);
        for (let i = 0; i < selects.length; i++) {
            selects[i].selectedIndex = 0;
        }

        const checkboxes = qsa('input[type="checkbox"]', panel);
        for (let i = 0; i < checkboxes.length; i++) {
            checkboxes[i].checked = false;
        }
    }

    /**
     * Create a search panel and append it to the container.
     *
     * @param {Element} container   - Parent element to append panel into
     * @param {Object}  options     - Configuration
     * @param {Array}   options.fields   - Field definitions
     * @param {Function} options.onSearch - Search callback (receives values object)
     * @param {Function} options.onReset  - Reset callback
     * @param {number}  options.columns  - Number of columns (default 4)
     * @returns {{ getValues: Function, reset: Function, el: Element }}
     */
    function create(container, options) {
        const fields = options.fields || [];
        const columns = options.columns || 4;
        const onSearch = options.onSearch;
        const onReset = options.onReset;

        // Panel root
        const panel = document.createElement('div');
        panel.className = 'spider-search-panel';

        // Grid container
        const grid = document.createElement('div');
        grid.className = 'spider-search-grid';
        grid.style.gridTemplateColumns = 'repeat(' + columns + ', 1fr)';

        // Render fields
        for (let i = 0; i < fields.length; i++) {
            const fieldWrapper = createFieldWrapper(fields[i]);

            // dateRange spans 2 columns
            if (fields[i].type === 'dateRange') {
                fieldWrapper.style.gridColumn = 'span 2';
            }

            grid.appendChild(fieldWrapper);
        }

        panel.appendChild(grid);

        // Action area
        const fireSearch = function () {
            if (typeof onSearch === 'function') {
                onSearch(getValues(panel));
            }
        };

        const fireReset = function () {
            resetFields(panel);
            if (typeof onReset === 'function') {
                onReset();
            }
        };

        const actions = createActions(fireSearch, fireReset);
        panel.appendChild(actions);

        // Enter key triggers search on text inputs
        delegate(panel, 'keydown', 'input[type="text"]', function (e) {
            if (e.key === 'Enter') {
                e.preventDefault();
                fireSearch();
            }
        });

        // Initialize date range defaults after DOM attachment
        if (container) {
            container.appendChild(panel);
        }

        // Re-initialize date defaults after panel is in the DOM
        for (let i = 0; i < fields.length; i++) {
            const field = fields[i];
            if (field.type === 'dateRange' && field.screen && window.SpiderDateDefaults) {
                const startSel = '[name="' + field.name + 'Start"]';
                const endSel = '[name="' + field.name + 'End"]';
                const startEl = qs(startSel, panel);
                const endEl = qs(endSel, panel);
                if (startEl && endEl) {
                    SpiderDateDefaults.init(field.screen, startSel, endSel);
                }
            }
        }

        return {
            el: panel,

            getValues: function () {
                return getValues(panel);
            },

            reset: function () {
                fireReset();
            },

            /** Set a specific field value programmatically */
            setValue: function (name, value) {
                const input = qs('[name="' + name + '"]', panel);
                if (!input) return;
                if (input.type === 'checkbox') {
                    input.checked = !!value;
                } else {
                    input.value = value;
                }
            },

            /** Get the root element */
            getElement: function () {
                return panel;
            },
        };
    }

    window.SpiderSearchPanel = {
        create: create,
    };
})();
