# API Design

## 1. 개요

REST API 설계 표준을 정의한다. Thymeleaf 서버 렌더링과 REST API가 공존하는 하이브리드 구조를 전제로 한다.

```
/api/{resource}    → REST API (@RestController)
/{page}            → Thymeleaf 페이지 (@Controller)
```

**적용 범위:**

| 포함 | 제외 |
|------|------|
| URL 설계, HTTP 메서드, 공통 응답 구조 | 인증·인가 (Auth & Auth 문서) |
| Controller·DTO 구조, 페이징 | 예외 처리 로직 (Exception Handling 문서) |
| Swagger/OpenAPI 어노테이션 | SQL·Mapper (SQL 문서) |

> API 버전 관리(`/v1/`, `/v2/`)는 내부 소비자만 존재하므로 도입하지 않는다.

---

## 2. URL 설계 규칙

### 2.1 기본 원칙

| 구분 | 패턴 | 예시 |
|------|------|------|
| REST API | `/api/{resource}` | `/api/users`, `/api/roles` |
| Thymeleaf 페이지 | `/{page}` | `/user`, `/role` |

- 소문자 복수 명사를 사용한다.
- trailing slash를 붙이지 않는다 (`/api/users/` → `/api/users`).

### 2.2 리소스 네이밍

| 규칙 | 올바른 예 | 잘못된 예 |
|------|-----------|-----------|
| 복수 명사 | `/api/users` | `/api/user` |
| 복수 명사 | `/api/roles` | `/api/role` |
| 복수 명사 | `/api/menus` | `/api/menu` |
| 멀티 워드 kebab-case | `/api/audit-logs` | `/api/auditLogs` |
| 멀티 워드 kebab-case | `/api/role-menus` | `/api/roleMenus` |

### 2.3 계층 관계

직접 소유 관계에만 중첩 경로를 사용하며, **최대 2 depth**로 제한한다. 중첩된 리소스의 특정 항목을 식별하는 경로는 별도의 리소스로 분리하는 것을 원칙으로 한다.


### 2.4 액션 엔드포인트

CRUD로 표현할 수 없는 비즈니스 작업은 `POST /api/{resource}/{id}/{action}` 패턴을 사용한다.

```
POST /api/orders/{orderId}/cancel
POST /api/users/{userId}/lock
```

### 2.5 셀렉트 옵션 엔드포인트

드롭다운·셀렉트 박스용 경량 목록은 `/select-options`로 제공한다.

```
GET /api/roles/select-options      → [{ "value": "ROLE01", "label": "관리자" }, ...]
GET /api/departments/select-options
```

### 2.6 금지 패턴

```
❌ /api/getUsers              → 동사 사용
❌ /api/user/list             → 단수형 + 동사
❌ /api/users/delete/{id}     → URL에 동작 포함 (DELETE 메서드 사용)
❌ /api/Users                 → 대문자
```

---

## 3. HTTP 메서드 매핑

### 3.1 CRUD 매핑 표준

| 작업 | 메서드 | URL 패턴 | 상태 코드 |
|------|--------|----------|-----------|
| 목록 조회 | GET | `/api/users` | 200 |
| 단건 조회 | GET | `/api/users/{id}` | 200 |
| 생성 | POST | `/api/users` | 201 |
| 전체 수정 | PUT | `/api/users/{id}` | 200 |
| 삭제 | DELETE | `/api/users/{id}` | 204 |

> **PATCH는 사용하지 않는다.** 부분 수정도 PUT으로 통일한다.

### 3.2 HTTP 상태 코드 매핑

| 상태 코드 | 의미 | 사용 시점 |
|-----------|------|-----------|
| 200 | OK | GET 조회 성공, PUT 수정 성공 |
| 201 | Created | POST 생성 성공 |
| 204 | No Content | DELETE 삭제 성공 |

> 에러 상태 코드(400, 401, 403, 404, 409, 422, 500, 502)는 Exception Handling 2.3절의 `ErrorType.httpStatus` 참고.

### 3.3 멱등성 규칙

