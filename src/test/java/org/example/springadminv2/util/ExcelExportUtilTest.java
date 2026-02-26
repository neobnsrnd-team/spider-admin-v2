package org.example.springadminv2.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.springadminv2.global.dto.ExcelColumnDefinition;
import org.example.springadminv2.global.util.ExcelExportUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportUtilTest {

    @Nested
    @DisplayName("generateFileName")
    class GenerateFileName {

        @Test
        @DisplayName("화면명과 날짜로 파일명을 생성한다")
        void generates_correct_file_name() {
            // when
            String result = ExcelExportUtil.generateFileName("거래내역", LocalDate.of(2025, 3, 15));

            // then
            assertThat(result).isEqualTo("거래내역_20250315.xlsx");
        }
    }

    @Nested
    @DisplayName("isWithinLimit")
    class IsWithinLimit {

        @Test
        @DisplayName("MAX_ROW_LIMIT 이하이면 true")
        void within_limit() {
            // when & then
            assertThat(ExcelExportUtil.isWithinLimit(50_000)).isTrue();
            assertThat(ExcelExportUtil.isWithinLimit(0)).isTrue();
        }

        @Test
        @DisplayName("MAX_ROW_LIMIT 초과이면 false")
        void exceeds_limit() {
            // when & then
            assertThat(ExcelExportUtil.isWithinLimit(50_001)).isFalse();
        }
    }

    @Nested
    @DisplayName("MAX_ROW_LIMIT 상수")
    class MaxRowLimit {

        @Test
        @DisplayName("MAX_ROW_LIMIT은 50000")
        void max_row_limit_is_50000() {
            // when & then
            assertThat(ExcelExportUtil.MAX_ROW_LIMIT).isEqualTo(50_000);
        }
    }

    @Nested
    @DisplayName("createWorkbook")
    class CreateWorkbook {

        private final List<ExcelColumnDefinition> columns =
                List.of(new ExcelColumnDefinition("이름", "name", 20), new ExcelColumnDefinition("나이", "age", 10));

        @Test
        @DisplayName("헤더와 데이터가 올바르게 생성된다")
        void creates_header_and_data() throws IOException {
            // given
            List<Map<String, Object>> data =
                    List.of(Map.of("name", "홍길동", "age", 30), Map.of("name", "김철수", "age", 25));

            // when
            byte[] bytes = ExcelExportUtil.createWorkbook("테스트", columns, data);

            // then
            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet sheet = wb.getSheet("테스트");
                assertThat(sheet).isNotNull();

                Row header = sheet.getRow(0);
                assertThat(header.getCell(0).getStringCellValue()).isEqualTo("이름");
                assertThat(header.getCell(1).getStringCellValue()).isEqualTo("나이");

                assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("홍길동");
                assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("30");
                assertThat(sheet.getRow(2).getCell(0).getStringCellValue()).isEqualTo("김철수");
            }
        }

        @Test
        @DisplayName("빈 데이터 리스트면 헤더만 생성된다")
        void empty_data_creates_header_only() throws IOException {
            // given
            List<Map<String, Object>> data = Collections.emptyList();

            // when
            byte[] bytes = ExcelExportUtil.createWorkbook("빈시트", columns, data);

            // then
            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet sheet = wb.getSheet("빈시트");
                assertThat(sheet.getRow(0)).isNotNull();
                assertThat(sheet.getRow(1)).isNull();
            }
        }

        @Test
        @DisplayName("null 값은 빈 문자열로 처리된다")
        void null_value_handled_as_empty() throws IOException {
            // given
            List<Map<String, Object>> data = List.of(Map.of("name", "테스트"));

            // when
            byte[] bytes = ExcelExportUtil.createWorkbook("null테스트", columns, data);

            // then
            try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
                Sheet sheet = wb.getSheet("null테스트");
                assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEmpty();
            }
        }

        @Test
        @DisplayName("유효한 XLSX 바이트 배열을 반환한다")
        void returns_valid_xlsx_bytes() throws IOException {
            // when
            byte[] bytes = ExcelExportUtil.createWorkbook("시트", columns, Collections.emptyList());

            // then
            assertThat(bytes).isNotEmpty();
            assertThat(bytes[0]).isEqualTo((byte) 0x50);
            assertThat(bytes[1]).isEqualTo((byte) 0x4B);
        }
    }
}
