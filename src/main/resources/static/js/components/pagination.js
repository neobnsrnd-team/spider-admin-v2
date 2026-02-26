/**
 * SpiderPagination - Carbon Pagination 패턴
 *
 * Usage:
 *   const pager = SpiderPagination.create(container, {
 *     pageSize: 20,
 *     pageSizes: [10, 20, 50, 100],
 *     onChange: (page, size) => loadData(page, size)
 *   });
 *   pager.setTotal(150);
 *   pager.getPage();  // { page: 1, size: 20, total: 150 }
 */
(function () {
    'use strict';

    /* ── SVG Icons (Carbon chevron style) ── */
    const ICON_FIRST = '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">'
        + '<path d="M11.3 12.3L7 8l4.3-4.3L10 2.3 4.3 8l5.7 5.7z"/>'
        + '<rect x="2" y="2" width="1.5" height="12"/></svg>';

    const ICON_PREV = '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">'
        + '<path d="M11.3 12.3L7 8l4.3-4.3L10 2.3 4.3 8l5.7 5.7z"/></svg>';

    const ICON_NEXT = '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">'
        + '<path d="M4.7 3.7L9 8l-4.3 4.3L6 13.7 11.7 8 6 2.3z"/></svg>';

    const ICON_LAST = '<svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">'
        + '<path d="M4.7 3.7L9 8l-4.3 4.3L6 13.7 11.7 8 6 2.3z"/>'
        + '<rect x="12.5" y="2" width="1.5" height="12"/></svg>';

    function t(key, params) {
        let msg = (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
        if (params) {
            Object.keys(params).forEach(function (k) {
                msg = msg.replace('{' + k + '}', params[k]);
            });
        }
        return msg;
    }

    /**
     * Create a Pagination instance
     */
    function create(container, options) {
        options = options || {};
        let currentPage = 1;
        let pageSize = options.pageSize || 20;
        const pageSizes = options.pageSizes || [10, 20, 50, 100];
        const onChange = options.onChange || null;
        let total = 0;

        /* ── Root element ── */
        const root = document.createElement('div');
        root.className = 'spider-pagination';
        Object.assign(root.style, {
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '0.5rem',
            padding: '0.75rem 1rem',
            fontSize: 'var(--cds-body-01-size)',
            color: 'var(--cds-text-primary)',
            background: 'var(--cds-layer-02)',
            borderTop: '1px solid var(--cds-border-subtle-00)',
            flexWrap: 'wrap',
            userSelect: 'none',
        });

        /* ── Left: page size selector + item range info ── */
        const leftSection = document.createElement('div');
        Object.assign(leftSection.style, {
            display: 'flex',
            alignItems: 'center',
            gap: '0.75rem',
        });

        // Page size selector
        const sizeLabel = document.createElement('label');
        sizeLabel.className = 'spider-page-size-label';
        Object.assign(sizeLabel.style, {
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            fontSize: 'var(--cds-body-01-size)',
            color: 'var(--cds-text-secondary)',
        });

        const sizeSelect = document.createElement('select');
        sizeSelect.className = 'spider-page-select';
        Object.assign(sizeSelect.style, {
            height: '2rem',
            padding: '0 2rem 0 0.75rem',
            fontSize: 'var(--cds-body-01-size)',
            color: 'var(--cds-text-primary)',
            backgroundColor: 'var(--cds-field-01)',
            border: 'none',
            borderBottom: '1px solid var(--cds-border-strong-01)',
            outline: 'none',
            cursor: 'pointer',
            appearance: 'auto',
        });
        pageSizes.forEach(function (size) {
            const opt = document.createElement('option');
            opt.value = size;
            opt.textContent = size;
            if (size === pageSize) opt.selected = true;
            sizeSelect.appendChild(opt);
        });
        sizeSelect.addEventListener('change', function () {
            pageSize = parseInt(sizeSelect.value, 10);
            currentPage = 1;
            render();
            fireChange();
        });
        sizeSelect.addEventListener('focus', function () {
            sizeSelect.style.borderBottomColor = 'var(--cds-border-interactive)';
        });
        sizeSelect.addEventListener('blur', function () {
            sizeSelect.style.borderBottomColor = 'var(--cds-border-strong-01)';
        });

        const sizeLabelText = document.createElement('span');
        sizeLabel.appendChild(sizeLabelText);
        sizeLabel.appendChild(sizeSelect);
        leftSection.appendChild(sizeLabel);

        // Separator
        const separator = document.createElement('span');
        Object.assign(separator.style, {
            width: '1px',
            height: '1rem',
            backgroundColor: 'var(--cds-border-subtle-01)',
        });
        leftSection.appendChild(separator);

        // Item range info
        const rangeInfo = document.createElement('span');
        rangeInfo.className = 'spider-page-info';
        Object.assign(rangeInfo.style, {
            fontSize: 'var(--cds-body-01-size)',
            color: 'var(--cds-text-secondary)',
            whiteSpace: 'nowrap',
        });
        leftSection.appendChild(rangeInfo);

        root.appendChild(leftSection);

        /* ── Right: navigation buttons + page numbers ── */
        const rightSection = document.createElement('div');
        Object.assign(rightSection.style, {
            display: 'flex',
            alignItems: 'center',
            gap: '0.125rem',
        });

        const btnFirst = createNavBtn(ICON_FIRST, 'pagination.first');
        const btnPrev = createNavBtn(ICON_PREV, 'pagination.prev');
        const pageNumbers = document.createElement('div');
        Object.assign(pageNumbers.style, {
            display: 'flex',
            alignItems: 'center',
            gap: '0.125rem',
        });
        const btnNext = createNavBtn(ICON_NEXT, 'pagination.next');
        const btnLast = createNavBtn(ICON_LAST, 'pagination.last');

        btnFirst.addEventListener('click', function () { goToPage(1); });
        btnPrev.addEventListener('click', function () { goToPage(currentPage - 1); });
        btnNext.addEventListener('click', function () { goToPage(currentPage + 1); });
        btnLast.addEventListener('click', function () { goToPage(getMaxPage()); });

        rightSection.appendChild(btnFirst);
        rightSection.appendChild(btnPrev);
        rightSection.appendChild(pageNumbers);
        rightSection.appendChild(btnNext);
        rightSection.appendChild(btnLast);
        root.appendChild(rightSection);

        container.appendChild(root);

        /* ── Helper: create nav button ── */
        function createNavBtn(icon, titleKey) {
            const btn = document.createElement('button');
            btn.className = 'spider-page-btn spider-page-nav-btn';
            btn.type = 'button';
            btn.innerHTML = icon;
            Object.assign(btn.style, {
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                width: '2rem',
                height: '2rem',
                background: 'transparent',
                border: 'none',
                color: 'var(--cds-icon-primary)',
                cursor: 'pointer',
                transition: 'background-color 80ms ease',
            });
            btn.addEventListener('mouseenter', function () {
                if (!btn.disabled) {
                    btn.style.backgroundColor = 'var(--cds-background-hover)';
                }
            });
            btn.addEventListener('mouseleave', function () {
                btn.style.backgroundColor = 'transparent';
            });
            // Title set on render for dynamic i18n
            btn._titleKey = titleKey;
            return btn;
        }

        /* ── Helper: create page number button ── */
        function createPageBtn(pageNum) {
            const btn = document.createElement('button');
            btn.className = 'spider-page-btn';
            btn.type = 'button';
            btn.textContent = pageNum;
            Object.assign(btn.style, {
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                minWidth: '2rem',
                height: '2rem',
                padding: '0 0.375rem',
                background: 'transparent',
                border: 'none',
                color: 'var(--cds-text-primary)',
                fontSize: 'var(--cds-body-01-size)',
                cursor: 'pointer',
                fontWeight: '400',
                transition: 'background-color 80ms ease, color 80ms ease',
            });
            btn.addEventListener('mouseenter', function () {
                if (!btn.classList.contains('active')) {
                    btn.style.backgroundColor = 'var(--cds-background-hover)';
                }
            });
            btn.addEventListener('mouseleave', function () {
                if (!btn.classList.contains('active')) {
                    btn.style.backgroundColor = 'transparent';
                }
            });
            btn.addEventListener('click', function () {
                goToPage(pageNum);
            });
            return btn;
        }

        /* ── Helper: create ellipsis ── */
        function createEllipsis() {
            const span = document.createElement('span');
            span.textContent = '\u2026';
            Object.assign(span.style, {
                display: 'inline-flex',
                alignItems: 'center',
                justifyContent: 'center',
                minWidth: '2rem',
                height: '2rem',
                color: 'var(--cds-text-secondary)',
                fontSize: 'var(--cds-body-01-size)',
            });
            return span;
        }

        function getMaxPage() {
            return Math.max(1, Math.ceil(total / pageSize));
        }

        function goToPage(page) {
            const max = getMaxPage();
            page = Math.max(1, Math.min(page, max));
            if (page === currentPage) return;
            currentPage = page;
            render();
            fireChange();
        }

        function fireChange() {
            if (onChange) {
                onChange(currentPage, pageSize);
            }
        }

        /* ── Compute page range (max 5 buttons) ── */
        function getPageRange() {
            const maxPage = getMaxPage();
            const maxVisible = 5;
            const pages = [];

            if (maxPage <= maxVisible) {
                for (let i = 1; i <= maxPage; i++) pages.push(i);
                return { pages: pages, leftEllipsis: false, rightEllipsis: false };
            }

            // Always include first and last page
            // Show current page +/- neighbors
            const half = Math.floor((maxVisible - 2) / 2); // 1 for center context
            let start = currentPage - half;
            let end = currentPage + half;

            if (start <= 2) {
                // Near the beginning
                start = 2;
                end = Math.min(maxVisible - 1, maxPage - 1);
            } else if (end >= maxPage - 1) {
                // Near the end
                end = maxPage - 1;
                start = Math.max(maxPage - maxVisible + 2, 2);
            }

            const leftEllipsis = start > 2;
            const rightEllipsis = end < maxPage - 1;

            pages.push(1);
            for (let i = start; i <= end; i++) {
                pages.push(i);
            }
            pages.push(maxPage);

            return { pages: pages, leftEllipsis: leftEllipsis, rightEllipsis: rightEllipsis };
        }

        /* ── Render all ── */
        function render() {
            const maxPage = getMaxPage();

            // Clamp current page
            if (currentPage > maxPage) currentPage = maxPage;
            if (currentPage < 1) currentPage = 1;

            // Update size label text
            sizeLabelText.textContent = t('pagination.pageSize');

            // Update range info
            if (total === 0) {
                rangeInfo.textContent = t('pagination.itemRange', {
                    start: 0,
                    end: 0,
                    total: 0,
                });
            } else {
                const start = (currentPage - 1) * pageSize + 1;
                const end = Math.min(currentPage * pageSize, total);
                rangeInfo.textContent = t('pagination.itemRange', {
                    start: start,
                    end: end,
                    total: total,
                });
            }

            // Update nav button disabled state & titles
            setNavDisabled(btnFirst, currentPage === 1);
            setNavDisabled(btnPrev, currentPage === 1);
            setNavDisabled(btnNext, currentPage === maxPage);
            setNavDisabled(btnLast, currentPage === maxPage);
            btnFirst.title = t(btnFirst._titleKey);
            btnPrev.title = t(btnPrev._titleKey);
            btnNext.title = t(btnNext._titleKey);
            btnLast.title = t(btnLast._titleKey);

            // Render page number buttons
            pageNumbers.innerHTML = '';
            const range = getPageRange();

            range.pages.forEach(function (pageNum, idx) {
                // Insert left ellipsis after first page if needed
                if (idx === 1 && range.leftEllipsis) {
                    pageNumbers.appendChild(createEllipsis());
                }

                const btn = createPageBtn(pageNum);
                if (pageNum === currentPage) {
                    btn.classList.add('active');
                    btn.style.backgroundColor = 'var(--cds-interactive)';
                    btn.style.color = 'var(--cds-text-on-color)';
                    btn.style.fontWeight = '600';
                }
                pageNumbers.appendChild(btn);

                // Insert right ellipsis before last page if needed
                if (idx === range.pages.length - 2 && range.rightEllipsis) {
                    pageNumbers.appendChild(createEllipsis());
                }
            });
        }

        function setNavDisabled(btn, disabled) {
            btn.disabled = disabled;
            if (disabled) {
                btn.style.opacity = '0.35';
                btn.style.cursor = 'not-allowed';
            } else {
                btn.style.opacity = '1';
                btn.style.cursor = 'pointer';
            }
        }

        /* ── Initial render ── */
        render();

        /* ── Public API ── */
        return {
            /**
             * Set total item count and re-render
             * @param {number} count
             */
            setTotal: function (count) {
                total = count || 0;
                if (currentPage > getMaxPage()) {
                    currentPage = getMaxPage();
                }
                render();
            },

            /**
             * Get current page state
             * @returns {{ page: number, size: number, total: number }}
             */
            getPage: function () {
                return {
                    page: currentPage,
                    size: pageSize,
                    total: total,
                };
            },

            /**
             * Go to a specific page
             * @param {number} page
             */
            goTo: function (page) {
                goToPage(page);
            },

            /**
             * Reset to page 1 (useful after search)
             */
            reset: function () {
                currentPage = 1;
                total = 0;
                render();
            },

            /**
             * Re-render (e.g., after language change)
             */
            refresh: function () {
                render();
            },

            /** Root DOM element */
            el: root,
        };
    }

    window.SpiderPagination = {
        create: create,
    };
})();
