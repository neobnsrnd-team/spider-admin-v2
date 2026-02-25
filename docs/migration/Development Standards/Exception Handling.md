# Exception Handling

## 1. 개요

예외는 **`BusinessException` + `ErrorType`** 조합으로 처리한다. `ErrorType`은 클라이언트에 노출되는 `ErrorCode`, 사용자 메시지, 로그 레벨을 포함한다.

> 공통 응답 구조(`ApiResponse<T>`, `ErrorDetail`)와 검증 위치 전략은 API Design 4절, 6.3절 참고.

```
throw new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "orderId=" + id)
                  │
                  ▼
    GlobalExceptionHandler
          ├── ErrorType.logLevel  → 로그 출력
          ├── ErrorType.errorCode → 응답 code 필드
          └── ErrorType.message   → 응답 message 필드
```

---

## 2. 구성 요소

### 2.1 ErrorCode — 클라이언트 계약

클라이언트가 분기 처리에 사용하는 coarse-grained 식별자다.
항목 수를 최소화하고, 의미가 명확히 다를 때만 추가한다.

```java
public enum ErrorCode {
    RESOURCE_NOT_FOUND,
    DUPLICATE_RESOURCE,
    DATA_INTEGRITY_VIOLATION,
    INVALID_INPUT,
    BUSINESS_RULE_VIOLATED,
    UNAUTHORIZED,
    FORBIDDEN,
    EXTERNAL_SERVICE_ERROR,
    INTERNAL_ERROR
}
```

> 클라이언트는 `error.code`로 분기한다. `"주문을 찾을 수 없습니다"`와 `"사용자를 찾을 수 없습니다"` 모두 `RESOURCE_NOT_FOUND`다. 클라이언트가 코드를 기반으로 다르게 동작해야 할 때만 새 `ErrorCode`를 추가한다.

### 2.2 LogLevel

```java
public enum LogLevel { DEBUG, INFO, WARN, ERROR }
```

### 2.3 ErrorType — 내부 정의

`ErrorCode` + 사용자 메시지 + HTTP 상태 + 로그 레벨을 한 곳에서 정의한다.
여러 `ErrorType`이 동일한 `ErrorCode`를 공유할 수 있다.

```java
@Getter
@RequiredArgsConstructor
public enum ErrorType {

    // ─── RESOURCE_NOT_FOUND ────────────────────────────────────────
    RESOURCE_NOT_FOUND(
        ErrorCode.RESOURCE_NOT_FOUND,
        "요청한 리소스를 찾을 수 없습니다.",
        HttpStatus.NOT_FOUND,
        LogLevel.WARN),

    // ─── DUPLICATE_RESOURCE ────────────────────────────────────────
    DUPLICATE_RESOURCE(
        ErrorCode.DUPLICATE_RESOURCE,
        "이미 존재하는 리소스입니다.",
        HttpStatus.CONFLICT,
        LogLevel.WARN),

    // ─── BUSINESS_RULE_VIOLATED ────────────────────────────────────
    INVALID_STATE(
        ErrorCode.BUSINESS_RULE_VIOLATED,
        "현재 상태에서 수행할 수 없는 작업입니다.",
        HttpStatus.CONFLICT,
        LogLevel.INFO),

    INSUFFICIENT_RESOURCE(
        ErrorCode.BUSINESS_RULE_VIOLATED,
        "리소스가 부족합니다.",
        HttpStatus.UNPROCESSABLE_ENTITY,
        LogLevel.INFO),

    // ─── INVALID_INPUT ─────────────────────────────────────────────
    INVALID_INPUT(
        ErrorCode.INVALID_INPUT,
        "입력값이 유효하지 않습니다.",
        HttpStatus.BAD_REQUEST,
        LogLevel.DEBUG),

    // ─── AUTH ───────────────────────────────────────────────────────
    UNAUTHORIZED(
        ErrorCode.UNAUTHORIZED,
        "인증이 필요합니다.",
        HttpStatus.UNAUTHORIZED,
        LogLevel.WARN),

    FORBIDDEN(
        ErrorCode.FORBIDDEN,
        "권한이 없습니다.",
        HttpStatus.FORBIDDEN,
        LogLevel.WARN),

    // ─── SERVER ERRORS ─────────────────────────────────────────────
    EXTERNAL_SERVICE_ERROR(
        ErrorCode.EXTERNAL_SERVICE_ERROR,
        "외부 서비스에 일시적인 오류가 발생했습니다.",
        HttpStatus.BAD_GATEWAY,
        LogLevel.ERROR),

    INTERNAL_ERROR(
        ErrorCode.INTERNAL_ERROR,
        "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.",
        HttpStatus.INTERNAL_SERVER_ERROR,
        LogLevel.ERROR);

    private final ErrorCode errorCode;
    private final String message;
    private final HttpStatus httpStatus;
    private final LogLevel logLevel;
}
```

