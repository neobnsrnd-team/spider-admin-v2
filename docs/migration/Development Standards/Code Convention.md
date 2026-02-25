# Code Convention

## 1. 개요

코드 스타일, 네이밍 규칙, 클래스 구조 패턴, Enum 매핑을 정의한다. Spotless + Google Java Format (AOSP 변형)으로 포맷팅을 자동 강제한다.

> 아키텍처 규칙(레이어 의존성, 패키지 구조, Entity 불사용)은 Project Architecture 문서 참고.

---

## 2. Spotless 설정

### 2.1 Maven 플러그인

```xml
<plugin>
    <groupId>com.diffplug.spotless</groupId>
    <artifactId>spotless-maven-plugin</artifactId>
    <version>2.44.0</version>
    <configuration>
        <java>
            <googleJavaFormat>
                <version>1.25.2</version>
                <style>AOSP</style>
            </googleJavaFormat>
            <importOrder>
                <order>java|javax,org,com,,\#</order>
            </importOrder>
            <removeUnusedImports/>
        </java>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
            <phase>validate</phase>
        </execution>
    </executions>
</plugin>
```

### 2.2 포맷팅 규칙 요약

| 항목 | 값 |
|------|-----|
| 스타일 | Google Java Format — AOSP 변형 |
| 들여쓰기 | 4칸 스페이스 |
| 줄 길이 | 100자 (Google Java Format 기본값) |
| import 순서 | `java\|javax` → `org` → `com` → 프로젝트 → `static` |
| 미사용 import | 자동 제거 |

### 2.3 실행

```bash
# 포맷 검사 (CI에서 실행)
mvn spotless:check

# 자동 포맷 적용
mvn spotless:apply
```

> CI 파이프라인에서 `spotless:check`를 강제한다. CI Strategy 참고.

---

## 3. 네이밍 규칙

### 3.1 클래스 네이밍

| 용도 | 패턴 | 예시 |
|------|------|------|
| REST Controller | `{Domain}Controller` | `UserController` |
| Page Controller | `{Domain}PageController` | `UserPageController` |
| Service | `{Domain}Service` | `UserService` |
| Mapper | `{Domain}Mapper` | `UserMapper` |
| 생성 요청 DTO | `{Domain}CreateRequestDTO` | `UserCreateRequestDTO` |
| 수정 요청 DTO | `{Domain}UpdateRequestDTO` | `UserUpdateRequestDTO` |
| 목록 응답 DTO | `{Domain}ResponseDTO` | `UserResponseDTO` |
| 상세 응답 DTO | `{Domain}DetailResponseDTO` | `UserDetailResponseDTO` |


### 3.2 금지 클래스명

| 금지 | 이유 | 대안 |
|------|------|------|
| `{Domain}ServiceImpl` | 인터페이스 + Impl 불사용 | `{Domain}Service` |
| `{Domain}Entity` | Entity 클래스 불사용 | DTO 직접 매핑 |
| `{Domain}Converter` | Converter 클래스 불사용 | ResultMap 직접 매핑 |
| `{Domain}VO` / `{Domain}Model` | DTO로 통일 | `{Domain}ResponseDTO` 등 |

> 아키텍처 근거는 Project Architecture 3.1절 참고.

### 3.3 메서드 네이밍

**Controller:**

| 작업 | 메서드명 | HTTP 매핑 |
|------|----------|-----------|
| 목록 조회 | `list()` | `@GetMapping` |
| 단건 조회 | `get()` | `@GetMapping("/{id}")` |
| 생성 | `create()` | `@PostMapping` |
| 수정 | `update()` | `@PutMapping("/{id}")` |
| 삭제 | `delete()` | `@DeleteMapping("/{id}")` |

**Service:**

| 작업 | 메서드명 |
|------|----------|
| 목록 조회 | `findAll(PageRequest)` |
| 단건 조회 | `getById(id)` |
| 생성 | `create(RequestDTO)` |
| 수정 | `update(id, RequestDTO)` |
| 삭제 | `delete(id)` |
| 존재 확인 | `existsById(id)` |

**Mapper:**

> SQL 4절의 MyBatis Statement ID 규칙을 따른다.

