/**
 * Similar Property Modal — Copy properties from other groups
 */
(function () {
    'use strict';

    var API = '/api/properties';
    var modal = qs('#propertySimilarModal');
    if (!modal) return;

    var callerContext = null; // 'detail' or 'group'
    var excludeGroupId = null;
    var foundProperties = [];
    var tbody = qs('#similarPropertyBody', modal);
    var selectAll = qs('#similarSelectAll', modal);
    var groupSelect = qs('#similarGroupSelect', modal);

    delegate(modal, 'click', '[data-action]', function () {
        var action = this.getAttribute('data-action');
        if (action === 'closeSimilar') close();
        else if (action === 'searchSimilar') search();
        else if (action === 'copySimilar') copySelected();
        else if (action === 'copyAllSimilar') copyAll();
    });

    if (selectAll) {
        selectAll.addEventListener('change', function () {
            qsa('input[data-row-check]', tbody).forEach(function (cb) { cb.checked = this.checked; }.bind(this));
        });
    }

    window.PropertySimilarModal = { open: open };

    function open(groupId, context) {
        excludeGroupId = groupId;
        callerContext = context;
        foundProperties = [];
        tbody.innerHTML = '';
        if (selectAll) selectAll.checked = false;
        qs('#similarKeyword', modal).value = '';
        loadGroups();
        modal.style.display = 'flex';
    }

    function close() {
        modal.style.display = 'none';
    }

    function loadGroups() {
        api.getJson(API + '/groups').then(function (resp) {
            if (!resp.success) return;
            groupSelect.innerHTML = '<option value="">-- 선택 --</option>';
            (resp.data || []).forEach(function (g) {
                if (g.groupId === excludeGroupId) return;
                var opt = document.createElement('option');
                opt.value = g.groupId;
                opt.textContent = g.groupId + ' - ' + g.groupName;
                groupSelect.appendChild(opt);
            });
        });
    }

    function search() {
        var gid = groupSelect.value;
        if (!gid) { SpiderToast.warning('그룹을 선택하세요'); return; }
        var keyword = qs('#similarKeyword', modal).value.trim();

        api.getJson(API + '/groups/' + encodeURIComponent(gid) + '/properties', {
            searchType: keyword ? 'propertyName' : '', keyword: keyword
        }).then(function (resp) {
            if (!resp.success) return;
            foundProperties = resp.data || [];
            renderTable();
        }).catch(function (err) { SpiderToast.error(err.message); });
    }

    function renderTable() {
        tbody.innerHTML = '';
        if (selectAll) selectAll.checked = false;
        foundProperties.forEach(function (p, i) {
            var tr = document.createElement('tr');
            tr.innerHTML =
                '<td><input type="checkbox" data-row-check data-index="' + i + '"></td>' +
                '<td>' + escapeHtml(p.propertyId || '') + '</td>' +
                '<td>' + escapeHtml(p.propertyName || '') + '</td>' +
                '<td>' + escapeHtml(p.propertyDesc || '') + '</td>' +
                '<td>' + escapeHtml(p.defaultValue || '') + '</td>' +
                '<td>' + escapeHtml(p.validData || '') + '</td>' +
                '<td>' + escapeHtml(dataTypeName(p.dataType)) + '</td>';
            tbody.appendChild(tr);
        });
    }

    function copySelected() {
        var selected = [];
        qsa('input[data-row-check]:checked', tbody).forEach(function (cb) {
            var idx = parseInt(cb.getAttribute('data-index'));
            if (foundProperties[idx]) selected.push(foundProperties[idx]);
        });
        if (selected.length === 0) {
            SpiderToast.warning(t('common.noData'));
            return;
        }

        if (callerContext === 'detail' && window.PropertyDetailModal) {
            PropertyDetailModal.addProperties(selected);
        } else if (callerContext === 'group' && window.PropertyGroupModal) {
            PropertyGroupModal.addProperties(selected);
        }
        SpiderToast.success(selected.length + ' items copied');
        close();
    }

    function copyAll() {
        if (foundProperties.length === 0) {
            SpiderToast.warning(t('common.noData'));
            return;
        }

        var allItems = foundProperties.slice();
        if (callerContext === 'detail' && window.PropertyDetailModal) {
            PropertyDetailModal.addProperties(allItems);
        } else if (callerContext === 'group' && window.PropertyGroupModal) {
            PropertyGroupModal.addProperties(allItems);
        }
        SpiderToast.success(allItems.length + ' items copied');
        close();
    }

    function dataTypeName(type) {
        if (type === 'C') return 'String';
        if (type === 'N') return 'Number';
        if (type === 'B') return 'Boolean';
        return type || '';
    }

    function t(key) { return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key; }
})();
