/**
 * WAS Property History Modal + WAS Backup Instance Selection
 * Legacy: was_property_group_history.jsp + select_was_instance.jsp + textarea_for_backup_reason.jsp
 */
(function () {
    'use strict';

    var API = '/api/properties';
    var RESOURCE = 'PROPERTY';

    // ── WAS History Modal ──
    var historyModal = qs('#propertyWasHistoryModal');
    var backupModal = qs('#propertyWasBackupModal');
    if (!historyModal && !backupModal) return;

    var currentGroupId = null;
    var cachedInstances = [];

    window.PropertyWasHistoryModal = { open: open, openBackup: openBackup };

    // ── WAS History ──

    function open(groupId) {
        currentGroupId = groupId;
        var instanceSelect = qs('#wasHistoryInstanceSelect', historyModal);
        var versionSelect = qs('#wasHistoryVersionSelect', historyModal);
        qs('#wasHistoryCurrentBody', historyModal).innerHTML = '';
        qs('#wasHistoryVersionBody', historyModal).innerHTML = '';
        versionSelect.innerHTML = '<option value="">-- 선택 --</option>';

        loadInstances(function () {
            instanceSelect.innerHTML = '';
            cachedInstances.forEach(function (inst) {
                var opt = document.createElement('option');
                opt.value = inst.instanceId;
                opt.textContent = inst.instanceId + ' - ' + inst.instanceName;
                instanceSelect.appendChild(opt);
            });
            if (cachedInstances.length > 0) loadWasVersions(cachedInstances[0].instanceId);
        });

        instanceSelect.onchange = function () {
            versionSelect.innerHTML = '<option value="">-- 선택 --</option>';
            qs('#wasHistoryCurrentBody', historyModal).innerHTML = '';
            qs('#wasHistoryVersionBody', historyModal).innerHTML = '';
            if (this.value) loadWasVersions(this.value);
        };

        versionSelect.onchange = function () {
            if (this.value) loadWasHistoryVersion(instanceSelect.value, parseInt(this.value));
            else qs('#wasHistoryVersionBody', historyModal).innerHTML = '';
        };

        historyModal.style.display = 'flex';
        SpiderPermission.apply(historyModal, RESOURCE);
    }

    delegate(historyModal, 'click', '[data-action]', function () {
        var action = this.getAttribute('data-action');
        if (action === 'closeWasHistory') historyModal.style.display = 'none';
        else if (action === 'restoreWasVersion') restoreWas();
    });

    function loadInstances(cb) {
        if (cachedInstances.length > 0) { cb(); return; }
        api.getJson(API + '/was/instances').then(function (resp) {
            if (resp.success) cachedInstances = resp.data || [];
            cb();
        });
    }

    function loadWasVersions(instanceId) {
        var versionSelect = qs('#wasHistoryVersionSelect', historyModal);
        api.getJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/was/' + encodeURIComponent(instanceId) + '/history/versions')
            .then(function (resp) {
                if (!resp.success) return;
                var versions = resp.data || [];
                if (versions.length === 0) {
                    SpiderToast.info(t('property.noWasBackupVersions'));
                    return;
                }
                versions.forEach(function (v) {
                    var opt = document.createElement('option');
                    opt.value = v.version;
                    opt.textContent = 'v' + v.version + ' - ' + (v.reason || '') + ' (' + (v.lastUpdateDtime || '') + ')';
                    versionSelect.appendChild(opt);
                });
            });

        // Load current WAS values
        api.getJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/was/' + encodeURIComponent(instanceId) + '/current')
            .then(function (resp) {
                if (!resp.success) return;
                renderWasTable(qs('#wasHistoryCurrentBody', historyModal), resp.data || []);
            });
    }

    function loadWasHistoryVersion(instanceId, version) {
        api.getJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/was/' + encodeURIComponent(instanceId) + '/history/' + version)
            .then(function (resp) {
                if (!resp.success) return;
                renderWasHistoryTable(qs('#wasHistoryVersionBody', historyModal), resp.data || []);
                highlightWasDiff();
            });
    }

    function renderWasTable(body, items) {
        body.innerHTML = '';
        items.forEach(function (w) {
            var tr = document.createElement('tr');
            tr.setAttribute('data-pid', w.propertyId || w.instanceId);
            tr.innerHTML =
                '<td>' + escapeHtml(w.propertyId || w.instanceId || '') + '</td>' +
                '<td>' + escapeHtml(w.propertyValue || '') + '</td>' +
                '<td>' + escapeHtml(w.propertyDesc || '') + '</td>';
            body.appendChild(tr);
        });
    }

    function renderWasHistoryTable(body, items) {
        body.innerHTML = '';
        items.forEach(function (w) {
            var tr = document.createElement('tr');
            tr.setAttribute('data-pid', w.propertyId);
            tr.innerHTML =
                '<td>' + escapeHtml(w.propertyId || '') + '</td>' +
                '<td>' + escapeHtml(w.propertyValue || '') + '</td>' +
                '<td>' + escapeHtml(w.propertyDesc || '') + '</td>';
            body.appendChild(tr);
        });
    }

    function highlightWasDiff() {
        var currentBody = qs('#wasHistoryCurrentBody', historyModal);
        var versionBody = qs('#wasHistoryVersionBody', historyModal);

        // Clear previous highlights
        qsa('tr[data-pid]', currentBody).forEach(function (tr) { tr.style.backgroundColor = ''; });
        qsa('tr[data-pid]', versionBody).forEach(function (tr) { tr.style.backgroundColor = ''; });

        var currentRows = {};
        qsa('tr[data-pid]', currentBody).forEach(function (tr) {
            var pid = tr.getAttribute('data-pid');
            currentRows[pid] = tr.textContent;
        });

        qsa('tr[data-pid]', versionBody).forEach(function (tr) {
            var pid = tr.getAttribute('data-pid');
            if (!(pid in currentRows)) {
                tr.style.backgroundColor = 'var(--cds-support-error, #f8d7da)';
            } else if (currentRows[pid] !== tr.textContent) {
                tr.style.backgroundColor = 'var(--cds-support-warning, #fff3cd)';
                var currentTr = qs('tr[data-pid="' + pid + '"]', currentBody);
                if (currentTr) currentTr.style.backgroundColor = 'var(--cds-support-warning, #fff3cd)';
            }
            delete currentRows[pid];
        });

        Object.keys(currentRows).forEach(function (pid) {
            var tr = qs('tr[data-pid="' + pid + '"]', currentBody);
            if (tr) tr.style.backgroundColor = 'var(--cds-support-success, #d1e7dd)';
        });
    }

    function restoreWas() {
        var instanceId = qs('#wasHistoryInstanceSelect', historyModal).value;
        var version = qs('#wasHistoryVersionSelect', historyModal).value;
        if (!instanceId || !version) return;

        SpiderDialog.confirm(t('property.restoreConfirm')).then(function (ok) {
            if (!ok) return;
            api.postJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/was/' + encodeURIComponent(instanceId) + '/restore/' + version, {})
                .then(function (resp) {
                    if (resp.success) {
                        SpiderToast.success(t('toast.saveSuccess'));
                        historyModal.style.display = 'none';
                    }
                })
                .catch(function (err) { SpiderToast.error(err.message); });
        });
    }

    // ── WAS Backup Modal ──

    function openBackup(groupId) {
        currentGroupId = groupId;
        var list = qs('#wasBackupInstanceList', backupModal);
        var reason = qs('#wasBackupReason', backupModal);
        list.innerHTML = '';
        reason.value = '';

        loadInstances(function () {
            cachedInstances.forEach(function (inst) {
                var label = document.createElement('label');
                label.className = 'flex items-center gap-2 text-sm';
                label.innerHTML = '<input type="checkbox" value="' + inst.instanceId + '" checked> ' +
                    escapeHtml(inst.instanceId) + ' - ' + escapeHtml(inst.instanceName);
                list.appendChild(label);
            });
        });

        backupModal.style.display = 'flex';
    }

    delegate(backupModal, 'click', '[data-action]', function () {
        var action = this.getAttribute('data-action');
        if (action === 'closeWasBackup') backupModal.style.display = 'none';
        else if (action === 'executeWasBackup') executeBackup();
    });

    function executeBackup() {
        var instanceIds = [];
        qsa('input[type="checkbox"]:checked', qs('#wasBackupInstanceList', backupModal)).forEach(function (cb) {
            instanceIds.push(cb.value);
        });
        var reason = qs('#wasBackupReason', backupModal).value.trim();

        if (instanceIds.length === 0) { SpiderToast.warning(t('property.selectInstance')); return; }

        SpiderDialog.confirm(currentGroupId + t('property.wasBackupConfirmMsg') + instanceIds.join(', ')).then(function (ok) {
            if (!ok) return;
            api.postJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/was/backup', {
                instanceIds: instanceIds, reason: reason
            }).then(function (resp) {
                if (resp.success) {
                    SpiderToast.success(t('toast.saveSuccess'));
                    backupModal.style.display = 'none';
                }
            }).catch(function (err) { SpiderToast.error(err.message); });
        });
    }

    function t(key) { return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key; }
})();
