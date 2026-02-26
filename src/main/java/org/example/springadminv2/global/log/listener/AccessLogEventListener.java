package org.example.springadminv2.global.log.listener;

import org.example.springadminv2.global.log.event.AccessLogEvent;
import org.example.springadminv2.global.log.mapper.AccessLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AccessLogEventListener {

    private static final Logger auditLogger = LoggerFactory.getLogger("audit");
    private final AccessLogMapper accessLogMapper;
    private final boolean rdbEnabled;

    public AccessLogEventListener(
            AccessLogMapper accessLogMapper, @Value("${log.event.rdb.enabled:false}") boolean rdbEnabled) {
        this.accessLogMapper = accessLogMapper;
        this.rdbEnabled = rdbEnabled;
    }

    @Async("logEventExecutor")
    @EventListener
    public void handle(AccessLogEvent event) {
        auditLogger.info(
                "[ACCESS] traceId={} userId={} ip={} url={} result={}",
                event.traceId(),
                event.userId(),
                event.accessIp(),
                event.accessUrl(),
                event.resultMessage());

        if (rdbEnabled) {
            try {
                accessLogMapper.insertAccessLog(event);
            } catch (Exception e) {
                log.warn("접근 로그 DB 기록 실패: traceId={}", event.traceId(), e);
            }
        }
    }
}
