/**
 * SpiderDialog — 확인/알림 다이얼로그 모듈
 */
(function () {
    'use strict';

    function t(key) {
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    function create(options) {
        var overlay = document.createElement('div');
        Object.assign(overlay.style, {
            position: 'fixed',
            inset: '0',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            backgroundColor: 'var(--cds-overlay)',
            zIndex: '10000',
            opacity: '0',
            transition: 'opacity 0.15s ease',
        });

        var dialog = document.createElement('div');
        dialog.setAttribute('role', 'dialog');
        dialog.setAttribute('aria-modal', 'true');
        dialog.setAttribute('aria-labelledby', 'spider-dialog-title');
        Object.assign(dialog.style, {
            background: 'var(--cds-layer-02)',
            minWidth: '400px',
            maxWidth: '520px',
            width: '100%',
            boxShadow: 'var(--cds-shadow)',
            transform: 'scale(0.95)',
            transition: 'transform 0.15s ease',
        });

        // Header
        var header = document.createElement('div');
        Object.assign(header.style, {
            padding: '1rem 1.5rem 0',
        });
        var title = document.createElement('h3');
        title.id = 'spider-dialog-title';
        title.textContent = options.title || '';
        Object.assign(title.style, {
            margin: '0',
            fontSize: '1.25rem',
            fontWeight: '600',
            color: 'var(--cds-text-primary)',
        });
        header.appendChild(title);

        // Body
        var body = document.createElement('div');
        Object.assign(body.style, {
            padding: '1rem 1.5rem',
            fontSize: '0.875rem',
            color: 'var(--cds-text-secondary)',
            lineHeight: '1.5',
        });
        body.textContent = options.message || '';

        // Footer (buttons)
        var footer = document.createElement('div');
        Object.assign(footer.style, {
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '0.5rem',
            padding: '0 1.5rem 1rem',
        });

        dialog.appendChild(header);
        dialog.appendChild(body);
        dialog.appendChild(footer);
        overlay.appendChild(dialog);

        return { overlay: overlay, dialog: dialog, footer: footer };
    }

    function makeButton(label, isPrimary, isDanger) {
        var btn = document.createElement('button');
        btn.textContent = label;
        var base = {
            padding: '0.5rem 1rem',
            fontSize: '0.875rem',
            fontWeight: '500',
            border: 'none',
            cursor: 'pointer',
            lineHeight: '1.43',
            minWidth: '80px',
            transition: 'background-color 0.15s ease',
        };
        if (isDanger) {
            Object.assign(btn.style, base, {
                backgroundColor: 'var(--cds-support-error)',
                color: 'var(--cds-text-on-color)',
            });
        } else if (isPrimary) {
            Object.assign(btn.style, base, {
                backgroundColor: 'var(--cds-interactive)',
                color: 'var(--cds-text-on-color)',
            });
        } else {
            Object.assign(btn.style, base, {
                backgroundColor: 'transparent',
                color: 'var(--cds-text-primary)',
                border: '1px solid var(--cds-border-strong-01)',
            });
        }
        return btn;
    }

    function showDialog(options) {
        return new Promise(function (resolve) {
            var ui = create(options);
            var isConfirm = options.type === 'confirm';
            var isDanger = options.danger === true;

            // Cancel button (for confirm type)
            if (isConfirm) {
                var cancelBtn = makeButton(options.cancelText || t('common.cancel'), false, false);
                cancelBtn.addEventListener('click', function () { close(false); });
                ui.footer.appendChild(cancelBtn);
            }

            // OK/Confirm button
            var okText = options.okText || t('common.confirm');
            var okBtn = makeButton(okText, true, isDanger);
            okBtn.addEventListener('click', function () { close(true); });
            ui.footer.appendChild(okBtn);

            document.body.appendChild(ui.overlay);

            // Entrance animation
            requestAnimationFrame(function () {
                ui.overlay.style.opacity = '1';
                ui.dialog.style.transform = 'scale(1)';
            });

            // Focus the OK button
            okBtn.focus();

            // Escape key handler
            function onKey(e) {
                if (e.key === 'Escape') {
                    close(isConfirm ? false : true);
                }
            }
            document.addEventListener('keydown', onKey);

            function close(result) {
                document.removeEventListener('keydown', onKey);
                ui.overlay.style.opacity = '0';
                ui.dialog.style.transform = 'scale(0.95)';
                setTimeout(function () {
                    if (ui.overlay.parentNode) {
                        ui.overlay.parentNode.removeChild(ui.overlay);
                    }
                    resolve(result);
                }, 150);
            }
        });
    }

    window.SpiderDialog = {
        confirm: function (message, options) {
            options = options || {};
            return showDialog({
                type: 'confirm',
                title: options.title || t('dialog.confirmTitle'),
                message: message,
                okText: options.okText,
                cancelText: options.cancelText,
                danger: options.danger,
            });
        },

        alert: function (message, options) {
            options = options || {};
            return showDialog({
                type: 'alert',
                title: options.title || t('dialog.alertTitle'),
                message: message,
                okText: options.okText,
            });
        },

        confirmDelete: function (message) {
            return showDialog({
                type: 'confirm',
                title: t('dialog.deleteTitle'),
                message: message || t('dialog.confirmDelete'),
                okText: t('common.delete'),
                cancelText: t('common.cancel'),
                danger: true,
            });
        },
    };
})();
