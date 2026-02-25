# Logging

## 1. 개요

### 1.1 두 계층 아키텍처

로깅은 목적이 다른 두 계층으로 분리한다. 두 계층은 독립적으로 동작하며, `LogbackLogEventAdapter`를 통해 이벤트 로그를 진단 로그 채널로 위임할 수 있다.

```
Business Code
    │
    ├─[진단 로그]──► SLF4J log.*(...)
    │                   Logback이 관리 — 개발자 디버깅, 운영 진단
    │                       ├── ConsoleAppender
    │                       ├── AsyncAppender → DBAppender  (H2/RDB)
    │                       └── AsyncAppender → LogstashAppender  (ELK)
    │
    └─[이벤트 로그]──► LogEventPort.record(event)
                        포트/어댑터 패턴 — 비즈니스 감사, 분석, 규정 준수
                            ├── RdbLogEventAdapter        (Oracle/MySQL 구조화 테이블)
                            ├── ElasticsearchLogEventAdapter  (ES JSON document)
                            └── LogbackLogEventAdapter    (SLF4J 위임 — 별도 인프라 없이 동작)
```

| 계층 | 용도 | 형태 | 관리 주체 |
|------|------|------|-----------|
| 진단 로그 | 문제 추적, 성능 분석, 개발 디버깅 | 텍스트 패턴 문자열 | Logback (설정 파일) |
| 이벤트 로그 | 감사 추적, 비즈니스 분석, 규정 준수 | 타입 정의된 객체 | LogEventPort (코드) |

### 1.2 설계 원칙

1. **자동화 우선.** Controller/Service 개발자가 직접 로그를 작성하는 것을 최소화한다. AOP·인터셉터·필터가 공통 패턴을 처리한다. 개발자가 추가하는 로그는 비즈니스 의사결정이 담긴 맥락으로 한정한다.
2. **구조화.** 이벤트 로그는 필드가 정의된 타입 객체다. 문자열 포맷에 의존하지 않아, 필드 추가·변경이 파서 수정 없이 가능하다.
3. **플러그인 가능한 백엔드.** `LogEventPort` 인터페이스로 추상화한다. RDB·Elasticsearch·Kafka 등 어떤 백엔드도 설정 변경만으로 추가·교체한다.
4. **추적 가능성.** 모든 로그는 `traceId`로 연결된다. 요청 하나의 전 생애주기를 재현할 수 있어야 한다.
5. **민감 정보 차단.** 비밀번호·토큰·개인정보는 로그에 도달하기 전에 마스킹한다.

---

## 2. 로그 분류 체계

### 2.1 이벤트 타입

| 타입 | 목적 | 생성 시점 | 기본 보관 |
|------|------|-----------|-----------|
| `ACCESS` | HTTP 요청/응답 추적 | 모든 API 호출 완료 시 | 90일 |
| `AUDIT` | 데이터 변경 추적 (누가 무엇을 언제 어떻게) | 생성·수정·삭제 성공 시 | 1년 |
| `SECURITY` | 인증·인가 이벤트 | 로그인 성공/실패, 권한 거부 | 1년 |
| `ERROR` | 예외 및 시스템 오류 | 5xx 예외 발생 시 | 90일 |
| `SYSTEM` | 앱 라이프사이클 | 기동, 셧다운, 배치 실행 | 30일 |

> **4xx는 ERROR 이벤트가 아니다.** 400·401·403·404는 클라이언트 측 문제이며, 예측 가능한 정상 흐름이다. ERROR 이벤트는 서버가 처리할 수 없는 5xx 상황에만 기록한다.

---

## 3. 로그 레벨 규칙

### 3.1 레벨 정의

| 레벨 | 의미 | 예시 |
|------|------|------|
| `TRACE` | 메서드 진입/종료 (매우 상세) | 서비스 메서드 호출 흔적 |
| `DEBUG` | 진단 상세 — 개발·문제 추적 시 활성화 | SQL, 파라미터 값, 캐시 히트 여부 |
| `INFO` | 비즈니스 이벤트 — 운영 모니터링 기준 | 로그인 성공, 파일 업로드, 배치 완료 |
| `WARN` | 복구 가능한 문제 — 주의 필요 | 4xx 오류, 재시도, 잘못된 입력값 |
| `ERROR` | 복구 불가 — 즉각 조치 필요 | 5xx 예외, DB 연결 실패, 예측 불가 오류 |

