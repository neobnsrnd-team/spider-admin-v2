package org.example.springadminv2.util;

import org.example.springadminv2.global.util.TraceIdUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class TraceIdUtilTest {

    @Test
    @DisplayName("MDC에 traceId가 있으면 해당 값 반환")
    void returnsExistingTraceId() {
        MDC.put("traceId", "existing-trace-id");
        try {
            assertThat(TraceIdUtil.getOrGenerate()).isEqualTo("existing-trace-id");
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    @DisplayName("MDC에 traceId가 없으면 16자 UUID 생성")
    void generatesNewTraceId() {
        MDC.remove("traceId");
        String traceId = TraceIdUtil.getOrGenerate();

        assertThat(traceId).isNotNull().hasSize(16).matches("[0-9a-f]+");
    }
}
