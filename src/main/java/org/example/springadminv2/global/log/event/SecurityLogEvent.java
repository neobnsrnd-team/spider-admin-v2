package org.example.springadminv2.global.log.event;

import java.time.Instant;

public record SecurityLogEvent(
        String traceId,
        String userId,
        Instant timestamp,
        String action,
        boolean success,
        String clientIp,
        String detail)
        implements LogEvent {

    public SecurityLogEvent(
            String traceId, String userId, String action, boolean success, String clientIp, String detail) {
        this(traceId, userId, Instant.now(), action, success, clientIp, detail);
    }
}
