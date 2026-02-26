/**
 * SpiderDateDefaults — 날짜 기본값 설정 공통 모듈
 */
(function () {
    'use strict';

    const DATE_TYPES = {
        HISTORY_AUDIT: 30,
        BATCH_EXECUTION: 0,
        TRANSACTION_TRACE: 1,
        ERROR_LOG: 7,
        SYSTEM_MONITOR: 0
    };

    function formatDate(date) {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return year + '-' + month + '-' + day;
    }

    function calcStartDate(screenType) {
        const daysBack = DATE_TYPES[screenType];
        if (daysBack === undefined) {
            return null;
        }
        const date = new Date();
        date.setDate(date.getDate() - daysBack);
        return date;
    }

    /* Track AbortControllers per element to prevent duplicate listeners */
    const abortMap = new WeakMap();

    window.SpiderDateDefaults = {
        init: function (screenType, startSelector, endSelector) {
            const startDate = calcStartDate(screenType);
            if (!startDate) {
                return;
            }

            const endDate = new Date();
            const start = qs(startSelector);
            const end = qs(endSelector);

            if (start) {
                start.value = formatDate(startDate);
            }
            if (end) {
                end.value = formatDate(endDate);
            }

            if (start) {
                if (abortMap.has(start)) abortMap.get(start).abort();
                const ac = new AbortController();
                abortMap.set(start, ac);
                start.addEventListener('change', function () {
                    const startVal = start.value;
                    const endVal = end ? end.value : '';
                    if (startVal && endVal && startVal > endVal) {
                        SpiderToast.warning('시작일이 종료일보다 클 수 없습니다.');
                        start.value = endVal;
                    }
                }, { signal: ac.signal });
            }

            if (end) {
                if (abortMap.has(end)) abortMap.get(end).abort();
                const ac = new AbortController();
                abortMap.set(end, ac);
                end.addEventListener('change', function () {
                    const startVal = start ? start.value : '';
                    const endVal = end.value;
                    if (startVal && endVal && startVal > endVal) {
                        SpiderToast.warning('종료일이 시작일보다 작을 수 없습니다.');
                        end.value = startVal;
                    }
                }, { signal: ac.signal });
            }
        }
    };
})();