위 항목들은 초기 세팅값이다. 프로젝트가 성장하면서 아래 기준으로 항목을 추가한다.

**도메인 특화 항목 추가 기준:**

| 기준 | 예시 |
|------|------|
| 범용 메시지로 UX가 불충분한 경우 | `"이미 존재하는 리소스입니다."` → `"이미 사용 중인 이메일입니다."` |
| HTTP 상태가 범용과 다른 경우 | 비즈니스 규칙 위반이지만 `409`가 아닌 `422`를 써야 하는 경우 |
| 로그 레벨을 달리해야 하는 경우 | 특정 오류는 `INFO`가 아닌 `ERROR`로 기록해야 하는 경우 |

```java
// ErrorType.java 추가 예시
ORDER_ALREADY_CANCELLED(
    ErrorCode.BUSINESS_RULE_VIOLATED,
    "이미 취소된 주문입니다.",
    HttpStatus.CONFLICT,
    LogLevel.INFO),
```

### 2.4 BusinessException

```java
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorType errorType;

    public BusinessException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }

    /**
     * @param detail 로그 식별용 파라미터 — 클라이언트에 노출되지 않는다.
     *               e.g., "orderId=ORD-001"
     */
    public BusinessException(ErrorType errorType, String detail) {
        super(errorType.getMessage() + " [" + detail + "]");
        this.errorType = errorType;
    }

    public BusinessException(ErrorType errorType, Throwable cause) {
        super(errorType.getMessage(), cause);
        this.errorType = errorType;
    }

    public BusinessException(ErrorType errorType, String detail, Throwable cause) {
        super(errorType.getMessage() + " [" + detail + "]", cause);
        this.errorType = errorType;
    }
}
```

---

## 3. GlobalExceptionHandler

> `ApiResponse<T>`, `ErrorDetail` 클래스 정의는 API Design 4절 참고.

> 로그 레벨 규칙(4xx=WARN, 5xx=ERROR)은 Logging 3절과 동일 원칙이다.

```java
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 1순위: BusinessException ─────────────────────────────────
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        ErrorType type = ex.getErrorType();
        String traceId = currentTraceId();

        logByType(type, traceId, ex);

        return ResponseEntity
                .status(type.getHttpStatus())
                .body(ApiResponse.error(ErrorDetail.builder()
                        .code(type.getErrorCode().name())
                        .message(type.getMessage())
                        .traceId(traceId)
                        .build()));
    }

    // ─── 2순위: Bean Validation (@Valid) ──────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex) {

        String traceId = currentTraceId();

        List<ErrorDetail.FieldError> fields = ex.getBindingResult()
                .getFieldErrors().stream()
                .map(fe -> ErrorDetail.FieldError.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        log.debug("[{}] Validation failed: fields={}", traceId, fields);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorDetail.builder()
                        .code(ErrorCode.INVALID_INPUT.name())
                        .message(ErrorType.INVALID_INPUT.getMessage())
                        .traceId(traceId)
                        .fields(fields)
                        .build()));
    }

    // ─── 3순위: DB 제약 조건 위반 ──────────────────────────────────
    // 서비스 레이어에서 먼저 처리하는 것이 원칙 (5.4절 참고).
    // DataIntegrityViolationException은 중복 키 외에도 NOT NULL, 데이터 타입 불일치 등
    // 다양한 원인으로 발생하므로 원인별로 분기한다.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex) {
        String traceId = currentTraceId();
        String message = ex.getMostSpecificCause().getMessage();
        log.warn("[{}] DataIntegrityViolationException: {}", traceId, message);

        // ORA-00001: unique constraint violated → 중복 키
        if (message != null && message.contains("ORA-00001")) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ApiResponse.error(ErrorDetail.builder()
                            .code(ErrorCode.DUPLICATE_RESOURCE.name())
                            .message(ErrorType.DUPLICATE_RESOURCE.getMessage())
                            .traceId(traceId)
                            .build()));
        }

        // 그 외 제약 조건 위반 (NOT NULL, FK, CHECK 등) → 400 Bad Request
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorDetail.builder()
                        .code(ErrorCode.DATA_INTEGRITY_VIOLATION.name())
                        .message("데이터 무결성 제약 조건 위반")
                        .traceId(traceId)
                        .build()));
    }

    // ─── 4순위: 최후 방어선 ──────────────────────────────────────
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        String traceId = currentTraceId();
        log.error("[{}] Unexpected error: {}", traceId, ex.getMessage(), ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error(ErrorDetail.builder()
                        .code(ErrorCode.INTERNAL_ERROR.name())
                        .message(ErrorType.INTERNAL_ERROR.getMessage())
                        .traceId(traceId)
                        .build()));
    }

    // ─── 유틸리티 ─────────────────────────────────────────────────
    private void logByType(ErrorType type, String traceId, Exception ex) {
        String fmt = "[{}] {}: {}";
        switch (type.getLogLevel()) {
            case DEBUG -> log.debug(fmt, traceId, type.name(), ex.getMessage());
            case INFO  -> log.info(fmt, traceId, type.name(), ex.getMessage());
            case WARN  -> log.warn(fmt, traceId, type.name(), ex.getMessage());
            case ERROR -> log.error(fmt, traceId, type.name(), ex.getMessage(), ex);
        }
    }

    private String currentTraceId() {
        // Micrometer Tracing / Sleuth 사용 시 MDC에 자동 설정됨
        String id = MDC.get("traceId");
        return (id != null) ? id : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
```

