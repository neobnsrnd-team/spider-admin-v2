package org.example.springadminv2.log;

import java.time.Instant;

import org.example.springadminv2.global.log.adapter.LogbackLogEventAdapter;
import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.LogEventType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LogbackLogEventAdapterTest {

    @Test
    void record_does_not_throw() {
        LogbackLogEventAdapter adapter = new LogbackLogEventAdapter();

        AccessLogEvent event = new AccessLogEvent(
                "trace-001", "user01", Instant.now(), LogEventType.ACCESS, "127.0.0.1", "/test", "{}", "OK");

        assertThatCode(() -> adapter.record(event)).doesNotThrowAnyException();
    }
}