| 메서드 | 멱등성 | 설명 |
|--------|--------|------|
| GET | 멱등 | 동일 요청 = 동일 결과 |
| PUT | 멱등 | 동일 데이터로 재전송 = 동일 결과 |
| DELETE | 멱등 | 이미 삭제된 리소스 재삭제 = 동일 결과 (404 또는 204) |
| POST | 비멱등 | 동일 요청을 반복하면 새 리소스 생성 |

---

## 4. 공통 응답 구조

### 4.1 ApiResponse\<T\>

```java
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorDetail error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).data(data).build();
    }

    public static <T> ApiResponse<T> error(ErrorDetail error) {
        return ApiResponse.<T>builder().success(false).error(error).build();
    }
}
```

### 4.2 ErrorDetail

```java
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDetail {

    /** ErrorCode enum name (e.g., "RESOURCE_NOT_FOUND") */
    private final String code;

    /** 사용자 친화적 메시지 */
    private final String message;

    /** 로그 추적용 ID — 클라이언트가 리포트 시 첨부하면 서버 로그와 즉시 연결된다. */
    private final String traceId;

    /** Bean Validation 필드 오류 — 검증 실패 시에만 포함 */
    private final List<FieldError> fields;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String message;
    }
}
```

### 4.3 JSON 응답 형식

**성공:**
```json
{
  "success": true,
  "data": { "orderId": "ORD-001", "status": "CONFIRMED" }
}
```

**비즈니스 에러:**
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
      { "field": "email",    "message": "올바른 이메일 형식이 아닙니다." },
      { "field": "quantity", "message": "수량은 1 이상이어야 합니다."   }
    ]
  }
}
```

### 4.4 응답 보안 규칙

> 로그 마스킹은 Logging 8절 참고.

| 항목 | 이유 |
|------|------|
| Stack trace | 내부 구조 노출, 공격 벡터 |
| 내부 파일 경로 | 서버 구조 노출 |
| DB 오류 메시지 | DB 종류·스키마 노출 |
| 식별자 원본값 | 유효 ID 확인 수단 제공 |
| 비밀번호·토큰 | 보안 사고 직결 |

---

## 5. Controller 구조

### 5.1 기본 CRUD Controller

> `@PreAuthorize` 인가 패턴은 Authentication & Authorization 8절 참고.

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 관리")
@PreAuthorize("hasAuthority('USER:READ')")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "사용자 목록 조회", description = "검색 조건과 페이징을 적용한 사용자 목록을 반환한다.")
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDTO>>> list(
            @ModelAttribute PageRequest pageRequest) {
        return ResponseEntity.ok(ApiResponse.success(userService.findAll(pageRequest)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "사용자 상세 조회")
    public ResponseEntity<ApiResponse<UserDetailResponseDTO>> get(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('USER:WRITE')")
    @Operation(summary = "사용자 생성")
    public ResponseEntity<ApiResponse<UserResponseDTO>> create(
            @Valid @RequestBody UserCreateRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    @Operation(summary = "사용자 수정")
    public ResponseEntity<ApiResponse<UserResponseDTO>> update(
            @PathVariable String id, @Valid @RequestBody UserUpdateRequestDTO dto) {
        return ResponseEntity.ok(ApiResponse.success(userService.update(id, dto)));
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

### 5.2 반환 타입 규칙

| 규칙 | 설명 |
|------|------|
| `ResponseEntity<ApiResponse<T>>` 통일 | 모든 API 메서드의 반환 타입 |
| DTO만 반환 | Mapper 결과를 Controller에서 직접 반환하지 않는다 |
| POST는 생성된 리소스 반환 | `ApiResponse.success(createdDTO)` + 201 |
| DELETE는 본문 없음 | `ResponseEntity.noContent().build()` + 204 |

### 5.3 PageController와 API Controller 분리

| 구분 | 어노테이션 | 네이밍 | 반환 |
|------|-----------|--------|------|
| 페이지 | `@Controller` | `UserPageController` | `String` (뷰 이름) |
| API | `@RestController` | `UserController` | `ResponseEntity<ApiResponse<T>>` |

```java
@Controller
@RequiredArgsConstructor
public class UserPageController {

