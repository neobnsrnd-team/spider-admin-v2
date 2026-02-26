package org.example.springadminv2.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorType {

    // ─── RESOURCE_NOT_FOUND ────────────────────────────────────────
    RESOURCE_NOT_FOUND(ErrorCode.RESOURCE_NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, LogLevel.WARN),

    // ─── DUPLICATE_RESOURCE ────────────────────────────────────────
    DUPLICATE_RESOURCE(ErrorCode.DUPLICATE_RESOURCE, "이미 존재하는 리소스입니다.", HttpStatus.CONFLICT, LogLevel.WARN),

    // ─── BUSINESS_RULE_VIOLATED ────────────────────────────────────
    INVALID_STATE(ErrorCode.BUSINESS_RULE_VIOLATED, "현재 상태에서 수행할 수 없는 작업입니다.", HttpStatus.CONFLICT, LogLevel.INFO),

    INSUFFICIENT_RESOURCE(
            ErrorCode.BUSINESS_RULE_VIOLATED, "리소스가 부족합니다.", HttpStatus.UNPROCESSABLE_ENTITY, LogLevel.INFO),

    // ─── INVALID_INPUT ─────────────────────────────────────────────
    INVALID_INPUT(ErrorCode.INVALID_INPUT, "입력값이 유효하지 않습니다.", HttpStatus.BAD_REQUEST, LogLevel.DEBUG),

    // ─── AUTH ───────────────────────────────────────────────────────
    UNAUTHORIZED(ErrorCode.UNAUTHORIZED, "인증이 필요합니다.", HttpStatus.UNAUTHORIZED, LogLevel.WARN),

    FORBIDDEN(ErrorCode.FORBIDDEN, "권한이 없습니다.", HttpStatus.FORBIDDEN, LogLevel.WARN),

    // ─── SERVER ERRORS ─────────────────────────────────────────────
    EXTERNAL_SERVICE_ERROR(
            ErrorCode.EXTERNAL_SERVICE_ERROR, "외부 서비스에 일시적인 오류가 발생했습니다.", HttpStatus.BAD_GATEWAY, LogLevel.ERROR),

    INTERNAL_ERROR(
            ErrorCode.INTERNAL_ERROR,
            "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
            HttpStatus.INTERNAL_SERVER_ERROR,
            LogLevel.ERROR);

    private final ErrorCode errorCode;
    private final String message;
    private final HttpStatus httpStatus;
    private final LogLevel logLevel;
}
