package org.example.springadminv2.exception;

import org.example.springadminv2.global.exception.BaseException;
import org.example.springadminv2.global.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessExceptionTest {

    @Test
    @DisplayName("기본 생성자 — errorType 메시지 사용")
    void constructor_errorTypeOnly() {
        BaseException ex = new BaseException(ErrorType.RESOURCE_NOT_FOUND);

        assertThat(ex.getErrorType()).isEqualTo(ErrorType.RESOURCE_NOT_FOUND);
        assertThat(ex.getMessage()).isEqualTo(ErrorType.RESOURCE_NOT_FOUND.getMessage());
    }

    @Test
    @DisplayName("detail 포함 생성자 — 메시지에 detail 포함")
    void constructor_withDetail() {
        BaseException ex = new BaseException(ErrorType.RESOURCE_NOT_FOUND, "orderId=ORD-001");

        assertThat(ex.getMessage()).contains("orderId=ORD-001");
        assertThat(ex.getErrorType()).isEqualTo(ErrorType.RESOURCE_NOT_FOUND);
    }

    @Test
    @DisplayName("cause 포함 생성자 — 원인 예외 유지")
    void constructor_withCause() {
        RuntimeException cause = new RuntimeException("root cause");
        BaseException ex = new BaseException(ErrorType.INTERNAL_ERROR, cause);

        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getMessage()).isEqualTo(ErrorType.INTERNAL_ERROR.getMessage());
    }

    @Test
    @DisplayName("detail + cause 포함 생성자")
    void constructor_withDetailAndCause() {
        RuntimeException cause = new RuntimeException("root cause");
        BaseException ex = new BaseException(ErrorType.INTERNAL_ERROR, "service=payment", cause);

        assertThat(ex.getMessage()).contains("service=payment");
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getErrorType()).isEqualTo(ErrorType.INTERNAL_ERROR);
    }
}
