package org.example.springadminv2.global.log.listener;

import org.example.springadminv2.global.log.event.SecurityLogEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SecurityLogEventListener {

    private static final Logger auditLogger = LoggerFactory.getLogger("audit");

    @Async("logEventExecutor")
    @EventListener
    public void handle(SecurityLogEvent event) {
        if (event.success()) {
            auditLogger.info(
                    "[SECURITY] action={} userId={} clientIp={} traceId={}",
                    event.action(),
                    event.userId(),
                    event.clientIp(),
                    event.traceId());
        } else {
            auditLogger.warn(
                    "[SECURITY] action={} userId={} clientIp={} detail={} traceId={}",
                    event.action(),
                    event.userId(),
                    event.clientIp(),
                    event.detail(),
                    event.traceId());
        }
    }
}
