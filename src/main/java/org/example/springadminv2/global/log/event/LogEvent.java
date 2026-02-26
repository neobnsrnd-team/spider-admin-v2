package org.example.springadminv2.global.log.event;

import java.time.Instant;

public sealed interface LogEvent permits AccessLogEvent, AuditLogEvent, SecurityLogEvent {
    String traceId();

    String userId();

    Instant timestamp();
}
