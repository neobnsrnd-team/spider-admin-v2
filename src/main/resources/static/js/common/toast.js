/**
 * SpiderToast — 토스트 알림 공통 모듈
 */
(function () {
    'use strict';

    const TOAST_CONTAINER_ID = 'spider-toast-container';
    const AUTO_CLOSE_MS = 3000;

    function getContainer() {
        let container = document.getElementById(TOAST_CONTAINER_ID);
        if (!container) {
            container = document.createElement('div');
            container.id = TOAST_CONTAINER_ID;
            container.style.cssText =
                'position:fixed;top:1rem;right:1rem;z-index:9999;display:flex;flex-direction:column;gap:0.5rem;';
            document.body.appendChild(container);
        }
        return container;
    }

    function createToast(message, type, autoClose) {
        const colors = {
            success: 'background:#16a34a;color:#fff;',
            error: 'background:#dc2626;color:#fff;',
            warning: 'background:#f59e0b;color:#fff;',
            info: 'background:#2563eb;color:#fff;'
        };

        const toast = document.createElement('div');
        toast.style.cssText =
            colors[type] +
            'padding:0.75rem 1.25rem;border-radius:0.375rem;box-shadow:0 4px 6px rgba(0,0,0,0.1);' +
            'min-width:250px;display:flex;justify-content:space-between;align-items:center;font-size:0.875rem;';

        const span = document.createElement('span');
        span.textContent = message;
        toast.appendChild(span);

        const closeBtn = document.createElement('button');
        closeBtn.textContent = '\u00d7';
        closeBtn.style.cssText = 'background:none;border:none;color:inherit;font-size:1.25rem;cursor:pointer;margin-left:1rem;';
        $(closeBtn).off('click').on('click', function () {
            removeToast(toast);
        });
        toast.appendChild(closeBtn);

        getContainer().appendChild(toast);

        if (autoClose) {
            setTimeout(function () {
                removeToast(toast);
            }, AUTO_CLOSE_MS);
        }
    }

    function removeToast(toast) {
        if (toast && toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }

    window.SpiderToast = {
        success: function (msg) {
            createToast(msg, 'success', true);
        },
        error: function (msg) {
            createToast(msg, 'error', false);
        },
        warning: function (msg) {
            createToast(msg, 'warning', true);
        },
        info: function (msg) {
            createToast(msg, 'info', true);
        }
    };
})();
