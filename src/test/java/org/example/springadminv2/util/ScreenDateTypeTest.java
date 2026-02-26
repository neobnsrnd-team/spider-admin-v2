package org.example.springadminv2.util;

import org.example.springadminv2.global.util.ScreenDateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScreenDateTypeTest {

    @Test
    @DisplayName("모든 enum 값이 존재하고 getDaysBack이 0 이상")
    void all_values_have_non_negative_days_back() {
        for (ScreenDateType type : ScreenDateType.values()) {
            assertThat(type.getDaysBack()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    @DisplayName("enum 값 개수 검증")
    void enum_value_count() {
        // when & then
        assertThat(ScreenDateType.values()).hasSize(5);
    }

    @Test
    @DisplayName("HISTORY_AUDIT는 30일")
    void history_audit() {
        // when & then
        assertThat(ScreenDateType.HISTORY_AUDIT.getDaysBack()).isEqualTo(30);
    }

    @Test
    @DisplayName("BATCH_EXECUTION은 당일(0일)")
    void batch_execution() {
        // when & then
        assertThat(ScreenDateType.BATCH_EXECUTION.getDaysBack()).isZero();
    }

    @Test
    @DisplayName("TRANSACTION_TRACE는 1일")
    void transaction_trace() {
        // when & then
        assertThat(ScreenDateType.TRANSACTION_TRACE.getDaysBack()).isEqualTo(1);
    }

    @Test
    @DisplayName("ERROR_LOG는 7일")
    void error_log() {
        // when & then
        assertThat(ScreenDateType.ERROR_LOG.getDaysBack()).isEqualTo(7);
    }

    @Test
    @DisplayName("SYSTEM_MONITOR는 당일(0일)")
    void system_monitor() {
        // when & then
        assertThat(ScreenDateType.SYSTEM_MONITOR.getDaysBack()).isZero();
    }

    @Test
    @DisplayName("valueOf로 enum 값 조회 가능")
    void value_of_works() {
        // when & then
        assertThat(ScreenDateType.valueOf("HISTORY_AUDIT")).isEqualTo(ScreenDateType.HISTORY_AUDIT);
    }
}
