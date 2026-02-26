/**
 * SpiderApp — Shell 초기화 모듈
 *
 * 모든 Core 모듈(Theme, I18n, Sidebar, TabManager)을 초기화하고,
 * 헤더 버튼 이벤트를 바인딩하며, initialTab이 설정되어 있으면 자동으로 탭을 연다.
 * 이 스크립트는 다른 모든 모듈 뒤에 로드되어야 한다.
 */
(function () {
    'use strict';

    // ── Theme icon update helper ──
    function updateThemeIcon() {
        const themeBtn = qs('#header-theme-toggle');
        if (!themeBtn) return;

        const theme = SpiderTheme.get();

        // Light mode: show moon icon (switch-to-dark hint)
        // Dark mode: show sun icon (switch-to-light hint)
        const sunIcon = qs('.icon-sun', themeBtn);
        const moonIcon = qs('.icon-moon', themeBtn);

        if (sunIcon) sunIcon.style.display = theme === 'dark' ? '' : 'none';
        if (moonIcon) moonIcon.style.display = theme === 'dark' ? 'none' : '';
    }

    // ── Language button text update helper ──
    function updateLangButton() {
        const langBtn = qs('#header-lang-toggle');
        if (!langBtn) return;
        langBtn.textContent = SpiderI18n.current().toUpperCase();
    }

    // ── Find a menu node in the tree by menuId (recursive) ──
    function findMenuInTree(tree, menuId) {
        for (let i = 0; i < tree.length; i++) {
            const node = tree[i];
            if (node.menuId === menuId) return node;
            if (node.children && node.children.length > 0) {
                const found = findMenuInTree(node.children, menuId);
                if (found) return found;
            }
        }
        return null;
    }

    window.SpiderApp = {
        /**
         * Initialize the entire Shell application.
         * Called on DOMContentLoaded (or immediately if DOM is already ready).
         */
        init: async function () {
            try {
                // 1. Initialize theme
                SpiderTheme.init();

                // 2. Initialize i18n
                await SpiderI18n.init();

                // 3. Initialize sidebar
                SpiderSidebar.init();

                // 4. Initialize tab manager
                SpiderTabManager.init();

                // 5. Set up header buttons

                // Theme toggle
                const themeBtn = qs('#header-theme-toggle');
                if (themeBtn) {
                    themeBtn.addEventListener('click', function () {
                        SpiderTheme.toggle();
                        updateThemeIcon();
                    });
                    updateThemeIcon();
                }

                // Language toggle
                const langBtn = qs('#header-lang-toggle');
                if (langBtn) {
                    langBtn.addEventListener('click', async function () {
                        const current = SpiderI18n.current();
                        const next = current === 'ko' ? 'en' : 'ko';
                        await SpiderI18n.change(next);
                        updateLangButton();
                        // Re-translate the entire page (in case any data-i18n elements exist outside panels)
                        await SpiderI18n.translate(document.body);
                    });
                    updateLangButton();
                }

                // 6. Open initial tab if configured
                const config = window.SpiderConfig || {};
                if (config.initialTab) {
                    const menuTree = config.menuTree || [];
                    const menu = findMenuInTree(menuTree, config.initialTab);
                    if (menu && menu.menuUrl) {
                        SpiderTabManager.open(menu.menuId, menu.menuName, menu.menuUrl);
                    }
                }

                // 7. Log init complete
                console.info('[SpiderApp] Initialization complete.');

            } catch (err) {
                console.error('[SpiderApp] Initialization failed:', err);
            }
        }
    };

    // ── Auto-run on DOM ready ──
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { SpiderApp.init(); });
    } else {
        SpiderApp.init();
    }
})();
