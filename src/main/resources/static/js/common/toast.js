/**
 * SpiderToast — 토스트 알림 모듈
 */
(function () {
    'use strict';

    let container = null;

    const ICONS = {
        success: '<svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor"><path d="M16 2a14 14 0 1 0 14 14A14 14 0 0 0 16 2zm-2 19.59l-5-5L10.59 15 14 18.41 21.41 11l1.596 1.586z"/></svg>',
        error: '<svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor"><path d="M16 2a14 14 0 1 0 14 14A14 14 0 0 0 16 2zm5.4 21L16 17.6 10.6 23 9 21.4l5.4-5.4L9 10.6 10.6 9l5.4 5.4L21.4 9 23 10.6l-5.4 5.4L23 21.4z"/></svg>',
        warning: '<svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor"><path d="M16 2L1 29h30zM17 25h-2v-2h2zm0-4h-2V14h2z"/></svg>',
        info: '<svg width="20" height="20" viewBox="0 0 32 32" fill="currentColor"><path d="M16 2a14 14 0 1 0 14 14A14 14 0 0 0 16 2zm-1 6h2v2h-2zm4 17h-6v-2h2v-7h-2v-2h4v9h2z"/></svg>',
    };

    const COLORS = {
        success: 'var(--cds-support-success)',
        error:   'var(--cds-support-error)',
        warning: 'var(--cds-support-warning)',
        info:    'var(--cds-support-info)',
    };

    function ensureContainer() {
        if (container && document.body.contains(container)) return container;
        container = document.createElement('div');
        container.id = 'spider-toast-container';
        Object.assign(container.style, {
            position: 'fixed',
            top: '1rem',
            right: '1rem',
            zIndex: '9999',
            display: 'flex',
            flexDirection: 'column',
            gap: '0.5rem',
            pointerEvents: 'none',
        });
        document.body.appendChild(container);
        return container;
    }

    function show(type, message, duration) {
        duration = duration || 3000;
        var wrap = ensureContainer();

        var toast = document.createElement('div');
        toast.setAttribute('role', 'alert');
        Object.assign(toast.style, {
            display: 'flex',
            alignItems: 'flex-start',
            gap: '0.75rem',
            minWidth: '320px',
            maxWidth: '420px',
            padding: '0.875rem 1rem',
            background: 'var(--cds-layer-01)',
            borderLeft: '3px solid ' + COLORS[type],
            boxShadow: 'var(--cds-shadow)',
            color: 'var(--cds-text-primary)',
            fontSize: '0.875rem',
            lineHeight: '1.43',
            pointerEvents: 'auto',
            transform: 'translateX(110%)',
            transition: 'transform 0.3s ease, opacity 0.3s ease',
            opacity: '0',
        });

        var iconSpan = document.createElement('span');
        iconSpan.innerHTML = ICONS[type] || ICONS.info;
        iconSpan.style.color = COLORS[type];
        iconSpan.style.flexShrink = '0';
        iconSpan.style.marginTop = '1px';

        var textSpan = document.createElement('span');
        textSpan.textContent = message;
        textSpan.style.flex = '1';

        var closeBtn = document.createElement('button');
        closeBtn.innerHTML = '<svg width="16" height="16" viewBox="0 0 32 32" fill="currentColor"><path d="M24 9.4L22.6 8 16 14.6 9.4 8 8 9.4l6.6 6.6L8 22.6 9.4 24l6.6-6.6 6.6 6.6 1.4-1.4-6.6-6.6z"/></svg>';
        Object.assign(closeBtn.style, {
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: 'var(--cds-icon-secondary)',
            padding: '0',
            flexShrink: '0',
            lineHeight: '1',
        });
        closeBtn.addEventListener('click', function () { dismiss(toast); });

        toast.appendChild(iconSpan);
        toast.appendChild(textSpan);
        toast.appendChild(closeBtn);
        wrap.appendChild(toast);

        // Trigger entrance animation
        requestAnimationFrame(function () {
            toast.style.transform = 'translateX(0)';
            toast.style.opacity = '1';
        });

        // Auto-dismiss
        if (duration > 0) {
            setTimeout(function () { dismiss(toast); }, duration);
        }

        return toast;
    }

    function dismiss(toast) {
        if (!toast || !toast.parentNode) return;
        toast.style.transform = 'translateX(110%)';
        toast.style.opacity = '0';
        setTimeout(function () {
            if (toast.parentNode) toast.parentNode.removeChild(toast);
        }, 300);
    }

    window.SpiderToast = {
        success: function (msg, duration) { return show('success', msg, duration); },
        error:   function (msg, duration) { return show('error', msg, duration); },
        warning: function (msg, duration) { return show('warning', msg, duration); },
        info:    function (msg, duration) { return show('info', msg, duration); },
    };
})();