---

## 4. 파일 구조

```
src/main/java/{base-package}/
│
├── global/exception/
│   ├── ErrorCode.java              # ErrorCode enum — 클라이언트 계약
│   ├── ErrorType.java              # ErrorType enum — 내부 정의
│   ├── LogLevel.java               # LogLevel enum
│   ├── BusinessException.java
│   └── GlobalExceptionHandler.java
│
└── global/dto/
    ├── ApiResponse.java            # API Design 4절 참고
    └── ErrorDetail.java            # API Design 4절 참고
```

도메인별 별도 파일은 만들지 않는다. `ErrorType`에 항목을 추가하는 것으로 확장한다.

---

## 5. 서비스 레이어 패턴

### 5.1 기본 사용

```java
// 범용 타입 사용
throw new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "orderId=" + orderId);
throw new BusinessException(ErrorType.INVALID_STATE);

// 도메인 특화 타입 사용 (2.3절 추가 예시 참고)
throw new BusinessException(ErrorType.ORDER_ALREADY_CANCELLED);

// 외부 예외 래핑
throw new BusinessException(ErrorType.EXTERNAL_SERVICE_ERROR, "service=payment", cause);
```

### 5.2 조회 패턴

```java
private Order findOrderOrThrow(String orderId) {
    return Optional.ofNullable(orderMapper.selectById(orderId))
            .orElseThrow(() -> new BusinessException(
                    ErrorType.RESOURCE_NOT_FOUND, "orderId=" + orderId));
}
```

### 5.3 Guard Clause 패턴

```java
@Transactional
public void cancelOrder(String orderId) {
    Order order = findOrderOrThrow(orderId);

    if (order.isCancelled())
        throw new BusinessException(ErrorType.ORDER_ALREADY_CANCELLED);

    if (!order.isCancellable())
        throw new BusinessException(ErrorType.INVALID_STATE);

    order.cancel();
}
```

### 5.4 DB 중복 키 처리

```java
// ✅ 서비스에서 직접 처리 — 어떤 필드가 중복인지 명확히 전달 가능
@Transactional
public void createUser(UserCreateRequest req) {
    try {
        userMapper.insert(toEntity(req));
    } catch (DataIntegrityViolationException e) {
        throw new BusinessException(ErrorType.DUPLICATE_EMAIL);
    }
}
```

---

## 6. 외부 시스템 예외 처리

외부 API 예외는 `BusinessException`으로 래핑하여 내부 구조를 숨긴다.

```java
public PaymentResult charge(PaymentRequest req) {
    try {
        return paymentClient.charge(req);
    } catch (Exception e) {
        log.error("Payment API error: {}", e.getMessage(), e);
        throw new BusinessException(ErrorType.EXTERNAL_SERVICE_ERROR, "provider=payment", e);
    }
}
```

---

## 7. 비동기 예외 처리

### 7.1 @Async

`@Async` 메서드의 예외는 `GlobalExceptionHandler`에 도달하지 않는다.

```java
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (ex, method, params) ->
            log.error("Async task failed — method={}, params={}: {}",
                      method.getName(), Arrays.toString(params), ex.getMessage(), ex);
    }
}
```

### 7.2 @Scheduled

