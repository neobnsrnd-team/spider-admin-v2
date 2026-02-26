package org.example.springadminv2.exception;

import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.exception.BusinessException;
import org.example.springadminv2.global.exception.ErrorType;
import org.example.springadminv2.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
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
    }

    @Test
    @DisplayName("BusinessException FORBIDDEN → 403")
    void businessException_forbidden() throws Exception {
        mockMvc.perform(get("/test-exception/forbidden"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("BusinessException 5xx → 502 응답")
    void businessException_5xx_returnsCorrectStatus() throws Exception {
        mockMvc.perform(get("/test-exception/external-error"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error.code").value("EXTERNAL_SERVICE_ERROR"));
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
    @DisplayName("최후 방어선 Exception → 500")
    void unexpectedException_returnsInternalServerError() throws Exception {
        mockMvc.perform(get("/test-exception/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty());
    }

    @Test
    @DisplayName("DataIntegrityViolation ORA-00001 → 409 DUPLICATE_RESOURCE")
    void dataIntegrityOra_returnsDuplicate() throws Exception {
        mockMvc.perform(get("/test-exception/data-integrity-oracle"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("DataIntegrityViolation MySQL Duplicate entry → 409 DUPLICATE_RESOURCE")
    void dataIntegrityMysql_returnsDuplicate() throws Exception {
        mockMvc.perform(get("/test-exception/data-integrity-mysql"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
    }

    @Test
    @DisplayName("DataIntegrityViolation 일반 → 400 DATA_INTEGRITY_VIOLATION")
    void dataIntegrityGeneric_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/test-exception/data-integrity-generic"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("DATA_INTEGRITY_VIOLATION"));
    }

    @Test
    @DisplayName("BusinessException INTERNAL_ERROR (5xx) → 500")
    void businessException_internalError() throws Exception {
        mockMvc.perform(get("/test-exception/internal-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
    }

    @Test
    @DisplayName("BusinessException INVALID_INPUT → 400")
    void businessException_invalidInput() throws Exception {
        mockMvc.perform(get("/test-exception/invalid-input"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }

    @Test
    @DisplayName("BusinessException INVALID_STATE → 409")
    void businessException_invalidState() throws Exception {
        mockMvc.perform(get("/test-exception/invalid-state"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATED"));
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

        @GetMapping("/data-integrity-oracle")
        public ApiResponse<String> dataIntegrityOracle() {
            throw new DataIntegrityViolationException(
                    "could not execute statement", new RuntimeException("ORA-00001: unique constraint violated"));
        }

        @GetMapping("/data-integrity-mysql")
        public ApiResponse<String> dataIntegrityMysql() {
            throw new DataIntegrityViolationException(
                    "could not execute statement", new RuntimeException("Duplicate entry 'foo' for key 'UK_name'"));
        }

        @GetMapping("/data-integrity-generic")
        public ApiResponse<String> dataIntegrityGeneric() {
            throw new DataIntegrityViolationException(
                    "could not execute statement", new RuntimeException("NOT NULL constraint failed"));
        }

        @GetMapping("/internal-error")
        public ApiResponse<String> internalError() {
            throw new BusinessException(ErrorType.INTERNAL_ERROR);
        }

        @GetMapping("/invalid-input")
        public ApiResponse<String> invalidInput() {
            throw new BusinessException(ErrorType.INVALID_INPUT);
        }

        @GetMapping("/invalid-state")
        public ApiResponse<String> invalidState() {
            throw new BusinessException(ErrorType.INVALID_STATE);
        }
    }

    @Getter
    @Setter
    static class TestRequest {
        @NotBlank(message = "이름은 필수입니다.")
        private String name;
    }
}
