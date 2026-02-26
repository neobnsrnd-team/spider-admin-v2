package org.example.springadminv2.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.springadminv2.global.dto.ExcelColumnDefinition;
import org.example.springadminv2.global.util.ExcelExportUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("manual")
class ExcelExportManualTest {

    @TempDir
    Path tempDir;

    private static final List<ExcelColumnDefinition> COLUMNS = List.of(
            new ExcelColumnDefinition("번호", "no", 10),
            new ExcelColumnDefinition("이름", "name", 20),
            new ExcelColumnDefinition("부서", "dept", 20),
            new ExcelColumnDefinition("금액", "amount", 15));

    @Nested
    @DisplayName("실제 파일 생성")
    class FileGeneration {

        @Test
        @DisplayName("주어진 데이터로 실제 엑셀 파일이 생성된다")
        void creates_actual_xlsx_file() throws IOException {
            // given
            List<Map<String, Object>> data = List.of(
                    createRow(1, "홍길동", "개발팀", 3_000_000),
                    createRow(2, "김영희", "기획팀", 2_500_000),
                    createRow(3, "이철수", "인프라팀", 2_800_000));

            // when
            byte[] bytes = ExcelExportUtil.createWorkbook("직원목록", COLUMNS, data);
            Path file = tempDir.resolve("test_output.xlsx");
            Files.write(file, bytes);

            // then
            assertThat(file).exists();
            assertThat(Files.size(file)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("대용량 처리")
    class LargeVolume {

        @Test
        @DisplayName("최대값인 50,000건 엑셀 생성이 무리 없이 동작한다")
        void generates_50k_rows_without_issue() throws IOException {
            // given
            int rowCount = ExcelExportUtil.MAX_ROW_LIMIT;
            List<Map<String, Object>> data = new ArrayList<>(rowCount);
            for (int i = 1; i <= rowCount; i++) {
                data.add(createRow(i, "사용자" + i, "부서" + (i % 10), i * 1000));
            }

            // when
            long startTime = System.currentTimeMillis();
            byte[] bytes = ExcelExportUtil.createWorkbook("대용량테스트", COLUMNS, data);
            long elapsed = System.currentTimeMillis() - startTime;

            // then
            assertThat(bytes).isNotEmpty();
            assertThat(elapsed).as("50,000건 생성 시간이 30초 이내").isLessThan(30_000);

            Path file = tempDir.resolve("large_output.xlsx");
            Files.write(file, bytes);
            assertThat(Files.size(file)).isGreaterThan(0);
        }
    }

    @Nested
    @DisplayName("학습 테스트: 서비스단 활용 시나리오")
    class ServiceUsageScenario {

        @Test
        @DisplayName("서비스에서 조회한 데이터를 엑셀로 변환하는 전형적 흐름")
        void typical_service_to_excel_flow() throws IOException {
            // given — 서비스에서 DB 조회 결과를 List<Map>으로 받은 상황을 시뮬레이션
            List<Map<String, Object>> queryResult = simulateDbQuery();

            // 1) 건수 제한 확인
            assertThat(ExcelExportUtil.isWithinLimit(queryResult.size()))
                    .as("조회 건수가 MAX_ROW_LIMIT 이하여야 엑셀 생성 가능")
                    .isTrue();

            // 2) 컬럼 정의 — 화면에 표시할 컬럼만 선택
            List<ExcelColumnDefinition> columns = List.of(
                    new ExcelColumnDefinition("거래번호", "trxId", 20),
                    new ExcelColumnDefinition("기관명", "orgName", 25),
                    new ExcelColumnDefinition("상태", "status", 15));

            // when — 엑셀 생성
            byte[] excelBytes = ExcelExportUtil.createWorkbook("거래내역", columns, queryResult);

            // then — 유효한 바이트 배열
            assertThat(excelBytes).isNotEmpty();

            // 3) 파일명 생성 — Controller에서 Content-Disposition에 사용
            String fileName = ExcelExportUtil.generateFileName("거래내역", java.time.LocalDate.now());
            assertThat(fileName).endsWith(".xlsx");
        }

        @Test
        @DisplayName("건수 초과 시 isWithinLimit으로 사전 차단")
        void rejects_when_exceeding_limit() {
            // given — MAX_ROW_LIMIT 초과 건수
            int overLimitCount = ExcelExportUtil.MAX_ROW_LIMIT + 1;

            // when & then
            assertThat(ExcelExportUtil.isWithinLimit(overLimitCount))
                    .as("MAX_ROW_LIMIT 초과 시 false → 서비스에서 예외 처리")
                    .isFalse();
        }

        @Test
        @DisplayName("DTO 리스트를 Map 리스트로 변환하여 활용하는 패턴")
        void dto_to_map_conversion_pattern() throws IOException {
            // given — 서비스에서 DTO 리스트를 반환하는 상황
            record UserDto(String userId, String userName, String roleName) {}

            List<UserDto> dtoList =
                    List.of(new UserDto("admin", "관리자", "ADMIN"), new UserDto("user01", "일반사용자", "USER"));

            // when — DTO → Map 변환 (실제 서비스에서 사용할 패턴)
            List<Map<String, Object>> mapList = dtoList.stream()
                    .map(dto -> {
                        Map<String, Object> row = new LinkedHashMap<>();
                        row.put("userId", dto.userId());
                        row.put("userName", dto.userName());
                        row.put("roleName", dto.roleName());
                        return row;
                    })
                    .collect(Collectors.toList());

            List<ExcelColumnDefinition> columns = List.of(
                    new ExcelColumnDefinition("사용자ID", "userId", 15),
                    new ExcelColumnDefinition("이름", "userName", 20),
                    new ExcelColumnDefinition("역할", "roleName", 15));

            byte[] bytes = ExcelExportUtil.createWorkbook("사용자목록", columns, mapList);

            // then
            assertThat(bytes).isNotEmpty();
        }

        private List<Map<String, Object>> simulateDbQuery() {
            List<Map<String, Object>> result = new ArrayList<>();
            for (int i = 1; i <= 100; i++) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("trxId", "TRX" + String.format("%06d", i));
                row.put("orgName", "기관" + (i % 5));
                row.put("status", i % 3 == 0 ? "실패" : "성공");
                result.add(row);
            }
            return result;
        }
    }

    private static Map<String, Object> createRow(int no, String name, String dept, int amount) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("no", no);
        row.put("name", name);
        row.put("dept", dept);
        row.put("amount", amount);
        return row;
    }
}
