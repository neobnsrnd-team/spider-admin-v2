/**
 * Property Detail Modal — Inline Editing Grid
 */
(function () {
    'use strict';

    var API = '/api/properties';
    var RESOURCE = 'PROPERTY';
    var modal = qs('#propertyDetailModal');
    if (!modal) return;

    var currentGroupId = null;
    var properties = [];
    var originalProperties = [];
    var allGroups = [];

    // ── DOM refs ──
    var groupSelect = qs('#detailGroupSelect', modal);
    var tbody = qs('#detailPropertyBody', modal);
    var selectAll = qs('#detailSelectAll', modal);
    var searchType = qs('#detailSearchType', modal);
    var searchKeyword = qs('#detailSearchKeyword', modal);
    var backupReasonArea = qs('#detailBackupReasonArea', modal);
    var backupReasonInput = qs('#detailBackupReason', modal);

    // ── Event Delegation ──
    delegate(modal, 'click', '[data-action]', function (e) {
        var action = this.getAttribute('data-action');
        var handlers = {
            close: close,
            detailSearch: loadProperties,
            addRow: addRow,
            deleteRow: deleteSelectedRows,
            similarProperty: openSimilarModal,
            saveDetail: save,
            backupAll: showBackupReasonArea,
            executeBackup: executeBackup,
            cancelBackup: hideBackupReasonArea,
            restoreAll: openHistoryModal,
            wasBackup: openWasBackupModal,
            wasRestore: openWasHistoryModal,
            deleteGroup: deleteGroup,
            detailExcel: exportDetailExcel
        };
        if (handlers[action]) handlers[action]();
    });

    delegate(modal, 'click', '[data-action="openWas"]', function () {
        var idx = parseInt(this.getAttribute('data-index'));
        var prop = properties[idx];
        if (!prop) return;
        // Block WAS settings for unsaved new rows (legacy: "저장 후 사용 가능합니다")
        if (prop.crud === 'C') {
            SpiderToast.warning(t('property.saveBeforeWas'));
            return;
        }
        if (window.PropertyWasModal) {
            PropertyWasModal.open(currentGroupId, prop.propertyId, prop.validData, prop.defaultValue);
        }
    });

    if (groupSelect) {
        groupSelect.addEventListener('change', function () {
            currentGroupId = this.value;
            loadProperties();
        });
    }

    if (selectAll) {
        selectAll.addEventListener('change', function () {
            var checked = this.checked;
            qsa('input[data-row-check]', tbody).forEach(function (cb) { cb.checked = checked; });
        });
    }

    // ── Public API ──
    window.PropertyDetailModal = { open: open, addProperties: addPropertiesFromSimilar };

    function open(groupId) {
        currentGroupId = groupId;
        hideBackupReasonArea();
        loadGroups(groupId);
        loadProperties();
        modal.style.display = 'flex';
        SpiderPermission.apply(modal, RESOURCE);
    }

    function close() {
        modal.style.display = 'none';
        properties = [];
        originalProperties = [];
        tbody.innerHTML = '';
    }

    function loadGroups(selectedId) {
        api.getJson(API + '/groups').then(function (resp) {
            if (!resp.success) return;
            allGroups = resp.data || [];
            groupSelect.innerHTML = '';
            allGroups.forEach(function (g) {
                var opt = document.createElement('option');
                opt.value = g.groupId;
                opt.textContent = g.groupId + ' - ' + g.groupName;
                groupSelect.appendChild(opt);
            });
            if (selectedId) groupSelect.value = selectedId;
        });
    }

    function loadProperties() {
        var params = {};
        if (searchType && searchType.value) params.searchType = searchType.value;
        if (searchKeyword && searchKeyword.value) params.keyword = searchKeyword.value;

        api.getJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/properties', params)
            .then(function (resp) {
                if (!resp.success) return;
                properties = (resp.data || []).map(function (p) {
                    return Object.assign({}, p, { crud: '', _checked: false });
                });
                originalProperties = JSON.parse(JSON.stringify(properties));
                renderTable();
            })
            .catch(function (err) { SpiderToast.error(err.message); });
    }

    function renderTable() {
        tbody.innerHTML = '';
        if (selectAll) selectAll.checked = false;
        properties.forEach(function (p, i) {
            if (p === null) return;
            tbody.appendChild(createRow(p, i, p.crud === 'C'));
        });
        updateChangeCount();
    }

    function createRow(p, index, isNew) {
        var tr = document.createElement('tr');
        tr.setAttribute('data-index', index);
        applyCrudStyle(tr, p.crud);

        var disabled = p.crud === 'D' ? ' disabled' : '';
        var idDisabled = isNew ? '' : ' disabled';

        tr.innerHTML =
            '<td><input type="checkbox" data-row-check data-index="' + index + '"></td>' +
            '<td>' + crudBadge(p.crud) + '</td>' +
            '<td><input type="text" class="input-field input-sm" value="' + esc(p.propertyId) + '" data-field="propertyId" data-index="' + index + '"' + idDisabled + disabled + '></td>' +
            '<td><input type="text" class="input-field input-sm" value="' + esc(p.propertyName) + '" data-field="propertyName" data-index="' + index + '"' + disabled + '></td>' +
            '<td><input type="text" class="input-field input-sm" value="' + esc(p.propertyDesc) + '" data-field="propertyDesc" data-index="' + index + '"' + disabled + '></td>' +
            '<td><select class="input-field input-sm" data-field="dataType" data-index="' + index + '"' + disabled + '>' +
                '<option value="C"' + (p.dataType === 'C' ? ' selected' : '') + '>String</option>' +
                '<option value="N"' + (p.dataType === 'N' ? ' selected' : '') + '>Number</option>' +
                '<option value="B"' + (p.dataType === 'B' ? ' selected' : '') + '>Boolean</option>' +
            '</select></td>' +
            '<td><input type="text" class="input-field input-sm" value="' + esc(p.validData) + '" data-field="validData" data-index="' + index + '"' + disabled + '></td>' +
            '<td><input type="text" class="input-field input-sm" value="' + esc(p.defaultValue) + '" data-field="defaultValue" data-index="' + index + '"' + disabled + '></td>' +
            '<td><button class="spider-toolbar-btn" data-action="openWas" data-index="' + index + '">WAS</button></td>';
        return tr;
    }

    // ── Inline Edit Events ──
    delegate(tbody, 'input', 'input, select', function () {
        var idx = parseInt(this.getAttribute('data-index'));
        var field = this.getAttribute('data-field');
        if (idx < 0 || !properties[idx]) return;
        properties[idx][field] = this.value;

        if (properties[idx].crud !== 'C' && properties[idx].crud !== 'D') {
            var orig = originalProperties.find(function (o) { return o.propertyId === properties[idx].propertyId; });
            if (orig && hasChanges(orig, properties[idx])) {
                properties[idx].crud = 'U';
            } else if (orig) {
                properties[idx].crud = '';
            }
            var tr = this.closest('tr');
            applyCrudStyle(tr, properties[idx].crud);
            tr.querySelector('td:nth-child(2)').innerHTML = crudBadge(properties[idx].crud);
        }
        updateChangeCount();
    });

    delegate(tbody, 'change', 'select[data-field]', function () {
        this.dispatchEvent(new Event('input', { bubbles: true }));
    });

    function addRow() {
        var newProp = {
            propertyId: '', propertyName: '', propertyDesc: '',
            dataType: 'C', validData: '', defaultValue: '',
            groupId: currentGroupId, crud: 'C', _checked: false
        };
        properties.push(newProp);
        var idx = properties.length - 1;
        tbody.appendChild(createRow(newProp, idx, true));
        updateChangeCount();
    }

    function deleteSelectedRows() {
        qsa('input[data-row-check]:checked', tbody).forEach(function (cb) {
            var idx = parseInt(cb.getAttribute('data-index'));
            var p = properties[idx];
            if (!p) return;
            if (p.crud === 'C') {
                properties[idx] = null;
                cb.closest('tr').remove();
            } else {
                p.crud = 'D';
                var tr = cb.closest('tr');
                applyCrudStyle(tr, 'D');
                tr.querySelector('td:nth-child(2)').innerHTML = crudBadge('D');
                qsa('input, select', tr).forEach(function (el) { if (el.type !== 'checkbox') el.disabled = true; });
            }
        });
        if (selectAll) selectAll.checked = false;
        updateChangeCount();
    }

    function save() {
        var changed = properties.filter(function (p) {
            return p && p.crud && (p.crud === 'C' || p.crud === 'U' || p.crud === 'D');
        });
        if (changed.length === 0) {
            SpiderToast.warning(t('property.noChanges'));
            return;
        }

        // Validate required fields for C/U items
        var invalid = changed.some(function (p) {
            if (p.crud === 'D') return false;
            return !p.propertyId || !p.propertyName || !p.propertyDesc;
        });
        if (invalid) {
            SpiderToast.warning(t('property.requiredColumns'));
            return;
        }

        var items = changed.map(function (p) {
            return {
                propertyId: p.propertyId, propertyName: p.propertyName,
                propertyDesc: p.propertyDesc, dataType: p.dataType,
                validData: p.validData, defaultValue: p.defaultValue, crud: p.crud
            };
        });

        SpiderDialog.confirm(t('dialog.confirmSave')).then(function (ok) {
            if (!ok) return;
            api.postJson(API + '/save', { groupId: currentGroupId, items: items })
                .then(function (resp) {
                    if (resp.success) {
                        SpiderToast.success(t('toast.saveSuccess'));
                        loadProperties();
                        if (window.PropertyPage) PropertyPage.reload();
                    } else {
                        SpiderToast.error(resp.error ? resp.error.message : t('toast.saveFail'));
                    }
                })
                .catch(function (err) { SpiderToast.error(err.message); });
        });
    }

    // ── Backup with reason ──
    function showBackupReasonArea() {
        SpiderDialog.confirm(currentGroupId + t('property.backupConfirmMsg')).then(function (ok) {
            if (!ok) return;
            if (backupReasonArea) {
                backupReasonArea.style.display = 'block';
                if (backupReasonInput) backupReasonInput.value = '';
                if (backupReasonInput) backupReasonInput.focus();
            }
        });
    }

    function hideBackupReasonArea() {
        if (backupReasonArea) backupReasonArea.style.display = 'none';
    }

    function executeBackup() {
        var reason = backupReasonInput ? backupReasonInput.value.trim() : '';
        api.postJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/backup', { reason: reason })
            .then(function (resp) {
                if (resp.success) {
                    SpiderToast.success(t('toast.saveSuccess'));
                    hideBackupReasonArea();
                }
            })
            .catch(function (err) { SpiderToast.error(err.message); });
    }

    function openHistoryModal() {
        if (window.PropertyHistoryModal) PropertyHistoryModal.open(currentGroupId);
    }

    function openWasBackupModal() {
        if (window.PropertyWasHistoryModal) PropertyWasHistoryModal.openBackup(currentGroupId);
    }

    function openWasHistoryModal() {
        if (window.PropertyWasHistoryModal) PropertyWasHistoryModal.open(currentGroupId);
    }

    function openSimilarModal() {
        if (window.PropertySimilarModal) PropertySimilarModal.open(currentGroupId, 'detail');
    }

    function deleteGroup() {
        SpiderDialog.confirmDelete(t('property.groupDeleteConfirm')).then(function (ok) {
            if (!ok) return;
            api.request(API + '/groups/' + encodeURIComponent(currentGroupId), { method: 'DELETE' })
                .then(function (r) { return r.json(); })
                .then(function (resp) {
                    if (resp.success) {
                        SpiderToast.success(t('toast.deleteSuccess'));
                        close();
                        if (window.PropertyPage) PropertyPage.reload();
                    } else {
                        SpiderToast.error(resp.error ? resp.error.message : t('toast.deleteFail'));
                    }
                })
                .catch(function (err) { SpiderToast.error(err.message); });
        });
    }

    function exportDetailExcel() {
        api.downloadBlob(API + '/excel', { groupId: currentGroupId })
            .then(function (blob) {
                var a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = 'property_' + currentGroupId + '_' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '.xlsx';
                a.click();
                URL.revokeObjectURL(a.href);
                SpiderToast.success(t('excel.downloadSuccess'));
            })
            .catch(function (err) { SpiderToast.error(err.message); });
    }

    function addPropertiesFromSimilar(items) {
        items.forEach(function (item) {
            var exists = properties.some(function (p) { return p && p.propertyId === item.propertyId; });
            if (exists) return;
            var newProp = Object.assign({}, item, { groupId: currentGroupId, crud: 'C', _checked: false });
            properties.push(newProp);
            tbody.appendChild(createRow(newProp, properties.length - 1, true));
        });
        updateChangeCount();
    }

    // ── Helpers ──

    function hasChanges(a, b) {
        var fields = ['propertyName', 'propertyDesc', 'defaultValue', 'validData', 'dataType'];
        return fields.some(function (f) { return (a[f] || '') !== (b[f] || ''); });
    }

    function applyCrudStyle(tr, crud) {
        tr.classList.remove('row-created', 'row-updated', 'row-deleted');
        if (crud === 'C') tr.classList.add('row-created');
        else if (crud === 'U') tr.classList.add('row-updated');
        else if (crud === 'D') tr.classList.add('row-deleted');
    }

    function crudBadge(crud) {
        if (crud === 'C') return '<span class="badge badge-success">' + t('property.crudCreate') + '</span>';
        if (crud === 'U') return '<span class="badge badge-warning">' + t('property.crudUpdate') + '</span>';
        if (crud === 'D') return '<span class="badge badge-danger">' + t('property.crudDelete') + '</span>';
        return '<span class="badge badge-secondary">-</span>';
    }

    function updateChangeCount() {
        var count = properties.filter(function (p) { return p && p.crud && p.crud !== ''; }).length;
        var el = qs('#detailChangeCount', modal);
        if (el) el.textContent = count > 0 ? (count + ' changes') : '';
    }

    function esc(val) { return escapeHtml(val || ''); }

    function t(key) {
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }
})();
