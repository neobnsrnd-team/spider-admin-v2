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

    window.SpiderDateDefaults = {
        init: function (screenType, startSelector, endSelector) {
            const startDate = calcStartDate(screenType);
            if (!startDate) {
                return;
            }

            const endDate = new Date();
            const $start = $(startSelector);
            const $end = $(endSelector);

            if ($start.length > 0) {
                $start.val(formatDate(startDate));
            }
            if ($end.length > 0) {
                $end.val(formatDate(endDate));
            }

            $start.off('change.spiderDate').on('change.spiderDate', function () {
                const startVal = $start.val();
                const endVal = $end.val();
                if (startVal && endVal && startVal > endVal) {
                    SpiderToast.warning('시작일이 종료일보다 클 수 없습니다.');
                    $start.val(endVal);
                }
            });

            $end.off('change.spiderDate').on('change.spiderDate', function () {
                const startVal = $start.val();
                const endVal = $end.val();
                if (startVal && endVal && startVal > endVal) {
                    SpiderToast.warning('종료일이 시작일보다 작을 수 없습니다.');
                    $end.val(startVal);
                }
            });
        }
    };
})();
