/**
 * Property DB Management — Main Page (Template A: Standard CRUD)
 */
(function () {
    'use strict';

    var container = qs('#property-manage');
    if (!container) return;

    var RESOURCE = 'PROPERTY';
    var API = '/api/properties';
    var currentPage = 0;
    var currentSize = 20;
    var currentSort = null;
    var currentDir = null;

    // ── Search Panel ──
    var searchPanel = SpiderSearchPanel.create(qs('#property-search-panel', container), {
        fields: [
            {
                name: 'searchType', type: 'select', label: 'property.searchType',
                options: [
                    { value: 'groupId', label: 'property.searchByGroupId' },
                    { value: 'groupName', label: 'property.searchByGroupName' }
                ]
            },
            { name: 'keyword', type: 'text', label: 'common.search', placeholder: 'common.searchPlaceholder' }
        ],
        onSearch: function () { currentPage = 0; loadData(); },
        onReset: function () { currentPage = 0; loadData(); }
    });

    // ── Toolbar ──
    SpiderToolbar.create(qs('#property-toolbar', container), {
        left: [
            { id: 'refresh', label: 'property.refresh', icon: 'refresh' },
            { id: 'excel', label: 'common.excel', icon: 'download' },
            { id: 'exportProps', label: 'property.exportProperties', icon: 'download' },
            { id: 'exportYaml', label: 'property.exportYaml', icon: 'download' }
        ],
        right: [
            { id: 'newGroup', label: 'property.newGroup', icon: 'plus', permission: 'W' }
        ],
        resource: RESOURCE,
        onClick: handleToolbar
    });

    // ── Data Table ──
    var table = SpiderDataTable.create(qs('#property-table', container), {
        columns: [
            { header: 'property.groupId', field: 'groupId', width: 200, sortable: true },
            { header: 'property.groupName', field: 'groupName', flex: 1, sortable: true },
            { header: 'property.propertyCount', field: 'propertyCount', width: 100, align: 'center' },
            {
                header: 'property.detail', field: '_detail', width: 100, type: 'actions', align: 'center',
                renderer: function (val, row) {
                    return '<button class="spider-toolbar-btn" data-action="openDetail" data-group-id="' + escapeHtml(row.groupId) + '">'
                        + t('property.detail') + '</button>';
                }
            },
            {
                header: 'property.wasSetting', field: '_was', width: 120, type: 'actions', align: 'center',
                renderer: function (val, row) {
                    return '<button class="spider-toolbar-btn" data-action="openWasSetting" data-group-id="' + escapeHtml(row.groupId) + '">'
                        + t('property.wasSetting') + '</button>';
                }
            },
            {
                header: 'Reload', field: '_reload', width: 100, type: 'actions', align: 'center',
                renderer: function (val, row) {
                    return '<button class="spider-toolbar-btn" data-action="reloadWas" data-group-id="' + escapeHtml(row.groupId) + '" data-permission="W">'
                        + 'Reload</button>';
                }
            }
        ],
        onSort: function (field, direction) {
            currentSort = field === 'groupId' ? 'PROPERTY_GROUP_ID' : field === 'groupName' ? 'groupName' : null;
            currentDir = direction ? direction.toUpperCase() : null;
            loadData();
        },
        onRowClick: function (row) {
            if (row && row.groupId && window.PropertyDetailModal) {
                PropertyDetailModal.open(row.groupId);
            }
        }
    });

    // ── Pagination ──
    var pager = SpiderPagination.create(qs('#property-pagination', container), {
        pageSize: currentSize,
        pageSizes: [10, 20, 50, 100],
        onChange: function (page, size) {
            currentPage = page - 1;
            currentSize = size;
            loadData();
        }
    });

    // ── Event Delegation ──
    delegate(container, 'click', '[data-action="openDetail"]', function (e) {
        e.stopPropagation();
        var groupId = this.getAttribute('data-group-id');
        if (window.PropertyDetailModal) PropertyDetailModal.open(groupId);
    });

    delegate(container, 'click', '[data-action="openWasSetting"]', function (e) {
        e.stopPropagation();
        var groupId = this.getAttribute('data-group-id');
        if (window.PropertyDetailModal) PropertyDetailModal.open(groupId);
    });

    delegate(container, 'click', '[data-action="reloadWas"]', function (e) {
        e.stopPropagation();
        var groupId = this.getAttribute('data-group-id');
        if (window.PropertyReloadModal) {
            PropertyReloadModal.open(groupId);
        }
    });

    // ── Functions ──

    function loadData() {
        var params = searchPanel.getValues();
        params.page = currentPage;
        params.size = currentSize;
        if (currentSort) params.sortBy = currentSort;
        if (currentDir) params.sortDirection = currentDir;

        api.getJson(API + '/groups/page', params)
            .then(function (resp) {
                if (resp.success && resp.data) {
                    table.setData(resp.data.content || []);
                    pager.setTotal(resp.data.totalElements || 0);
                }
            })
            .catch(function (err) {
                SpiderToast.error(err.message);
            });
    }

    function handleToolbar(btnId) {
        if (btnId === 'refresh') {
            loadData();
        } else if (btnId === 'excel') {
            exportExcel();
        } else if (btnId === 'exportProps') {
            exportFile('properties');
        } else if (btnId === 'exportYaml') {
            exportFile('yaml');
        } else if (btnId === 'newGroup') {
            if (window.PropertyGroupModal) PropertyGroupModal.open();
        }
    }

    function exportExcel() {
        api.downloadBlob(API + '/excel', {})
            .then(function (blob) {
                var a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = 'property_' + new Date().toISOString().slice(0, 10).replace(/-/g, '') + '.xlsx';
                a.click();
                URL.revokeObjectURL(a.href);
                SpiderToast.success(t('excel.downloadSuccess'));
            })
            .catch(function (err) {
                SpiderToast.error(err.message || t('excel.downloadFail'));
            });
    }

    function exportFile(format) {
        api.request(API + '/export?format=' + format)
            .then(function (resp) { return resp.blob(); })
            .then(function (blob) {
                var ext = format === 'yaml' ? '.yaml' : '.properties';
                var a = document.createElement('a');
                a.href = URL.createObjectURL(blob);
                a.download = 'property-export' + ext;
                a.click();
                URL.revokeObjectURL(a.href);
            })
            .catch(function (err) {
                SpiderToast.error(err.message);
            });
    }

    function t(key) {
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    // ── Init ──
    loadData();

    // Apply permission visibility
    SpiderPermission.apply(container, RESOURCE);

    if (window.SpiderI18n && SpiderI18n.translate) {
        SpiderI18n.translate(container);
    }

    // Public API for other modules
    window.PropertyPage = {
        reload: loadData
    };
})();
