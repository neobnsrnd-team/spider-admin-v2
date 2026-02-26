package org.example.springadminv2.dto;

import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.dto.ErrorDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("success() — 성공 응답 생성")
    void success_createsSuccessResponse() {
        ApiResponse<String> response = ApiResponse.success("data");

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isEqualTo("data");
        assertThat(response.getError()).isNull();
    }

    @Test
    @DisplayName("error() — 에러 응답 생성")
    void error_createsErrorResponse() {
        ErrorDetail detail = ErrorDetail.builder()
                .code("TEST_ERROR")
                .message("test message")
                .traceId("trace-001")
                .build();

        ApiResponse<Void> response = ApiResponse.error(detail);

        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getData()).isNull();
        assertThat(response.getError().getCode()).isEqualTo("TEST_ERROR");
        assertThat(response.getError().getMessage()).isEqualTo("test message");
        assertThat(response.getError().getTraceId()).isEqualTo("trace-001");
    }
}