### 3.2 HTTP 상태 코드별 레벨

```
2xx, 3xx  → log.info()   (정상)
4xx       → log.warn()   (클라이언트 오류 — 복구 가능)
5xx       → log.error()  (서버 오류 — 즉각 조치)
```

### 3.3 레이어별 규칙

| 레이어 | 직접 로그 작성 범위 | 자동 처리 |
|--------|-------------------|-----------|
| Controller | 금지 (특별한 경우 없음) | ApiLoggingInterceptor |
| Service | 비즈니스 의사결정·분기 (INFO) | ServiceLoggingAspect (TRACE 수준) |
| Mapper | 금지 | MyBatis SQL 로그 (DEBUG) |
| Exception Handler | 금지 — 레벨 규칙을 AOP에서 통합 관리 | GlobalExceptionHandler AOP |

```java
// 금지 — Interceptor가 자동 처리
@GetMapping
public ResponseEntity<...> list() {
    log.info("리스트 조회 요청");  // 중복
    ...
}

// 금지 — 4xx에 ERROR
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<...> handleValidation(...) {
    log.error("Validation failed", ex);  // 4xx는 WARN
    ...
}

// 서비스에서 비즈니스 맥락 로그
@Transactional
public void approve(String orderId) {
    log.info("주문 승인 처리: orderId={}, approvedBy={}", orderId, SecurityUtil.getCurrentUserId());
    ...
}
```

---

## 4. MDC 추적 컨텍스트

### 4.1 MDC 키 표준

| 키 | 설정 시점 | 설정 주체 | 예시 값 |
|----|-----------|-----------|---------|
| `traceId` | 요청 진입 | `ApiLoggingInterceptor` | `a3f8c1d2e4b59071` |
| `userId` | 인증 성공 후 | `ApiLoggingInterceptor` | `admin01` |
| `menuId` | 필요 시 (옵션) | `MenuAccessAuthorizationAspect` | `USER_MANAGEMENT` |

> **이 MDC 키 정의가 프로젝트 단일 표준이다.** 다른 문서에서 MDC 키를 참조할 때 이 표를 기준으로 한다.

```java
// Logback 패턴 — MDC 키는 %X{키명:-기본값} 형식
%d{yyyy-MM-dd HH:mm:ss.SSS} [%X{traceId:-SYSTEM}] [%X{userId:-ANONYMOUS}] %-5level %logger{36} - %msg%n
```

### 4.2 traceId 생성 규칙

```java
private String generateTraceId() {
    return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
}
```

단일 서비스 환경에서는 16자리 UUID 접두어로 충분하다. 마이크로서비스 전환 시 W3C TraceContext 형식 (`traceparent` 헤더)으로 교체한다.

### 4.3 MDC 생명주기

```
요청 진입 (preHandle)
    MDC.put("traceId", traceId)
    MDC.put("userId", userId)
    │
    └─ 요청 처리 (모든 스레드 로그에 traceId 자동 포함)
    │
요청 완료 (afterCompletion)
    MDC.clear()   ← 반드시 명시 (스레드 풀 재사용 시 오염 방지)
```

> **비동기 처리 주의.** `@Async`, `CompletableFuture`, Reactor 등 별도 스레드로 실행되는 코드는 MDC가 전파되지 않는다. 비동기 실행 직전 `MDC.getCopyOfContextMap()`으로 컨텍스트를 캡처하여 자식 스레드에서 `MDC.setContextMap()`으로 복원한다.

---

## 5. 이벤트 모델

### 5.1 최상위 인터페이스

```java
// global/log/event/LogEvent.java
public sealed interface LogEvent
        permits AccessLogEvent, AuditLogEvent, SecurityLogEvent, ErrorLogEvent, SystemLogEvent {

    String traceId();
    String userId();
    Instant timestamp();
    LogEventType type();
}

public enum LogEventType { ACCESS, AUDIT, SECURITY, ERROR, SYSTEM }
```

### 5.2 이벤트 타입별 필드

