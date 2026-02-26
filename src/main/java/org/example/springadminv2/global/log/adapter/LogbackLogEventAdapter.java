package org.example.springadminv2.global.log.adapter;

import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.AuditLogEvent;
import org.example.springadminv2.global.log.event.ErrorLogEvent;
import org.example.springadminv2.global.log.event.LogEvent;
import org.example.springadminv2.global.log.event.SecurityLogEvent;
import org.example.springadminv2.global.log.event.SystemLogEvent;
import org.example.springadminv2.global.log.port.LogEventPort;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LogbackLogEventAdapter implements LogEventPort {

    @Override
    public void record(LogEvent event) {
        if (event instanceof AccessLogEvent e) {
            logAccess(e);
        } else if (event instanceof AuditLogEvent e) {
            logAudit(e);
        } else if (event instanceof SecurityLogEvent e) {
            logSecurity(e);
        } else if (event instanceof ErrorLogEvent e) {
            logError(e);
        } else if (event instanceof SystemLogEvent e) {
            logSystem(e);
        }
    }

    private void logAccess(AccessLogEvent e) {
        log.info(
                "[ACCESS] {} traceId={} userId={} ip={} url={}",
                e.resultMessage(),
                e.traceId(),
                e.userId(),
                e.accessIp(),
                e.accessUrl());
    }

    private void logAudit(AuditLogEvent e) {
        log.info(
                "[AUDIT] action={} entityType={} entityId={} traceId={} userId={}",
                e.action(),
                e.entityType(),
                e.entityId(),
                e.traceId(),
                e.userId());
    }

    private void logSecurity(SecurityLogEvent e) {
        if (e.success()) {
            log.info(
                    "[SECURITY] action={} userId={} clientIp={} traceId={}",
                    e.action(),
                    e.userId(),
                    e.clientIp(),
                    e.traceId());
        } else {
            log.warn(
                    "[SECURITY] action={} userId={} clientIp={} detail={} traceId={}",
                    e.action(),
                    e.userId(),
                    e.clientIp(),
                    e.detail(),
                    e.traceId());
        }
    }

    private void logError(ErrorLogEvent e) {
        log.error(
                "[ERROR] {} {} errorCode={} errorClass={} traceId={}",
                e.httpMethod(),
                e.uri(),
                e.errorCode(),
                e.errorClass(),
                e.traceId());
    }

    private void logSystem(SystemLogEvent e) {
        log.info("[SYSTEM] action={} detail={} traceId={}", e.action(), e.detail(), e.traceId());
    }
}
