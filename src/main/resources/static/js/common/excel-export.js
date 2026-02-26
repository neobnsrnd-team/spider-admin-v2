/**
 * SpiderExcel — 엑셀 다운로드 공통 모듈
 */
(function () {
    'use strict';

    function t(key) {
        return (window.SpiderI18n && SpiderI18n.t) ? SpiderI18n.t(key) : key;
    }

    window.SpiderExcel = {
        download: async function (url, params, screenName) {
            SpiderToast.info(t('excel.downloading'));

            try {
                const resp = await api.request(url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(params),
                });

                let fileName = screenName + '.xlsx';
                const disposition = resp.headers.get('Content-Disposition');
                if (disposition) {
                    const match = disposition.match(/filename[^;=\n]*=["']?([^"';\n]*)["']?/);
                    if (match && match[1]) {
                        fileName = decodeURIComponent(match[1]);
                    }
                }

                const blob = await resp.blob();
                const objectUrl = URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = objectUrl;
                link.download = fileName;
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                URL.revokeObjectURL(objectUrl);

                SpiderToast.success(t('excel.downloadSuccess'));
            } catch (error) {
                SpiderToast.error(t('excel.downloadFail') + ': ' + error.message);
            }
        }
    };
})();
