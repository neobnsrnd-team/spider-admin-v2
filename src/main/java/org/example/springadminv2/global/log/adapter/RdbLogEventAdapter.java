package org.example.springadminv2.global.log.adapter;

import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.event.LogEvent;
import org.example.springadminv2.global.log.mapper.AccessLogMapper;
import org.example.springadminv2.global.log.port.LogEventPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "log.event.rdb.enabled", havingValue = "true")
public class RdbLogEventAdapter implements LogEventPort {

    private final AccessLogMapper accessLogMapper;

    @Override
    public void record(LogEvent event) {
        if (event instanceof AccessLogEvent accessLog) {
            accessLogMapper.insertAccessLog(accessLog);
        }
    }
}
