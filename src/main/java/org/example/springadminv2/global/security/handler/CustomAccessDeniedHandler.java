package org.example.springadminv2.global.security.handler;

import java.io.IOException;

import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.dto.ErrorDetail;
import org.example.springadminv2.global.exception.ErrorCode;
import org.example.springadminv2.global.exception.ErrorType;
import org.example.springadminv2.global.log.adapter.CompositeLogEventAdapter;
import org.example.springadminv2.global.log.event.SecurityLogEvent;
import org.example.springadminv2.global.util.TraceIdUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final CompositeLogEventAdapter logEventAdapter;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper, CompositeLogEventAdapter logEventAdapter) {
        this.objectMapper = objectMapper;
        this.logEventAdapter = logEventAdapter;
    }

    @Override
    public void handle(
            HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {

        recordSecurityEvent(request, accessDeniedException);

        if (isAjaxRequest(request)) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> body = ApiResponse.error(ErrorDetail.builder()
                    .code(ErrorCode.FORBIDDEN.name())
                    .message(ErrorType.FORBIDDEN.getMessage())
                    .build());

            objectMapper.writeValue(response.getOutputStream(), body);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    private void recordSecurityEvent(HttpServletRequest request, AccessDeniedException ex) {
        try {
            String traceId = TraceIdUtil.getOrGenerate();

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = (auth != null && auth.getName() != null) ? auth.getName() : "ANONYMOUS";

            logEventAdapter.record(new SecurityLogEvent(
                    traceId,
                    userId,
                    "ACCESS_DENIED",
                    false,
                    request.getRemoteAddr(),
                    "uri=" + request.getRequestURI() + ", " + ex.getMessage()));
        } catch (Exception e) {
            log.warn("Failed to record security event: {}", e.getMessage());
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}
