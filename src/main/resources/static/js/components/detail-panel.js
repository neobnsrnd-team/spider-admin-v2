/**
 * SpiderDetailPanel -- Carbon Side Panel (right slide-in)
 *
 * Usage:
 *   const panel = SpiderDetailPanel.create({ title, width, fields, onSave, onDelete, resource });
 *   panel.open('create');
 *   panel.open('edit', rowData);
 *   panel.open('view', rowData);
 *   panel.close();
 *   panel.getData();
 */
(function () {
    'use strict';

    /* ── Close (X) SVG icon ── */
    const CLOSE_ICON = '<svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor">' +
        '<path d="M24 9.4L22.6 8 16 14.6 9.4 8 8 9.4l6.6 6.6L8 22.6 9.4 24l6.6-6.6 6.6 6.6 1.4-1.4-6.6-6.6z"/>' +
        '</svg>';

    /* ── Mode label i18n keys ── */
    const MODE_LABELS = {
        create: 'panel.mode.create',
        edit:   'panel.mode.edit',
        view:   'panel.mode.view'
    };

    /* ── Helpers ── */
    function t(key) {
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    function hasWritePermission(resource) {
        return resource && window.SpiderPermission && SpiderPermission.canWrite(resource);
    }

    /* ================================================================
       create(options) -- factory
       ================================================================ */
    function create(options) {
        options = options || {};

        const title    = options.title || '';
        const width    = options.width || '480px';
        const fields   = options.fields || [];
        const onSave   = options.onSave || null;
        const onDelete = options.onDelete || null;
        const resource = options.resource || '';

        let mode        = 'create';
        let rowData     = null;
        let overlay     = null;
        let panelEl     = null;
        let bodyEl      = null;
        let footerEl    = null;
        let modeLabelEl = null;

        /* ── Build DOM once ── */
        buildDom();

        /* ────────────────────────────────────────────
           DOM construction
           ──────────────────────────────────────────── */
        function buildDom() {
            /* Overlay */
            overlay = document.createElement('div');
            overlay.className = 'spider-panel-overlay';
            Object.assign(overlay.style, {
                position:        'fixed',
                inset:           '0',
                backgroundColor: 'var(--cds-overlay)',
                zIndex:          '8000',
                opacity:         '0',
                visibility:      'hidden',
                transition:      'opacity 200ms ease, visibility 200ms ease'
            });

            /* Panel */
            panelEl = document.createElement('div');
            panelEl.className = 'spider-detail-panel';
            Object.assign(panelEl.style, {
                position:        'fixed',
                top:             '0',
                right:           '0',
                bottom:          '0',
                width:           width,
                maxWidth:        '100vw',
                backgroundColor: 'var(--cds-layer-02)',
                boxShadow:       'var(--cds-shadow)',
                display:         'flex',
                flexDirection:   'column',
                zIndex:          '8001',
                transform:       'translateX(100%)',
                transition:      'transform 200ms ease'
            });

            /* Header */
            const headerEl = document.createElement('div');
            headerEl.className = 'spider-panel-header';
            Object.assign(headerEl.style, {
                display:        'flex',
                alignItems:     'center',
                justifyContent: 'space-between',
                padding:        '1rem 1.5rem',
                flexShrink:     '0',
                borderBottom:   '1px solid var(--cds-border-subtle-00)'
            });

            const titleArea = document.createElement('div');
            titleArea.style.display = 'flex';
            titleArea.style.alignItems = 'center';
            titleArea.style.gap = '0.5rem';
            titleArea.style.overflow = 'hidden';

            const titleEl = document.createElement('h3');
            Object.assign(titleEl.style, {
                margin:       '0',
                fontSize:     'var(--cds-heading-03-size)',
                fontWeight:   '600',
                color:        'var(--cds-text-primary)',
                whiteSpace:   'nowrap',
                overflow:     'hidden',
                textOverflow: 'ellipsis'
            });
            titleEl.textContent = t(title);

            modeLabelEl = document.createElement('span');
            Object.assign(modeLabelEl.style, {
                fontSize:   'var(--cds-label-01-size)',
                color:      'var(--cds-text-secondary)',
                flexShrink: '0'
            });

            titleArea.appendChild(titleEl);
            titleArea.appendChild(modeLabelEl);

            const closeBtn = document.createElement('button');
            closeBtn.className = 'header-btn';
            closeBtn.innerHTML = CLOSE_ICON;
            closeBtn.setAttribute('aria-label', 'Close');
            Object.assign(closeBtn.style, {
                display:        'inline-flex',
                alignItems:     'center',
                justifyContent: 'center',
                width:          '2rem',
                height:         '2rem',
                background:     'transparent',
                border:         'none',
                color:          'var(--cds-icon-secondary)',
                cursor:         'pointer',
                flexShrink:     '0'
            });
            closeBtn.addEventListener('click', close);

            headerEl.appendChild(titleArea);
            headerEl.appendChild(closeBtn);

            /* Body (scrollable form area) */
            bodyEl = document.createElement('div');
            bodyEl.className = 'spider-panel-body';
            Object.assign(bodyEl.style, {
                flex:      '1',
                overflowY: 'auto',
                padding:   '1.5rem'
            });

            /* Footer */
            footerEl = document.createElement('div');
            footerEl.className = 'spider-panel-footer';
            Object.assign(footerEl.style, {
                display:        'flex',
                justifyContent: 'flex-end',
                gap:            '0.5rem',
                padding:        '1rem 1.5rem',
                flexShrink:     '0',
                borderTop:      '1px solid var(--cds-border-subtle-00)'
            });

            panelEl.appendChild(headerEl);
            panelEl.appendChild(bodyEl);
            panelEl.appendChild(footerEl);

            overlay.appendChild(panelEl);
            document.body.appendChild(overlay);

            /* Overlay click to close */
            overlay.addEventListener('click', function (e) {
                if (e.target === overlay) close();
            });
        }

        /* ────────────────────────────────────────────
           Field rendering
           ──────────────────────────────────────────── */
        function renderFields() {
            bodyEl.innerHTML = '';

            if (fields.length === 0) return;

            fields.forEach(function (field) {
                const wrapper = document.createElement('div');
                wrapper.className = 'spider-panel-field';
                Object.assign(wrapper.style, {
                    marginBottom: '1.25rem'
                });

                /* Label */
                const label = document.createElement('label');
                Object.assign(label.style, {
                    display:      'block',
                    fontSize:     'var(--cds-label-01-size)',
                    fontWeight:   '500',
                    color:        'var(--cds-text-secondary)',
                    marginBottom: '0.5rem'
                });
                label.textContent = t(field.label || field.name);
                label.setAttribute('for', 'panel-field-' + field.name);
                wrapper.appendChild(label);

                /* Input element */
                let input;
                const fieldType = field.type || 'text';

                if (fieldType === 'select') {
                    input = document.createElement('select');
                    input.className = 'input-field';
                    Object.assign(input.style, {
                        height: '2.5rem'
                    });
                    const opts = field.options || [];
                    opts.forEach(function (opt) {
                        const option = document.createElement('option');
                        option.value = opt.value;
                        option.textContent = opt.label || opt.value;
                        input.appendChild(option);
                    });
                } else if (fieldType === 'textarea') {
                    input = document.createElement('textarea');
                    input.className = 'input-field';
                    Object.assign(input.style, {
                        height:  '6rem',
                        padding: '0.5rem 1rem',
                        resize:  'vertical'
                    });
                } else if (fieldType === 'checkbox') {
                    input = document.createElement('input');
                    input.type = 'checkbox';
                    Object.assign(input.style, {
                        width:       '1rem',
                        height:      '1rem',
                        accentColor: 'var(--cds-interactive)'
                    });
                } else {
                    /* text, number, etc. */
                    input = document.createElement('input');
                    input.type = fieldType === 'text' ? 'text' : fieldType;
                    input.className = 'input-field';
                }

                input.id = 'panel-field-' + field.name;
                input.name = field.name;

                /* Validation data attribute */
                if (field.validate) {
                    input.setAttribute('data-validate', field.validate);
                    input.setAttribute('data-label', t(field.label || field.name));
                }

                /* Readonly fields get disabled in all modes */
                if (field.readonly) {
                    input.disabled = true;
                }

                wrapper.appendChild(input);
                bodyEl.appendChild(wrapper);
            });
        }

        /* ────────────────────────────────────────────
           Footer buttons
           ──────────────────────────────────────────── */
        function renderFooter() {
            footerEl.innerHTML = '';
            const hasWrite = hasWritePermission(resource);

            if (mode === 'create') {
                if (hasWrite) {
                    footerEl.appendChild(makeBtn(t('panel.save'), 'btn-primary', 'W', handleSave));
                }
                footerEl.appendChild(makeBtn(t('panel.cancel'), 'btn-secondary', null, close));
            } else if (mode === 'edit') {
                if (hasWrite) {
                    footerEl.appendChild(makeBtn(t('panel.save'), 'btn-primary', 'W', handleSave));
                    footerEl.appendChild(makeBtn(t('panel.delete'), 'btn-danger', 'W', handleDelete));
                }
                footerEl.appendChild(makeBtn(t('panel.cancel'), 'btn-secondary', null, close));
            } else {
                /* view mode */
                if (hasWrite) {
                    footerEl.appendChild(makeBtn(t('panel.edit'), 'btn-primary', 'W', function () {
                        open('edit', rowData);
                    }));
                }
                footerEl.appendChild(makeBtn(t('panel.cancel'), 'btn-secondary', null, close));
            }
        }

        function makeBtn(text, cls, permission, handler) {
            const btn = document.createElement('button');
            btn.className = 'btn ' + cls;
            btn.textContent = text;
            if (permission) {
                btn.setAttribute('data-permission', permission);
            }
            btn.addEventListener('click', handler);
            return btn;
        }

        /* ────────────────────────────────────────────
           Data helpers
           ──────────────────────────────────────────── */
        function populateFields(data) {
            if (!data || fields.length === 0) return;

            fields.forEach(function (field) {
                const input = qs('#panel-field-' + field.name, bodyEl);
                if (!input) return;

                let value = data[field.name];
                if (value === undefined || value === null) value = '';

                if (field.type === 'checkbox') {
                    input.checked = value === true || value === 'Y' || value === 1;
                } else {
                    input.value = value;
                }
            });
        }

        function setDisabledState(disabled) {
            fields.forEach(function (field) {
                if (field.readonly) return; /* already disabled */
                const input = qs('#panel-field-' + field.name, bodyEl);
                if (input) input.disabled = disabled;
            });
        }

        function getData() {
            const result = {};
            fields.forEach(function (field) {
                const input = qs('#panel-field-' + field.name, bodyEl);
                if (!input) return;
                if (field.type === 'checkbox') {
                    result[field.name] = input.checked;
                } else {
                    result[field.name] = input.value;
                }
            });
            return result;
        }

        /* ────────────────────────────────────────────
           Event handlers
           ──────────────────────────────────────────── */
        function handleSave() {
            /* Validation */
            if (window.SpiderValidation) {
                const result = SpiderValidation.validate(bodyEl);
                if (!result.valid) return;
            }
            if (typeof onSave === 'function') {
                onSave(getData(), mode);
            }
        }

        function handleDelete() {
            if (typeof onDelete !== 'function') return;

            const idFieldDef = fields.find(function (f) { return f.readonly; }) || fields[0];
            const idValue = idFieldDef ? getData()[idFieldDef.name] : null;

            if (window.SpiderDialog) {
                SpiderDialog.confirmDelete().then(function (confirmed) {
                    if (confirmed) onDelete(idValue);
                });
            } else {
                onDelete(idValue);
            }
        }

        function onKeyDown(e) {
            if (e.key === 'Escape') close();
        }

        /* ────────────────────────────────────────────
           Public API
           ──────────────────────────────────────────── */
        function open(newMode, data) {
            mode = newMode || 'create';
            rowData = data || null;

            /* Mode label */
            modeLabelEl.textContent = t(MODE_LABELS[mode] || '');

            /* Render form fields */
            renderFields();

            /* Populate & set disabled state */
            if (mode === 'create') {
                /* empty form, clear validation */
                if (window.SpiderValidation) SpiderValidation.clear(bodyEl);
            } else {
                populateFields(rowData);
            }

            if (mode === 'view') {
                setDisabledState(true);
            }

            /* Render footer buttons based on mode + permissions */
            renderFooter();

            /* Show panel */
            overlay.style.visibility = 'visible';
            requestAnimationFrame(function () {
                overlay.style.opacity = '1';
                panelEl.style.transform = 'translateX(0)';
            });

            document.addEventListener('keydown', onKeyDown);
        }

        function close() {
            overlay.style.opacity = '0';
            panelEl.style.transform = 'translateX(100%)';

            document.removeEventListener('keydown', onKeyDown);

            setTimeout(function () {
                overlay.style.visibility = 'hidden';
                /* Clear validation errors */
                if (window.SpiderValidation) SpiderValidation.clear(bodyEl);
            }, 200);
        }

        function setContent(html) {
            bodyEl.innerHTML = html;
        }

        /* ── Return public interface ── */
        return {
            open:       open,
            close:      close,
            getData:    getData,
            setContent: setContent,
            /** Direct access for advanced usage */
            el:         panelEl,
            overlay:    overlay,
            body:       bodyEl
        };
    }

    /* ================================================================
       Export
       ================================================================ */
    window.SpiderDetailPanel = {
        create: create
    };
})();
