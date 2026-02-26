package org.example.springadminv2.global.util;

import java.util.UUID;

import org.slf4j.MDC;

public final class TraceIdUtil {

    private TraceIdUtil() {}

    public static String getOrGenerate() {
        String id = MDC.get("traceId");
        return (id != null) ? id : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