```java
// ACCESS — HTTP 요청/응답
public record AccessLogEvent(
                String traceId,
                String userId,
                Instant timestamp,
                LogEventType type,         // ACCESS
                String httpMethod,         // GET, POST, ...
                String uri,
                int statusCode,
                long latencyMs,
                String clientIp,
                String requestData,        // 마스킹된 쿼리스트링 또는 body
                String errorClass,         // 4xx/5xx 시
                String errorMessage
        ) implements LogEvent {}
```

| 이벤트 | 고유 필드 |
|--------|----------|
| `AuditLogEvent` | `action`, `entityType`, `entityId`, `beforeSnapshot`, `afterSnapshot` |
| `SecurityLogEvent` | `action`, `success`, `clientIp`, `detail` |
| `ErrorLogEvent` | `errorCode`, `errorClass`, `errorMessage`, `stackTrace`, `uri`, `httpMethod` |
| `SystemLogEvent` | `action`, `detail` |

> 모든 이벤트는 `LogEvent` 공통 필드(`traceId`, `userId`, `timestamp`, `type`)를 포함한다.

> 필드를 추가할 때 기존 `record` 대신 **새 필드를 포함한 새 record**를 만들어 기존 코드를 깨지 않는다. 어댑터가 모르는 필드는 무시한다.

---

## 6. 로그 포트 패턴

포트/어댑터 패턴으로 이벤트 백엔드를 추상화한다. 이벤트를 생성하는 코드(인터셉터, AOP, 서비스)는 `LogEventPort`만 의존하며, 어떤 백엔드가 활성화되어 있는지 알 필요가 없다.

### 6.1 포트 인터페이스

```java
// global/log/port/LogEventPort.java
public interface LogEventPort {
    void record(LogEvent event);
}
```

### 6.2 어댑터 구성

```
global/log/
├── event/
│   ├── LogEvent.java
│   ├── AccessLogEvent.java
│   ├── AuditLogEvent.java
│   ├── SecurityLogEvent.java
│   ├── ErrorLogEvent.java
│   └── SystemLogEvent.java
├── port/
│   └── LogEventPort.java
└── adapter/
    ├── CompositeLogEventAdapter.java    # 멀티 어댑터 디스패처 (Bean으로 등록되는 실체)
    ├── RdbLogEventAdapter.java          # Oracle/MySQL 구조화 테이블
    ├── ElasticsearchLogEventAdapter.java
    └── LogbackLogEventAdapter.java      # SLF4J 위임 (항상 활성)
```

### 6.3 CompositeLogEventAdapter

```java
// global/log/adapter/CompositeLogEventAdapter.java
@Component
@RequiredArgsConstructor
public class CompositeLogEventAdapter implements LogEventPort {

    private final List<LogEventPort> adapters;  // Spring이 LogEventPort 구현체 전부 주입

    @Override
    public void record(LogEvent event) {
        for (LogEventPort adapter : adapters) {
            try {
                adapter.record(event);
            } catch (Exception e) {
                // 어댑터 하나가 실패해도 나머지는 계속 실행
                log.warn("[LogPort] Adapter {} failed: {}", adapter.getClass().getSimpleName(), e.getMessage());
            }
        }
    }
}
```

### 6.4 LogbackLogEventAdapter

별도 인프라 없이 SLF4J로 위임한다. Logback 설정(Console, H2, Logstash)을 그대로 재사용한다.

```java
@Component
@Slf4j
public class LogbackLogEventAdapter implements LogEventPort {

    @Override
    public void record(LogEvent event) {
        switch (event) {
            case AccessLogEvent e  -> logAccess(e);
            case AuditLogEvent e   -> logAudit(e);
            case SecurityLogEvent e -> logSecurity(e);
            case ErrorLogEvent e   -> logError(e);
            case SystemLogEvent e  -> logSystem(e);
        }
    }

    private void logAccess(AccessLogEvent e) {
        if (e.statusCode() >= 500) {
            log.error("[ACCESS] {} {} → {} ({}ms) traceId={}", e.httpMethod(), e.uri(), e.statusCode(), e.latencyMs(), e.traceId());
        } else if (e.statusCode() >= 400) {
            log.warn("[ACCESS] {} {} → {} ({}ms) traceId={}", e.httpMethod(), e.uri(), e.statusCode(), e.latencyMs(), e.traceId());
        } else {
            log.info("[ACCESS] {} {} → {} ({}ms) traceId={}", e.httpMethod(), e.uri(), e.statusCode(), e.latencyMs(), e.traceId());
        }
    }
}
```