```java
@Scheduled(cron = "0 0 2 * * *")
public void dailySync() {
    try {
        syncService.run();
    } catch (Exception e) {
        log.error("Scheduled task failed: {}", e.getMessage(), e);
    }
}
```

---

## 8. 커스텀 Validator

> 검증 위치 전략(형식→DTO, 비즈니스→Service, DB→Service)은 API Design 6.3절 참고.

반복되는 형식 검증은 어노테이션으로 추출한다.

```java
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PhoneNumberValidator.class)
public @interface ValidPhoneNumber {
    String message() default "올바른 전화번호 형식이 아닙니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
```

---

## 9. traceId 설정

Logging 4절 (MDC 추적 컨텍스트)이 프로젝트 단일 표준이다. Spring Boot 3.x + Micrometer Tracing 사용 시 MDC에 `traceId`가 자동 설정된다. 미사용 환경에서는 Logging 4.2절의 생성 규칙을 따르는 필터를 구현한다.

---

## 10. 테스트

### 10.1 Service 단위 테스트

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderMapper orderMapper;
    @InjectMocks private OrderService orderService;

    @Test
    @DisplayName("존재하지 않는 주문 취소 시 RESOURCE_NOT_FOUND")
    void cancelOrder_notFound() {
        given(orderMapper.selectById("ORD-999")).willReturn(null);

        assertThatThrownBy(() -> orderService.cancelOrder("ORD-999"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorType())
                        .isEqualTo(ErrorType.RESOURCE_NOT_FOUND));
    }

    @Test
    @DisplayName("이미 취소된 주문 재취소 시 ORDER_ALREADY_CANCELLED")
    void cancelOrder_alreadyCancelled() {
        // ORDER_ALREADY_CANCELLED는 2.3절 추가 예시 기준으로 ErrorType.java에 등록된 항목
        Order cancelled = Order.builder().status(OrderStatus.CANCELLED).build();
        given(orderMapper.selectById("ORD-001")).willReturn(cancelled);

        assertThatThrownBy(() -> orderService.cancelOrder("ORD-001"))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorType())
                        .isEqualTo(ErrorType.ORDER_ALREADY_CANCELLED));
    }
}
```

### 10.2 Controller 통합 테스트

```java
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private OrderService orderService;

    @Test
    @DisplayName("존재하지 않는 주문 조회 시 404 + RESOURCE_NOT_FOUND")
    void getOrder_notFound() throws Exception {
        given(orderService.getById("ORD-999"))
                .willThrow(new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "orderId=ORD-999"));

        mockMvc.perform(get("/api/orders/ORD-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.error.traceId").isNotEmpty())
                .andExpect(jsonPath("$.error.message").value(ErrorType.RESOURCE_NOT_FOUND.getMessage()));
    }

    @Test
    @DisplayName("잘못된 입력 시 400 + INVALID_INPUT + 필드 오류")
    void createOrder_validationFailed() throws Exception {
        String body = """{ "productId": "", "quantity": -1 }""";

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.error.fields").isArray())
                .andExpect(jsonPath("$.error.fields.length()").value(greaterThan(0)));
    }
}
```

### 10.3 테스트 체크리스트

- [ ] 정상 동작 (Happy path)
- [ ] 리소스 없음 → 404 + `RESOURCE_NOT_FOUND`
- [ ] 중복 리소스 → 409 + `DUPLICATE_RESOURCE`
- [ ] 상태 충돌 → 409/422 + `BUSINESS_RULE_VIOLATED`
- [ ] 입력값 검증 실패 → 400 + `INVALID_INPUT` + `fields[]`
- [ ] `error.message`에 내부 식별자 미포함 확인
- [ ] `error.traceId` 존재 확인

---

## 11. 체크리스트

```
□ 1. 기존 ErrorType 항목으로 처리 가능한지 먼저 확인
      → 범용 타입이 적합하면 추가 없이 그대로 사용

□ 2. 새 ErrorType이 필요하면 ErrorType.java에만 추가
      → ErrorCode 추가는 클라이언트가 코드를 기반으로
        다르게 동작해야 할 때만 고려

□ 3. Service에서 BusinessException으로 발생
      → detail 파라미터에 식별자 포함 (클라이언트 비노출)

□ 4. GlobalExceptionHandler 수정 금지

□ 5. 테스트 작성 (10.3절 체크리스트)

□ 6. PR 코멘트에 신규 ErrorType 항목 기록
      → 기존 항목으로 대체 가능한지 리뷰어 확인
```

---

*Last updated: 2026-02-24*
