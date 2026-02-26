/**
 * SpiderSidebar — 사이드바 메뉴 트리 모듈
 *
 * SpiderConfig.menuTree 데이터를 기반으로 사이드바 메뉴를 렌더링하고,
 * 카테고리 접기/펼치기, 메뉴 검색, 메뉴 클릭 이벤트를 처리한다.
 */
(function () {
    'use strict';

    // ── Constants ──
    const STORAGE_KEY_COLLAPSED = 'spider-sidebar-collapsed';
    const STORAGE_KEY_CATEGORIES = 'spider-sidebar-categories';
    const DEBOUNCE_MS = 200;

    // ── SVG Icon Map (viewBox="0 0 16 16", stroke, no fill) ──
    const ICON_MAP = {
        'icon-system': '<path d="M8 1.5a1.5 1.5 0 0 1 1.5 1.5v.34a5.5 5.5 0 0 1 1.29.54l.24-.24a1.5 1.5 0 0 1 2.12 2.12l-.24.24c.23.4.41.84.54 1.29h.34a1.5 1.5 0 0 1 0 3h-.34a5.5 5.5 0 0 1-.54 1.29l.24.24a1.5 1.5 0 0 1-2.12 2.12l-.24-.24c-.4.23-.84.41-1.29.54v.34a1.5 1.5 0 0 1-3 0v-.34a5.5 5.5 0 0 1-1.29-.54l-.24.24a1.5 1.5 0 1 1-2.12-2.12l.24-.24A5.5 5.5 0 0 1 3.16 9.5H2.82a1.5 1.5 0 0 1 0-3h.34c.13-.45.31-.89.54-1.29l-.24-.24a1.5 1.5 0 0 1 2.12-2.12l.24.24c.4-.23.84-.41 1.29-.54V3A1.5 1.5 0 0 1 8 1.5z"/><circle cx="8" cy="8" r="2"/>',
        'icon-infra': '<rect x="3" y="2" width="10" height="4" rx="0.5"/><rect x="3" y="7" width="10" height="4" rx="0.5"/><path d="M6 13h4"/><path d="M8 11v2"/><circle cx="5" cy="4" r="0.5"/><circle cx="5" cy="9" r="0.5"/>',
        'icon-integration': '<circle cx="4" cy="8" r="2"/><circle cx="12" cy="4" r="2"/><circle cx="12" cy="12" r="2"/><path d="M6 7.2l4-2.4"/><path d="M6 8.8l4 2.4"/>',
        'icon-transaction': '<path d="M4 2h8l-1.5 3H5.5L4 2z"/><rect x="4" y="5" width="8" height="9" rx="0.5"/><path d="M6.5 7.5h3"/><path d="M6.5 9.5h3"/><path d="M6.5 11.5h2"/>',
        'icon-batch': '<circle cx="8" cy="8" r="6"/><path d="M8 4.5V8l2.5 1.5"/>',
        'icon-error': '<path d="M8 1.5l6.5 12H1.5L8 1.5z"/><path d="M8 6.5v3"/><circle cx="8" cy="11.5" r="0.5"/>',
        'icon-monitor': '<rect x="2" y="2" width="12" height="8" rx="0.5"/><path d="M5 13h6"/><path d="M8 10v3"/><path d="M4 6h2"/><path d="M4 8h3"/><path d="M9 5v3"/><path d="M11 4v4"/>',
        'icon-audit': '<rect x="3" y="1.5" width="10" height="13" rx="0.5"/><path d="M5.5 4.5h5"/><path d="M5.5 7h5"/><path d="M5.5 9.5h3"/><path d="M5 12l1 1 2-2.5"/>',
        'icon-board': '<rect x="2" y="3" width="12" height="9" rx="1"/><path d="M5 6.5h6"/><path d="M5 9h4"/>'
    };

    const ICON_DEFAULT = '<circle cx="8" cy="8" r="2"/>';

    // ── DOM references (set in init) ──
    let sidebarEl = null;
    let menuTreeEl = null;
    let searchInput = null;
    let toggleBtn = null;
    let debounceTimer = null;

    // ── Build a category icon SVG string ──
    function buildIconSvg(menuImage) {
        const paths = ICON_MAP[menuImage] || ICON_DEFAULT;
        return '<svg class="menu-category-icon" viewBox="0 0 16 16" fill="none" '
            + 'stroke="currentColor" stroke-width="1.3" aria-hidden="true">'
            + paths + '</svg>';
    }

    // ── Chevron SVG for categories ──
    function buildChevronSvg() {
        return '<svg class="menu-category-chevron" viewBox="0 0 16 16" fill="none" '
            + 'stroke="currentColor" stroke-width="1.5" stroke-linecap="round" '
            + 'stroke-linejoin="round" aria-hidden="true">'
            + '<path d="M4 6l4 4 4-4"/></svg>';
    }

    // ── Render the full menu tree into the nav container ──
    function renderTree(menuTree) {
        if (!menuTreeEl) return;
        menuTreeEl.innerHTML = '';

        const collapsedCategories = loadCategoryState();

        for (let i = 0; i < menuTree.length; i++) {
            const cat = menuTree[i];
            const catEl = document.createElement('div');
            catEl.className = 'menu-category';
            catEl.setAttribute('data-menu-id', cat.menuId);

            // Apply saved collapsed state
            if (collapsedCategories[cat.menuId]) {
                catEl.classList.add('collapsed');
            }

            // Category header
            const headerEl = document.createElement('div');
            headerEl.className = 'menu-category-header';
            headerEl.innerHTML = buildIconSvg(cat.menuImage)
                + '<span>' + escapeHtml(cat.menuName) + '</span>'
                + buildChevronSvg();

            catEl.appendChild(headerEl);

            // Category items container
            const itemsEl = document.createElement('div');
            itemsEl.className = 'menu-category-items';

            const children = cat.children || [];
            for (let j = 0; j < children.length; j++) {
                const item = children[j];
                const itemEl = document.createElement('div');
                itemEl.className = 'menu-item';
                itemEl.setAttribute('data-menu-id', item.menuId);
                itemEl.setAttribute('data-menu-url', item.menuUrl || '');
                itemEl.setAttribute('data-menu-name', item.menuName);
                itemEl.textContent = item.menuName;
                itemsEl.appendChild(itemEl);
            }

            catEl.appendChild(itemsEl);
            menuTreeEl.appendChild(catEl);
        }
    }

    // ── Escape HTML to prevent XSS ──
    function escapeHtml(str) {
        const div = document.createElement('div');
        div.textContent = str;
        return div.innerHTML;
    }

    // ── Category collapse/expand state persistence ──
    function loadCategoryState() {
        try {
            return JSON.parse(localStorage.getItem(STORAGE_KEY_CATEGORIES)) || {};
        } catch (e) {
            return {};
        }
    }

    function saveCategoryState() {
        const state = {};
        const categories = qsa('.menu-category', menuTreeEl);
        for (let i = 0; i < categories.length; i++) {
            const cat = categories[i];
            const menuId = cat.getAttribute('data-menu-id');
            if (cat.classList.contains('collapsed')) {
                state[menuId] = true;
            }
        }
        localStorage.setItem(STORAGE_KEY_CATEGORIES, JSON.stringify(state));
    }

    // ── Toggle a single category ──
    function toggleCategory(categoryEl) {
        categoryEl.classList.toggle('collapsed');
        saveCategoryState();
    }

    // ── Menu item click handler ──
    function onMenuItemClick(itemEl) {
        const menuId = itemEl.getAttribute('data-menu-id');
        const menuUrl = itemEl.getAttribute('data-menu-url');
        const menuName = itemEl.getAttribute('data-menu-name');

        if (!menuUrl) return; // Skip if no URL

        document.dispatchEvent(new CustomEvent('spider:menu:click', {
            detail: {
                menuId: menuId,
                menuUrl: menuUrl,
                menuName: menuName
            }
        }));
    }

    // ── Search / filter ──
    function filterMenus(term) {
        const normalizedTerm = term.toLowerCase().trim();
        const categories = qsa('.menu-category', menuTreeEl);

        for (let i = 0; i < categories.length; i++) {
            const cat = categories[i];
            const items = qsa('.menu-item', cat);
            let hasVisibleChild = false;

            for (let j = 0; j < items.length; j++) {
                const item = items[j];
                const name = item.textContent.toLowerCase();

                if (!normalizedTerm || name.indexOf(normalizedTerm) !== -1) {
                    item.style.display = '';
                    hasVisibleChild = true;
                } else {
                    item.style.display = 'none';
                }
            }

            // Show/hide category based on whether any child matches
            cat.style.display = hasVisibleChild ? '' : 'none';

            // When searching, expand categories that have matching children
            if (normalizedTerm && hasVisibleChild) {
                cat.classList.remove('collapsed');
            }
        }

        // If search is cleared, restore original collapsed states
        if (!normalizedTerm) {
            const savedState = loadCategoryState();
            for (let i = 0; i < categories.length; i++) {
                const cat = categories[i];
                const menuId = cat.getAttribute('data-menu-id');
                if (savedState[menuId]) {
                    cat.classList.add('collapsed');
                } else {
                    cat.classList.remove('collapsed');
                }
            }
        }
    }

    // ── Sidebar collapse/expand ──
    function setSidebarCollapsed(collapsed) {
        if (!sidebarEl) return;

        if (collapsed) {
            sidebarEl.classList.add('sidebar-collapsed');
        } else {
            sidebarEl.classList.remove('sidebar-collapsed');
        }

        localStorage.setItem(STORAGE_KEY_COLLAPSED, collapsed ? 'true' : 'false');

        // Update toggle button icons
        updateToggleIcon(collapsed);
    }

    function updateToggleIcon(collapsed) {
        if (!toggleBtn) return;
        const collapseIcon = qs('.sidebar-toggle-collapse', toggleBtn);
        const expandIcon = qs('.sidebar-toggle-expand', toggleBtn);
        if (collapseIcon) collapseIcon.style.display = collapsed ? 'none' : '';
        if (expandIcon) expandIcon.style.display = collapsed ? '' : 'none';
    }

    // ── Public API ──
    window.SpiderSidebar = {
        /**
         * Initialize the sidebar: render menu tree, bind events, restore state.
         */
        init: function () {
            sidebarEl = qs('#spider-sidebar');
            menuTreeEl = qs('#sidebar-menu-tree');
            searchInput = qs('#sidebar-search');
            toggleBtn = qs('#sidebar-toggle-btn');

            if (!sidebarEl || !menuTreeEl) return;

            // Render menu tree
            const menuTree = (window.SpiderConfig && SpiderConfig.menuTree) || [];
            renderTree(menuTree);

            // Restore sidebar collapsed state
            const wasCollapsed = localStorage.getItem(STORAGE_KEY_COLLAPSED) === 'true';
            setSidebarCollapsed(wasCollapsed);

            // ── Event: Category header click (expand/collapse) ──
            delegate(menuTreeEl, 'click', '.menu-category-header', function () {
                const category = this.closest('.menu-category');
                if (category) {
                    toggleCategory(category);
                }
            });

            // ── Event: Menu item click ──
            delegate(menuTreeEl, 'click', '.menu-item', function () {
                onMenuItemClick(this);
            });

            // ── Event: Search input ──
            if (searchInput) {
                searchInput.addEventListener('input', function () {
                    if (debounceTimer) clearTimeout(debounceTimer);
                    debounceTimer = setTimeout(function () {
                        filterMenus(searchInput.value);
                    }, DEBOUNCE_MS);
                });
            }

            // ── Event: Toggle button ──
            if (toggleBtn) {
                toggleBtn.addEventListener('click', function () {
                    SpiderSidebar.toggle();
                });
            }
        },

        /**
         * Toggle sidebar collapsed/expanded state.
         */
        toggle: function () {
            const isNowCollapsed = !SpiderSidebar.isCollapsed();
            setSidebarCollapsed(isNowCollapsed);
        },

        /**
         * Check if sidebar is currently collapsed.
         * @returns {boolean}
         */
        isCollapsed: function () {
            return sidebarEl ? sidebarEl.classList.contains('sidebar-collapsed') : false;
        },

        /**
         * Highlight a menu item as active and ensure its parent category is expanded.
         * @param {string} menuId - The menuId to activate
         */
        setActiveMenu: function (menuId) {
            if (!menuTreeEl) return;

            // Remove existing active state
            const activeItems = qsa('.menu-item.active', menuTreeEl);
            for (let i = 0; i < activeItems.length; i++) {
                activeItems[i].classList.remove('active');
            }

            // Find and activate the target item
            const target = qs('.menu-item[data-menu-id="' + menuId + '"]', menuTreeEl);
            if (!target) return;

            target.classList.add('active');

            // Ensure parent category is expanded
            const parentCategory = target.closest('.menu-category');
            if (parentCategory && parentCategory.classList.contains('collapsed')) {
                parentCategory.classList.remove('collapsed');
                saveCategoryState();
            }

            // Scroll into view if needed
            target.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
        },

        /**
         * Re-fetch the menu tree from the server and re-render.
         */
        refresh: function () {
            api.getJson(((window.SpiderConfig && SpiderConfig.contextPath) || '') + '/api/menus/tree')
                .then(function (data) {
                    if (window.SpiderConfig) {
                        SpiderConfig.menuTree = data;
                    }
                    renderTree(data);
                })
                .catch(function (err) {
                    console.error('[SpiderSidebar] Failed to refresh menu tree:', err);
                });
        }
    };
})();
