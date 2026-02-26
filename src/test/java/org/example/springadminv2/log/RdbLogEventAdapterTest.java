package org.example.springadminv2.log;

import java.time.Instant;

import org.example.springadminv2.global.log.adapter.RdbLogEventAdapter;
import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.LogEventType;
import org.example.springadminv2.global.log.mapper.AccessLogMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RdbLogEventAdapterTest {

    @Autowired
    private RdbLogEventAdapter rdbLogEventAdapter;

    @Autowired
    private AccessLogMapper accessLogMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("AccessLogEvent → FWK_USER_ACCESS_HIS INSERT 성공")
    void insertAccessLog() {
        AccessLogEvent event = new AccessLogEvent(
                "trace-rdb-1",
                "admin",
                Instant.now(),
                LogEventType.ACCESS,
                "192.168.1.1",
                "/api/test",
                "{\"key\":\"value\"}",
                "SUCCESS");

        rdbLogEventAdapter.record(event);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM FWK_USER_ACCESS_HIS WHERE USER_ID = 'admin' AND ACCESS_URL = '/api/test'",
                Integer.class);

        assertThat(count).isEqualTo(1);
    }
}
