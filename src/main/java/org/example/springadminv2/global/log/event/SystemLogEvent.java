package org.example.springadminv2.global.log.event;

import java.time.Instant;

public record SystemLogEvent(
        String traceId, String userId, Instant timestamp, LogEventType type, String action, String detail)
        implements LogEvent {

    public SystemLogEvent(String traceId, String action, String detail) {
        this(traceId, "SYSTEM", Instant.now(), LogEventType.SYSTEM, action, detail);
    }
}
