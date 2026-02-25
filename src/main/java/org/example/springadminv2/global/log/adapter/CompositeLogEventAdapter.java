package org.example.springadminv2.global.log.adapter;

import java.util.List;

import org.example.springadminv2.global.log.event.LogEvent;
import org.example.springadminv2.global.log.port.LogEventPort;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompositeLogEventAdapter {

    private final List<LogEventPort> adapters;

    public void record(LogEvent event) {
        for (LogEventPort adapter : adapters) {
            try {
                adapter.record(event);
            } catch (Exception e) {
                log.warn("LogEventPort adapter failed: {}", adapter.getClass().getSimpleName(), e);
            }
        }
    }
}
