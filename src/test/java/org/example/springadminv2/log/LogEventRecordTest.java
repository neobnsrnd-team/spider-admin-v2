package org.example.springadminv2.log;

import java.time.Instant;

import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.AuditLogEvent;
import org.example.springadminv2.global.log.event.LogEvent;
import org.example.springadminv2.global.log.event.SecurityLogEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogEventRecordTest {

    @Test
    @DisplayName("AccessLogEvent — accessDtime 포맷 확인")
    void accessLogEvent_accessDtime() {
        Instant ts = Instant.parse("2025-01-15T10:30:00Z");
        AccessLogEvent event = new AccessLogEvent("t1", "user01", ts, "127.0.0.1", "/api", "{}", "OK");

        assertThat(event.accessDtime()).hasSize(14).matches("\\d{14}");
    }

    @Test
    @DisplayName("AccessLogEvent — sealed interface 구현 확인")
    void accessLogEvent_isLogEvent() {
        AccessLogEvent event = new AccessLogEvent("t1", "user01", Instant.now(), "127.0.0.1", "/api", "{}", "OK");

        assertThat(event).isInstanceOf(LogEvent.class);
        assertThat(event.traceId()).isEqualTo("t1");
        assertThat(event.userId()).isEqualTo("user01");
        assertThat(event.timestamp()).isNotNull();
        assertThat(event.accessIp()).isEqualTo("127.0.0.1");
        assertThat(event.accessUrl()).isEqualTo("/api");
        assertThat(event.inputData()).isEqualTo("{}");
        assertThat(event.resultMessage()).isEqualTo("OK");
    }

    @Test
    @DisplayName("AuditLogEvent — 편의 생성자 timestamp 자동 설정")
    void auditLogEvent_convenienceConstructor() {
        Instant before = Instant.now();
        AuditLogEvent event = new AuditLogEvent("t2", "user02", "UPDATE", "Entity", "1", "{}", "{}");
        Instant after = Instant.now();

        assertThat(event.traceId()).isEqualTo("t2");
        assertThat(event.userId()).isEqualTo("user02");
        assertThat(event.timestamp()).isBetween(before, after);
        assertThat(event.action()).isEqualTo("UPDATE");
        assertThat(event.entityType()).isEqualTo("Entity");
        assertThat(event.entityId()).isEqualTo("1");
        assertThat(event.beforeSnapshot()).isEqualTo("{}");
        assertThat(event.afterSnapshot()).isEqualTo("{}");
    }

    @Test
    @DisplayName("AuditLogEvent — 정규 생성자")
    void auditLogEvent_canonicalConstructor() {
        Instant ts = Instant.now();
        AuditLogEvent event = new AuditLogEvent("t3", "user03", ts, "DELETE", "Entity", "2", "{}", null);

        assertThat(event.timestamp()).isEqualTo(ts);
        assertThat(event.afterSnapshot()).isNull();
    }

    @Test
    @DisplayName("SecurityLogEvent — 편의 생성자 timestamp 자동 설정")
    void securityLogEvent_convenienceConstructor() {
        Instant before = Instant.now();
        SecurityLogEvent event = new SecurityLogEvent("t4", "user04", "LOGIN", true, "10.0.0.1", "ok");
        Instant after = Instant.now();

        assertThat(event.timestamp()).isBetween(before, after);
        assertThat(event.action()).isEqualTo("LOGIN");
        assertThat(event.success()).isTrue();
        assertThat(event.clientIp()).isEqualTo("10.0.0.1");
        assertThat(event.detail()).isEqualTo("ok");
    }

    @Test
    @DisplayName("SecurityLogEvent — 정규 생성자")
    void securityLogEvent_canonicalConstructor() {
        Instant ts = Instant.now();
        SecurityLogEvent event = new SecurityLogEvent("t5", "user05", ts, "LOGOUT", false, "10.0.0.2", "fail");

        assertThat(event.timestamp()).isEqualTo(ts);
        assertThat(event.success()).isFalse();
    }
}
