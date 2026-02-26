package org.example.springadminv2.global.log.port;

import org.example.springadminv2.global.log.event.LogEvent;

public interface LogEventPort {
    void record(LogEvent event);
}
