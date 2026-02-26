package org.example.springadminv2.global.log.event;

import java.time.Instant;

public record ErrorLogEvent(
        String traceId,
        String userId,
        Instant timestamp,
        LogEventType type,
        String errorCode,
        String errorClass,
        String errorMessage,
        String stackTrace,
        String uri,
        String httpMethod)
        implements LogEvent {

    public ErrorLogEvent(
            String traceId,
            String userId,
            String errorCode,
            String errorClass,
            String errorMessage,
            String stackTrace,
            String uri,
            String httpMethod) {
        this(
                traceId,
                userId,
                Instant.now(),
                LogEventType.ERROR,
                errorCode,
                errorClass,
                errorMessage,
                stackTrace,
                uri,
                httpMethod);
    }
}
