/**
 * WAS Reload Modal — Select instances & execute reload
 * Legacy: was_reload.jsp ($compo_operationReload)
 */
(function () {
    'use strict';

    var API = '/api/properties';
    var RESOURCE = 'PROPERTY';
    var modal = qs('#propertyReloadModal');
    if (!modal) return;

    var currentGroupId = null;
    var instances = [];
    var tbody = qs('#reloadInstanceBody', modal);
    var selectAllCb = qs('#reloadSelectAll', modal);
    var resultArea = qs('#reloadResultArea', modal);
    var resultList = qs('#reloadResultList', modal);

    // ── Event Delegation ──
    delegate(modal, 'click', '[data-action]', function () {
        var action = this.getAttribute('data-action');
        if (action === 'closeReload') close();
        else if (action === 'executeReload') executeReload();
    });

    if (selectAllCb) {
        selectAllCb.addEventListener('change', function () {
            var checked = this.checked;
            qsa('input[data-reload-check]', tbody).forEach(function (cb) { cb.checked = checked; });
        });
    }

    // ── Public API ──
    window.PropertyReloadModal = { open: open };

    function open(groupId) {
        currentGroupId = groupId;
        qs('#reloadGroupId', modal).textContent = groupId || '';
        if (resultArea) { resultArea.style.display = 'none'; resultList.innerHTML = ''; }
        if (selectAllCb) selectAllCb.checked = true;
        loadInstances();
        modal.style.display = 'flex';
        SpiderPermission.apply(modal, RESOURCE);
    }

    function close() {
        modal.style.display = 'none';
        instances = [];
        tbody.innerHTML = '';
    }

    function loadInstances() {
        api.getJson(API + '/was/instances')
            .then(function (resp) {
                if (!resp.success) return;
                instances = resp.data || [];
                renderTable();
            })
            .catch(function (err) { SpiderToast.error(err.message); });
    }

    function renderTable() {
        tbody.innerHTML = '';
        instances.forEach(function (inst, i) {
            var tr = document.createElement('tr');
            tr.setAttribute('data-index', i);
            tr.setAttribute('data-instance-id', inst.instanceId);
            var ipPort = (inst.ip || '') + (inst.port ? ':' + inst.port : '');
            tr.innerHTML =
                '<td><input type="checkbox" data-reload-check data-index="' + i + '" checked></td>' +
                '<td>' + escapeHtml(inst.instanceId || '') + ' - ' + escapeHtml(inst.instanceName || '') + '</td>' +
                '<td>' + escapeHtml(ipPort) + '</td>' +
                '<td class="reload-status" data-index="' + i + '"><span class="reload-pending">-</span></td>';
            tbody.appendChild(tr);
        });
    }

    function executeReload() {
        var selectedIds = [];
        qsa('input[data-reload-check]:checked', tbody).forEach(function (cb) {
            var idx = parseInt(cb.getAttribute('data-index'));
            if (instances[idx]) selectedIds.push(instances[idx].instanceId);
        });

        if (selectedIds.length === 0) {
            SpiderToast.warning(t('property.selectInstance'));
            return;
        }

        SpiderDialog.confirm(t('property.reloadConfirm')).then(function (ok) {
            if (!ok) return;

            // Show result area
            if (resultArea) { resultArea.style.display = 'block'; resultList.innerHTML = ''; }

            // Reset all status cells to "loading"
            selectedIds.forEach(function (id) {
                var statusCell = qs('tr[data-instance-id="' + id + '"] .reload-status', tbody);
                if (statusCell) statusCell.innerHTML = '<span class="reload-pending">' + t('common.loading') + '</span>';
            });

            // Execute reload for each selected instance
            var promises = selectedIds.map(function (instanceId) {
                return api.postJson('/api/reload', {
                    groupId: currentGroupId,
                    instanceId: instanceId
                }).then(function (resp) {
                    var data = resp.data || {};
                    var ok = resp.success && data.success !== false;
                    var msg = ok ? t('property.reloadSuccess') : (data.message || (resp.error ? resp.error.message : t('property.reloadFail')));
                    updateStatus(instanceId, ok, msg);
                    return { instanceId: instanceId, success: ok };
                }).catch(function (err) {
                    updateStatus(instanceId, false, err.message || t('property.reloadFail'));
                    return { instanceId: instanceId, success: false };
                });
            });

            Promise.all(promises).then(function (results) {
                var successCount = results.filter(function (r) { return r.success; }).length;
                var failCount = results.length - successCount;
                var summary = t('property.reloadComplete')
                    .replace('{success}', successCount)
                    .replace('{fail}', failCount);
                resultList.innerHTML = '<div class="mt-2 font-medium">' + escapeHtml(summary) + '</div>';

                if (failCount === 0) {
                    SpiderToast.success(t('property.reloadAllSuccess'));
                } else {
                    SpiderToast.warning(summary);
                }
            });
        });
    }

    function updateStatus(instanceId, success, message) {
        var statusCell = qs('tr[data-instance-id="' + instanceId + '"] .reload-status', tbody);
        if (statusCell) {
            var cls = success ? 'reload-success' : 'reload-fail';
            var icon = success ? 'OK' : 'FAIL';
            statusCell.innerHTML = '<span class="' + cls + '">' + icon + '</span>';
        }
        // Append to result list
        if (resultList) {
            var div = document.createElement('div');
            div.className = success ? 'reload-success' : 'reload-fail';
            div.textContent = instanceId + ': ' + message;
            resultList.appendChild(div);
        }
    }

    function t(key) { return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key; }
})();