| 메서드 | 로그 |
|--------|------|
| `logAudit` | `INFO` — action, entityType, entityId, userId |
| `logSecurity` | 성공 `INFO` / 실패 `WARN` — action, userId, clientIp |
| `logError` | `ERROR` — httpMethod, uri, errorCode, errorClass |
| `logSystem` | `INFO` — action, detail |

### 6.5 RdbLogEventAdapter

```java
@Component
@ConditionalOnProperty(name = "app.logging.rdb.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RdbLogEventAdapter implements LogEventPort {

    private final LogEventMapper logEventMapper;  // MyBatis Mapper

    @Override
    public void record(LogEvent event) {
        switch (event) {
            case AccessLogEvent e   -> logEventMapper.insertAccess(e);
            case AuditLogEvent e    -> logEventMapper.insertAudit(e);
            case SecurityLogEvent e -> logEventMapper.insertSecurity(e);
            case ErrorLogEvent e    -> logEventMapper.insertError(e);
            case SystemLogEvent e   -> logEventMapper.insertSystem(e);
        }
    }
}
```

RDB 어댑터는 이벤트 타입별로 **전용 테이블**을 사용한다.

| 테이블 | 이벤트 타입 | 핵심 컬럼 |
|--------|-------------|-----------|
| `LOG_ACCESS` | ACCESS | `trace_id`, `user_id`, `method`, `uri`, `status_code`, `latency_ms`, `client_ip`, `request_data` |
| `LOG_AUDIT` | AUDIT | `trace_id`, `user_id`, `action`, `entity_type`, `entity_id`, `before_snapshot`, `after_snapshot` |
| `LOG_SECURITY` | SECURITY | `trace_id`, `user_id`, `action`, `success_yn`, `client_ip`, `detail` |
| `LOG_ERROR` | ERROR | `trace_id`, `user_id`, `error_code`, `error_class`, `error_message`, `stack_trace`, `uri` |
| `LOG_SYSTEM` | SYSTEM | `trace_id`, `action`, `detail` |

공통 컬럼: `id` (PK), `event_type`, `created_at`

### 6.6 ElasticsearchLogEventAdapter

```java
@Component
@ConditionalOnProperty(name = "app.logging.elasticsearch.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class ElasticsearchLogEventAdapter implements LogEventPort {

    private final RestHighLevelClient esClient;
    private final ObjectMapper objectMapper;

    @Override
    public void record(LogEvent event) {
        try {
            String index = resolveIndex(event);  // e.g., "log-access-2026.02"
            String source = objectMapper.writeValueAsString(event);
            IndexRequest request = new IndexRequest(index).source(source, XContentType.JSON);
            esClient.indexAsync(request, RequestOptions.DEFAULT, ActionListener.wrap(
                    r -> {}, e -> log.warn("[ES] Index failed: {}", e.getMessage())
            ));
        } catch (Exception e) {
            log.warn("[ES] Record failed: {}", e.getMessage());
        }
    }

    private String resolveIndex(LogEvent event) {
        String month = YearMonth.now().toString();  // "2026-02"
        return "log-" + event.type().name().toLowerCase() + "-" + month;
    }
}
```

### 6.7 어댑터 활성화 설정

```yaml
# application-base.yml
app:
  logging:
    rdb:
      enabled: false        # 기본 비활성 — 환경 프로파일에서 활성화
    elasticsearch:
      enabled: false

# application-local.yml
app:
  logging:
    rdb:
      enabled: true         # 로컬: RDB만 활성 (H2 로그 DB)

# application-prod.yml (예시)
app:
  logging:
    rdb:
      enabled: true         # 운영: RDB + ES
    elasticsearch:
      enabled: true
```

### 6.8 이벤트 생성 위치

| 이벤트 타입 | 생성 위치 |
|-------------|-----------|
| `ACCESS` | `ApiLoggingInterceptor.afterCompletion()` |
| `AUDIT` | Service `@Transactional` 메서드 완료 후 (AOP 또는 명시적 호출) |
| `SECURITY` | Spring Security 이벤트 리스너 (`AuthenticationEventListener`) |
| `ERROR` | `GlobalExceptionHandler` (5xx 핸들러에서만) |
| `SYSTEM` | `ApplicationListener<ApplicationReadyEvent>` 등 |