### 3.4 DB 네이밍

| 대상 | 규칙 | 예시 |
|------|------|------|
| 테이블 | `UPPER_SNAKE_CASE` | `USER`, `ROLE_MENU` |
| 컬럼 | `UPPER_SNAKE_CASE` | `USER_ID`, `CREATED_AT` |
| 프라이머리 키 | `{TABLE}_ID` 또는 `ID` | `USER_ID` |
| 시퀀스 (Oracle) | `SEQ_{TABLE}` | `SEQ_USER` |

---

## 4. 클래스 구조

### 4.1 Controller

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 관리")
@PreAuthorize("hasAuthority('USER:READ')")
public class UserController {

    private final UserService userService;

    // 메서드 순서: list → get → create → update → delete
}
```

> 전체 CRUD Controller 예제는 API Design 5.1절 참고.

### 4.2 Service

```java
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // 조회 메서드: readOnly = true (클래스 레벨 상속)
    // 쓰기 메서드: @Transactional 오버라이드
}
```

> Guard Clause + BusinessException 패턴은 Exception Handling 5절 참고.

### 4.3 Mapper

```java
@Mapper
public interface UserMapper {
    // MyBatis XML과 1:1 매핑
    // Statement ID 규칙은 SQL 4절 참고
}
```

### 4.4 Request DTO

```java
@Getter
@Setter
public class UserCreateRequestDTO {

    @NotBlank(message = "사용자 ID는 필수입니다.")
    @Size(max = 20)
    @Schema(description = "사용자 ID", example = "user01")
    private String userId;
}
```

> 검증 위치 전략은 API Design 6.3절 참고.

### 4.5 Response DTO

```java
@Getter
@Setter
public class UserResponseDTO {

    @Schema(description = "사용자 ID", example = "user01")
    private String userId;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String userName;

    // Mapper resultType/resultMap에서 직접 매핑
}
```

---

## 5. Enum & TypeHandler

### 5.1 CodeEnum 인터페이스

DB에 코드값(`"01"`, `"02"`)으로 저장되는 Enum은 공통 인터페이스를 구현한다.

```java
public interface CodeEnum {
    String getCode();
}
```

### 5.2 Enum 구현

```java
@Getter
@RequiredArgsConstructor
public enum UserStatus implements CodeEnum {
    ACTIVE("01"),
    INACTIVE("02"),
    LOCKED("03");

    private final String code;

    public static UserStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(e -> e.getCode().equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown code: " + code));
    }
}
```

### 5.3 TypeHandler

```java
@MappedTypes(UserStatus.class)
public class UserStatusTypeHandler extends BaseTypeHandler<UserStatus> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i,
                                     UserStatus parameter, JdbcType jdbcType) throws SQLException {
        ps.setString(i, parameter.getCode());
    }

    @Override
    public UserStatus getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return UserStatus.fromCode(rs.getString(columnName));
    }

    @Override
    public UserStatus getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return UserStatus.fromCode(rs.getString(columnIndex));
    }

    @Override
    public UserStatus getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return UserStatus.fromCode(cs.getString(columnIndex));
    }
}
```

### 5.4 등록

```yaml
mybatis:
  type-handlers-package: {base-package}.global.typehandler
```

### 5.5 사용 범위

| 계층 | Enum 사용 |
|------|-----------|
| Controller / DTO | `UserStatus` (Enum 타입) |
| Service | `UserStatus` (Enum 타입) |
| Mapper XML | TypeHandler가 자동 변환 (`code` ↔ Enum) |
| DB | `VARCHAR` 코드값 (`"01"`, `"02"`) |

---

## 6. 체크리스트

```
□ 1. Spotless 포맷 통과 (mvn spotless:check)

□ 2. 클래스명이 {Domain}{Role} 패턴인지 확인
      → ServiceImpl, Entity, Converter, VO, Model 금지

□ 3. Controller 메서드: list/get/create/update/delete

□ 4. DB 테이블·컬럼: UPPER_SNAKE_CASE

□ 5. CodeEnum 기반 Enum에 TypeHandler 등록
```

---

*Last updated: 2026-02-24*
