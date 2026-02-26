package org.example.springadminv2.util;

import org.example.springadminv2.global.util.ScreenDateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenDateTypeTest {

    @Test
    @DisplayName("모든 enum 값이 존재하고 getDaysBack이 0 이상")
    void allValuesHaveNonNegativeDaysBack() {
        for (ScreenDateType type : ScreenDateType.values()) {
            assertThat(type.getDaysBack()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("enum 값 개수 검증")
    void enumValueCount() {
        assertThat(ScreenDateType.values()).hasSize(5);
    }

    @Test
    @DisplayName("HISTORY_AUDIT는 30일")
    void historyAudit() {
        assertThat(ScreenDateType.HISTORY_AUDIT.getDaysBack()).isEqualTo(30);
    }

    @Test
    @DisplayName("BATCH_EXECUTION은 당일(0일)")
    void batchExecution() {
        assertThat(ScreenDateType.BATCH_EXECUTION.getDaysBack()).isEqualTo(0);
    }

    @Test
    @DisplayName("TRANSACTION_TRACE는 1일")
    void transactionTrace() {
        assertThat(ScreenDateType.TRANSACTION_TRACE.getDaysBack()).isEqualTo(1);
    }

    @Test
    @DisplayName("ERROR_LOG는 7일")
    void errorLog() {
        assertThat(ScreenDateType.ERROR_LOG.getDaysBack()).isEqualTo(7);
    }

    @Test
    @DisplayName("SYSTEM_MONITOR는 당일(0일)")
    void systemMonitor() {
        assertThat(ScreenDateType.SYSTEM_MONITOR.getDaysBack()).isEqualTo(0);
    }

    @Test
    @DisplayName("valueOf로 enum 값 조회 가능")
    void valueOfWorks() {
        assertThat(ScreenDateType.valueOf("HISTORY_AUDIT")).isEqualTo(ScreenDateType.HISTORY_AUDIT);
    }
}
