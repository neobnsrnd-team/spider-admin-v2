/**
 * SpiderDialog — 확인/에러 다이얼로그 공통 모듈
 */
(function () {
    'use strict';

    const OVERLAY_STYLE =
        'position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);' +
        'display:flex;justify-content:center;align-items:center;z-index:10000;';

    const DIALOG_STYLE =
        'background:#fff;border-radius:0.5rem;padding:1.5rem;min-width:320px;max-width:480px;' +
        'box-shadow:0 20px 60px rgba(0,0,0,0.3);';

    const BTN_BASE =
        'padding:0.5rem 1.25rem;border:none;border-radius:0.25rem;cursor:pointer;font-size:0.875rem;';

    function createOverlay() {
        const overlay = document.createElement('div');
        overlay.style.cssText = OVERLAY_STYLE;
        return overlay;
    }

    function closeOverlay(overlay) {
        if (overlay && overlay.parentNode) {
            overlay.parentNode.removeChild(overlay);
        }
    }

    window.SpiderDialog = {
        confirm: function (msg, onOk, onCancel) {
            const overlay = createOverlay();
            const dialog = document.createElement('div');
            dialog.style.cssText = DIALOG_STYLE;

            const message = document.createElement('p');
            message.style.cssText = 'margin:0 0 1.5rem 0;font-size:0.95rem;line-height:1.5;';
            message.textContent = msg;
            dialog.appendChild(message);

            const btnArea = document.createElement('div');
            btnArea.style.cssText = 'display:flex;justify-content:flex-end;gap:0.5rem;';

            const cancelBtn = document.createElement('button');
            cancelBtn.textContent = '취소';
            cancelBtn.style.cssText = BTN_BASE + 'background:#e5e7eb;color:#374151;';
            $(cancelBtn).off('click').on('click', function () {
                closeOverlay(overlay);
                if (typeof onCancel === 'function') {
                    onCancel();
                }
            });

            const okBtn = document.createElement('button');
            okBtn.textContent = '확인';
            okBtn.style.cssText = BTN_BASE + 'background:#2563eb;color:#fff;';
            $(okBtn).off('click').on('click', function () {
                closeOverlay(overlay);
                if (typeof onOk === 'function') {
                    onOk();
                }
            });

            btnArea.appendChild(cancelBtn);
            btnArea.appendChild(okBtn);
            dialog.appendChild(btnArea);
            overlay.appendChild(dialog);
            document.body.appendChild(overlay);
        },

        error: function (msg, detail) {
            const overlay = createOverlay();
            const dialog = document.createElement('div');
            dialog.style.cssText = DIALOG_STYLE;

            const title = document.createElement('p');
            title.style.cssText = 'margin:0 0 0.5rem 0;font-weight:bold;color:#dc2626;font-size:1rem;';
            title.textContent = msg;
            dialog.appendChild(title);

            if (detail) {
                const detailEl = document.createElement('p');
                detailEl.style.cssText =
                    'margin:0 0 1.5rem 0;font-size:0.85rem;color:#6b7280;line-height:1.4;white-space:pre-wrap;';
                detailEl.textContent = detail;
                dialog.appendChild(detailEl);
            }

            const btnArea = document.createElement('div');
            btnArea.style.cssText = 'display:flex;justify-content:flex-end;';

            const closeBtn = document.createElement('button');
            closeBtn.textContent = '닫기';
            closeBtn.style.cssText = BTN_BASE + 'background:#dc2626;color:#fff;';
            $(closeBtn).off('click').on('click', function () {
                closeOverlay(overlay);
            });

            btnArea.appendChild(closeBtn);
            dialog.appendChild(btnArea);
            overlay.appendChild(dialog);
            document.body.appendChild(overlay);
        }
    };
})();
