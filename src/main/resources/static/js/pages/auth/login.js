/**
 * Login Page
 */
(function () {
    'use strict';

    // SVG icons (Carbon Design style, 20x20)
    const ICON_SUN = '<svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor">'
        + '<path d="M16 12a4 4 0 1 1-4 4 4 4 0 0 1 4-4m0-2a6 6 0 1 0 6 6 6 6 0 0 0-6-6z"/>'
        + '<rect x="15" y="2" width="2" height="4"/>'
        + '<rect x="15" y="26" width="2" height="4"/>'
        + '<rect x="26" y="15" width="4" height="2"/>'
        + '<rect x="2" y="15" width="4" height="2"/>'
        + '<rect x="23.07" y="5.93" width="2" height="4" transform="rotate(-45 24.07 7.93)"/>'
        + '<rect x="6.93" y="22.07" width="2" height="4" transform="rotate(-45 7.93 24.07)"/>'
        + '<rect x="22.07" y="22.93" width="4" height="2" transform="rotate(-45 24.07 23.93)"/>'
        + '<rect x="5.93" y="6.93" width="4" height="2" transform="rotate(-45 7.93 7.93)"/>'
        + '</svg>';

    const ICON_MOON = '<svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor">'
        + '<path d="M13.5 2A11.5 11.5 0 0 0 2 13.5 11.5 11.5 0 0 0 13.5 25c.98 0 1.94-.12 '
        + '2.86-.36A13.5 13.5 0 0 1 12 13.5 13.5 13.5 0 0 1 23.36 2.36 11.4 11.4 0 0 0 '
        + '13.5 2zm0 21A9.5 9.5 0 0 1 4 13.5a9.5 9.5 0 0 1 7.64-9.32A15.5 15.5 0 0 0 '
        + '10 13.5a15.5 15.5 0 0 0 9.32 8.86A9.47 9.47 0 0 1 13.5 23z"/>'
        + '</svg>';

    // Initialize theme (applies .dark class if needed)
    SpiderTheme.init();

    // Initialize i18n (translates data-i18n elements)
    SpiderI18n.init();

    // ── Dark mode toggle ──
    const themeBtn = qs('#theme-toggle');
    if (themeBtn) {
        themeBtn.addEventListener('click', function () {
            const theme = SpiderTheme.toggle();
            updateThemeIcon(theme);
        });
        updateThemeIcon(SpiderTheme.get());
    }

    // ── Language toggle ──
    const langBtn = qs('#lang-toggle');
    if (langBtn) {
        langBtn.addEventListener('click', function () {
            const current = SpiderI18n.current();
            const next = current === 'ko' ? 'en' : 'ko';
            SpiderI18n.change(next);
            langBtn.textContent = next.toUpperCase();
        });
        langBtn.textContent = SpiderI18n.current().toUpperCase();
    }

    /**
     * Update the theme toggle button icon.
     * Sun icon in dark mode (click to switch to light),
     * Moon icon in light mode (click to switch to dark).
     */
    function updateThemeIcon(theme) {
        if (!themeBtn) return;
        themeBtn.innerHTML = theme === 'dark' ? ICON_SUN : ICON_MOON;
    }

    // ── Enter key on password field submits form ──
    const passwordField = qs('#password');
    if (passwordField) {
        passwordField.addEventListener('keydown', function (e) {
            if (e.key === 'Enter') {
                const form = qs('#login-form');
                if (form) form.submit();
            }
        });
    }
})();