    @GetMapping("/user")
    public String userPage() {
        return "user/list";
    }
}
```

---

## 6. DTO 설계

### 6.1 네이밍 규칙

패턴: **`{Domain}{Action}{Direction}DTO`**

| 용도 | 예시 |
|------|------|
| 생성 요청 | `UserCreateRequestDTO` |
| 수정 요청 | `UserUpdateRequestDTO` |
| 목록 응답 | `UserResponseDTO` |
| 상세 응답 | `UserDetailResponseDTO` |


### 6.2 계층별 분리

| 계층 | DTO 사용 |
|------|----------|
| Controller | Request DTO (입력), Response DTO (출력) |
| Service | RequestDTO → Mapper 전달, ResponseDTO → Controller 반환 |
| Mapper | `resultType`/`resultMap`으로 Response DTO 직접 매핑 |

> Mapper에서 DTO 직접 매핑은 SQL 5.4절 참고.
> Entity 불사용 아키텍처의 근거는 Project Architecture 3.1절 참고.

### 6.3 검증 위치

> 커스텀 Validator(`@ValidPhoneNumber` 등)는 Exception Handling 8절 참고.
> `BusinessException` + `ErrorType`은 Exception Handling 2절 참고.

| 유형 | 위치 | 도구 |
|------|------|------|
| 형식 (null, 길이, 정규식) | 요청 DTO | `@NotNull`, `@Size`, `@Pattern` |
| 비즈니스 규칙 (상태 전이, 권한) | Service | Guard Clause + `BusinessException` |
| DB 제약 조건 (유니크 키) | Service | `DataIntegrityViolationException` catch |
| 크로스 필드 (비밀번호 확인) | Service | Guard Clause + `BusinessException` |

```java
// 요청 DTO — 형식 검증
public class UserCreateRequestDTO {

    @NotBlank(message = "사용자 ID는 필수입니다.")
    @Size(max = 20, message = "사용자 ID는 20자 이내여야 합니다.")
    @Schema(description = "사용자 ID", example = "user01")
    private String userId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Schema(description = "비밀번호")
    private String password;

    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(description = "이메일", example = "user@example.com")
    private String email;
}
```

### 6.4 공통 DTO

| DTO | 용도 | 위치 |
|-----|------|------|
| `ApiResponse<T>` | 공통 응답 래퍼 | `global/dto/` |
| `ErrorDetail` | 에러 상세 | `global/dto/` |
| `PageRequest` | 페이징 요청 | `global/dto/` |
| `PageResponse<T>` | 페이징 응답 | `global/dto/` |
| `SelectOptionDTO` | 셀렉트 옵션 | `global/dto/` |

```java
@Getter
@Setter
public class SelectOptionDTO {
    private String value;
    private String label;
}
```

---

## 7. 페이징

### 7.1 PageRequest 구조

```java
@Getter
@Setter
public class PageRequest {

    @Min(value = 1, message = "페이지는 1 이상이어야 합니다.")
    private int page = 1;

    @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
    @Max(value = 100, message = "페이지 크기는 100 이하여야 합니다.")
    private int size = 20;

    private String sortBy;

    private String sortDirection = "DESC";
}
```

Controller에서 `@ModelAttribute`로 바인딩한다.

```java
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<UserResponseDTO>>> list(
        @ModelAttribute PageRequest pageRequest) { ... }
```

### 7.2 PageResponse 구조

```java
@Getter
@Builder
public class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
}
```

### 7.3 Controller → Service 흐름

```java
// Controller
@GetMapping
public ResponseEntity<ApiResponse<PageResponse<UserResponseDTO>>> list(
        @ModelAttribute PageRequest pageRequest) {
    return ResponseEntity.ok(ApiResponse.success(userService.findAll(pageRequest)));
}
```

---

## 8. Swagger/OpenAPI

### 8.1 의존성

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

### 8.2 어노테이션 표준

| 위치 | 어노테이션 | 용도 |
|------|-----------|------|
| Controller 클래스 | `@Tag(name = "...")` | API 그룹 분류 |
| Controller 메서드 | `@Operation(summary, description)` | 엔드포인트 설명 |
| DTO 필드 | `@Schema(description, example)` | 필드 설명 |
| Controller 메서드 | `@ApiResponse(responseCode, description)` | 상태 코드별 응답 설명 |

```java
@RestController
@RequestMapping("/api/users")
@Tag(name = "사용자 관리")
public class UserController {

