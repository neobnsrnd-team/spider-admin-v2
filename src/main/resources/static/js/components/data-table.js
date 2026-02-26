/**
 * SpiderDataTable - Carbon DataTable 패턴 데이터 그리드
 *
 * Usage:
 *   const table = SpiderDataTable.create(container, {
 *     columns: [
 *       { header: 'common.name', field: 'menuName', width: 200, sortable: true },
 *       { header: 'URL', field: 'menuUrl', flex: 1 },
 *       { header: '', field: '_actions', type: 'actions', width: 80 }
 *     ],
 *     selectable: 'multi',
 *     onRowClick: (row) => {},
 *     onSort: (field, direction) => {},
 *     onSelectionChange: (selected) => {}
 *   });
 *   table.setData(rows);
 *   table.getSelected();
 *   table.setLoading(true);
 */
(function () {
    'use strict';

    /* ── SVG Icons ── */
    const ICON_SORT_NONE = '<svg class="spider-table-sort-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">'
        + '<path d="M8 2l3.5 4H4.5zM8 14l-3.5-4h7z" opacity="0.3"/></svg>';

    const ICON_SORT_ASC = '<svg class="spider-table-sort-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">'
        + '<path d="M8 2l3.5 4H4.5z"/><path d="M8 14l-3.5-4h7z" opacity="0.3"/></svg>';

    const ICON_SORT_DESC = '<svg class="spider-table-sort-icon" width="16" height="16" viewBox="0 0 16 16" fill="currentColor">'
        + '<path d="M8 2l3.5 4H4.5z" opacity="0.3"/><path d="M8 14l-3.5-4h7z"/></svg>';

    const ICON_CHECKBOX_EMPTY = '<svg width="18" height="18" viewBox="0 0 20 20" fill="none" stroke="var(--cds-icon-secondary)" stroke-width="1.5">'
        + '<rect x="2" y="2" width="16" height="16" rx="2"/></svg>';

    const ICON_CHECKBOX_CHECKED = '<svg width="18" height="18" viewBox="0 0 20 20" fill="var(--cds-interactive)">'
        + '<rect x="2" y="2" width="16" height="16" rx="2"/>'
        + '<path d="M8.5 13.5l-3-3 1.06-1.06L8.5 11.38l4.94-4.94L14.5 7.5z" fill="var(--cds-icon-on-color)"/></svg>';

    const ICON_CHECKBOX_INDETERMINATE = '<svg width="18" height="18" viewBox="0 0 20 20" fill="var(--cds-interactive)">'
        + '<rect x="2" y="2" width="16" height="16" rx="2"/>'
        + '<rect x="6" y="9" width="8" height="2" fill="var(--cds-icon-on-color)"/></svg>';

    const ICON_SPINNER = '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="var(--cds-interactive)" stroke-width="2">'
        + '<path d="M12 2a10 10 0 0 1 10 10" stroke-linecap="round">'
        + '<animateTransform attributeName="transform" type="rotate" from="0 12 12" to="360 12 12" dur="0.8s" repeatCount="indefinite"/>'
        + '</path></svg>';

    /**
     * Detect i18n key: starts with a-z and contains a dot
     */
    function translateHeader(header) {
        if (!header) return '';
        if (/^[a-z]/.test(header) && header.indexOf('.') !== -1) {
            return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(header) : header;
        }
        return header;
    }

    function t(key) {
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    /**
     * Build inline style string for a column cell
     */
    function cellStyle(col) {
        const parts = [];
        if (col.width) {
            parts.push('width:' + col.width + 'px');
            parts.push('min-width:' + col.width + 'px');
            parts.push('max-width:' + col.width + 'px');
        }
        if (col.flex) {
            parts.push('flex:' + col.flex);
        }
        if (!col.width && !col.flex) {
            parts.push('flex:1');
        }
        const align = col.align || (col.type === 'actions' ? 'center' : 'left');
        parts.push('text-align:' + align);
        if (col.type === 'checkbox') {
            parts.push('justify-content:center');
        }
        return parts.join(';');
    }

    /**
     * Create a DataTable instance
     */
    function create(container, options) {
        options = options || {};
        const columns = options.columns || [];
        const selectable = options.selectable || 'none'; // none | single | multi
        const onRowClick = options.onRowClick || null;
        const onSort = options.onSort || null;
        const onSelectionChange = options.onSelectionChange || null;

        let data = [];
        let selected = new Set();
        let sortField = null;
        let sortDirection = null; // 'asc' | 'desc' | null
        let loading = false;

        // Build effective columns: prepend checkbox column if selectable
        const effectiveColumns = [];
        if (selectable !== 'none') {
            effectiveColumns.push({
                header: '',
                field: '_checkbox',
                type: 'checkbox',
                width: 44,
                sortable: false,
            });
        }
        columns.forEach(function (col) {
            effectiveColumns.push(col);
        });

        /* ── Root element ── */
        const root = document.createElement('div');
        root.className = 'spider-table';
        Object.assign(root.style, {
            display: 'flex',
            flexDirection: 'column',
            width: '100%',
            height: '100%',
            border: '1px solid var(--cds-border-subtle-00)',
            background: 'var(--cds-layer-02)',
            fontSize: 'var(--cds-body-01-size)',
            position: 'relative',
            overflow: 'hidden',
        });

        /* ── Header row ── */
        const headerRow = document.createElement('div');
        headerRow.className = 'spider-table-header';
        Object.assign(headerRow.style, {
            display: 'flex',
            alignItems: 'center',
            minHeight: '2.5rem',
            background: 'var(--cds-layer-01)',
            borderBottom: '2px solid var(--cds-border-subtle-01)',
            fontWeight: '600',
            fontSize: 'var(--cds-label-01-size)',
            color: 'var(--cds-text-secondary)',
            letterSpacing: '0.02em',
            userSelect: 'none',
        });
        root.appendChild(headerRow);

        /* ── Body ── */
        const body = document.createElement('div');
        body.className = 'spider-table-body';
        Object.assign(body.style, {
            flex: '1',
            overflowY: 'auto',
            overflowX: 'hidden',
        });
        root.appendChild(body);

        /* ── Empty state ── */
        const emptyEl = document.createElement('div');
        emptyEl.className = 'spider-table-empty';
        Object.assign(emptyEl.style, {
            display: 'none',
            padding: '3rem 1rem',
            textAlign: 'center',
            color: 'var(--cds-text-placeholder)',
            fontSize: 'var(--cds-body-01-size)',
        });
        root.appendChild(emptyEl);

        /* ── Loading overlay ── */
        const loadingEl = document.createElement('div');
        loadingEl.className = 'spider-table-loading';
        Object.assign(loadingEl.style, {
            display: 'none',
            position: 'absolute',
            inset: '0',
            background: 'var(--cds-overlay)',
            zIndex: '10',
            justifyContent: 'center',
            alignItems: 'center',
        });
        loadingEl.innerHTML = ICON_SPINNER;
        root.appendChild(loadingEl);

        container.appendChild(root);

        /* ── Render header cells ── */
        function renderHeader() {
            headerRow.innerHTML = '';
            effectiveColumns.forEach(function (col) {
                const cell = document.createElement('div');
                cell.className = 'spider-table-cell spider-table-header-cell';
                cell.setAttribute('data-field', col.field);
                Object.assign(cell.style, {
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.25rem',
                    padding: '0 0.75rem',
                    height: '100%',
                    boxSizing: 'border-box',
                    overflow: 'hidden',
                    whiteSpace: 'nowrap',
                    textOverflow: 'ellipsis',
                });
                cell.style.cssText += ';' + cellStyle(col);

                if (col.type === 'checkbox' && selectable === 'multi') {
                    // Header select-all checkbox
                    const cb = document.createElement('span');
                    cb.className = 'spider-table-checkbox spider-table-header-checkbox';
                    cb.style.cursor = 'pointer';
                    cb.style.display = 'inline-flex';
                    cb.style.alignItems = 'center';
                    cb.innerHTML = ICON_CHECKBOX_EMPTY;
                    cb.addEventListener('click', function () {
                        toggleSelectAll();
                    });
                    cell.appendChild(cb);
                } else if (col.type !== 'checkbox') {
                    const label = document.createElement('span');
                    label.textContent = translateHeader(col.header);
                    cell.appendChild(label);

                    if (col.sortable) {
                        const sortIcon = document.createElement('span');
                        sortIcon.className = 'spider-table-sort';
                        sortIcon.style.display = 'inline-flex';
                        sortIcon.style.alignItems = 'center';
                        sortIcon.style.cursor = 'pointer';
                        sortIcon.style.marginLeft = '0.25rem';
                        sortIcon.innerHTML = ICON_SORT_NONE;
                        cell.appendChild(sortIcon);
                        cell.style.cursor = 'pointer';

                        cell.addEventListener('click', function () {
                            handleSort(col.field);
                        });
                    }
                }

                headerRow.appendChild(cell);
            });
        }

        /* ── Render body rows ── */
        function renderBody() {
            body.innerHTML = '';

            if (data.length === 0 && !loading) {
                emptyEl.textContent = t('common.noData');
                emptyEl.style.display = 'block';
                body.style.display = 'none';
                return;
            }

            emptyEl.style.display = 'none';
            body.style.display = 'block';

            data.forEach(function (rowData, rowIndex) {
                const row = document.createElement('div');
                row.className = 'spider-table-row';
                row.__data = rowData;
                row.setAttribute('data-row-index', rowIndex);
                Object.assign(row.style, {
                    display: 'flex',
                    alignItems: 'center',
                    minHeight: '2.5rem',
                    borderBottom: '1px solid var(--cds-border-subtle-00)',
                    transition: 'background-color 80ms ease',
                    cursor: onRowClick ? 'pointer' : 'default',
                });

                // Hover effect
                row.addEventListener('mouseenter', function () {
                    if (!selected.has(rowIndex)) {
                        row.style.backgroundColor = 'var(--cds-layer-hover-01)';
                    }
                });
                row.addEventListener('mouseleave', function () {
                    if (!selected.has(rowIndex)) {
                        row.style.backgroundColor = '';
                    }
                });

                // Row click
                row.addEventListener('click', function (e) {
                    // Ignore clicks on checkbox or action buttons
                    if (e.target.closest('.spider-table-checkbox') || e.target.closest('.spider-table-action')) {
                        return;
                    }
                    if (selectable === 'single') {
                        toggleSelect(rowIndex);
                    }
                    if (onRowClick) {
                        onRowClick(rowData);
                    }
                });

                effectiveColumns.forEach(function (col) {
                    const cell = document.createElement('div');
                    cell.className = 'spider-table-cell';
                    cell.setAttribute('data-field', col.field);
                    Object.assign(cell.style, {
                        display: 'flex',
                        alignItems: 'center',
                        padding: '0 0.75rem',
                        height: '100%',
                        boxSizing: 'border-box',
                        overflow: 'hidden',
                        whiteSpace: 'nowrap',
                        textOverflow: 'ellipsis',
                        color: 'var(--cds-text-primary)',
                    });
                    cell.style.cssText += ';' + cellStyle(col);

                    if (col.type === 'checkbox') {
                        const cb = document.createElement('span');
                        cb.className = 'spider-table-checkbox';
                        cb.style.cursor = 'pointer';
                        cb.style.display = 'inline-flex';
                        cb.style.alignItems = 'center';
                        cb.innerHTML = selected.has(rowIndex) ? ICON_CHECKBOX_CHECKED : ICON_CHECKBOX_EMPTY;
                        cb.addEventListener('click', function (e) {
                            e.stopPropagation();
                            toggleSelect(rowIndex);
                        });
                        cell.appendChild(cb);
                    } else if (col.renderer) {
                        const html = col.renderer(rowData[col.field], rowData, col.field);
                        cell.innerHTML = html;
                    } else {
                        const val = rowData[col.field];
                        cell.textContent = val != null ? String(val) : '';
                        cell.title = val != null ? String(val) : '';
                    }

                    row.appendChild(cell);
                });

                // Apply selected style
                if (selected.has(rowIndex)) {
                    row.style.backgroundColor = 'var(--cds-background-selected)';
                }

                body.appendChild(row);
            });
        }

        /* ── Selection logic ── */
        function toggleSelect(rowIndex) {
            if (selectable === 'single') {
                if (selected.has(rowIndex)) {
                    selected.clear();
                } else {
                    selected.clear();
                    selected.add(rowIndex);
                }
            } else if (selectable === 'multi') {
                if (selected.has(rowIndex)) {
                    selected.delete(rowIndex);
                } else {
                    selected.add(rowIndex);
                }
            }
            updateSelectionUI();
            fireSelectionChange();
        }

        function toggleSelectAll() {
            if (selected.size === data.length && data.length > 0) {
                selected.clear();
            } else {
                data.forEach(function (_, i) { selected.add(i); });
            }
            updateSelectionUI();
            fireSelectionChange();
        }

        function updateSelectionUI() {
            // Update row checkboxes and background
            const rows = qsa('.spider-table-row', body);
            rows.forEach(function (row) {
                const idx = parseInt(row.getAttribute('data-row-index'), 10);
                const cb = qs('.spider-table-checkbox', row);
                if (cb) {
                    cb.innerHTML = selected.has(idx) ? ICON_CHECKBOX_CHECKED : ICON_CHECKBOX_EMPTY;
                }
                row.style.backgroundColor = selected.has(idx) ? 'var(--cds-background-selected)' : '';
            });

            // Update header checkbox (multi only)
            if (selectable === 'multi') {
                const headerCb = qs('.spider-table-header-checkbox', headerRow);
                if (headerCb) {
                    if (data.length === 0 || selected.size === 0) {
                        headerCb.innerHTML = ICON_CHECKBOX_EMPTY;
                    } else if (selected.size === data.length) {
                        headerCb.innerHTML = ICON_CHECKBOX_CHECKED;
                    } else {
                        headerCb.innerHTML = ICON_CHECKBOX_INDETERMINATE;
                    }
                }
            }
        }

        function fireSelectionChange() {
            if (onSelectionChange) {
                const items = [];
                selected.forEach(function (idx) {
                    if (data[idx]) items.push(data[idx]);
                });
                onSelectionChange(items);
            }
        }

        /* ── Sort logic ── */
        function handleSort(field) {
            if (sortField === field) {
                // Cycle: asc -> desc -> none
                if (sortDirection === 'asc') {
                    sortDirection = 'desc';
                } else if (sortDirection === 'desc') {
                    sortDirection = null;
                    sortField = null;
                } else {
                    sortDirection = 'asc';
                }
            } else {
                sortField = field;
                sortDirection = 'asc';
            }
            updateSortUI();
            if (onSort) {
                onSort(sortField, sortDirection);
            }
        }

        function updateSortUI() {
            const headerCells = qsa('.spider-table-header-cell', headerRow);
            headerCells.forEach(function (cell) {
                const icon = qs('.spider-table-sort', cell);
                if (!icon) return;
                const field = cell.getAttribute('data-field');
                if (field === sortField) {
                    if (sortDirection === 'asc') {
                        icon.innerHTML = ICON_SORT_ASC;
                    } else if (sortDirection === 'desc') {
                        icon.innerHTML = ICON_SORT_DESC;
                    } else {
                        icon.innerHTML = ICON_SORT_NONE;
                    }
                } else {
                    icon.innerHTML = ICON_SORT_NONE;
                }
            });
        }

        /* ── Initial render ── */
        renderHeader();
        renderBody();

        /* ── Public API ── */
        return {
            /**
             * Set data rows and re-render
             * @param {Array} rows
             */
            setData: function (rows) {
                data = rows || [];
                selected.clear();
                renderBody();
                updateSelectionUI();
            },

            /**
             * Get currently selected row data
             * @returns {Array}
             */
            getSelected: function () {
                const items = [];
                selected.forEach(function (idx) {
                    if (data[idx]) items.push(data[idx]);
                });
                return items;
            },

            /**
             * Set loading overlay
             * @param {boolean} isLoading
             */
            setLoading: function (isLoading) {
                loading = isLoading;
                loadingEl.style.display = isLoading ? 'flex' : 'none';
            },

            /**
             * Get all data
             * @returns {Array}
             */
            getData: function () {
                return data.slice();
            },

            /**
             * Get sort state
             * @returns {{ field: string|null, direction: string|null }}
             */
            getSort: function () {
                return { field: sortField, direction: sortDirection };
            },

            /**
             * Clear selection
             */
            clearSelection: function () {
                selected.clear();
                updateSelectionUI();
                fireSelectionChange();
            },

            /**
             * Re-render (e.g., after language change)
             */
            refresh: function () {
                renderHeader();
                renderBody();
                updateSelectionUI();
                updateSortUI();
            },

            /** Root DOM element */
            el: root,
        };
    }

    window.SpiderDataTable = {
        create: create,
    };
})();
