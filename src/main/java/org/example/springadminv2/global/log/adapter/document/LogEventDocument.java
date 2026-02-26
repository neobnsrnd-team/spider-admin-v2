package org.example.springadminv2.global.log.adapter.document;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.AuditLogEvent;
import org.example.springadminv2.global.log.event.ErrorLogEvent;
import org.example.springadminv2.global.log.event.LogEvent;
import org.example.springadminv2.global.log.event.LogEventType;
import org.example.springadminv2.global.log.event.SecurityLogEvent;
import org.example.springadminv2.global.log.event.SystemLogEvent;

public record LogEventDocument(
        String traceId, String userId, Instant timestamp, LogEventType type, Map<String, Object> payload) {

    public static LogEventDocument from(LogEvent event) {
        Map<String, Object> payload = new LinkedHashMap<>();

        if (event instanceof AccessLogEvent e) {
            payload.put("accessIp", e.accessIp());
            payload.put("accessUrl", e.accessUrl());
            payload.put("inputData", e.inputData());
            payload.put("resultMessage", e.resultMessage());
        } else if (event instanceof ErrorLogEvent e) {
            payload.put("errorCode", e.errorCode());
            payload.put("errorClass", e.errorClass());
            payload.put("errorMessage", e.errorMessage());
            payload.put("stackTrace", e.stackTrace());
            payload.put("uri", e.uri());
            payload.put("httpMethod", e.httpMethod());
        } else if (event instanceof SecurityLogEvent e) {
            payload.put("action", e.action());
            payload.put("success", e.success());
            payload.put("clientIp", e.clientIp());
            payload.put("detail", e.detail());
        } else if (event instanceof AuditLogEvent e) {
            payload.put("action", e.action());
            payload.put("entityType", e.entityType());
            payload.put("entityId", e.entityId());
            payload.put("beforeSnapshot", e.beforeSnapshot());
            payload.put("afterSnapshot", e.afterSnapshot());
        } else if (event instanceof SystemLogEvent e) {
            payload.put("action", e.action());
            payload.put("detail", e.detail());
        }

        return new LogEventDocument(event.traceId(), event.userId(), event.timestamp(), event.type(), payload);
    }
}