    @Operation(
        summary = "사용자 목록 조회",
        description = "검색 조건과 페이징을 적용한 사용자 목록을 반환한다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공"),
        @ApiResponse(responseCode = "401", description = "미인증"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<UserResponseDTO>>> list(...) { ... }
}
```

```java
// DTO 필드 어노테이션
public class UserCreateRequestDTO {

    @NotBlank
    @Schema(description = "사용자 ID", example = "user01")
    private String userId;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;
}
```

> Swagger 2.x 어노테이션(`@Api`, `@ApiOperation`, `@ApiParam`)은 사용하지 않는다.

### 8.3 그룹 설정

단일 패널은 별도 그룹 설정 없이 기본값을 사용한다. 기능 영역이 확장되면 `GroupedOpenApi`로 분리한다.

```java
// 확장 시 그룹 분리 예시
@Bean
public GroupedOpenApi adminApi() {
    return GroupedOpenApi.builder()
            .group("admin")
            .pathsToMatch("/api/users/**", "/api/roles/**")
            .build();
}
```

### 8.4 환경별 노출 제한

> Security 11절 참고.

```yaml
# application-base.yml
springdoc:
  api-docs:
    enabled: true

# application-prod.yml
springdoc:
  api-docs:
    enabled: false      # 운영 환경에서 Swagger UI 비활성화
```

---

## 9. Content-Type

### 9.1 요청

| Content-Type | 사용 시점 |
|-------------|-----------|
| `application/json` | POST, PUT 요청 본문 |
| `multipart/form-data` | 파일 업로드 |

### 9.2 응답

| 구분 | Content-Type |
|------|-------------|
| REST API | `application/json` |
| Thymeleaf 페이지 | `text/html` |

---

## 10. 파일 구조

```
src/main/java/{base-package}/
│
├── global/dto/
│   ├── ApiResponse.java
│   ├── ErrorDetail.java
│   ├── PageRequest.java
│   ├── PageResponse.java
│   └── SelectOptionDTO.java
│
└── domain/{domain}/
    ├── controller/
    │   ├── {Domain}Controller.java          # @RestController (API)
    │   └── {Domain}PageController.java      # @Controller (Thymeleaf)
    └── dto/
        ├── request/
        │   ├── {Domain}CreateRequestDTO.java
        │   └── {Domain}UpdateRequestDTO.java
        └── response/
            ├── {Domain}ResponseDTO.java
            └── {Domain}DetailResponseDTO.java
```

---

## 11. 체크리스트

```
□ 1. URL이 복수 명사 + kebab-case인지 확인
      → /api/users, /api/audit-logs

□ 2. HTTP 메서드와 상태 코드 매핑
      → GET=200, POST=201, PUT=200, DELETE=204

□ 3. 모든 API가 ApiResponse<T>로 래핑되는지 확인
      → ResponseEntity<ApiResponse<T>> 반환

□ 4. DTO 네이밍: {Domain}{Action}{Direction}DTO
      → DB 결과를 DTO에 직접 매핑, Entity 클래스 없음

□ 5. 검증 위치 확인
      → 형식=DTO @Valid, 비즈니스=Service Guard Clause

□ 6. 페이징: PageRequest/PageResponse 사용

□ 7. Swagger 어노테이션 부여
      → @Tag, @Operation, @Schema, @ApiResponse

□ 8. Controller에 @PreAuthorize 부여
      → Authentication & Authorization 8절 패턴

□ 9. 응답에 민감 정보 미포함 확인
      → stack trace, DB 오류, 내부 경로 없음
```

---

*Last updated: 2026-02-24*
