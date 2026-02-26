/**
 * SpiderTheme — 다크모드 토글 모듈
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'spider-theme';
    const COOKIE_NAME = 'spider-theme';

    function getPreferred() {
        // 1. localStorage
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored === 'dark' || stored === 'light') return stored;
        // 2. System preference
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }

    function setCookie(value) {
        document.cookie = COOKIE_NAME + '=' + value + ';path=/;max-age=31536000;SameSite=Lax';
    }

    function apply(theme) {
        const html = document.documentElement;
        if (theme === 'dark') {
            html.classList.add('dark');
        } else {
            html.classList.remove('dark');
        }
        localStorage.setItem(STORAGE_KEY, theme);
        setCookie(theme);
    }

    window.SpiderTheme = {
        init: function () {
            apply(getPreferred());
            // Listen for system preference changes
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', function (e) {
                if (!localStorage.getItem(STORAGE_KEY)) {
                    apply(e.matches ? 'dark' : 'light');
                }
            });
        },

        toggle: function () {
            const current = document.documentElement.classList.contains('dark') ? 'dark' : 'light';
            const next = current === 'dark' ? 'light' : 'dark';
            apply(next);
            return next;
        },

        get: function () {
            return document.documentElement.classList.contains('dark') ? 'dark' : 'light';
        },
    };
})();
