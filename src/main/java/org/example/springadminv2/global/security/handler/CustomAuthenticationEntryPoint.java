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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;
    private final CompositeLogEventAdapter logEventAdapter;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper, CompositeLogEventAdapter logEventAdapter) {
        this.objectMapper = objectMapper;
        this.logEventAdapter = logEventAdapter;
    }

    @Override
    public void commence(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {

        recordSecurityEvent(request, authException);

        if (isAjaxRequest(request)) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");

            ApiResponse<Void> body = ApiResponse.error(ErrorDetail.builder()
                    .code(ErrorCode.UNAUTHORIZED.name())
                    .message(ErrorType.UNAUTHORIZED.getMessage())
                    .build());

            objectMapper.writeValue(response.getOutputStream(), body);
        } else {
            response.sendRedirect(request.getContextPath() + "/login");
        }
    }

    private void recordSecurityEvent(HttpServletRequest request, AuthenticationException ex) {
        try {
            String traceId = TraceIdUtil.getOrGenerate();

            logEventAdapter.record(new SecurityLogEvent(
                    traceId, "ANONYMOUS", "AUTHENTICATION_FAILURE", false, request.getRemoteAddr(), ex.getMessage()));
        } catch (Exception e) {
            log.warn("Failed to record security event: {}", e.getMessage());
        }
    }

    private boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }
}
