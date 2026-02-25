package org.example.springadminv2.log;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.example.springadminv2.global.log.adapter.CompositeLogEventAdapter;
import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.LogEvent;
import org.example.springadminv2.global.log.event.LogEventType;
import org.example.springadminv2.global.log.port.LogEventPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class LogEventPortTest {

    @Test
    @DisplayName("CompositeAdapter가 모든 adapter에 이벤트 전파")
    void compositeAdapter_propagatesToAll() {
        List<LogEvent> recorded1 = new ArrayList<>();
        List<LogEvent> recorded2 = new ArrayList<>();

        LogEventPort adapter1 = recorded1::add;
        LogEventPort adapter2 = recorded2::add;

        CompositeLogEventAdapter composite = new CompositeLogEventAdapter(List.of(adapter1, adapter2));

        AccessLogEvent event = new AccessLogEvent(
                "trace-1", "user01", Instant.now(), LogEventType.ACCESS, "127.0.0.1", "/test", null, "OK");

        composite.record(event);

        assertThat(recorded1).hasSize(1);
        assertThat(recorded2).hasSize(1);
        assertThat(recorded1.get(0)).isEqualTo(event);
    }

    @Test
    @DisplayName("개별 adapter 실패 시 다른 adapter는 계속 동작")
    void compositeAdapter_continuesOnFailure() {
        List<LogEvent> recorded = new ArrayList<>();

        LogEventPort failingAdapter = event -> {
            throw new RuntimeException("adapter error");
        };
        LogEventPort workingAdapter = recorded::add;

        CompositeLogEventAdapter composite = new CompositeLogEventAdapter(List.of(failingAdapter, workingAdapter));

        AccessLogEvent event = new AccessLogEvent(
                "trace-2", "user01", Instant.now(), LogEventType.ACCESS, "127.0.0.1", "/test", null, "OK");

        assertThatNoException().isThrownBy(() -> composite.record(event));
        assertThat(recorded).hasSize(1);
    }
}
