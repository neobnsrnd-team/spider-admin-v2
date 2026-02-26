package org.example.springadminv2.util;

import org.example.springadminv2.global.util.TraceIdUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdUtilTest {

    @Test
    @DisplayName("MDC에 traceId가 있으면 해당 값을 반환한다")
    void returns_existing_trace_id() {
        // given
        MDC.put("traceId", "existing-trace-id");

        try {
            // when
            String result = TraceIdUtil.getOrGenerate();

            // then
            assertThat(result).isEqualTo("existing-trace-id");
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    @DisplayName("MDC에 traceId가 없으면 16자 UUID를 생성한다")
    void generates_new_trace_id() {
        // given
        MDC.remove("traceId");

        // when
        String traceId = TraceIdUtil.getOrGenerate();

        // then
        assertThat(traceId).isNotNull().hasSize(16).matches("[0-9a-f]+");
    }
}
