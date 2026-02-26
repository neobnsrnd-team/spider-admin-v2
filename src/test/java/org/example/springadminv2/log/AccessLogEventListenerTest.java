package org.example.springadminv2.log;

import java.time.Instant;

import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.listener.AccessLogEventListener;
import org.example.springadminv2.global.log.mapper.AccessLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AccessLogEventListenerTest {

    @Mock
    private AccessLogMapper accessLogMapper;

    @Test
    @DisplayName("RDB enabled → DB INSERT 수행")
    void handle_rdbEnabled_insertsToDb() {
        AccessLogEventListener listener = new AccessLogEventListener(accessLogMapper, true);
        AccessLogEvent event =
                new AccessLogEvent("trace-001", "user01", Instant.now(), "127.0.0.1", "/api/test", "{}", "OK");

        assertThatNoException().isThrownBy(() -> listener.handle(event));
        verify(accessLogMapper).insertAccessLog(event);
    }

    @Test
    @DisplayName("RDB disabled → DB INSERT 미수행")
    void handle_rdbDisabled_skipsDb() {
        AccessLogEventListener listener = new AccessLogEventListener(accessLogMapper, false);
        AccessLogEvent event =
                new AccessLogEvent("trace-002", "user01", Instant.now(), "127.0.0.1", "/api/test", "{}", "OK");

        assertThatNoException().isThrownBy(() -> listener.handle(event));
        verify(accessLogMapper, never()).insertAccessLog(event);
    }

    @Test
    @DisplayName("DB INSERT 실패 시 예외 전파 없음")
    void handle_dbFailure_doesNotPropagate() {
        AccessLogEventListener listener = new AccessLogEventListener(accessLogMapper, true);
        AccessLogEvent event =
                new AccessLogEvent("trace-003", "user01", Instant.now(), "127.0.0.1", "/api/test", "{}", "OK");

        org.mockito.Mockito.doThrow(new RuntimeException("DB error"))
                .when(accessLogMapper)
                .insertAccessLog(event);

        assertThatNoException().isThrownBy(() -> listener.handle(event));
    }
}
