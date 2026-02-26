# Logging

## 1. 개요

### 1.1 아키텍처

로깅은 **진단 로그**와 **이벤트 로그** 두 계층으로 분리한다. 이벤트 로그는 Spring `ApplicationEventPublisher` + `@EventListener` 패턴으로 구현하며, 비즈니스 코드와 로그 기록 로직을 완전히 분리한다.

```
Business Code
    |
    +-[진단 로그]-----> SLF4J log.*(...)
    |                    Logback이 관리 -- 개발자 디버깅, 운영 진단
    |                        +-- ConsoleAppender       (항상)
    |                        +-- APP_FILE Appender      (staging/prod)
    |                        +-- JSON_FILE Appender     (prod, LogstashEncoder)
    |
    +-[이벤트 로그]---> ApplicationEventPublisher.publishEvent(LogEvent)
                         Spring Event + @EventListener -- 비즈니스 감사, 보안 추적
                             +-- AccessLogEventListener    ("audit" 로거 + 선택적 RDB)
                             +-- SecurityLogEventListener  ("audit" 로거)
                             +-- AUDIT_FILE Appender        (LogstashEncoder, JSON 파일)
```

| 계층 | 용도 | 형태 | 관리 주체 |
|------|------|------|-----------|
| 진단 로그 | 문제 추적, 성능 분석, 개발 디버깅 | 텍스트 패턴 문자열 | Logback (설정 파일) |
| 이벤트 로그 | 감사 추적, 보안 이벤트, 접근 기록 | sealed interface 기반 타입 객체 | Spring Event + `@EventListener` |

### 1.2 설계 원칙

1. **이벤트 기반 비동기 처리.** 이벤트 로그는 `ApplicationEventPublisher`로 발행하고, `@Async("logEventExecutor")` + `@EventListener`로 비동기 수신한다. 비즈니스 트랜잭션에 로깅 지연이 영향을 주지 않는다.
2. **구조화.** 이벤트 로그는 `sealed interface`와 `record`로 필드가 정의된 타입 객체다. 문자열 포맷에 의존하지 않아, 필드 추가/변경이 파서 수정 없이 가능하다.
3. **추적 가능성.** 모든 로그는 `traceId`로 연결된다. 요청 하나의 전 생애주기를 재현할 수 있어야 한다.
4. **감사 로그 분리.** 이벤트 리스너는 전용 `"audit"` 로거로 기록하며, Logback의 `AUDIT_FILE` 어펜더가 JSON 형식으로 별도 파일에 저장한다.
5. **선택적 RDB 저장.** `log.event.rdb.enabled` 설정으로 접근 로그의 DB 기록을 환경별로 제어한다.

---

## 2. 로그 분류 체계

### 2.1 이벤트 타입

| 타입 | 목적 | 생성 시점 | 기본 보관 |
|------|------|-----------|-----------|
| `ACCESS` | HTTP 요청/응답 추적 | API 호출 완료 시 | 90일 |
| `AUDIT` | 데이터 변경 추적 (누가 무엇을 언제 어떻게) | 생성/수정/삭제 성공 시 | 1년 |
| `SECURITY` | 인증/인가 이벤트 | 로그인 실패, 권한 거부 시 | 1년 |

> 이벤트 타입은 `LogEvent` sealed interface의 구현체 3종(`AccessLogEvent`, `AuditLogEvent`, `SecurityLogEvent`)으로 한정된다.

---

## 3. 로그 레벨 규칙

### 3.1 레벨 정의

| 레벨 | 의미 | 예시 |
|------|------|------|
| `TRACE` | 메서드 진입/종료 (매우 상세) | 서비스 메서드 호출 흔적 |
| `DEBUG` | 진단 상세 -- 개발/문제 추적 시 활성화 | SQL, 파라미터 값, 캐시 히트 여부 |
| `INFO` | 비즈니스 이벤트 -- 운영 모니터링 기준 | 로그인 성공, 파일 업로드, 배치 완료 |
| `WARN` | 복구 가능한 문제 -- 주의 필요 | 4xx 오류, 재시도, DB 기록 실패 |
| `ERROR` | 복구 불가 -- 즉각 조치 필요 | 5xx 예외, DB 연결 실패, 예측 불가 오류 |

