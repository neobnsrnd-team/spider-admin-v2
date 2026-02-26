/**
 * SpiderToolbar — Carbon Toolbar 패턴 기반 툴바 컴포넌트
 *
 * 좌/우 버튼 그룹을 제공하며, 리소스 기반 권한 제어를 지원한다.
 * 아이콘은 인라인 SVG (16x16, stroke-based, currentColor)를 사용한다.
 */
(function () {
    'use strict';

    /**
     * Translate a key if it looks like an i18n key (contains '.').
     */
    function t(key) {
        if (!key) return '';
        if (key.indexOf('.') === -1) return key;
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    /**
     * Built-in SVG icon set (16x16, stroke-based, currentColor).
     */
    const ICONS = {
        plus:     '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 2v12M2 8h12" stroke="currentColor" stroke-width="1.5"/></svg>',
        trash:    '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 4h12M5 4V3a1 1 0 011-1h4a1 1 0 011 1v1M6 7v5M10 7v5M3 4l1 9a1 1 0 001 1h6a1 1 0 001-1l1-9" stroke="currentColor" stroke-width="1.2"/></svg>',
        download: '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M8 2v8m0 0l-3-3m3 3l3-3M2 12v1a1 1 0 001 1h10a1 1 0 001-1v-1" stroke="currentColor" stroke-width="1.5"/></svg>',
        save:     '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M12 14H4a1 1 0 01-1-1V3a1 1 0 011-1h6l3 3v8a1 1 0 01-1 1z" stroke="currentColor" stroke-width="1.2"/><path d="M10 14v-4H6v4M6 2v3h4" stroke="currentColor" stroke-width="1.2"/></svg>',
        edit:     '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M11.5 2.5l2 2L5 13H3v-2l8.5-8.5z" stroke="currentColor" stroke-width="1.2"/></svg>',
        refresh:  '<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 8a6 6 0 0110.472-4M14 8a6 6 0 01-10.472 4M12.472 1v3h-3M3.528 15v-3h3" stroke="currentColor" stroke-width="1.2"/></svg>',
    };

    /**
     * Create a single toolbar button element.
     */
    function createButton(config, resource, onClick) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'spider-toolbar-btn';
        btn.setAttribute('data-btn-id', config.id);

        // Danger style via CSS class .spider-toolbar-btn.danger
        if (config.danger) {
            btn.classList.add('danger');
        }

        // Icon
        if (config.icon && ICONS[config.icon]) {
            const iconSpan = document.createElement('span');
            iconSpan.innerHTML = ICONS[config.icon];
            iconSpan.style.display = 'inline-flex';
            iconSpan.style.alignItems = 'center';
            btn.appendChild(iconSpan);
        }

        // Label
        const labelSpan = document.createElement('span');
        if (config.label && config.label.indexOf('.') !== -1) {
            labelSpan.setAttribute('data-i18n', config.label);
        }
        labelSpan.textContent = t(config.label);
        btn.appendChild(labelSpan);

        // Click handler
        btn.addEventListener('click', function () {
            if (typeof onClick === 'function') {
                onClick(config.id);
            }
        });

        // Permission-based visibility
        if (resource && config.permission === 'W') {
            if (window.SpiderPermission && !SpiderPermission.canWrite(resource)) {
                btn.style.display = 'none';
            }
        }
        if (resource && config.permission === 'R') {
            if (window.SpiderPermission && !SpiderPermission.canRead(resource)) {
                btn.style.display = 'none';
            }
        }

        return btn;
    }

    /**
     * Create a toolbar and append it to the container.
     *
     * @param {Element} container     - Parent element
     * @param {Object}  options       - Configuration
     * @param {Array}   options.left  - Left-side button definitions
     * @param {Array}   options.right - Right-side button definitions
     * @param {string}  options.resource - Resource key for permission checks
     * @param {Function} options.onClick - Button click handler (receives buttonId)
     * @returns {{ setEnabled: Function, show: Function, el: Element }}
     */
    function create(container, options) {
        const leftDefs = options.left || [];
        const rightDefs = options.right || [];
        const resource = options.resource || '';
        const onClick = options.onClick;

        // Toolbar root
        const toolbar = document.createElement('div');
        toolbar.className = 'spider-toolbar';

        // Left group
        const leftGroup = document.createElement('div');
        leftGroup.className = 'spider-toolbar-left';
        for (let i = 0; i < leftDefs.length; i++) {
            leftGroup.appendChild(createButton(leftDefs[i], resource, onClick));
        }

        // Right group
        const rightGroup = document.createElement('div');
        rightGroup.className = 'spider-toolbar-right';
        for (let i = 0; i < rightDefs.length; i++) {
            rightGroup.appendChild(createButton(rightDefs[i], resource, onClick));
        }

        toolbar.appendChild(leftGroup);
        toolbar.appendChild(rightGroup);

        if (container) {
            container.appendChild(toolbar);
        }

        /**
         * Find a button by its id within the toolbar.
         */
        function findButton(id) {
            return qs('[data-btn-id="' + id + '"]', toolbar);
        }

        return {
            el: toolbar,

            /**
             * Enable or disable a button by id.
             * @param {string}  id      - Button id
             * @param {boolean} enabled - true to enable, false to disable
             */
            setEnabled: function (id, enabled) {
                const btn = findButton(id);
                if (btn) {
                    btn.disabled = !enabled;
                }
            },

            /**
             * Show or hide a button by id.
             * @param {string}  id      - Button id
             * @param {boolean} visible - true to show, false to hide
             */
            show: function (id, visible) {
                const btn = findButton(id);
                if (btn) {
                    btn.style.display = visible ? '' : 'none';
                }
            },

            /** Get the root element */
            getElement: function () {
                return toolbar;
            },
        };
    }

    window.SpiderToolbar = {
        create: create,
    };
})();
