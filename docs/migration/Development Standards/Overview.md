# Overview

## 1. 개요

Spring Boot + MyBatis + Thymeleaf 하이브리드 프로젝트의 개발 표준이다. REST API와 서버 렌더링 페이지가 공존하며, 하나의 코드베이스로 Oracle과 MySQL을 모두 지원한다.

**핵심 설계 결정:**

| 결정 | 근거 |
|------|------|
| Entity 클래스 없음 | MyBatis ResultMap이 DTO에 직접 매핑 — Converter/VO/Model 불필요 |
| 단방향 레이어 | Controller → Service → Mapper, 역방향·우회 금지 |
| 인터페이스 없는 Service | 내부 소비자만 존재 — ServiceImpl 추상화 불필요 |
| PUT 전용 (PATCH 없음) | 부분 수정도 전체 필드 전송으로 통일 |
| 단일 DB 추상화 | `databaseId`로 SQL 분기, Java 코드 변경 없이 DB 전환 |

**문서 맵:**

이 문서의 각 절은 하나의 상세 문서에 대응한다. 절을 읽고 더 알고 싶으면 대응 문서로 이동한다.

| 절 | 문서 | 한 줄 설명 |
|----|------|-----------|
| §2 | Tech Stack | 기술 스택, 버전, 버전 관리 원칙 |
| §3 | Project Architecture | 레이어, 의존성 규칙, 패키지 구조 |
| §4 | Code Convention | 포맷팅, 네이밍, Enum & TypeHandler |
| §5 | API Design | URL·HTTP 메서드, 응답 구조, Controller·DTO |
| §6 | SQL | MyBatis XML 매핑, 동적 SQL, 페이징 |
| §7 | Multi-DB | databaseId 기반 DB별 SQL 분기 |
| §8 | Exception Handling | ErrorType, BusinessException, Guard Clause |
| §9 | Authentication & Authorization | 세션 인증, 메뉴 기반 인가 |
| §10 | Security | SecurityFilterChain, CSRF, CORS, 보안 헤더 |
| §11 | Logging | SLF4J/Logback, MDC traceId, 마스킹 |
| §12 | Caching | Caffeine 인메모리 캐시, 무효화 전략 |
| §13 | Configuration | 12-Factor, 프로파일, 환경 변수 |
| §14 | Postman | Newman 컬렉션, API 자동 검증 |
| §15 | CI Strategy | GitHub Actions, 멀티 DB 테스트, 자동 강제 |
| §16 | Design Principle | Apple HIG 기반 디자인 원칙 |
| §17 | Design System | IBM Carbon Design System 기반 시각 체계 |
| §18 | Frontend Code Convention | ESLint, Tailwind CSS, 프론트엔드 규칙 |
| §19 | Document Structure | 문서 작성 형식 규칙 |
| §20 | Test Strategy | 테스트 계층별 커버리지 목표, 검증 항목 |
| §21 | Postman Test Criteria | 도메인별 API 엔드포인트, 테스트 시나리오 |
| §22 | Project Specific Requirements | 프로젝트 고유 DB 스키마, 도메인 규칙 |

---

## 2. 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 런타임 | Java (OpenJDK) | 17 |
| 프레임워크 | Spring Boot | 3.2.1 |
| 보안 | Spring Security | 6.2.x |
| SQL 매핑 | MyBatis Spring Boot Starter | 3.0.3 |
| 뷰 | Thymeleaf | 3.1.x |
| API 문서 | SpringDoc OpenAPI | 2.3.0 |
| DB | Oracle 19c+ / MySQL 8.0+ | — |
| 캐시 | Caffeine | Spring Boot 관리 |
| 빌드 | Maven | 3.x |
| CSS | Tailwind CSS | 3.4.x |
| JS 유틸 | jQuery | 3.7.x |
| 코드 품질 | SonarCloud / ESLint / HTMLHint / Husky | — / 9.x / 1.x / 9.x |

전체 의존성 목록과 버전 관리 원칙은 Tech Stack 문서를 참고한다.

> 상세: Tech Stack

---

## 3. 아키텍처

### 3.1 레이어 구조

```
Client
  │
  ▼
Controller (@RestController / @Controller)
  │  ← RequestDTO (입력), ResponseDTO (출력)
  ▼
Service (@Service, @Transactional)
  │  ← 비즈니스 로직, 검증, BusinessException
  ▼
Mapper (@Mapper interface + XML)
  │  ← SQL 실행, ResultMap → ResponseDTO 직접 매핑
  ▼
Database (Oracle / MySQL)
```

