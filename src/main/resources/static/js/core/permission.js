/**
 * SpiderPermission — 권한 헬퍼 모듈
 *
 * SpiderConfig.authorities (e.g. ['v3_menu_manage:W', 'v3_user_manage:R'])를
 * 기반으로 리소스별 읽기/쓰기 권한을 확인하고, DOM 요소에 권한을 적용한다.
 */
(function () {
    'use strict';

    /**
     * Check if user has a specific authority.
     * @param {string} resource - e.g. 'v3_menu_manage'
     * @param {string} level   - 'W' or 'R'
     * @returns {boolean}
     */
    function has(resource, level) {
        const authorities = (window.SpiderConfig && SpiderConfig.authorities) || [];
        return authorities.indexOf(resource + ':' + level) !== -1;
    }

    /**
     * Check if user has write access to resource.
     * @param {string} resource
     * @returns {boolean}
     */
    function canWrite(resource) {
        return has(resource, 'W');
    }

    /**
     * Check if user has at least read access to resource.
     * @param {string} resource
     * @returns {boolean}
     */
    function canRead(resource) {
        return has(resource, 'W') || has(resource, 'R');
    }

    /**
     * Apply permission-based visibility to elements within a container.
     *
     * Elements with data-permission="W" are hidden if the user lacks write access.
     * Elements with data-permission="R" are hidden if the user lacks read access.
     *
     * @param {Element} container - DOM container to scope the query
     * @param {string}  resource  - menuId / resource identifier
     */
    function apply(container, resource) {
        if (!container) return;

        const writeEls = qsa('[data-permission="W"]', container);
        const readEls = qsa('[data-permission="R"]', container);

        if (!canWrite(resource)) {
            for (let i = 0; i < writeEls.length; i++) {
                writeEls[i].style.display = 'none';
            }
        }

        if (!canRead(resource)) {
            for (let i = 0; i < readEls.length; i++) {
                readEls[i].style.display = 'none';
            }
        }
    }

    window.SpiderPermission = {
        has: has,
        canWrite: canWrite,
        canRead: canRead,
        apply: apply
    };
})();
