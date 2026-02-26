package org.example.springadminv2.global.exception;

import java.util.List;

import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.dto.ErrorDetail;
import org.example.springadminv2.global.util.TraceIdUtil;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 1순위: BusinessException ─────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorType type = ex.getErrorType();
        String traceId = currentTraceId();

        logByType(type, traceId, ex);

        return ResponseEntity.status(type.getHttpStatus())
                .body(ApiResponse.error(ErrorDetail.builder()
                        .code(type.getErrorCode().name())
                        .message(type.getMessage())
                        .traceId(traceId)
                        .build()));
    }

    // ─── 2순위: Bean Validation (@Valid) ──────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String traceId = currentTraceId();

        List<ErrorDetail.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorDetail.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        log.debug("[{}] Validation failed: fields={}", traceId, fields);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorDetail.builder()
                        .code(ErrorCode.INVALID_INPUT.name())
                        .message(ErrorType.INVALID_INPUT.getMessage())
                        .traceId(traceId)
                        .fields(fields)
                        .build()));
    }

    // ─── 3순위: DB 제약 조건 위반 ──────────────────────────────────
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String traceId = currentTraceId();
        String message = ex.getMostSpecificCause().getMessage();
        log.warn("[{}] DataIntegrityViolationException: {}", traceId, message);

        if (message != null
                && (message.contains("ORA-00001") || message.toLowerCase().contains("duplicate entry"))) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ErrorDetail.builder()
                            .code(ErrorCode.DUPLICATE_RESOURCE.name())
                            .message(ErrorType.DUPLICATE_RESOURCE.getMessage())
                            .traceId(traceId)
                            .build()));
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorDetail.builder()
                        .code(ErrorCode.DATA_INTEGRITY_VIOLATION.name())
                        .message("데이터 무결성 제약 조건 위반")
                        .traceId(traceId)
                        .build()));
    }

    // ─── Security 예외 → Spring Security에 위임 ────────────────
    @ExceptionHandler(AccessDeniedException.class)
    public void handleAccessDenied(AccessDeniedException ex) throws AccessDeniedException {
        throw ex;
    }

    @ExceptionHandler(AuthenticationException.class)
    public void handleAuthentication(AuthenticationException ex) throws AuthenticationException {
        throw ex;
    }

    // ─── 4순위: 최후 방어선 ──────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        String traceId = currentTraceId();
        log.error("[{}] Unexpected error: {}", traceId, ex.getMessage(), ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorDetail.builder()
                        .code(ErrorCode.INTERNAL_ERROR.name())
                        .message(ErrorType.INTERNAL_ERROR.getMessage())
                        .traceId(traceId)
                        .build()));
    }

    // ─── 유틸리티 ─────────────────────────────────────────────────
    private void logByType(ErrorType type, String traceId, Exception ex) {
        String fmt = "[{}] {}: {}";
        switch (type.getLogLevel()) {
            case TRACE, DEBUG -> log.debug(fmt, traceId, type.name(), ex.getMessage());
            case INFO -> log.info(fmt, traceId, type.name(), ex.getMessage());
            case WARN -> log.warn(fmt, traceId, type.name(), ex.getMessage());
            case ERROR, FATAL -> log.error(fmt, traceId, type.name(), ex.getMessage(), ex);
            case OFF -> {}
        }
    }

    private String currentTraceId() {
        return TraceIdUtil.getOrGenerate();
    }
}