```java
// 이벤트 생성 예시 — ApiLoggingInterceptor
@Override
public void afterCompletion(HttpServletRequest request, HttpServletResponse response, ...) {
    ...
    logEventPort.record(AccessLogEvent.builder()
            .traceId(traceId)
            .userId(userId)
            .timestamp(Instant.now())
            .type(LogEventType.ACCESS)
            .httpMethod(request.getMethod())
            .uri(request.getRequestURI())
            .statusCode(response.getStatus())
            .latencyMs(durationMs)
            .clientIp(request.getRemoteAddr())
            .requestData(maskedData)
            .build());
}

// AUDIT 이벤트 — Service에서 명시적 호출
@Transactional
public UserResponseDTO update(String id, UserUpdateRequestDTO dto) {
    UserResponseDTO before = userMapper.selectById(id);
    // ... update logic
    userMapper.update(id, dto);
    UserResponseDTO after = userMapper.selectById(id);  // 변경 후 재조회

    logEventPort.record(AuditLogEvent.builder()
            .traceId(MDC.get("traceId"))
            .userId(SecurityUtil.getCurrentUserId())
            .timestamp(Instant.now())
            .type(LogEventType.AUDIT)
            .action("UPDATE")
            .entityType("User")
            .entityId(id)
            .beforeSnapshot(toJson(before))    // 변경 전
            .afterSnapshot(toJson(after))      // 변경 후
            .build());
}
```

---

## 7. 진단 로그 — Logback 설정

진단 로그는 Logback이 관리하며, 개발자의 `log.*()` 호출을 처리한다.

### 7.1 레이어별 패턴

```xml
<!-- 공통 패턴 — MDC traceId, userId 포함 -->
<property name="LOG_PATTERN"
          value="%d{HH:mm:ss.SSS} [%X{traceId:-SYSTEM}] [%X{userId:-ANON}] %-5level %logger{30} - %msg%n"/>
```

### 7.2 프로파일별 구성

| 프로파일 | 어펜더 | 루트 레벨 |
|---------|--------|-----------|
| `local` | Console + H2 | INFO (app=DEBUG) |
| `ci` | Console | INFO |
| `staging` | Console + RDB | INFO |
| `prod` | Console + RDB + Logstash | WARN (app=INFO) |

```xml
<!-- prod: Console + RDB + Logstash -->
<springProfile name="prod">
    <root level="WARN">        <!-- prod은 WARN 이상만 -->
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="ASYNC_DB"/>
        <appender-ref ref="ASYNC_LOGSTASH"/>
    </root>
    <logger name="com.example.admin_demo" level="INFO"/>
</springProfile>
```

### 7.3 H2 DBAppender

H2 DBAppender는 `local` 프로파일에서만 사용한다. staging/prod에서는 RDB 어댑터(LogEventPort)가 구조화된 로그를 담당한다.

```xml
<springProfile name="local">
    <appender name="DB" class="ch.qos.logback.classic.db.DBAppender">
        <connectionSource class="ch.qos.logback.core.db.DriverManagerConnectionSource">
            <driverClass>org.h2.Driver</driverClass>
            <url>jdbc:h2:file:./logs/h2/logdb;AUTO_SERVER=TRUE;INIT=RUNSCRIPT FROM 'classpath:db/h2/schema-log.sql'</url>
            <user>sa</user>
            <password></password>
        </connectionSource>
    </appender>

    <appender name="ASYNC_DB" class="ch.qos.logback.classic.AsyncAppender">
        <appender-ref ref="DB"/>
        <queueSize>512</queueSize>
        <discardingThreshold>0</discardingThreshold>
        <includeCallerData>true</includeCallerData>
    </appender>
</springProfile>
```

### 7.4 ServiceLoggingAspect

서비스 메서드 자동 추적은 `log.trace()`를 사용한다. prod에서는 루트 레벨이 `WARN` 또는 `INFO`이므로 자동 비활성화된다.

```java
log.trace("{}↳ {}.{}()", indent, className, methodName);
```

개발 시 서비스 호출 흐름을 보려면:
```yaml
# application-local.yml
logging:
  level:
    com.example.admin_demo.domain: TRACE
```

