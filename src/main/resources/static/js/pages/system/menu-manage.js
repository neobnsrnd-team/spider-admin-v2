/**
 * Menu Management Page (Template B: Tree + Form)
 */
(function () {
    'use strict';

    const container = qs('#menu-manage');
    if (!container) return;

    const RESOURCE = 'MENU';
    const API_BASE = '/api/system/menu';

    let currentMode = null; // 'create' | 'edit' | null
    let currentMenuId = null;
    let allMenus = [];  // flat list for parent select

    // ── Toolbar: Tree actions ──
    const treeToolbar = SpiderToolbar.create(qs('#menu-tree-toolbar', container), {
        left: [
            { id: 'add', label: 'common.add', icon: 'plus', permission: 'W' },
            { id: 'delete', label: 'common.delete', icon: 'trash', permission: 'W', danger: true }
        ],
        right: [
            { id: 'expandAll', label: 'tree.expandAll', icon: 'refresh' },
            { id: 'collapseAll', label: 'tree.collapseAll', icon: 'refresh' }
        ],
        resource: RESOURCE,
        onClick: handleTreeToolbar
    });

    // ── Toolbar: Form actions ──
    const formToolbar = SpiderToolbar.create(qs('#menu-form-toolbar', container), {
        left: [],
        right: [
            { id: 'save', label: 'common.save', icon: 'save', permission: 'W' },
            { id: 'cancel', label: 'common.cancel', icon: 'refresh' }
        ],
        resource: RESOURCE,
        onClick: handleFormToolbar
    });

    // ── Tree ──
    const tree = SpiderTree.create(qs('#menu-tree', container), {
        data: [],
        labelField: 'menuName',
        idField: 'menuId',
        childrenField: 'children',
        draggable: SpiderPermission.canWrite(RESOURCE),
        onSelect: handleNodeSelect,
        onDrop: handleDrop,
        renderLabel: function (node) {
            const name = node.menuName || node.menuId;
            return node.useYn === 'N' ? '<span style="opacity:0.5">' + name + '</span>' : name;
        }
    });

    // ── Form reference ──
    const form = qs('#menu-detail-form', container);

    // Disable form initially
    setFormEnabled(false);

    // ── Init: Load tree data ──
    loadTree();

    // ── Functions ──

    function loadTree() {
        api.getJson(API_BASE + '/tree')
            .then(function (resp) {
                if (resp.success) {
                    allMenus = flattenTree(resp.data);
                    tree.setData(resp.data);
                    buildParentSelect();
                    tree.expandAll();
                    // Re-select current node if editing
                    if (currentMenuId) {
                        tree.selectNode(currentMenuId);
                    }
                }
            })
            .catch(function (err) {
                SpiderToast.error(err.message);
            });
    }

    function flattenTree(nodes) {
        const result = [];
        (function walk(list) {
            for (let i = 0; i < list.length; i++) {
                result.push(list[i]);
                if (list[i].children && list[i].children.length) {
                    walk(list[i].children);
                }
            }
        })(nodes);
        return result;
    }

    function buildParentSelect() {
        const select = qs('[name="priorMenuId"]', form);
        const currentVal = select.value;
        // Clear options except ROOT
        select.innerHTML = '<option value="ROOT">' + (SpiderI18n.t('menu.rootMenu') || '최상위') + '</option>';

        for (let i = 0; i < allMenus.length; i++) {
            const m = allMenus[i];
            // Only show menus that can be parents (categories or menus with no URL or any menu)
            if (m.menuId !== currentMenuId) {
                const opt = document.createElement('option');
                opt.value = m.menuId;
                opt.textContent = m.menuName;
                select.appendChild(opt);
            }
        }

        // Restore value if it still exists
        if (currentVal) {
            select.value = currentVal;
        }
    }

    function handleNodeSelect(node) {
        if (!node) return;
        currentMenuId = node.menuId;
        currentMode = 'edit';
        fillForm(node);
        setFormEnabled(true);
        // MenuId is not editable in edit mode
        qs('[name="menuId"]', form).disabled = true;

        if (!SpiderPermission.canWrite(RESOURCE)) {
            setFormEnabled(false);
        }
    }

    function handleTreeToolbar(btnId) {
        if (btnId === 'add') {
            startCreate();
        } else if (btnId === 'delete') {
            deleteSelected();
        } else if (btnId === 'expandAll') {
            tree.expandAll();
        } else if (btnId === 'collapseAll') {
            tree.collapseAll();
        }
    }

    function handleFormToolbar(btnId) {
        if (btnId === 'save') {
            saveMenu();
        } else if (btnId === 'cancel') {
            cancelEdit();
        }
    }

    function startCreate() {
        currentMode = 'create';
        currentMenuId = null;
        clearForm();
        setFormEnabled(true);
        buildParentSelect();

        // Pre-select parent from current tree selection
        const selected = tree.getSelected();
        if (selected) {
            qs('[name="priorMenuId"]', form).value = selected.menuId;
        }

        qs('[name="menuId"]', form).disabled = false;
        qs('[name="menuId"]', form).focus();
    }

    function cancelEdit() {
        currentMode = null;
        currentMenuId = null;
        clearForm();
        setFormEnabled(false);
        SpiderValidation.clear(form);
    }

    function fillForm(data) {
        SpiderValidation.clear(form);
        qs('[name="menuId"]', form).value = data.menuId || '';
        qs('[name="menuName"]', form).value = data.menuName || '';
        qs('[name="menuUrl"]', form).value = data.menuUrl || '';
        qs('[name="priorMenuId"]', form).value = data.priorMenuId || 'ROOT';
        qs('[name="menuImage"]', form).value = data.menuImage || '';
        qs('[name="sortOrder"]', form).value = data.sortOrder || 0;
        qs('[name="useYn"]', form).value = data.useYn || 'Y';
        qs('[name="displayYn"]', form).value = data.displayYn || 'Y';
    }

    function clearForm() {
        form.reset();
        qs('[name="priorMenuId"]', form).value = 'ROOT';
    }

    function getFormData() {
        return {
            menuId: qs('[name="menuId"]', form).value.trim(),
            menuName: qs('[name="menuName"]', form).value.trim(),
            menuUrl: qs('[name="menuUrl"]', form).value.trim(),
            priorMenuId: qs('[name="priorMenuId"]', form).value,
            menuImage: qs('[name="menuImage"]', form).value.trim(),
            sortOrder: parseInt(qs('[name="sortOrder"]', form).value, 10) || 0,
            useYn: qs('[name="useYn"]', form).value,
            displayYn: qs('[name="displayYn"]', form).value
        };
    }

    function setFormEnabled(enabled) {
        const fields = qsa('input, select, textarea', form);
        for (let i = 0; i < fields.length; i++) {
            fields[i].disabled = !enabled;
        }
    }

    function saveMenu() {
        const result = SpiderValidation.validate(form);
        if (!result.valid) return;

        const data = getFormData();

        SpiderDialog.confirm(SpiderI18n.t('dialog.confirmSave')).then(function (confirmed) {
            if (!confirmed) return;

            let promise;
            if (currentMode === 'create') {
                promise = api.postJson(API_BASE, data);
            } else {
                promise = api.request(API_BASE + '/' + data.menuId, {
                    method: 'PUT',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                }).then(function (r) { return r.json(); });
            }

            promise.then(function (resp) {
                if (resp.success) {
                    SpiderToast.success(SpiderI18n.t('toast.saveSuccess'));
                    currentMenuId = data.menuId;
                    currentMode = 'edit';
                    loadTree();
                } else {
                    const msg = (resp.error && resp.error.message) || SpiderI18n.t('toast.saveFail');
                    SpiderToast.error(msg);
                }
            }).catch(function (err) {
                SpiderToast.error(err.message || SpiderI18n.t('toast.saveFail'));
            });
        });
    }

    function deleteSelected() {
        if (!currentMenuId) {
            SpiderToast.warning(SpiderI18n.t('common.noData'));
            return;
        }

        SpiderDialog.confirmDelete(SpiderI18n.t('menu.deleteConfirm')).then(function (confirmed) {
            if (!confirmed) return;

            api.request(API_BASE + '/' + currentMenuId, { method: 'DELETE' })
                .then(function (r) { return r.json(); })
                .then(function (resp) {
                    if (resp.success) {
                        SpiderToast.success(SpiderI18n.t('toast.deleteSuccess'));
                        currentMenuId = null;
                        currentMode = null;
                        clearForm();
                        setFormEnabled(false);
                        loadTree();
                    } else {
                        const msg = (resp.error && resp.error.message) || SpiderI18n.t('toast.deleteFail');
                        SpiderToast.error(msg);
                    }
                })
                .catch(function (err) {
                    SpiderToast.error(err.message || SpiderI18n.t('toast.deleteFail'));
                });
        });
    }

    function handleDrop(dragNode, dropNode, position) {
        if (!dragNode || !dropNode) return;

        const data = {
            sortOrder: dropNode.sortOrder || 0,
            priorMenuId: position === 'inside' ? dropNode.menuId : (dropNode.priorMenuId || 'ROOT')
        };

        api.request(API_BASE + '/' + dragNode.menuId + '/sort', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        }).then(function (r) { return r.json(); })
          .then(function (resp) {
              if (resp.success) {
                  loadTree();
              } else {
                  SpiderToast.error(SpiderI18n.t('toast.saveFail'));
              }
          })
          .catch(function (err) {
              SpiderToast.error(err.message);
              loadTree(); // Reload to reset positions
          });
    }

    // Apply permission visibility
    SpiderPermission.apply(container, RESOURCE);

    // Apply i18n
    if (window.SpiderI18n && SpiderI18n.translate) {
        SpiderI18n.translate(container);
    }

})();