모든 의존은 위에서 아래로만 흐른다. Controller가 Mapper를 직접 호출하거나, Service가 Controller를 참조하는 것은 금지다. 크로스 도메인 참조가 필요하면 **Service → Service만** 허용한다. 이 규칙은 ArchUnit 테스트로 자동 강제된다 (§15).

### 3.2 Entity 없는 데이터 흐름

별도 Entity 클래스를 만들지 않는다. MyBatis ResultMap이 SQL 결과를 DTO에 직접 매핑하므로 Entity ↔ DTO 변환 코드가 불필요하다.

```
조회: DB → ResultMap → ResponseDTO → Service → Controller → Client
생성: Client → RequestDTO → Controller → Service → Mapper → DB
삭제: Client → id → Controller → Service → Mapper → DB
```

이 결정으로 `entity/`, `converter/`, `model/`, `vo/`, `repository/` 패키지는 프로젝트에 존재하지 않으며, 해당 접미사를 가진 클래스 생성도 ArchUnit이 차단한다.

### 3.3 패키지 구조

```
src/main/java/{base-package}/
├── domain/                      # 도메인별 비즈니스 로직
│   └── {domain}/
│       ├── controller/          # @RestController, @Controller
│       ├── service/             # @Service (구체 클래스만, Impl 금지)
│       ├── mapper/              # @Mapper interface
│       └── dto/
│           ├── request/         # {Domain}CreateRequestDTO, ...
│           └── response/        # {Domain}ResponseDTO, ...
│
├── global/                      # 횡단 관심사
│   ├── config/                  # @Configuration
│   ├── exception/               # ErrorType, BusinessException, GlobalExceptionHandler
│   ├── dto/                     # ApiResponse, PageRequest, PageResponse
│   ├── security/                # SecurityFilterChain, MenuGuard
│   ├── typehandler/             # CodeEnum TypeHandler
│   └── ...
│
└── resources/mapper/            # MyBatis XML
    └── {domain}/
```

최상위는 `domain/`(비즈니스)과 `global/`(횡단) 두 개뿐이다. `util/`, `helper/` 같은 별도 최상위 패키지를 만들지 않는다. 의존성 주입은 `@RequiredArgsConstructor` + `private final` 전용이며, `@Autowired` 필드 주입은 금지다.

> 상세: Project Architecture

---

## 4. 코드 규칙

### 4.1 자동 포맷팅

Spotless + Palantir Java Format으로 포맷을 강제한다. 4칸 스페이스, 120자 줄 길이, import 자동 정리. CI에서 `mvn spotless:check`가 실패하면 PR이 블로킹된다.

```bash
mvn spotless:apply    # 로컬에서 자동 포맷 적용
mvn spotless:check    # CI에서 포맷 검사
```

### 4.2 네이밍

**클래스:**

| 역할 | 패턴 | 예시 |
|------|------|------|
| REST Controller | `{Domain}Controller` | `UserController` |
| Page Controller | `{Domain}PageController` | `UserPageController` |
| Service | `{Domain}Service` | `UserService` |
| Mapper | `{Domain}Mapper` | `UserMapper` |
| 요청 DTO | `{Domain}{Action}RequestDTO` | `UserCreateRequestDTO` |
| 응답 DTO | `{Domain}[Detail]ResponseDTO` | `UserResponseDTO`, `UserDetailResponseDTO` |

`{Domain}ServiceImpl`, `{Domain}Entity`, `{Domain}Converter`, `{Domain}VO`는 금지다 — ArchUnit이 차단한다.

**메서드:**

| 계층 | 패턴 |
|------|------|
| Controller | `list()`, `get()`, `create()`, `update()`, `delete()` |
| Service | `findAll()`, `getById()`, `create()`, `update()`, `delete()` |
| Mapper | SQL ID 네이밍 표준을 따른다 (§6) |

**DB:** 테이블·컬럼 모두 `UPPER_SNAKE_CASE` — `USER`, `USER_ID`, `CREATED_AT`.

### 4.3 Enum & TypeHandler

DB에 코드값(`"01"`, `"02"`)으로 저장되는 Enum은 `CodeEnum` 인터페이스를 구현하고, TypeHandler를 등록하면 Mapper XML에서 자동 변환된다.

