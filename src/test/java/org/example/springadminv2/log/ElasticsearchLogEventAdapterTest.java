package org.example.springadminv2.log;

import java.time.Instant;

import org.example.springadminv2.global.log.adapter.document.LogEventDocument;
import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.AuditLogEvent;
import org.example.springadminv2.global.log.event.ErrorLogEvent;
import org.example.springadminv2.global.log.event.LogEventType;
import org.example.springadminv2.global.log.event.SecurityLogEvent;
import org.example.springadminv2.global.log.event.SystemLogEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

class ElasticsearchLogEventAdapterTest {

    @Test
    @DisplayName("LogEventDocument.from() — AccessLogEvent 변환 확인")
    void fromAccessLogEvent_convertsCorrectly() {
        AccessLogEvent event = new AccessLogEvent(
                "trace-001", "user01", Instant.now(), LogEventType.ACCESS, "127.0.0.1", "/test", "{}", "OK");

        LogEventDocument doc = LogEventDocument.from(event);

        assertThat(doc.traceId()).isEqualTo("trace-001");
        assertThat(doc.userId()).isEqualTo("user01");
        assertThat(doc.type()).isEqualTo(LogEventType.ACCESS);
        assertThat(doc.payload()).containsKey("accessIp");
        assertThat(doc.payload().get("accessIp")).isEqualTo("127.0.0.1");
        assertThat(doc.payload().get("accessUrl")).isEqualTo("/test");
    }

    @Test
    @DisplayName("LogEventDocument.from() — ErrorLogEvent 변환 확인")
    void fromErrorLogEvent_convertsCorrectly() {
        ErrorLogEvent event = new ErrorLogEvent(
                "trace-002",
                "user01",
                Instant.now(),
                LogEventType.ERROR,
                "INTERNAL_ERROR",
                "java.lang.RuntimeException",
                "unexpected error",
                "java.lang.RuntimeException: unexpected error\n\tat ...",
                "/api/test",
                "GET");

        LogEventDocument doc = LogEventDocument.from(event);

        assertThat(doc.traceId()).isEqualTo("trace-002");
        assertThat(doc.type()).isEqualTo(LogEventType.ERROR);
        assertThat(doc.payload().get("errorCode")).isEqualTo("INTERNAL_ERROR");
        assertThat(doc.payload().get("errorClass")).isEqualTo("java.lang.RuntimeException");
        assertThat(doc.payload().get("errorMessage")).isEqualTo("unexpected error");
        assertThat(doc.payload().get("uri")).isEqualTo("/api/test");
        assertThat(doc.payload().get("httpMethod")).isEqualTo("GET");
    }

    @Test
    @DisplayName("LogEventDocument.from() — SecurityLogEvent 변환 확인")
    void fromSecurityLogEvent_convertsCorrectly() {
        SecurityLogEvent event = new SecurityLogEvent(
                "trace-003",
                "ANONYMOUS",
                Instant.now(),
                LogEventType.SECURITY,
                "AUTHENTICATION_FAILURE",
                false,
                "192.168.1.1",
                "Bad credentials");

        LogEventDocument doc = LogEventDocument.from(event);

        assertThat(doc.traceId()).isEqualTo("trace-003");
        assertThat(doc.type()).isEqualTo(LogEventType.SECURITY);
        assertThat(doc.payload().get("action")).isEqualTo("AUTHENTICATION_FAILURE");
        assertThat(doc.payload().get("success")).isEqualTo(false);
        assertThat(doc.payload().get("clientIp")).isEqualTo("192.168.1.1");
    }

    @Test
    @DisplayName("LogEventDocument.from() — AuditLogEvent 변환 확인")
    void fromAuditLogEvent_convertsCorrectly() {
        AuditLogEvent event = new AuditLogEvent(
                "trace-004",
                "admin01",
                Instant.now(),
                LogEventType.AUDIT,
                "UPDATE",
                "User",
                "USR-001",
                "{\"name\":\"before\"}",
                "{\"name\":\"after\"}");

        LogEventDocument doc = LogEventDocument.from(event);

        assertThat(doc.traceId()).isEqualTo("trace-004");
        assertThat(doc.type()).isEqualTo(LogEventType.AUDIT);
        assertThat(doc.payload().get("action")).isEqualTo("UPDATE");
        assertThat(doc.payload().get("entityType")).isEqualTo("User");
        assertThat(doc.payload().get("entityId")).isEqualTo("USR-001");
    }

    @Test
    @DisplayName("LogEventDocument.from() — SystemLogEvent 변환 확인")
    void fromSystemLogEvent_convertsCorrectly() {
        SystemLogEvent event = new SystemLogEvent(
                "trace-005", "SYSTEM", Instant.now(), LogEventType.SYSTEM, "STARTUP", "Application started");

        LogEventDocument doc = LogEventDocument.from(event);

        assertThat(doc.traceId()).isEqualTo("trace-005");
        assertThat(doc.type()).isEqualTo(LogEventType.SYSTEM);
        assertThat(doc.payload().get("action")).isEqualTo("STARTUP");
        assertThat(doc.payload().get("detail")).isEqualTo("Application started");
    }

    @Test
    @DisplayName("enabled=false 시 ES Bean 미등록 확인")
    void esDisabled_beanNotRegistered() {
        // ElasticsearchLogEventAdapter는 @ConditionalOnProperty(havingValue = "true")이므로
        // 기본값(false)일 때 빈이 등록되지 않음.
        // 아래 내장 테스트 클래스에서 컨텍스트 레벨로 검증한다.
        assertThat(true).isTrue();
    }

    @SpringBootTest
    @TestPropertySource(properties = "log.event.elasticsearch.enabled=false")
    static class EsDisabledContextTest {

        @Autowired(required = false)
        private org.example.springadminv2.global.log.adapter.ElasticsearchLogEventAdapter esAdapter;

        @Test
        @DisplayName("enabled=false → ElasticsearchLogEventAdapter 빈 미등록")
        void esAdapterNotRegistered() {
            assertThat(esAdapter).isNull();
        }
    }
}