---

## 8. 민감 정보 마스킹

### 8.1 마스킹 범위

| 대상 | 마스킹 규칙 | 예시 |
|------|-------------|------|
| 비밀번호 | 필드명 매칭, 값 전체를 `****`로 대체 | `"password":"****"` |
| 토큰·키 | 필드명 매칭 | `"token":"****"` |
| 개인정보 (선택) | 프로젝트 정책에 따라 추가 | 이메일 앞 일부, 전화번호 가운데 |

### 8.2 MaskingUtil

```java
// global/log/MaskingUtil.java
public final class MaskingUtil {

    // 문자열 값: "key": "value"
    private static final Pattern SENSITIVE_JSON_STRING = Pattern.compile(
            "(?i)\"(password|passwd|pwd|secret|token|apiKey|api_key|accessKey)\"\\s*:\\s*\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE
    );

    // 숫자·불리언 등 따옴표 없는 값: "key": 12345, "key": true
    private static final Pattern SENSITIVE_JSON_NON_STRING = Pattern.compile(
            "(?i)\"(password|passwd|pwd|secret|token|apiKey|api_key|accessKey)\"\\s*:\\s*([0-9a-zA-Z_.\\-]+)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern SENSITIVE_FORM_FIELD = Pattern.compile(
            "(?i)(password|passwd|pwd|secret|token)=[^&]*",
            Pattern.CASE_INSENSITIVE
    );

    public static String mask(String input) {
        if (input == null || input.isBlank()) return input;
        String result = SENSITIVE_JSON_STRING.matcher(input).replaceAll("\"$1\":\"****\"");
        result = SENSITIVE_JSON_NON_STRING.matcher(result).replaceAll("\"$1\":\"****\"");
        return SENSITIVE_FORM_FIELD.matcher(result).replaceAll("$1=****");
    }

    private MaskingUtil() {}
}
```

### 8.3 마스킹 적용 시점

```java
// ApiLoggingInterceptor — body 추출 후 즉시 마스킹
String body = new String(content, StandardCharsets.UTF_8);
return MaskingUtil.mask(body);  // 마스킹 후 저장
```

**절대 마스킹하지 않아도 되는 것:** URL 경로 파라미터, Content-Type, 타임스탬프

> 클라이언트 응답 보안 규칙은 API Design 4.4절 참고.

---

## 9. 환경별 전략

| 환경 | 진단 로그 (Logback) | 이벤트 로그 (LogEventPort) | 루트 레벨 |
|------|--------------------|-----------------------------|-----------|
| `local` | Console + H2 | LogbackLogEventAdapter + RdbLogEventAdapter(H2) | DEBUG |
| `ci` | Console | LogbackLogEventAdapter | INFO |
| `staging` | Console + RDB | LogbackLogEventAdapter + RdbLogEventAdapter | INFO |
| `prod` | Console + RDB + Logstash | LogbackLogEventAdapter + RdbLogEventAdapter + EsLogEventAdapter | WARN |

> 환경별 YAML 상세 설정은 6.7절 (어댑터 활성화 설정), 7.2절 (프로파일별 Logback 구성) 참고.

---

## 부록 — 이 프로젝트 설정 레퍼런스

### Logstash JSON 필드 매핑

LogstashEncoder는 MDC 키와 커스텀 필드를 JSON에 포함한다.

```xml
<encoder class="net.logstash.logback.encoder.LogstashEncoder">
    <includeMdcKeyName>traceId</includeMdcKeyName>
    <includeMdcKeyName>userId</includeMdcKeyName>
    <customFields>{"application":"spider-admin","environment":"${SPRING_PROFILES_ACTIVE}"}</customFields>
</encoder>
```

Elasticsearch에 수신되는 문서 예시:
```json
{
  "@timestamp": "2026-02-23T12:34:56.789Z",
  "level": "INFO",
  "logger_name": "c.e.a.global.log.ApiLoggingInterceptor",
  "message": "[ACCESS] POST /api/users → 201 (32ms)",
  "traceId": "a3f8c1d2e4b59071",
  "userId": "admin01",
  "application": "spider-admin",
  "environment": "prod"
}
```

---

*Last updated: 2026-02-26*
