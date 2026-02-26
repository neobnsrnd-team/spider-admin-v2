package org.example.springadminv2.log;

import org.example.springadminv2.global.log.event.SecurityLogEvent;
import org.example.springadminv2.global.log.listener.SecurityLogEventListener;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;

class SecurityLogEventListenerTest {

    private final SecurityLogEventListener listener = new SecurityLogEventListener();

    @Test
    @DisplayName("SecurityLogEvent(success=true) 처리 시 예외 없음")
    void handle_successEvent_noException() {
        SecurityLogEvent event =
                new SecurityLogEvent("trace-001", "user01", "LOGIN", true, "192.168.1.1", "Login successful");

        assertThatNoException().isThrownBy(() -> listener.handle(event));
    }

    @Test
    @DisplayName("SecurityLogEvent(success=false) 처리 시 예외 없음")
    void handle_failureEvent_noException() {
        SecurityLogEvent event = new SecurityLogEvent(
                "trace-002", "ANONYMOUS", "AUTHENTICATION_FAILURE", false, "192.168.1.1", "Bad credentials");

        assertThatNoException().isThrownBy(() -> listener.handle(event));
    }
}
