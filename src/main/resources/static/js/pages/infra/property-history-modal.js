/**
 * Property History Modal — Backup/Restore with side-by-side comparison
 * Legacy: property_group_history.jsp
 */
(function () {
    'use strict';

    var API = '/api/properties';
    var modal = qs('#propertyHistoryModal');
    if (!modal) return;

    var currentGroupId = null;
    var versionSelect = qs('#historyVersionSelect', modal);
    var currentBody = qs('#historyCurrentBody', modal);
    var versionBody = qs('#historyVersionBody', modal);
    var groupIdDisplay = qs('#historyGroupId', modal);
    var backupReasonArea = qs('#historyBackupReasonArea', modal);
    var backupReasonDisplay = qs('#historyBackupReason', modal);

    delegate(modal, 'click', '[data-action]', function () {
        var action = this.getAttribute('data-action');
        if (action === 'closeHistory') close();
        else if (action === 'restoreVersion') restore();
    });

    if (versionSelect) {
        versionSelect.addEventListener('change', function () {
            if (this.value) loadHistoryVersion(parseInt(this.value));
            else {
                versionBody.innerHTML = '';
                hideBackupReason();
            }
        });
    }

    window.PropertyHistoryModal = { open: open };

    function open(groupId) {
        currentGroupId = groupId;
        currentBody.innerHTML = '';
        versionBody.innerHTML = '';
        versionSelect.innerHTML = '<option value="">-- 선택 --</option>';
        if (groupIdDisplay) groupIdDisplay.textContent = groupId;
        hideBackupReason();
        loadCurrent();
        loadVersions();
        modal.style.display = 'flex';
        SpiderPermission.apply(modal, 'PROPERTY');
    }

    function close() {
        modal.style.display = 'none';
    }

    function loadCurrent() {
        api.getJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/properties')
            .then(function (resp) {
                if (!resp.success) return;
                renderTable(currentBody, resp.data || []);
            });
    }

    function loadVersions() {
        api.getJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/history/versions')
            .then(function (resp) {
                if (!resp.success) return;
                var versions = resp.data || [];
                if (versions.length === 0) {
                    // No backup versions exist
                    var opt = document.createElement('option');
                    opt.value = '';
                    opt.textContent = t('property.noBackupVersions');
                    versionSelect.appendChild(opt);
                    return;
                }
                versions.forEach(function (v) {
                    var opt = document.createElement('option');
                    opt.value = v.version;
                    opt.textContent = 'v' + v.version + ' - ' + (v.reason || '') + ' (' + (v.lastUpdateUserId || '') + ', ' + (v.lastUpdateDtime || '') + ')';
                    versionSelect.appendChild(opt);
                });
            });
    }

    function loadHistoryVersion(version) {
        api.getJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/history/' + version)
            .then(function (resp) {
                if (!resp.success) return;
                var items = resp.data || [];
                renderTable(versionBody, items);
                // Show backup reason from version metadata
                showBackupReason(version);
                highlightDiff();
            });
    }

    function showBackupReason(version) {
        // Find reason from version select option text
        var opts = versionSelect.options;
        for (var i = 0; i < opts.length; i++) {
            if (opts[i].value === String(version)) {
                var text = opts[i].textContent;
                var match = text.match(/^v\d+ - (.+) \(/);
                if (match && match[1] && backupReasonDisplay) {
                    backupReasonDisplay.textContent = match[1];
                    if (backupReasonArea) backupReasonArea.style.display = 'block';
                }
                break;
            }
        }
    }

    function hideBackupReason() {
        if (backupReasonArea) backupReasonArea.style.display = 'none';
        if (backupReasonDisplay) backupReasonDisplay.textContent = '';
    }

    function renderTable(body, items) {
        body.innerHTML = '';
        items.forEach(function (p) {
            var tr = document.createElement('tr');
            tr.setAttribute('data-pid', p.propertyId);
            tr.innerHTML =
                '<td>' + escapeHtml(p.propertyId || '') + '</td>' +
                '<td>' + escapeHtml(p.propertyName || '') + '</td>' +
                '<td>' + escapeHtml(p.propertyDesc || '') + '</td>' +
                '<td>' + escapeHtml(p.defaultValue || '') + '</td>';
            body.appendChild(tr);
        });
    }

    function highlightDiff() {
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
                // Exists in history but not in current = was deleted since then
                tr.style.backgroundColor = 'var(--cds-support-error, #f8d7da)';
            } else if (currentRows[pid] !== tr.textContent) {
                // Changed
                tr.style.backgroundColor = 'var(--cds-support-warning, #fff3cd)';
                var currentTr = qs('tr[data-pid="' + pid + '"]', currentBody);
                if (currentTr) currentTr.style.backgroundColor = 'var(--cds-support-warning, #fff3cd)';
            }
            delete currentRows[pid];
        });

        // Remaining = added after backup (exists in current but not in history)
        Object.keys(currentRows).forEach(function (pid) {
            var tr = qs('tr[data-pid="' + pid + '"]', currentBody);
            if (tr) tr.style.backgroundColor = 'var(--cds-support-success, #d1e7dd)';
        });
    }

    function restore() {
        var version = versionSelect.value;
        if (!version) { SpiderToast.warning(t('property.selectVersion')); return; }

        SpiderDialog.confirm(t('property.restoreConfirm')).then(function (ok) {
            if (!ok) return;
            api.postJson(API + '/groups/' + encodeURIComponent(currentGroupId) + '/restore/' + version, {})
                .then(function (resp) {
                    if (resp.success) {
                        SpiderToast.success(t('toast.saveSuccess'));
                        close();
                        if (window.PropertyDetailModal) PropertyDetailModal.open(currentGroupId);
                        if (window.PropertyPage) PropertyPage.reload();
                    }
                })
                .catch(function (err) { SpiderToast.error(err.message); });
        });
    }

    function t(key) { return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key; }
})();
