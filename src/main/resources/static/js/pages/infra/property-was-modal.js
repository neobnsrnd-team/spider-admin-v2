/**
 * WAS Property Modal — Per-instance value management
 * Legacy: value_per_was.jsp
 *
 * Buttons:
 *   - 디폴트값 일괄적용: set all values to actual defaultValue
 *   - USE_DEFAULT 일괄적용: set all values to literal "USE_DEFAULT"
 *   - 미설정 일괄적용: set unset values to "NO DATA"
 */
(function () {
    'use strict';

    var API = '/api/properties';
    var modal = qs('#propertyWasModal');
    if (!modal) return;

    var currentGroupId = null;
    var currentPropertyId = null;
    var currentDefaultValue = null;
    var wasProperties = [];
    var originalWas = [];
    var tbody = qs('#wasPropertyBody', modal);

    delegate(modal, 'click', '[data-action]', function () {
        var action = this.getAttribute('data-action');
        if (action === 'closeWas') close();
        else if (action === 'saveWas') save();
        else if (action === 'applyDefaultValue') applyDefaultValue();
        else if (action === 'applyUseDefault') applyUseDefault();
        else if (action === 'applyNoData') applyNoData();
        else if (action === 'wasReload') wasReload();
    });

    window.PropertyWasModal = { open: open };

    function open(groupId, propertyId, validData, defaultValue) {
        currentGroupId = groupId;
        currentPropertyId = propertyId;
        currentDefaultValue = defaultValue || '';
        qs('#wasGroupId', modal).textContent = groupId || '';
        qs('#wasPropertyId', modal).textContent = propertyId || '';
        qs('#wasDefaultValue', modal).textContent = defaultValue || '';
        qs('#wasValidData', modal).textContent = validData || '';
        loadWasProperties();
        modal.style.display = 'flex';
        SpiderPermission.apply(modal, 'PROPERTY');
    }

    function close() {
        modal.style.display = 'none';
        wasProperties = [];
        originalWas = [];
        tbody.innerHTML = '';
    }

    function loadWasProperties() {
        api.getJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/properties/' + encodeURIComponent(currentPropertyId) + '/was')
            .then(function (resp) {
                if (!resp.success) return;
                wasProperties = (resp.data || []).map(function (w) { return Object.assign({}, w); });
                originalWas = JSON.parse(JSON.stringify(wasProperties));
                renderTable();
            })
            .catch(function (err) { SpiderToast.error(err.message); });
    }

    function renderTable() {
        tbody.innerHTML = '';
        wasProperties.forEach(function (w, i) {
            var tr = document.createElement('tr');
            tr.setAttribute('data-index', i);
            var canWrite = SpiderPermission.canWrite('PROPERTY');
            var disabled = canWrite ? '' : ' disabled';
            tr.innerHTML =
                '<td>' + escapeHtml(w.instanceId || '') + ' - ' + escapeHtml(w.instanceName || '') + '</td>' +
                '<td><input type="text" class="input-field input-sm was-editable" data-field="propertyValue" data-index="' + i + '" value="' + escapeHtml(w.propertyValue || '') + '"' + disabled + '></td>' +
                '<td><input type="text" class="input-field input-sm was-editable" data-field="propertyDesc" data-index="' + i + '" value="' + escapeHtml(w.propertyDesc || '') + '"' + disabled + '></td>';
            tbody.appendChild(tr);
        });
    }

    delegate(tbody, 'input', 'input', function () {
        var idx = parseInt(this.getAttribute('data-index'));
        var field = this.getAttribute('data-field');
        wasProperties[idx][field] = this.value;

        var orig = originalWas[idx];
        if (orig && (orig[field] || '') !== (this.value || '')) {
            this.classList.add('field-changed');
        } else {
            this.classList.remove('field-changed');
        }
    });

    // ── 디폴트값 일괄적용: set all PROPERTY_VALUE to actual defaultValue ──
    function applyDefaultValue() {
        if (!currentDefaultValue) {
            SpiderToast.warning(t('property.noDefaultValue'));
            return;
        }
        SpiderDialog.confirm(t('property.applyDefaultValueConfirm')).then(function (ok) {
            if (!ok) return;
            wasProperties.forEach(function (w, i) {
                w.propertyValue = currentDefaultValue;
                var input = qs('input[data-field="propertyValue"][data-index="' + i + '"]', tbody);
                if (input) { input.value = currentDefaultValue; input.classList.add('field-changed'); }
            });
        });
    }

    // ── USE_DEFAULT 일괄적용: set all values to literal "USE_DEFAULT" ──
    function applyUseDefault() {
        SpiderDialog.confirm(t('property.applyUseDefaultConfirm')).then(function (ok) {
            if (!ok) return;
            wasProperties.forEach(function (w, i) {
                w.propertyValue = 'USE_DEFAULT';
                var input = qs('input[data-field="propertyValue"][data-index="' + i + '"]', tbody);
                if (input) { input.value = 'USE_DEFAULT'; input.classList.add('field-changed'); }
            });
        });
    }

    // ── 미설정 일괄적용: set empty values to "NO DATA" ──
    function applyNoData() {
        SpiderDialog.confirm(t('property.applyNoDataConfirm')).then(function (ok) {
            if (!ok) return;
            wasProperties.forEach(function (w, i) {
                if (!w.propertyValue || w.propertyValue.trim() === '') {
                    w.propertyValue = 'NO DATA';
                    var input = qs('input[data-field="propertyValue"][data-index="' + i + '"]', tbody);
                    if (input) { input.value = 'NO DATA'; input.classList.add('field-changed'); }
                }
            });
        });
    }

    function wasReload() {
        if (window.PropertyReloadModal) {
            PropertyReloadModal.open(currentGroupId);
        }
    }

    function save() {
        var items = wasProperties.map(function (w) {
            return {
                instanceId: w.instanceId, groupId: currentGroupId,
                propertyId: currentPropertyId, value: w.propertyValue || '', desc: w.propertyDesc || ''
            };
        });

        SpiderDialog.confirm(t('dialog.confirmSave')).then(function (ok) {
            if (!ok) return;
            api.postJson(API + '/was/save', items)
                .then(function (resp) {
                    if (resp.success) {
                        SpiderToast.success(t('toast.saveSuccess'));
                        loadWasProperties();
                    } else {
                        SpiderToast.error(resp.error ? resp.error.message : t('toast.saveFail'));
                    }
                })
                .catch(function (err) { SpiderToast.error(err.message); });
        });
    }

    function t(key) { return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key; }
})();
