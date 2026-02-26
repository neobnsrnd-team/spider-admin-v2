package org.example.springadminv2.global.exception;

import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {

    private final ErrorType errorType;

    public BaseException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    /**
     * @param detail 로그 식별용 파라미터 — 클라이언트에 노출되지 않는다.
     *               e.g., "orderId=ORD-001"
     */
    public BaseException(ErrorType errorType, String detail) {
        super(errorType.getMessage() + " [" + detail + "]");
        this.errorType = errorType;
    }

    public BaseException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
    }

    public BaseException(ErrorType errorType, String detail, Throwable cause) {
        super(errorType.getMessage() + " [" + detail + "]", cause);
        this.errorType = errorType;
    }
}
