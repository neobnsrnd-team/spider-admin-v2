package org.example.springadminv2.log;

import java.time.Instant;

import org.example.springadminv2.global.log.adapter.LogbackLogEventAdapter;
import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.AuditLogEvent;
import org.example.springadminv2.global.log.event.ErrorLogEvent;
import org.example.springadminv2.global.log.event.LogEventType;
import org.example.springadminv2.global.log.event.SecurityLogEvent;
import org.example.springadminv2.global.log.event.SystemLogEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LogbackLogEventAdapterTest {

    private final LogbackLogEventAdapter adapter = new LogbackLogEventAdapter();

    @Test
    @DisplayName("AccessLogEvent 기록 시 예외 없음")
    void record_accessLogEvent() {
        AccessLogEvent event = new AccessLogEvent(
                "trace-001", "user01", Instant.now(), LogEventType.ACCESS, "127.0.0.1", "/test", "{}", "OK");

        assertThatCode(() -> adapter.record(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("AuditLogEvent 기록 시 예외 없음")
    void record_auditLogEvent() {
        AuditLogEvent event = new AuditLogEvent(
                "trace-002", "admin01", "UPDATE", "User", "USR-001", "{\"before\":1}", "{\"after\":2}");

        assertThatCode(() -> adapter.record(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SecurityLogEvent(success=true) 기록 시 예외 없음")
    void record_securityLogEvent_success() {
        SecurityLogEvent event =
                new SecurityLogEvent("trace-003", "user01", "LOGIN", true, "192.168.1.1", "Login successful");

        assertThatCode(() -> adapter.record(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SecurityLogEvent(success=false) 기록 시 예외 없음")
    void record_securityLogEvent_failure() {
        SecurityLogEvent event = new SecurityLogEvent(
                "trace-004", "ANONYMOUS", "AUTHENTICATION_FAILURE", false, "192.168.1.1", "Bad credentials");

        assertThatCode(() -> adapter.record(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ErrorLogEvent 기록 시 예외 없음")
    void record_errorLogEvent() {
        ErrorLogEvent event = new ErrorLogEvent(
                "trace-005",
                "user01",
                "INTERNAL_ERROR",
                "java.lang.RuntimeException",
                "unexpected error",
                "stacktrace...",
                "/api/test",
                "GET");

        assertThatCode(() -> adapter.record(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("SystemLogEvent 기록 시 예외 없음")
    void record_systemLogEvent() {
        SystemLogEvent event = new SystemLogEvent("trace-006", "STARTUP", "Application started");

        assertThatCode(() -> adapter.record(event)).doesNotThrowAnyException();
    }
}
