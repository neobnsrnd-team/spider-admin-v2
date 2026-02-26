package org.example.springadminv2.global.log.event;

import java.time.Instant;

public record AuditLogEvent(
        String traceId,
        String userId,
        Instant timestamp,
        String action,
        String entityType,
        String entityId,
        String beforeSnapshot,
        String afterSnapshot)
        implements LogEvent {

    public AuditLogEvent(
            String traceId,
            String userId,
            String action,
            String entityType,
            String entityId,
            String beforeSnapshot,
            String afterSnapshot) {
        this(traceId, userId, Instant.now(), action, entityType, entityId, beforeSnapshot, afterSnapshot);
    }
}
