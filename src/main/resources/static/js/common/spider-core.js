/**
 * Spider Core — Vanilla JS 유틸리티 레이어
 */
(function () {
    'use strict';

    /** querySelector shortcut, scoped to context */
    const qs = (sel, ctx = document) => ctx.querySelector(sel);

    /** querySelectorAll shortcut, returns Array */
    const qsa = (sel, ctx = document) => [...ctx.querySelectorAll(sel)];

    /**
     * Event delegation (replaces jQuery .on('event', '.selector', handler))
     * @param {Element} el - parent element to listen on
     * @param {string} event - event type
     * @param {string} selector - CSS selector for target
     * @param {Function} handler - event handler (this = matched element)
     */
    function delegate(el, event, selector, handler) {
        el.addEventListener(event, function (e) {
            const target = e.target.closest(selector);
            if (target && el.contains(target)) {
                handler.call(target, e);
            }
        });
    }

    /**
     * Fetch wrapper with CSRF, JSON handling, and auth redirect
     */
    const api = {
        async request(url, options = {}) {
            const config = window.SpiderConfig || {};
            const headers = Object.assign({
                'X-Requested-With': 'XMLHttpRequest',
            }, options.headers || {});

            // Add CSRF token for state-changing methods
            if (config.csrfHeader && config.csrfToken) {
                headers[config.csrfHeader] = config.csrfToken;
            }

            const resp = await fetch(url, Object.assign({}, options, { headers }));

            if (resp.status === 401) {
                window.location.href = (config.contextPath || '') + '/login';
                throw new Error('Unauthorized');
            }
            if (resp.status === 403) {
                SpiderToast.error('접근 권한이 없습니다.');
                throw new Error('Forbidden');
            }
            if (!resp.ok) {
                const body = await resp.json().catch(() => ({}));
                const msg = (body.error && body.error.message) || resp.statusText;
                throw new Error(msg);
            }
            return resp;
        },

        async getJson(url, params) {
            const query = params ? '?' + new URLSearchParams(params).toString() : '';
            const resp = await this.request(url + query);
            return resp.json();
        },

        async postJson(url, data) {
            const resp = await this.request(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data),
            });
            return resp.json();
        },

        async getHtml(url, params) {
            const query = params ? '?' + new URLSearchParams(params).toString() : '';
            const resp = await this.request(url + query, {
                headers: { 'Accept': 'text/html' },
            });
            return resp.text();
        },

        async downloadBlob(url, data) {
            const resp = await this.request(url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(data),
            });
            return resp.blob();
        },
    };

    // Export to window (globally available)
    window.qs = qs;
    window.qsa = qsa;
    window.delegate = delegate;
    window.api = api;
})();
