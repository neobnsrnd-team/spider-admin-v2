package org.example.springadminv2.util;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.example.springadminv2.global.dto.DateRangeResult;
import org.example.springadminv2.global.util.DateRangeUtil;
import org.example.springadminv2.global.util.ScreenDateType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateRangeUtilTest {

    private static final LocalDate TODAY = LocalDate.of(2025, 3, 15);

    @Nested
    @DisplayName("defaultRange")
    class DefaultRange {

        @Test
        @DisplayName("HISTORY_AUDIT — 30일 전부터 오늘까지")
        void historyAudit() {
            DateRangeResult result = DateRangeUtil.defaultRange(ScreenDateType.HISTORY_AUDIT, TODAY);
            assertThat(result.startDate()).isEqualTo(LocalDate.of(2025, 2, 13));
            assertThat(result.endDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("BATCH_EXECUTION — 당일")
        void batchExecution() {
            DateRangeResult result = DateRangeUtil.defaultRange(ScreenDateType.BATCH_EXECUTION, TODAY);
            assertThat(result.startDate()).isEqualTo(TODAY);
            assertThat(result.endDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("TRANSACTION_TRACE — 1일 전부터 오늘까지")
        void transactionTrace() {
            DateRangeResult result = DateRangeUtil.defaultRange(ScreenDateType.TRANSACTION_TRACE, TODAY);
            assertThat(result.startDate()).isEqualTo(LocalDate.of(2025, 3, 14));
            assertThat(result.endDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("ERROR_LOG — 7일 전부터 오늘까지")
        void errorLog() {
            DateRangeResult result = DateRangeUtil.defaultRange(ScreenDateType.ERROR_LOG, TODAY);
            assertThat(result.startDate()).isEqualTo(LocalDate.of(2025, 3, 8));
            assertThat(result.endDate()).isEqualTo(TODAY);
        }

        @Test
        @DisplayName("SYSTEM_MONITOR — 당일")
        void systemMonitor() {
            DateRangeResult result = DateRangeUtil.defaultRange(ScreenDateType.SYSTEM_MONITOR, TODAY);
            assertThat(result.startDate()).isEqualTo(TODAY);
            assertThat(result.endDate()).isEqualTo(TODAY);
        }
    }

    @Nested
    @DisplayName("isValidRange")
    class IsValidRange {

        @Test
        @DisplayName("시작일이 종료일 이전이면 유효")
        void validRange() {
            assertThat(DateRangeUtil.isValidRange(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31)))
                    .isTrue();
        }

        @Test
        @DisplayName("시작일과 종료일이 동일하면 유효")
        void sameDate() {
            assertThat(DateRangeUtil.isValidRange(TODAY, TODAY)).isTrue();
        }

        @Test
        @DisplayName("시작일이 종료일 이후이면 무효")
        void reversedRange() {
            assertThat(DateRangeUtil.isValidRange(LocalDate.of(2025, 2, 1), LocalDate.of(2025, 1, 1)))
                    .isFalse();
        }

        @Test
        @DisplayName("시작일이 null이면 무효")
        void nullStart() {
            assertThat(DateRangeUtil.isValidRange(null, TODAY)).isFalse();
        }

        @Test
        @DisplayName("종료일이 null이면 무효")
        void nullEnd() {
            assertThat(DateRangeUtil.isValidRange(TODAY, null)).isFalse();
        }
    }

    @Nested
    @DisplayName("formatDate / formatDateTime")
    class Formatting {

        @Test
        @DisplayName("날짜를 yyyy-MM-dd 형식으로 포맷")
        void formatDate() {
            assertThat(DateRangeUtil.formatDate(LocalDate.of(2025, 3, 15))).isEqualTo("2025-03-15");
        }

        @Test
        @DisplayName("날짜시간을 yyyy-MM-dd HH:mm:ss 형식으로 포맷")
        void formatDateTime() {
            LocalDateTime dt = LocalDateTime.of(2025, 3, 15, 14, 30, 45);
            assertThat(DateRangeUtil.formatDateTime(dt)).isEqualTo("2025-03-15 14:30:45");
        }
    }

    @Nested
    @DisplayName("parseDate / parseDateTime")
    class Parsing {

        @Test
        @DisplayName("yyyy-MM-dd 문자열을 LocalDate로 파싱")
        void parseDate() {
            assertThat(DateRangeUtil.parseDate("2025-03-15")).isEqualTo(LocalDate.of(2025, 3, 15));
        }

        @Test
        @DisplayName("yyyy-MM-dd HH:mm:ss 문자열을 LocalDateTime으로 파싱")
        void parseDateTime() {
            assertThat(DateRangeUtil.parseDateTime("2025-03-15 14:30:45"))
                    .isEqualTo(LocalDateTime.of(2025, 3, 15, 14, 30, 45));
        }
    }
}
