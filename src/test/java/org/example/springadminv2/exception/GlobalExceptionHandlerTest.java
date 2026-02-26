package org.example.springadminv2.exception;

import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.exception.BusinessException;
import org.example.springadminv2.global.exception.ErrorType;
import org.example.springadminv2.global.exception.GlobalExceptionHandler;
import org.example.springadminv2.global.log.adapter.CompositeLogEventAdapter;
import org.example.springadminv2.global.log.event.ErrorLogEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    private CompositeLogEventAdapter logEventAdapter;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler(logEventAdapter))
                .build();
    }

    @Test
    @DisplayName("BusinessException → 적절한 HTTP 상태 + JSON 응답")
    void businessException_returnsCorrectStatusAndJson() throws Exception {
        mockMvc.perform(get("/test-exception/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value(ErrorType.RESOURCE_NOT_FOUND.getMessage()))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());

        // 4xx는 ERROR 이벤트를 기록하지 않는다
        verify(logEventAdapter, never()).record(any(ErrorLogEvent.class));
    }

    @Test
    @DisplayName("BusinessException FORBIDDEN → 403, ERROR 이벤트 미기록")
    void businessException_forbidden() throws Exception {
        mockMvc.perform(get("/test-exception/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));

        verify(logEventAdapter, never()).record(any(ErrorLogEvent.class));
    }

    @Test
    @DisplayName("BusinessException 5xx → ERROR 이벤트 기록")
    void businessException_5xx_recordsErrorEvent() throws Exception {
        mockMvc.perform(get("/test-exception/external-error"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("EXTERNAL_SERVICE_ERROR"));

        verify(logEventAdapter).record(any(ErrorLogEvent.class));
    }

    @Test
    @DisplayName("MethodArgumentNotValidException → 400 + fields")
    void validationException_returnsBadRequestWithFields() throws Exception {
        String body = "{}";

        mockMvc.perform(post("/test-exception/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fields").isArray())
                .andExpect(jsonPath("$.error.fields[0].field").value("name"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("최후 방어선 Exception → 500 + ERROR 이벤트 기록")
    void unexpectedException_returnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test-exception/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());

        verify(logEventAdapter).record(any(ErrorLogEvent.class));
    }

    // ─── 테스트 전용 컨트롤러 ─────────────────────────────────────
    @RestController
    @RequestMapping("/test-exception")
    @Validated
    static class TestController {

        @GetMapping("/not-found")
        public ApiResponse<String> notFound() {
            throw new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "testId=1");
        }

        @GetMapping("/forbidden")
        public ApiResponse<String> forbidden() {
            throw new BusinessException(ErrorType.FORBIDDEN);
        }

        @GetMapping("/external-error")
        public ApiResponse<String> externalError() {
            throw new BusinessException(ErrorType.EXTERNAL_SERVICE_ERROR, "service=payment");
        }

        @PostMapping("/validate")
        public ApiResponse<String> validate(@Valid @RequestBody TestRequest request) {
            return ApiResponse.success("ok");
        }

        @GetMapping("/unexpected")
        public ApiResponse<String> unexpected() {
            throw new RuntimeException("unexpected error");
        }
    }

    @Getter
    @Setter
    static class TestRequest {
        @NotBlank(message = "이름은 필수입니다.")
        private String name;
    }
}