```java
// Enum
public enum UserStatus implements CodeEnum {
    ACTIVE("01"), INACTIVE("02"), LOCKED("03");
    ...
}

// TypeHandler가 DB "01" ↔ Java UserStatus.ACTIVE 자동 변환
```

> 상세: Code Convention

---

## 5. API 설계

### 5.1 URL과 HTTP 메서드

REST API는 `/api/{resource}`, Thymeleaf 페이지는 `/{page}`로 분리한다.

```
GET    /api/users              → 200  목록 조회
GET    /api/users/{id}         → 200  단건 조회
POST   /api/users              → 201  생성
PUT    /api/users/{id}         → 200  수정
DELETE /api/users/{id}         → 204  삭제

POST   /api/orders/{id}/cancel → 200  비CRUD 액션
GET    /api/roles/select-options → 200  드롭다운 목록
```

URL은 소문자 복수 명사 + kebab-case다. 동사 URL(`/api/getUsers`), 단수형(`/api/user`), camelCase(`/api/auditLogs`), PATCH는 사용하지 않는다.

### 5.2 공통 응답 구조

모든 API는 `ApiResponse<T>`로 래핑하여 `ResponseEntity<ApiResponse<T>>`로 반환한다. 성공과 에러의 JSON 구조가 통일되므로 클라이언트는 항상 `success` 필드로 먼저 분기한다.

**성공:**

```json
{
  "success": true,
  "data": { "userId": "user01", "userName": "홍길동" }
}
```

**에러:**

```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "요청한 리소스를 찾을 수 없습니다.",
    "traceId": "4bf92f3577b34da6"
  }
}
```

**검증 에러:**

```json
{
  "success": false,
  "error": {
    "code": "INVALID_INPUT",
    "message": "입력값이 유효하지 않습니다.",
    "traceId": "4bf92f3577b34da6",
    "fields": [
      { "field": "email", "message": "올바른 이메일 형식이 아닙니다." }
    ]
  }
}
```

`error.code`는 클라이언트가 분기에 사용하는 coarse-grained 식별자다. 9개 코드(`RESOURCE_NOT_FOUND`, `DUPLICATE_RESOURCE`, `DATA_INTEGRITY_VIOLATION`, `INVALID_INPUT`, `BUSINESS_RULE_VIOLATED`, `UNAUTHORIZED`, `FORBIDDEN`, `EXTERNAL_SERVICE_ERROR`, `INTERNAL_ERROR`)로 모든 에러를 표현하며, 클라이언트가 다르게 동작해야 할 때만 새 코드를 추가한다. `error.traceId`는 서버 로그의 MDC `traceId`와 동일 값이다 (§11) — 사용자가 이 값을 리포트하면 서버 로그를 즉시 추적할 수 있다.

