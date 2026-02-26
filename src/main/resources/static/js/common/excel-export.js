/**
 * SpiderExcel — 엑셀 다운로드 공통 모듈
 */
(function () {
    'use strict';

    window.SpiderExcel = {
        download: async function (url, params, screenName) {
            SpiderToast.info('엑셀 파일을 생성 중입니다...');

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

                SpiderToast.success('엑셀 다운로드가 완료되었습니다.');
            } catch (error) {
                SpiderToast.error('엑셀 다운로드에 실패했습니다: ' + error.message);
            }
        }
    };
})();
