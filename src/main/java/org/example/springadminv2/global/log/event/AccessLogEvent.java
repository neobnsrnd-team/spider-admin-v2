package org.example.springadminv2.global.log.event;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public record AccessLogEvent(
        String traceId,
        String userId,
        Instant timestamp,
        LogEventType type,
        String accessIp,
        String accessUrl,
        String inputData,
        String resultMessage)
        implements LogEvent {

    private static final DateTimeFormatter DTIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault());

    public String accessDtime() {
        return DTIME_FORMAT.format(timestamp);
    }
}
