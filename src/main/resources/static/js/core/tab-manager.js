/**
 * SpiderTabManager — 탭 생명주기 관리 모듈
 *
 * LRU 기반 최대 15탭 관리, 탭 열기/닫기/활성화, 콘텐츠 AJAX 로딩,
 * 우클릭 컨텍스트 메뉴(다른 탭 닫기, 모든 탭 닫기, 오른쪽 탭 닫기)를 처리한다.
 */
(function () {
    'use strict';

    // ── Constants ──
    const MAX_TABS = 15;

    // ── Internal state ──
    let tabs = [];            // { menuId, menuName, menuUrl, openedAt }
    let activeMenuId = null;
    let lruOrder = [];        // menuIds ordered by last access (most recent = last)

    // ── DOM references (set in init) ──
    let tabList = null;
    let contentArea = null;
    let contextMenu = null;
    let contextTargetMenuId = null;

    // ── Find a tab object by menuId ──
    function findTab(menuId) {
        for (let i = 0; i < tabs.length; i++) {
            if (tabs[i].menuId === menuId) return tabs[i];
        }
        return null;
    }

    // ── Find tab index ──
    function findTabIndex(menuId) {
        for (let i = 0; i < tabs.length; i++) {
            if (tabs[i].menuId === menuId) return i;
        }
        return -1;
    }

    // ── Update LRU: move menuId to the end (most recently used) ──
    function touchLru(menuId) {
        const idx = lruOrder.indexOf(menuId);
        if (idx !== -1) {
            lruOrder.splice(idx, 1);
        }
        lruOrder.push(menuId);
    }

    // ── Remove from LRU ──
    function removeLru(menuId) {
        const idx = lruOrder.indexOf(menuId);
        if (idx !== -1) {
            lruOrder.splice(idx, 1);
        }
    }

    // ── Evict the least recently used tab (that isn't active) ──
    function evictLru() {
        for (let i = 0; i < lruOrder.length; i++) {
            if (lruOrder[i] !== activeMenuId) {
                SpiderTabManager.close(lruOrder[i]);
                return;
            }
        }
        // Fallback: close the oldest anyway
        if (lruOrder.length > 0) {
            SpiderTabManager.close(lruOrder[0]);
        }
    }

    // ── Execute inline/external scripts found in loaded AJAX content ──
    function executeScripts(container) {
        const scripts = qsa('script', container);
        for (let i = 0; i < scripts.length; i++) {
            const oldScript = scripts[i];
            const newScript = document.createElement('script');
            if (oldScript.src) {
                newScript.src = oldScript.src;
            } else {
                newScript.textContent = oldScript.textContent;
            }
            // Copy attributes (e.g. type)
            const attrs = oldScript.attributes;
            for (let a = 0; a < attrs.length; a++) {
                if (attrs[a].name !== 'src') {
                    newScript.setAttribute(attrs[a].name, attrs[a].value);
                }
            }
            oldScript.parentNode.replaceChild(newScript, oldScript);
        }
    }

    // ── Load tab content via AJAX ──
    function loadContent(menuId, menuUrl, panel) {
        const contextPath = (window.SpiderConfig && SpiderConfig.contextPath) || '';
        const fullUrl = contextPath + menuUrl;

        api.getHtml(fullUrl)
            .then(function (html) {
                panel.innerHTML = html;

                // Post-processing
                SpiderI18n.translate(panel);
                SpiderPermission.apply(panel, menuId);
                executeScripts(panel);
            })
            .catch(function (err) {
                panel.innerHTML = '<div style="padding:2rem;color:var(--cds-text-error);">'
                    + '<p style="font-weight:600;margin-bottom:0.5rem;">' + escapeHtml(t('tab.loadError')) + '</p>'
                    + '<p style="font-size:var(--cds-label-01-size);color:var(--cds-text-secondary);">'
                    + escapeHtml(err.message || String(err)) + '</p></div>';
                console.error('[SpiderTabManager] Failed to load content for ' + menuId + ':', err);
            });
    }

    // ── i18n helper ──
    function t(key) {
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    // ── Show context menu at position ──
    function showContextMenu(x, y, menuId) {
        if (!contextMenu) return;
        contextTargetMenuId = menuId;
        contextMenu.style.left = x + 'px';
        contextMenu.style.top = y + 'px';
        contextMenu.classList.remove('hidden');
    }

    // ── Hide context menu ──
    function hideContextMenu() {
        if (!contextMenu) return;
        contextMenu.classList.add('hidden');
        contextTargetMenuId = null;
    }

    // ── Public API ──
    window.SpiderTabManager = {
        /**
         * Initialize tab manager: get DOM refs, set up event delegation.
         */
        init: function () {
            tabList = qs('#tab-list');
            contentArea = qs('#content-area');
            contextMenu = qs('#tab-context-menu');

            if (!tabList || !contentArea) return;

            // ── Tab click: activate ──
            delegate(tabList, 'click', '.tab-item', function (e) {
                // Ignore if the close button was clicked
                if (e.target.closest('.tab-item-close')) return;
                const menuId = this.getAttribute('data-menu-id');
                if (menuId) SpiderTabManager.activate(menuId);
            });

            // ── Tab close button click ──
            delegate(tabList, 'click', '.tab-item-close', function (e) {
                e.stopPropagation();
                const tabItem = this.closest('.tab-item');
                if (tabItem) {
                    const menuId = tabItem.getAttribute('data-menu-id');
                    if (menuId) SpiderTabManager.close(menuId);
                }
            });

            // ── Tab right-click: context menu ──
            delegate(tabList, 'contextmenu', '.tab-item', function (e) {
                e.preventDefault();
                const menuId = this.getAttribute('data-menu-id');
                if (menuId) {
                    showContextMenu(e.clientX, e.clientY, menuId);
                }
            });

            // ── Context menu actions ──
            if (contextMenu) {
                delegate(contextMenu, 'click', 'button[data-action]', function () {
                    const action = this.getAttribute('data-action');
                    const targetId = contextTargetMenuId;
                    hideContextMenu();

                    if (!targetId) return;

                    if (action === 'closeOthers') {
                        SpiderTabManager.closeOthers(targetId);
                    } else if (action === 'closeAll') {
                        SpiderTabManager.closeAll();
                    } else if (action === 'closeRight') {
                        SpiderTabManager.closeRight(targetId);
                    }
                });
            }

            // ── Close context menu on outside click ──
            document.addEventListener('click', function (e) {
                if (contextMenu && !contextMenu.contains(e.target)) {
                    hideContextMenu();
                }
            });

            // ── Listen for sidebar menu click events ──
            document.addEventListener('spider:menu:click', function (e) {
                const d = e.detail;
                if (d && d.menuId && d.menuUrl) {
                    SpiderTabManager.open(d.menuId, d.menuName, d.menuUrl);
                }
            });
        },

        /**
         * Open a new tab or activate an existing one.
         * @param {string} menuId
         * @param {string} menuName
         * @param {string} menuUrl
         */
        open: function (menuId, menuName, menuUrl) {
            // If tab already exists, just activate
            if (findTab(menuId)) {
                SpiderTabManager.activate(menuId);
                return;
            }

            // Evict LRU if at max capacity
            if (tabs.length >= MAX_TABS) {
                evictLru();
            }

            // Create tab item element
            const tabItem = document.createElement('div');
            tabItem.className = 'tab-item';
            tabItem.setAttribute('data-menu-id', menuId);
            tabItem.innerHTML = '<span class="tab-item-label">' + escapeHtml(menuName) + '</span>'
                + '<button class="tab-item-close" aria-label="' + escapeHtml(t('common.close')) + '">&times;</button>';
            tabList.appendChild(tabItem);

            // Create tab panel element
            const panel = document.createElement('div');
            panel.className = 'tab-panel';
            panel.setAttribute('data-menu-id', menuId);
            panel.innerHTML = '<div class="tab-panel-loading" style="display:flex;align-items:center;'
                + 'justify-content:center;height:100%;color:var(--cds-text-secondary);'
                + 'font-size:var(--cds-body-01-size);">'
                + escapeHtml(t('common.loading')) + '</div>';
            contentArea.appendChild(panel);

            // Add to internal state
            tabs.push({
                menuId: menuId,
                menuName: menuName,
                menuUrl: menuUrl,
                openedAt: Date.now()
            });

            // Load content
            loadContent(menuId, menuUrl, panel);

            // Activate the new tab
            SpiderTabManager.activate(menuId);
        },

        /**
         * Close a tab by menuId.
         * @param {string} menuId
         */
        close: function (menuId) {
            const tabIdx = findTabIndex(menuId);
            if (tabIdx === -1) return;

            // Remove tab item from DOM
            const tabItem = qs('.tab-item[data-menu-id="' + menuId + '"]', tabList);
            if (tabItem) tabItem.remove();

            // Remove tab panel from DOM
            const panel = qs('.tab-panel[data-menu-id="' + menuId + '"]', contentArea);
            if (panel) panel.remove();

            // Remove from internal state
            tabs.splice(tabIdx, 1);
            removeLru(menuId);

            // If the closed tab was active, activate the most recently used remaining tab
            if (activeMenuId === menuId) {
                activeMenuId = null;
                if (lruOrder.length > 0) {
                    SpiderTabManager.activate(lruOrder[lruOrder.length - 1]);
                } else {
                    // No tabs left: reset header title
                    const titleEl = qs('#header-page-title');
                    if (titleEl) titleEl.textContent = 'Spider Admin';
                }
            }

            // Dispatch closed event
            document.dispatchEvent(new CustomEvent('spider:tab:closed', {
                detail: { menuId: menuId }
            }));
        },

        /**
         * Activate (switch to) a tab.
         * @param {string} menuId
         */
        activate: function (menuId) {
            const tab = findTab(menuId);
            if (!tab) return;

            // Deactivate current tab item and panel
            if (activeMenuId) {
                const curTabItem = qs('.tab-item.active', tabList);
                if (curTabItem) curTabItem.classList.remove('active');

                const curPanel = qs('.tab-panel.active', contentArea);
                if (curPanel) curPanel.classList.remove('active');
            }

            // Activate new tab item
            const newTabItem = qs('.tab-item[data-menu-id="' + menuId + '"]', tabList);
            if (newTabItem) {
                newTabItem.classList.add('active');
                // Scroll tab into view
                newTabItem.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'smooth' });
            }

            // Activate new panel
            const newPanel = qs('.tab-panel[data-menu-id="' + menuId + '"]', contentArea);
            if (newPanel) newPanel.classList.add('active');

            // Update state
            activeMenuId = menuId;
            touchLru(menuId);

            // Update header page title
            const titleEl = qs('#header-page-title');
            if (titleEl) titleEl.textContent = tab.menuName;

            // Update sidebar active item
            if (window.SpiderSidebar) {
                SpiderSidebar.setActiveMenu(menuId);
            }

            // Dispatch activated event
            document.dispatchEvent(new CustomEvent('spider:tab:activated', {
                detail: { menuId: menuId, menuName: tab.menuName }
            }));
        },

        /**
         * Get the currently active tab's menuId.
         * @returns {string|null}
         */
        getActive: function () {
            return activeMenuId;
        },

        /**
         * Check if a tab with the given menuId is currently open.
         * @param {string} menuId
         * @returns {boolean}
         */
        isOpen: function (menuId) {
            return findTab(menuId) !== null;
        },

        /**
         * Close all tabs except the one with the given menuId.
         * @param {string} menuId
         */
        closeOthers: function (menuId) {
            const toClose = [];
            for (let i = 0; i < tabs.length; i++) {
                if (tabs[i].menuId !== menuId) {
                    toClose.push(tabs[i].menuId);
                }
            }
            for (let i = 0; i < toClose.length; i++) {
                SpiderTabManager.close(toClose[i]);
            }
        },

        /**
         * Close all open tabs.
         */
        closeAll: function () {
            const toClose = [];
            for (let i = 0; i < tabs.length; i++) {
                toClose.push(tabs[i].menuId);
            }
            for (let i = 0; i < toClose.length; i++) {
                SpiderTabManager.close(toClose[i]);
            }
        },

        /**
         * Close all tabs to the right of the given menuId (by DOM order).
         * @param {string} menuId
         */
        closeRight: function (menuId) {
            const tabItems = qsa('.tab-item', tabList);
            let found = false;
            const toClose = [];

            for (let i = 0; i < tabItems.length; i++) {
                const itemId = tabItems[i].getAttribute('data-menu-id');
                if (found) {
                    toClose.push(itemId);
                }
                if (itemId === menuId) {
                    found = true;
                }
            }

            for (let i = 0; i < toClose.length; i++) {
                SpiderTabManager.close(toClose[i]);
            }
        }
    };
})();
