/**
 * Property Group Create Modal
 */
(function () {
    'use strict';

    var API = '/api/properties';
    var modal = qs('#propertyGroupModal');
    if (!modal) return;

    var properties = [];
    var duplicateChecked = false;
    var checkedGroupId = null;
    var tbody = qs('#groupPropertyBody', modal);
    var selectAll = qs('#groupSelectAll', modal);

    delegate(modal, 'click', '[data-action]', function () {
        var action = this.getAttribute('data-action');
        var handlers = {
            closeGroup: close,
            duplicateCheck: checkDuplicate,
            addGroupRow: addRow,
            deleteGroupRow: deleteSelectedRows,
            openSimilar: openSimilar,
            saveGroup: save
        };
        if (handlers[action]) handlers[action]();
    });

    if (selectAll) {
        selectAll.addEventListener('change', function () {
            var checked = this.checked;
            qsa('input[data-row-check]', tbody).forEach(function (cb) { cb.checked = checked; });
        });
    }

    var groupIdInput = qs('#newGroupId', modal);
    if (groupIdInput) {
        groupIdInput.addEventListener('input', function () {
            duplicateChecked = false;
            checkedGroupId = null;
        });
    }

    window.PropertyGroupModal = { open: open, addProperties: addFromSimilar };

    function open() {
        properties = [];
        duplicateChecked = false;
        checkedGroupId = null;
        qs('#newGroupId', modal).value = '';
        qs('#newGroupName', modal).value = '';
        tbody.innerHTML = '';
        if (selectAll) selectAll.checked = false;
        modal.style.display = 'flex';
    }

    function close() {
        modal.style.display = 'none';
    }

    function checkDuplicate() {
        var gid = qs('#newGroupId', modal).value.trim();
        if (!gid) { SpiderToast.warning(t('validation.required').replace('{field}', t('property.groupId'))); return; }

        api.getJson(API + '/groups/' + encodeURIComponent(gid) + '/exists')
            .then(function (resp) {
                if (resp.success) {
                    if (resp.data) {
                        SpiderToast.warning(t('property.duplicateExists'));
                        duplicateChecked = false;
                    } else {
                        SpiderToast.success(t('property.duplicateAvailable'));
                        duplicateChecked = true;
                        checkedGroupId = gid;
                    }
                }
            })
            .catch(function (err) { SpiderToast.error(err.message); });
    }

    function addRow() {
        var prop = {
            propertyId: '', propertyName: '', propertyDesc: '',
            dataType: 'C', validData: '', defaultValue: ''
        };
        properties.push(prop);
        var idx = properties.length - 1;
        var tr = document.createElement('tr');
        tr.setAttribute('data-index', idx);
        tr.innerHTML =
            '<td><input type="checkbox" data-row-check data-index="' + idx + '"></td>' +
            '<td><input type="text" class="input-field input-sm" data-field="propertyId" data-index="' + idx + '"></td>' +
            '<td><input type="text" class="input-field input-sm" data-field="propertyName" data-index="' + idx + '"></td>' +
            '<td><input type="text" class="input-field input-sm" data-field="propertyDesc" data-index="' + idx + '"></td>' +
            '<td><select class="input-field input-sm" data-field="dataType" data-index="' + idx + '">' +
                '<option value="C">String</option><option value="N">Number</option><option value="B">Boolean</option></select></td>' +
            '<td><input type="text" class="input-field input-sm" data-field="validData" data-index="' + idx + '"></td>' +
            '<td><input type="text" class="input-field input-sm" data-field="defaultValue" data-index="' + idx + '"></td>';
        tbody.appendChild(tr);
    }

    delegate(tbody, 'input', 'input, select', function () {
        var idx = parseInt(this.getAttribute('data-index'));
        var field = this.getAttribute('data-field');
        if (properties[idx]) properties[idx][field] = this.value;
    });

    function deleteSelectedRows() {
        qsa('input[data-row-check]:checked', tbody).forEach(function (cb) {
            var idx = parseInt(cb.getAttribute('data-index'));
            properties[idx] = null;
            cb.closest('tr').remove();
        });
        if (selectAll) selectAll.checked = false;
    }

    function openSimilar() {
        if (window.PropertySimilarModal) PropertySimilarModal.open(null, 'group');
    }

    function addFromSimilar(items) {
        items.forEach(function (item) {
            var exists = properties.some(function (p) { return p && p.propertyId === item.propertyId; });
            if (exists) return;
            properties.push(item);
            var idx = properties.length - 1;
            var tr = document.createElement('tr');
            tr.setAttribute('data-index', idx);
            tr.innerHTML =
                '<td><input type="checkbox" data-row-check data-index="' + idx + '"></td>' +
                '<td><input type="text" class="input-field input-sm" data-field="propertyId" data-index="' + idx + '" value="' + escapeHtml(item.propertyId || '') + '"></td>' +
                '<td><input type="text" class="input-field input-sm" data-field="propertyName" data-index="' + idx + '" value="' + escapeHtml(item.propertyName || '') + '"></td>' +
                '<td><input type="text" class="input-field input-sm" data-field="propertyDesc" data-index="' + idx + '" value="' + escapeHtml(item.propertyDesc || '') + '"></td>' +
                '<td><select class="input-field input-sm" data-field="dataType" data-index="' + idx + '">' +
                    '<option value="C"' + (item.dataType === 'C' ? ' selected' : '') + '>String</option>' +
                    '<option value="N"' + (item.dataType === 'N' ? ' selected' : '') + '>Number</option>' +
                    '<option value="B"' + (item.dataType === 'B' ? ' selected' : '') + '>Boolean</option></select></td>' +
                '<td><input type="text" class="input-field input-sm" data-field="validData" data-index="' + idx + '" value="' + escapeHtml(item.validData || '') + '"></td>' +
                '<td><input type="text" class="input-field input-sm" data-field="defaultValue" data-index="' + idx + '" value="' + escapeHtml(item.defaultValue || '') + '"></td>';
            tbody.appendChild(tr);
        });
    }

    function save() {
        var gid = qs('#newGroupId', modal).value.trim();
        var gname = qs('#newGroupName', modal).value.trim();

        if (!gid || !gname) {
            SpiderToast.warning(t('common.required'));
            return;
        }
        if (!duplicateChecked || checkedGroupId !== gid) {
            SpiderToast.warning(t('property.duplicateCheck'));
            return;
        }

        var items = properties.filter(function (p) { return p !== null; }).map(function (p) {
            return {
                propertyId: p.propertyId, propertyName: p.propertyName,
                propertyDesc: p.propertyDesc, dataType: p.dataType || 'C',
                validData: p.validData, defaultValue: p.defaultValue
            };
        });

        if (items.length === 0) {
            SpiderToast.warning(t('common.noData'));
            return;
        }

        api.postJson(API + '/groups', { groupId: gid, groupName: gname, properties: items })
            .then(function (resp) {
                if (resp.success) {
                    SpiderToast.success(t('toast.saveSuccess'));
                    close();
                    if (window.PropertyPage) PropertyPage.reload();
                } else {
                    SpiderToast.error(resp.error ? resp.error.message : t('toast.saveFail'));
                }
            })
            .catch(function (err) { SpiderToast.error(err.message); });
    }

    function t(key) { return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key; }
})();
