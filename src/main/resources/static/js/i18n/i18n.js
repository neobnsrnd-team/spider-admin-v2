/**
 * SpiderI18n — 클라이언트 i18n 엔진
 */
(function () {
    'use strict';

    const STORAGE_KEY = 'spider-lang';
    const COOKIE_NAME = 'spider-lang';
    const SUPPORTED = ['ko', 'en'];
    const bundles = {};

    function getCurrent() {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored && SUPPORTED.includes(stored)) return stored;
        const nav = navigator.language.slice(0, 2);
        return SUPPORTED.includes(nav) ? nav : 'ko';
    }

    function setCookie(lang) {
        document.cookie = COOKIE_NAME + '=' + lang + ';path=/;max-age=31536000;SameSite=Lax';
    }

    async function loadBundle(lang) {
        if (bundles[lang]) return bundles[lang];
        const config = window.SpiderConfig || {};
        const base = config.contextPath || '';
        const resp = await fetch(base + '/js/i18n/' + lang + '.json');
        bundles[lang] = await resp.json();
        return bundles[lang];
    }

    function resolve(key, bundle) {
        return key.split('.').reduce(function (obj, k) {
            return obj && obj[k];
        }, bundle) || key;
    }

    function translateDom(container, bundle) {
        const root = container || document;
        var els = root.querySelectorAll('[data-i18n]');
        els.forEach(function (el) {
            const key = el.getAttribute('data-i18n');
            const text = resolve(key, bundle);
            if (text !== key) {
                if (el.hasAttribute('data-i18n-attr')) {
                    el.setAttribute(el.getAttribute('data-i18n-attr'), text);
                } else {
                    el.textContent = text;
                }
            }
        });

        // title attribute
        root.querySelectorAll('[data-i18n-title]').forEach(function (el) {
            const text = resolve(el.getAttribute('data-i18n-title'), bundle);
            if (text !== el.getAttribute('data-i18n-title')) el.setAttribute('title', text);
        });

        // placeholder attribute
        root.querySelectorAll('[data-i18n-placeholder]').forEach(function (el) {
            const text = resolve(el.getAttribute('data-i18n-placeholder'), bundle);
            if (text !== el.getAttribute('data-i18n-placeholder')) el.setAttribute('placeholder', text);
        });

        // aria-label attribute
        root.querySelectorAll('[data-i18n-aria]').forEach(function (el) {
            const text = resolve(el.getAttribute('data-i18n-aria'), bundle);
            if (text !== el.getAttribute('data-i18n-aria')) el.setAttribute('aria-label', text);
        });
    }

    window.SpiderI18n = {
        current: getCurrent,

        init: async function () {
            const lang = getCurrent();
            const bundle = await loadBundle(lang);
            translateDom(null, bundle);
            return lang;
        },

        change: async function (lang) {
            if (!SUPPORTED.includes(lang)) return;
            localStorage.setItem(STORAGE_KEY, lang);
            setCookie(lang);
            const bundle = await loadBundle(lang);
            translateDom(null, bundle);
        },

        translate: async function (container) {
            const lang = getCurrent();
            const bundle = await loadBundle(lang);
            translateDom(container, bundle);
        },

        t: function (key) {
            const lang = getCurrent();
            const bundle = bundles[lang];
            if (!bundle) return key;
            return resolve(key, bundle);
        },
    };
})();
