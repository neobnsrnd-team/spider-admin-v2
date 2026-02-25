package org.example.springadminv2.global.log.adapter;

import org.example.springadminv2.global.log.event.LogEvent;
import org.example.springadminv2.global.log.port.LogEventPort;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class LogbackLogEventAdapter implements LogEventPort {

    @Override
    public void record(LogEvent event) {
        log.info(
                "[{}] traceId={} userId={} type={}",
                event.getClass().getSimpleName(),
                event.traceId(),
                event.userId(),
                event.type());
    }
}