### 3.2 HTTP 상태 코드별 레벨

```
2xx, 3xx  -> log.info()   (정상)
4xx       -> log.warn()   (클라이언트 오류 -- 복구 가능)
5xx       -> log.error()  (서버 오류 -- 즉각 조치)
```

### 3.3 레이어별 규칙

| 레이어 | 직접 로그 작성 범위 | 비고 |
|--------|-------------------|------|
| Controller | 최소화 | 공통 처리에 위임 |
| Service | 비즈니스 의사결정/분기 (INFO) | 이벤트 발행은 `eventPublisher.publishEvent()` |
| Mapper | 금지 | MyBatis SQL 로그 (DEBUG) |
| Exception Handler | 금지 -- 레벨 규칙을 핸들러에서 통합 관리 | GlobalExceptionHandler |

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
// global/util/TraceIdUtil.java
public final class TraceIdUtil {
    private TraceIdUtil() {}

    public static String getOrGenerate() {
        String id = MDC.get("traceId");
        return (id != null) ? id : UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
```

단일 서비스 환경에서는 16자리 UUID 접두어로 충분하다. 마이크로서비스 전환 시 W3C TraceContext 형식 (`traceparent` 헤더)으로 교체한다.

### 4.3 MDC 생명주기

```
요청 진입
    MDC.put("traceId", traceId)
    |
    +-- 요청 처리 (동기 스레드 로그에 traceId 자동 포함)
    |
요청 완료
    MDC.clear()   <-- 반드시 명시 (스레드 풀 재사용 시 오염 방지)
```

> **비동기 처리 주의.** `@Async`, `CompletableFuture` 등 별도 스레드로 실행되는 코드는 MDC가 전파되지 않는다. 이벤트 리스너에서는 `LogEvent.traceId()`로 이벤트 객체에 포함된 traceId를 직접 사용한다.

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
}
```

`sealed interface`로 허용된 구현체를 컴파일 타임에 제한한다. `switch` 패턴 매칭에서 exhaustive check가 가능하다.

### 5.2 AccessLogEvent

HTTP 요청/응답 추적 이벤트. `accessDtime()` 메서드로 `yyyyMMddHHmmss` 형식의 문자열을 반환하여 RDB 저장 시 활용한다.

```java
// global/log/event/AccessLogEvent.java
public record AccessLogEvent(
        String traceId,
        String userId,
        Instant timestamp,
        String accessIp,
        String accessUrl,
        String inputData,
        String resultMessage)
        implements LogEvent {

    private static final DateTimeFormatter DTIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault());

    public String accessDtime() {
        return DTIME_FORMAT.format(timestamp);
    }
}
```

| 필드 | 설명 |
|------|------|
| `accessIp` | 클라이언트 IP |
| `accessUrl` | 요청 URL |
| `inputData` | 요청 데이터 (쿼리스트링 또는 body) |
| `resultMessage` | 처리 결과 메시지 |

### 5.3 AuditLogEvent

데이터 변경 추적 이벤트. 변경 전/후 스냅샷을 JSON으로 기록한다. `timestamp`를 생략하는 편의 생성자를 제공한다.

```java
// global/log/event/AuditLogEvent.java
public record AuditLogEvent(
        String traceId,
        String userId,
        Instant timestamp,
        String action,
        String entityType,
        String entityId,
        String beforeSnapshot,
        String afterSnapshot)
        implements LogEvent {

    public AuditLogEvent(
            String traceId, String userId,
            String action, String entityType, String entityId,
            String beforeSnapshot, String afterSnapshot) {
        this(traceId, userId, Instant.now(), action, entityType, entityId, beforeSnapshot, afterSnapshot);
    }
}
```

| 필드 | 설명 |
|------|------|
| `action` | `CREATE`, `UPDATE`, `DELETE` 등 |
| `entityType` | 대상 엔티티 타입 (e.g., `User`, `Menu`) |
| `entityId` | 대상 엔티티 식별자 |
| `beforeSnapshot` | 변경 전 JSON 스냅샷 |
| `afterSnapshot` | 변경 후 JSON 스냅샷 |

### 5.4 SecurityLogEvent

인증/인가 보안 이벤트. 성공/실패 여부에 따라 로그 레벨이 달라진다. `timestamp`를 생략하는 편의 생성자를 제공한다.

```java
// global/log/event/SecurityLogEvent.java
public record SecurityLogEvent(
        String traceId,
        String userId,
        Instant timestamp,
        String action,
        boolean success,
        String clientIp,
        String detail)
        implements LogEvent {

    public SecurityLogEvent(
            String traceId, String userId,
            String action, boolean success, String clientIp, String detail) {
        this(traceId, userId, Instant.now(), action, success, clientIp, detail);
    }
}
```

| 필드 | 설명 |
|------|------|
| `action` | `ACCESS_DENIED`, `AUTHENTICATION_FAILURE` 등 |
| `success` | 성공 여부 |
| `clientIp` | 클라이언트 IP |
| `detail` | 상세 정보 (실패 사유 등) |

---

## 6. 이벤트 리스너

이벤트 리스너는 Spring `@EventListener`와 `@Async("logEventExecutor")`를 사용하여 비동기로 이벤트를 처리한다. 전용 `"audit"` 로거를 통해 `AUDIT_FILE` 어펜더(LogstashEncoder, JSON 형식)로 기록한다.

### 6.1 AccessLogEventListener

접근 로그를 `"audit"` 로거에 기록하고, `log.event.rdb.enabled=true`일 때 `AccessLogMapper`를 통해 RDB에도 저장한다. RDB 기록 실패 시 예외를 잡아 `log.warn()`으로 경고만 남기며 비즈니스 흐름에 영향을 주지 않는다.

```java
// global/log/listener/AccessLogEventListener.java
@Slf4j
@Component
public class AccessLogEventListener {

    private static final Logger auditLogger = LoggerFactory.getLogger("audit");
    private final AccessLogMapper accessLogMapper;
    private final boolean rdbEnabled;

    public AccessLogEventListener(
            AccessLogMapper accessLogMapper,
            @Value("${log.event.rdb.enabled:false}") boolean rdbEnabled) {
        this.accessLogMapper = accessLogMapper;
        this.rdbEnabled = rdbEnabled;
    }

    @Async("logEventExecutor")
    @EventListener
    public void handle(AccessLogEvent event) {
        auditLogger.info("[ACCESS] traceId={} userId={} ip={} url={} result={}",
                event.traceId(), event.userId(), event.accessIp(),
                event.accessUrl(), event.resultMessage());

        if (rdbEnabled) {
            try {
                accessLogMapper.insertAccessLog(event);
            } catch (Exception e) {
                log.warn("접근 로그 DB 기록 실패: traceId={}", event.traceId(), e);
            }
        }
    }
}
```

### 6.2 SecurityLogEventListener

보안 이벤트를 `"audit"` 로거에 기록한다. 성공 이벤트는 `INFO`, 실패 이벤트는 `WARN` 레벨로 구분한다.

```java
// global/log/listener/SecurityLogEventListener.java
@Component
public class SecurityLogEventListener {

    private static final Logger auditLogger = LoggerFactory.getLogger("audit");

    @Async("logEventExecutor")
    @EventListener
    public void handle(SecurityLogEvent event) {
        if (event.success())
            auditLogger.info("[SECURITY] action={} userId={} clientIp={} traceId={}",
                    event.action(), event.userId(), event.clientIp(), event.traceId());
        else
            auditLogger.warn("[SECURITY] action={} userId={} clientIp={} detail={} traceId={}",
                    event.action(), event.userId(), event.clientIp(),
                    event.detail(), event.traceId());
    }
}
```

### 6.3 AccessLogMapper

MyBatis Mapper로 접근 로그를 RDB에 저장한다.

```java
// global/log/mapper/AccessLogMapper.java
@Mapper
public interface AccessLogMapper {
    void insertAccessLog(AccessLogEvent event);
}
```

---

## 7. 이벤트 발행

이벤트 발행은 Spring `ApplicationEventPublisher.publishEvent()`를 사용한다. `traceId`는 `TraceIdUtil.getOrGenerate()`로 얻는다.

### 7.1 보안 이벤트 발행 예시

Spring Security 핸들러에서 인증/인가 실패 시 `SecurityLogEvent`를 발행한다.

```java
// CustomAccessDeniedHandler (권한 거부 시)
private void recordSecurityEvent(HttpServletRequest request, AccessDeniedException ex) {
    try {
        String traceId = TraceIdUtil.getOrGenerate();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = (auth != null && auth.getName() != null) ? auth.getName() : "ANONYMOUS";

        eventPublisher.publishEvent(new SecurityLogEvent(
                traceId, userId, "ACCESS_DENIED", false,
                request.getRemoteAddr(),
                "uri=" + request.getRequestURI() + ", " + ex.getMessage()));
    } catch (Exception e) {
        log.warn("Failed to record security event: {}", e.getMessage());
    }
}
```

```java
// CustomAuthenticationEntryPoint (인증 실패 시)
eventPublisher.publishEvent(new SecurityLogEvent(
        traceId, "ANONYMOUS", "AUTHENTICATION_FAILURE", false,
        request.getRemoteAddr(), ex.getMessage()));
```

### 7.2 감사 이벤트 발행 예시

서비스에서 데이터 변경 시 `AuditLogEvent`를 발행한다. 편의 생성자를 사용하면 `timestamp`는 `Instant.now()`로 자동 설정된다.

```java
// Service 메서드 내부
eventPublisher.publishEvent(new AuditLogEvent(
        TraceIdUtil.getOrGenerate(),
        SecurityUtil.getCurrentUserId(),
        "UPDATE",
        "User",
        userId,
        toJson(before),
        toJson(after)));
```

### 7.3 이벤트 생성 위치 요약

| 이벤트 타입 | 생성 위치 |
|-------------|-----------|
| `AccessLogEvent` | HTTP 요청 처리 완료 후 |
| `AuditLogEvent` | Service 메서드에서 데이터 변경 시 (`eventPublisher.publishEvent()`) |
| `SecurityLogEvent` | `CustomAccessDeniedHandler`, `CustomAuthenticationEntryPoint` |

---

## 8. 비동기 실행 설정

### 8.1 AsyncConfig

`@EnableAsync`와 전용 스레드 풀 `logEventExecutor`를 설정한다. `CallerRunsPolicy`를 사용하여 큐가 가득 차면 호출 스레드에서 직접 실행한다.

```java
// global/config/AsyncConfig.java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean("logEventExecutor")
    public Executor logEventExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("log-event-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        return executor;
    }
}
```

| 설정 | 값 | 설명 |
|------|----|------|
| `corePoolSize` | 2 | 기본 스레드 수 |
| `maxPoolSize` | 4 | 최대 스레드 수 |
| `queueCapacity` | 500 | 대기 큐 용량 |
| `threadNamePrefix` | `log-event-` | 스레드 이름 접두어 (디버깅 시 식별용) |
| `rejectedExecutionHandler` | `CallerRunsPolicy` | 큐 초과 시 호출 스레드에서 직접 실행 |

---

## 9. Logback 설정

진단 로그는 Logback이 관리하며, 개발자의 `log.*()` 호출을 처리한다. 이벤트 리스너의 `"audit"` 로거 출력도 Logback이 라우팅한다.

### 9.1 어펜더 구성

| 어펜더 | 대상 | 인코더 | 활성 프로파일 |
|--------|------|--------|---------------|
| `CONSOLE` | 콘솔 출력 | 텍스트 패턴 | 항상 |
| `APP_FILE` | `logs/application.log` | 텍스트 패턴 | `staging`, `prod` |
| `AUDIT_FILE` | `logs/audit.log` | `LogstashEncoder` (JSON) | 항상 |
| `JSON_FILE` | `logs/application.json` | `LogstashEncoder` (JSON) | `prod` |

### 9.2 패턴 정의

```xml
<!-- 콘솔 패턴 -->
<property name="CONSOLE_PATTERN"
          value="%d{HH:mm:ss.SSS} %highlight(%-5level) [%thread] %cyan(%logger{36}) - %msg%n"/>

<!-- 파일 패턴 -->
<property name="FILE_PATTERN"
          value="%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] %logger{36} - %msg%n"/>
```

### 9.3 감사 로그 전용 어펜더

`"audit"` 로거는 `AUDIT_FILE` 어펜더로만 출력되며(`additivity="false"`), `LogstashEncoder`를 사용하여 JSON 형식으로 기록한다. 90일 보관한다.

```xml
<appender name="AUDIT_FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>logs/audit.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
        <fileNamePattern>logs/audit.%d{yyyy-MM-dd}.log</fileNamePattern>
        <maxHistory>90</maxHistory>
    </rollingPolicy>
    <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
</appender>

<logger name="audit" level="INFO" additivity="false">
    <appender-ref ref="AUDIT_FILE"/>
</logger>
```

### 9.4 프로파일별 구성 요약

| 프로파일 | 어펜더 | 루트 레벨 |
|---------|--------|-----------|
| `local` / `dev` / `test` | CONSOLE + AUDIT_FILE | INFO |
| `staging` | CONSOLE + APP_FILE + AUDIT_FILE | INFO |
| `prod` | CONSOLE + APP_FILE + JSON_FILE + AUDIT_FILE | INFO |

---

## 10. 환경별 설정

### 10.1 RDB 기록 활성화

접근 로그의 RDB 저장은 `log.event.rdb.enabled` 프로퍼티로 제어한다.

```yaml
# application-base.yml
log:
  event:
    rdb:
      enabled: ${LOG_EVENT_RDB_ENABLED:false}
```

| 환경 변수 | 기본값 | 설명 |
|-----------|--------|------|
| `LOG_EVENT_RDB_ENABLED` | `false` | `true`로 설정 시 `AccessLogEventListener`가 `AccessLogMapper`를 통해 RDB에 저장 |

### 10.2 환경별 전략 요약

| 환경 | 진단 로그 (Logback) | 이벤트 로그 | RDB 저장 |
|------|---------------------|-------------|----------|
| `local` | CONSOLE | `"audit"` 로거 -> AUDIT_FILE | 선택 (`LOG_EVENT_RDB_ENABLED`) |
| `staging` | CONSOLE + APP_FILE | `"audit"` 로거 -> AUDIT_FILE | 선택 |
| `prod` | CONSOLE + APP_FILE + JSON_FILE | `"audit"` 로거 -> AUDIT_FILE | 선택 |

---

## 11. 프로젝트 파일 구조

```
global/
+-- log/
|   +-- event/
|   |   +-- LogEvent.java              # sealed interface (공통 필드)
|   |   +-- AccessLogEvent.java        # HTTP 접근 로그 record
|   |   +-- AuditLogEvent.java         # 데이터 변경 감사 record
|   |   +-- SecurityLogEvent.java      # 보안 이벤트 record
|   +-- listener/
|   |   +-- AccessLogEventListener.java    # @EventListener + @Async
|   |   +-- SecurityLogEventListener.java  # @EventListener + @Async
|   +-- mapper/
|       +-- AccessLogMapper.java       # MyBatis Mapper (RDB 저장)
+-- config/
|   +-- AsyncConfig.java               # logEventExecutor 스레드 풀
+-- util/
    +-- TraceIdUtil.java               # MDC traceId 조회/생성
```

---

*Last updated: 2026-02-26*
