package org.example.springadminv2.log;

import java.util.List;

import org.example.springadminv2.global.log.adapter.LogbackLogEventAdapter;
import org.example.springadminv2.global.log.adapter.RdbLogEventAdapter;
import org.example.springadminv2.global.log.port.LogEventPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class LogEventConfigTest {

    @Autowired
    private List<LogEventPort> adapters;

    @Test
    @DisplayName("RDB enabled=true → RdbLogEventAdapter + LogbackLogEventAdapter 활성화")
    void rdbEnabled_bothAdaptersActive() {
        assertThat(adapters).hasSize(2);
        assertThat(adapters)
                .anyMatch(a -> a instanceof RdbLogEventAdapter)
                .anyMatch(a -> a instanceof LogbackLogEventAdapter);
    }
}