### 5.3 Controller 패턴

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 관리")
@PreAuthorize("hasAuthority('USER:READ')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "사용자 목록 조회",
               description = "검색 조건과 페이징을 적용한 사용자 목록을 반환한다.")
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDTO>>> list(
            @ModelAttribute PageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.findAll(pageRequest)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER:WRITE')")
    @Operation(summary = "사용자 생성")
    public ResponseEntity<ApiResponse<UserResponseDTO>> create(
            @Valid @RequestBody UserCreateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.create(dto)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    @Operation(summary = "사용자 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

클래스 레벨 `@PreAuthorize`는 READ 권한, 쓰기 메서드에는 WRITE로 오버라이드한다 (§8). Swagger 어노테이션(`@Tag`, `@Operation`, `@Schema`, `@ApiResponse`)은 모든 Controller와 DTO에 부여한다.

### 5.4 DTO와 검증

DTO 네이밍은 `{Domain}{Action}{Direction}DTO` 패턴이다. 검증은 유형에 따라 위치가 다르다.

| 유형 | 위치 | 도구 |
|------|------|------|
| 형식 (null, 길이, 정규식) | Request DTO | `@NotBlank`, `@Size`, `@Pattern` + `@Valid` |
| 비즈니스 규칙 (상태 전이 등) | Service | Guard Clause + `BusinessException` |
| DB 제약 조건 (유니크 키) | Service | `DataIntegrityViolationException` catch |

형식 검증이 실패하면 `GlobalExceptionHandler`가 `INVALID_INPUT` + `fields[]`로 자동 변환한다. 비즈니스 검증이 실패하면 Service에서 `BusinessException`을 직접 던진다 (§8).

> 상세: API Design

---

## 6. SQL 작성

### 6.1 MyBatis XML 매핑

Mapper interface와 XML이 1:1로 대응한다. SQL 결과는 `resultType`으로 ResponseDTO에 직접 매핑하며, `<association>`/`<collection>`은 사용하지 않는다. 관련 데이터가 필요하면 조인 + DTO 직접 매핑으로 해결한다.

```xml
<select id="findAllWithSearch" resultType="UserDetailResponseDTO">
    SELECT u.USER_ID   AS userId,
           u.USER_NAME AS userName,
           r.ROLE_NAME AS roleName
    FROM USER u
    LEFT JOIN ROLE r ON u.ROLE_ID = r.ROLE_ID
    <where>
        <if test="userName != null and userName != ''">
            AND u.USER_NAME LIKE CONCAT('%', #{userName}, '%')
        </if>
    </where>
</select>
```

파라미터 바인딩은 `#{}` 전용이다. `${}`는 SQL injection 위험이 있으므로 금지한다.

### 6.2 페이징

클라이언트는 `page`, `size` 쿼리 파라미터로 요청하고, `PageResponse<T>`(content, page, size, totalElements, totalPages)를 응답받는다.

> 상세: SQL

---

## 7. 멀티 DB

`databaseId`로 동일 Statement ID에 DB별 SQL을 분기한다. Java 코드는 변경하지 않는다.

```xml
<select id="selectNextId" resultType="string" databaseId="oracle">
    SELECT SEQ_USER.NEXTVAL FROM DUAL
</select>

<select id="selectNextId" resultType="string" databaseId="mysql">
    SELECT NULL    <!-- AUTO_INCREMENT 사용 -->
</select>
```

페이징 SQL은 `databaseId`로 분기하여 MyBatis XML에서 직접 처리한다.

> 상세: Multi-DB

---

## 8. 예외 처리

### 8.1 구조

예외의 모든 속성 — 클라이언트 코드, 사용자 메시지, HTTP 상태, 로그 레벨 — 을 `ErrorType` enum 하나에 정의한다. Service에서 `BusinessException`을 던지면 `GlobalExceptionHandler`가 `ApiResponse.error()`로 변환하여 응답한다.

```
Service                             GlobalExceptionHandler             Client
  │                                       │                              │
  throw BusinessException ─────────────► catch                           │
       (ErrorType.RESOURCE_NOT_FOUND)     ├── logByType(WARN)            │
                                          └── ApiResponse.error() ─────► JSON
```

### 8.2 ErrorType과 사용 패턴

```java
// ErrorType enum — 한 곳에서 관리, 새 항목만 추가
RESOURCE_NOT_FOUND(
    ErrorCode.RESOURCE_NOT_FOUND,
    "요청한 리소스를 찾을 수 없습니다.",
    HttpStatus.NOT_FOUND,
    LogLevel.WARN),

ORDER_ALREADY_CANCELLED(                          // 도메인 특화 항목
    ErrorCode.BUSINESS_RULE_VIOLATED,
    "이미 취소된 주문입니다.",
    HttpStatus.CONFLICT,
    LogLevel.INFO),
```

```java
// Service — Guard Clause 패턴
public void cancelOrder(String orderId) {
    Order order = Optional.ofNullable(orderMapper.selectById(orderId))
            .orElseThrow(() -> new BusinessException(
                    ErrorType.RESOURCE_NOT_FOUND, "orderId=" + orderId));

    if (order.isCancelled())
        throw new BusinessException(ErrorType.ORDER_ALREADY_CANCELLED);

    orderMapper.updateStatus(orderId, OrderStatus.CANCELLED);
}
```

`BusinessException`의 `detail` 파라미터(`"orderId=ORD-001"`)는 서버 로그에만 기록되고, 클라이언트에는 `ErrorType.message`만 전달된다. 범용 `ErrorType`으로 충분하면 그대로 사용하고, 사용자 메시지가 불충분할 때만 도메인 특화 항목을 추가한다. `GlobalExceptionHandler`는 수정하지 않는다.

> 상세: Exception Handling

---

## 9. 인증·인가

### 9.1 인증

Spring Security `DaoAuthenticationProvider` + 세션 기반이다. 로그인 폼에서 CSRF 토큰과 함께 credentials를 제출하면 세션이 생성된다. 비밀번호는 BCrypt로 인코딩한다.

### 9.2 인가

메뉴 기반 권한 모델이다. `@PreAuthorize("hasAuthority(…)")`로 API 단위 접근 제어를 수행한다. 사용자의 메뉴 권한(읽기/쓰기)은 DB(`USER_MENU` 또는 `ROLE_MENU`)에서 로드하여 `GrantedAuthority`로 변환하며, 크로스 리소스 참조는 YAML 기반 리소스 파생으로 해결한다.

```java
@PreAuthorize("hasAuthority('USER:READ')")                 // 클래스 레벨: 읽기 권한
public class UserController {

    @PreAuthorize("hasAuthority('USER:WRITE')")            // 메서드 레벨: 쓰기 권한
    public ResponseEntity<...> create(...) { ... }
}
```

다른 메뉴에서 공유하는 API는 `hasAnyAuthority` OR 조건으로 리소스 접근을 허용한다. CUD 작업은 본래 메뉴 권한만 허용하여 참조 권한으로 수정을 차단한다.

> 상세: Authentication & Authorization

---

## 10. 보안

`SecurityFilterChain` **한 곳**에서 선언적으로 구성한다. 보안 설정을 여러 곳에 분산시키지 않는다.

- **CSRF**: Thymeleaf `<meta>` 태그 + REST `X-XSRF-TOKEN` 쿠키 이중 방어
- **CORS**: 허용 도메인 화이트리스트
- **보안 헤더**: `X-Content-Type-Options`, `X-Frame-Options`, `Strict-Transport-Security` 등
- **환경별**: 운영 환경에서 Swagger UI 비활성화, 프로파일별 보안 토글

> 상세: Security

---

## 11. 로깅

진단 로그(SLF4J/Logback)와 이벤트 로그(LogEventPort) 두 계층으로 분리한다. 진단 로그는 개발자가 문제를 추적하기 위한 것이고, 이벤트 로그는 감사·분석 등 비즈니스 목적이다.

MDC `traceId`가 요청 단위로 모든 로그에 자동 부여되며, 에러 응답의 `traceId` (§5)와 동일한 값이다. 사용자가 이 값을 리포트하면 서버 로그를 즉시 특정할 수 있다. 민감 정보(주민번호, 카드번호 등)는 패턴 기반으로 자동 마스킹한다.

> 상세: Logging

---

## 12. 캐싱

Caffeine 인메모리 캐시를 **Service 레이어에서만** 사용한다. `@Cacheable`로 조회 캐시, `@CacheEvict`로 쓰기 시 즉시 무효화한다. 캐시 이름·TTL·최대 크기는 `CacheManager`에서 중앙 관리한다.

> 상세: Caching

---

## 13. 설정 관리

12-Factor App 원칙을 따른다. `application-base.yml`에 공통 설정, `application-{profile}.yml`에 환경별 오버라이드를 둔다.

- **민감 정보**(DB 비밀번호, API 키)는 **환경 변수로만** 주입한다. yml 파일에 직접 기록하지 않는다.
- `@ConfigurationProperties` + `@Validated`로 애플리케이션 기동 시 설정 누락을 즉시 검증한다.
- 로컬 개발은 `.env` 파일로 환경 변수를 공급하며, `.env`는 `.gitignore`에 등록한다.

> 상세: Configuration

---

## 14. API 테스트

Postman 컬렉션으로 API를 검증한다. 컬렉션은 **Setup → Create → Read → Update → Delete → Cleanup** 순서로 구성하여 Newman CLI가 순차 실행할 때 의존 관계가 보장된다.

모든 요청은 반드시 상태 코드 + `ApiResponse` 구조 (§5)를 검증한다. 생성 응답의 ID를 변수에 저장하고 후속 요청에서 사용하는 체이닝 패턴으로 CRUD 전체를 자동 검증한다.

> 상세: Postman

---

## 15. CI

GitHub Actions로 PR이 열리거나 업데이트될 때마다 자동 실행된다. Oracle과 MySQL **양쪽에서 독립적으로** 전체 테스트를 수행하며, **둘 다 통과해야** 병합 가능하다.

**자동 강제 항목:**

| 도구 | 검증 대상 | 실패 시 |
|------|-----------|---------|
| Spotless | 코드 포맷 (Palantir, 120자) | PR 블로킹 |
| ArchUnit | 레이어 의존성, 금지 클래스, DI 규칙 | PR 블로킹 |
| ESLint | JS 코드 품질 (`static/js/**`) | PR 블로킹 |
| HTMLHint | HTML 구조 (`templates/**`) | PR 블로킹 |
| Newman | API 동작, 응답 구조 (Oracle + MySQL) | PR 블로킹 |
| SonarCloud | 코드 품질 + SAST 보안 분석 | PR 블로킹 (Quality Gate) |
| Dependabot | 의존성 보안 취약점 | PR 자동 생성 |

코드 스타일과 아키텍처 규칙은 리뷰어가 지적하는 것이 아니라, CI가 자동으로 차단한다.

> 상세: CI Strategy

---

## 16. 디자인 원칙

Apple Human Interface Guidelines (HIG)를 기반으로 명확성·존중·깊이를 핵심 테마로 한다. 일관성, 직접 조작, 피드백, 메타포, 사용자 제어의 5대 원칙을 따른다.

> 상세: Design Principle

---

## 17. 디자인 시스템

IBM Carbon Design System을 기반으로 레이아웃(8pt 그리드), 색상(시맨틱 토큰), 타이포그래피(역할 기반), 컴포넌트, 접근성 규칙을 통일한다.

> 상세: Design System

---

## 18. 프론트엔드 코드 규칙

### 18.1 Thymeleaf 템플릿 구조

모든 페이지는 `fragments/layout.html`을 베이스 레이아웃으로 사용한다. 페이지 콘텐츠는 `pages/*-content.html`에 `<div class="content-inner">`로 시작하는 fragment로 작성하며, `th:replace`로 조립한다. `th:insert`는 금지다.

### 18.2 핵심 규칙

- 스타일은 Tailwind 유틸리티 클래스만 사용한다. Inline `style=""`, Raw hex 직접 기재 금지.
- `window.{Domain}Page` 네임스페이스 패턴으로 전역 오염을 방지한다.
- AJAX는 `$.ajax()`를 사용하며 `fetch()`는 금지다.
- `var`, `==` 비교, 직접 `addEventListener` 사용도 금지한다.

### 18.3 자동 강제

| 도구 | 대상 | 강제 수준 |
|------|------|-----------|
| ESLint | `static/js/**/*.js` | CI 빌드 실패 |
| HTMLHint | `templates/**/*.html` | CI 빌드 실패 |
| Husky pre-commit | HTML 금지 패턴 | 커밋 차단 |

> 상세: Frontend Code Convention

---

## 19. 문서 체계

이 폴더의 모든 문서는 동일한 구조를 따른다.

| 항목 | 규칙 |
|------|------|
| 제목 | `# [파일명]` — 접미사·부제목 없음 |
| 첫 섹션 | `## 1. 개요` 고정 |
| 번호 체계 | `## N.` / `### N.M` 순차 |
| 마지막 번호 섹션 | `## N. 체크리스트` (포함 시) |
| 섹션 구분 | `---` |
| 푸터 | `*Last updated: YYYY-MM-DD*` |

각 문서는 하나의 주제에 대한 **권위 소스(single source of truth)**다. 동일 내용을 여러 문서에 중복 기술하지 않고, 교차 참조로 연결한다.

> 상세: Document Structure

---

## 20. 테스트 전략

테스트 계층별 커버리지 목표와 검증 항목을 정의한다. Security/Logging/Util은 100%, Mapper는 100%, Controller는 70%, Service는 50%를 목표로 한다. CI에서 커버리지 미달 시 빌드를 실패시킨다.

> 상세: Test Strategy

---

## 21. API 테스트 기준

도메인별 API 엔드포인트와 테스트 시나리오를 정의하는 검증 기준서(WHAT)다. Postman 문서(§14)가 컬렉션 작성 규칙(HOW)을 다룬다면, 이 문서는 무엇을 검증해야 하는지를 다룬다.

> 상세: Postman Test Criteria

---

## 22. 프로젝트 고유 요구사항

다른 개발 표준 문서에서 다루지 않는 이 프로젝트 고유의 DB 스키마, 도메인 규칙, 설정 규약을 정의한다. 메뉴 계층 구조, 도메인별 특수 규칙 등 프로젝트 특수 구성에 집중한다.

> 상세: Project Specific Requirements

---

*Last updated: 2026-02-26*
