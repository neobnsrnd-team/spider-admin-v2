# Authentication & Authorization

## 1. 개요

Spring Security 기반 메뉴 단위 접근 제어 표준이다. 메뉴→리소스 매핑을 YAML로 선언적으로 관리하며, 권한 소스(USER_MENU / ROLE_MENU)를 설정으로 교체할 수 있다.

### 1.1 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│  Authentication                                             │
│                                                             │
│  POST /login (userId, password)                             │
│    → DaoAuthenticationProvider                              │
│      → CustomUserDetailsService                             │
│        → AuthenticatedUserFinder                            │
│          → MenuAuthorityLoader (설정에 따라 구현체 교체)      │
│            → AuthorityConverter (리소스 파생 포함)            │
│              → CustomUserDetails (Principal)                │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  Authorization (@EnableMethodSecurity)                       │
│                                                             │
│  GET /api/resources                                         │
│    → @PreAuthorize("hasAuthority('RESOURCE_MGMT:READ')")    │
│      → Spring Method Security                               │
│        → Principal.getAuthorities()에서 매칭                 │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **API 단위 보호** | 모든 `@RestController`에 `@PreAuthorize` 필수 |
| **DB가 유일한 판단 기준** | 권한 테이블에 레코드 없으면 403 |
| **리소스 의존성은 YAML** | 메뉴→리소스 매핑을 설정 파일에서 선언적으로 관리 |
| **권한 소스 교체 가능** | USER_MENU / ROLE_MENU 중 YAML 설정으로 전환 |
| **WRITE ⊃ READ** | WRITE 권한은 READ를 포함 |

### 1.3 금지 사항

| 금지 항목 | 대안 |
|-----------|------|
| `SecurityContextHolder` 직접 호출 | `SecurityUtil.getCurrentUser()` |
| `@PreAuthorize("hasRole(…)")` | `@PreAuthorize("hasAuthority(…)")` |
| `HttpSession.getAttribute()` 로 사용자 접근 | `SecurityUtil.getCurrentUserId()` |
| `@PreAuthorize` 없는 `@RestController` | 모든 컨트롤러에 필수 부여 |
| 에러 메시지에서 "사용자 없음"/"비밀번호 틀림" 구분 | 동일 메시지 (User Enumeration 방지) |

---

## 2. DB Schema

```
USER                             ROLE
┌──────────────────┐             ┌──────────────────┐
│ USER_ID (PK)     │             │ ROLE_ID (PK)     │
│ PASSWORD         │             │ ROLE_NAME        │
│ ROLE_ID (FK) ────│────────────→│ USE_YN           │
│ USER_STATE       │             └──────────────────┘
│ LOGIN_FAIL_CNT   │                      │ 1:N
└──────────────────┘             ┌──────────────────┐
         │                       │ ROLE_MENU        │
         │                       │ ROLE_ID (PK,FK)  │
         │                       │ MENU_ID (PK,FK)  │
         │                       │ AUTH_CODE (R/W)  │
         │ 1:N                   └──────────────────┘
┌──────────────────┐             ┌──────────────────┐
│ USER_MENU        │             │ MENU             │
│ USER_ID (PK,FK)  │             │ MENU_ID (PK)     │
│ MENU_ID (PK,FK) ─│────────────→│ PARENT_MENU_ID   │
│ AUTH_CODE (R/W)  │             │ MENU_NAME        │
└──────────────────┘             │ MENU_URL         │
                                 │ DISPLAY_YN       │
                                 │ USE_YN           │
                                 │ SORT_ORDER       │
                                 └──────────────────┘
```

| 테이블 | 용도 |
|--------|------|
| `USER_MENU` | 사용자별 개별 메뉴 권한 (세밀한 제어) |
| `ROLE_MENU` | 역할별 메뉴 권한 템플릿 (일괄 제어) |
| `MENU` | 메뉴 정의 (계층 구조, 표시 여부) |

두 테이블은 **동일한 구조**(MENU_ID + AUTH_CODE)를 가지므로 설정으로 교체 가능하다.

---

## 3. 설정 파일 설계

### 3.1 security-access.yml

> **52개 메뉴의 전체 리소스 의존성 정의:**
> [`src/main/resources/menu-resource-permissions.yml`](../../../src/main/resources/menu-resource-permissions.yml)
>
> - 화면명: `Product Requirements/00-overview.md` 기준
> - `RES_XXX`: `Package Structure.md` 도메인 패키지명 (대문자)
> - 양식: `화면명_LEVEL : RES_자기도메인_LEVEL, RES_참조도메인_READ, ...`
> - `_WRITE` 미정의 = 조회 전용 (Template B/C)

```yaml
# ==============================================
# Menu-Based Access Control Configuration
# ==============================================

security:
  access:

    # ------------------------------------------
    # 권한 소스 설정
    # ------------------------------------------
    authority-source: USER_MENU

    # ------------------------------------------
    # 리소스 의존성 정의
    # ------------------------------------------
    # 양식: 화면명_LEVEL : RES_자기도메인_LEVEL, RES_참조도메인_READ
    # _WRITE 시 자기 도메인만 WRITE 승격, 참조 도메인은 READ 유지
    # 예외: RES_RELOAD_WRITE 등 실행 행위가 필요한 경우
    #
    # 전체 목록: src/main/resources/menu-resource-permissions.yml
    # ------------------------------------------
    # 예시:
    역할 관리_READ: RES_ROLE_READ, RES_MENU_READ
    역할 관리_WRITE: RES_ROLE_WRITE, RES_MENU_READ

    거래 관리_READ: RES_TRANSACTION_READ, RES_WASGROUP_READ, RES_WASINSTANCE_READ, RES_ORG_READ, RES_MESSAGE_READ
    거래 관리_WRITE: RES_TRANSACTION_WRITE, RES_WASGROUP_READ, RES_WASINSTANCE_READ, RES_ORG_READ, RES_MESSAGE_READ

    요청처리 APP 맵핑 관리_READ: RES_TRANSPORT_READ, RES_TRANSACTION_READ, RES_WASINSTANCE_READ, RES_GATEWAY_READ
    요청처리 APP 맵핑 관리_WRITE: RES_TRANSPORT_WRITE, RES_TRANSACTION_READ, RES_WASINSTANCE_READ, RES_GATEWAY_READ, RES_RELOAD_WRITE
```

### 3.2 application.yml에서 import

```yaml
spring:
  config:
    import: classpath:security-access.yml
```

---

## 4. 인증 흐름

```
POST /login (userId, password)
  │
  ├─ DaoAuthenticationProvider
  │     ├─ CustomUserDetailsService.loadUserByUsername(userId)
  │     │     └─ AuthenticatedUserFinder.findByUserId(userId)
  │     │           ├─ UserMapper.selectById()
  │     │           ├─ MenuAuthorityLoader.loadAuthorities(userId)
  │     │           │     ├─ [USER_MENU 모드] UserMenuMapper로 직접 조회
  │     │           │     └─ [ROLE_MENU 모드] user.roleId → RoleMenuMapper로 조회
  │     │           └─ AuthorityConverter.convert()
  │     │                 ├─ 메뉴 권한 변환 (WRITE → READ + WRITE)
  │     │                 └─ 리소스 권한 파생 (YAML 기반)
  │     │
  │     ├─ PasswordEncoder.matches(raw, stored)
  │     └─ UserDetails 상태 검증
  │           ├─ isEnabled()          → userState == NORMAL
  │           └─ isAccountNonLocked() → loginFailCount < MAX
  │
  ├─ 성공 → SuccessHandler
  │     ├─ loginFailCount 초기화
  │     └─ 메인 페이지 리다이렉트
  │
  └─ 실패 → FailureHandler
        ├─ loginFailCount 증가
        └─ 로그인 페이지 리다이렉트
```

### 4.1 CustomUserDetailsService

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthenticatedUserFinder authenticatedUserFinder;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        AuthenticatedUser user = authenticatedUserFinder.findByUserId(userId);
        if (user == null) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId);
        }
        return new CustomUserDetails(user);
    }
}
```

### 4.2 AuthenticatedUserFinder

```java
@Component
@RequiredArgsConstructor
public class AuthenticatedUserFinderImpl implements AuthenticatedUserFinder {

    private final UserMapper userMapper;
    private final MenuAuthorityLoader menuAuthorityLoader;
    private final AuthorityConverter authorityConverter;

    @Override
    @Transactional(readOnly = true)
    public AuthenticatedUser findByUserId(String userId) {
        User user = userMapper.selectById(userId);
        if (user == null) return null;

        // 설정에 따라 USER_MENU 또는 ROLE_MENU에서 로드
        List<MenuPermission> rawPermissions = menuAuthorityLoader.loadAuthorities(user);

        // 메뉴 권한 변환 + 리소스 권한 파생
        Set<MenuAuthority> authorities = authorityConverter.convert(rawPermissions);

        return new AuthenticatedUser(
            user.getUserId(), user.getPassword(),
            user.getUserState(), user.getLoginFailCount(),
            authorities
        );
    }
}
```

---

## 5. 권한 소스 교체 (USER_MENU / ROLE_MENU)

### 5.1 공통 인터페이스

```java
/**
 * 사용자의 메뉴 권한 원본 데이터를 로드하는 전략 인터페이스.
 * 설정에 따라 USER_MENU 또는 ROLE_MENU 구현체가 선택된다.
 */
public interface MenuAuthorityLoader {
    List<MenuPermission> loadAuthorities(User user);
}

/**
 * DB에서 로드한 원시 권한 데이터.
 * USER_MENU, ROLE_MENU 모두 동일한 구조 (menuId + authCode).
 */
public record MenuPermission(
    String menuId,
    String authCode     // "R" 또는 "W"
) {}
```

### 5.2 USER_MENU 구현체

```java
/**
 * USER_MENU 테이블에서 사용자별 메뉴 권한을 직접 로드.
 * 사용자마다 서로 다른 메뉴 접근 범위를 가질 수 있다.
 *
 * 활성화 조건: security.access.authority-source=USER_MENU
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "security.access.authority-source",
    havingValue = "USER_MENU",
    matchIfMissing = true       // 기본값
)
public class UserMenuAuthorityLoader implements MenuAuthorityLoader {

    private final UserMenuMapper userMenuMapper;

    @Override
    public List<MenuPermission> loadAuthorities(User user) {
        return userMenuMapper.selectAllByUserId(user.getUserId());
    }
}
```

```sql
<!-- UserMenuMapper.xml -->
<select id="selectAllByUserId" resultType="MenuPermission">
    SELECT MENU_ID, AUTH_CODE
    FROM USER_MENU
    WHERE USER_ID = #{userId}
</select>
```

### 5.3 ROLE_MENU 구현체

```java
/**
 * ROLE_MENU 테이블에서 역할 기반 메뉴 권한을 로드.
 * 사용자의 ROLE_ID로 역할을 찾고, 해당 역할에 부여된 메뉴 권한을 반환한다.
 * 같은 역할을 가진 사용자들은 동일한 메뉴 접근 범위를 가진다.
 *
 * 활성화 조건: security.access.authority-source=ROLE_MENU
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "security.access.authority-source",
    havingValue = "ROLE_MENU"
)
public class RoleMenuAuthorityLoader implements MenuAuthorityLoader {

    private final RoleMenuMapper roleMenuMapper;

    @Override
    public List<MenuPermission> loadAuthorities(User user) {
        if (user.getRoleId() == null) {
            return List.of();
        }
        return roleMenuMapper.selectAllByRoleId(user.getRoleId());
    }
}
```

```sql
<!-- RoleMenuMapper.xml -->
<select id="selectAllByRoleId" resultType="MenuPermission">
    SELECT MENU_ID, AUTH_CODE
    FROM ROLE_MENU
    WHERE ROLE_ID = #{roleId}
</select>
```

### 5.4 전환 방법

```yaml
# security-access.yml

# 사용자별 개별 권한 (기본값)
security:
  access:
    authority-source: USER_MENU

# 역할 기반 일괄 권한으로 전환
security:
  access:
    authority-source: ROLE_MENU
```

**재시작만으로 전환**된다. 코드 변경 불필요.

### 5.5 비교

| 항목 | USER_MENU | ROLE_MENU |
|------|-----------|-----------|
| 권한 부여 단위 | 사용자 개인 | 역할 (그룹) |
| 세밀한 제어 | 사용자마다 다르게 가능 | 같은 역할은 동일 |
| 관리 편의성 | 사용자 수에 비례하여 복잡 | 역할만 관리하면 일괄 적용 |
| 적합한 상황 | 소규모 조직, 예외 권한 필요 시 | 대규모 조직, 표준화된 역할 체계 |
| 권한 변경 영향 | 해당 사용자만 | 역할에 속한 모든 사용자 |

---

## 6. 리소스 의존성 파생

### 6.1 문제

메뉴 A의 화면이 메뉴 B의 데이터를 참조하는 경우 (셀렉트박스 등), 사용자에게 메뉴 A 권한만 있으면 메뉴 B의 API에서 403이 발생한다.

```
[배치 관리] 화면 → GET /api/was/instances (WAS 관리 메뉴 소속)
  사용자 권한: BATCH_APP만 보유
  → WAS_INSTANCE 권한 없음 → 403
```

### 6.2 해결: YAML 기반 리소스 파생

로그인 시 메뉴 권한을 변환할 때, YAML에 정의된 리소스 권한을 자동으로 추가한다.

```
FWK_USER_MENU: user1 → BATCH_APP (W)

AuthorityConverter.convert()
  ├─ 메뉴 권한: {BATCH_APP:READ, BATCH_APP:WRITE}
  └─ YAML 조회: BATCH_APP → [{resource: RES_WAS_INSTANCE, level: READ}]
     → 파생: {RES_WAS_INSTANCE:READ}

최종: {BATCH_APP:READ, BATCH_APP:WRITE, RES_WAS_INSTANCE:READ}
```

### 6.3 Properties 클래스

```java
@Component
@ConfigurationProperties(prefix = "security.access")
@Getter @Setter
public class SecurityAccessProperties {

    /** 권한 소스: USER_MENU 또는 ROLE_MENU */
    private String authoritySource = "USER_MENU";

    /** 메뉴별 파생 리소스 권한 */
    private Map<String, List<ResourceGrant>> resourceDependencies = new LinkedHashMap<>();

    @Getter @Setter
    public static class ResourceGrant {
        private String resource;
        private String level = "READ";      // 기본값: READ
    }

    /**
     * 메뉴 ID에 대한 파생 MenuAuthority 목록 반환
     */
    public List<MenuAuthority> getDerivedAuthorities(String menuId) {
        List<ResourceGrant> grants = resourceDependencies.get(menuId);
        if (grants == null) return List.of();

        return grants.stream()
                .map(g -> new MenuAuthority(
                    g.getResource(),
                    MenuAccessLevel.valueOf(g.getLevel())
                ))
                .toList();
    }
}
```

### 6.4 AuthorityConverter

```java
@Component
@RequiredArgsConstructor
public class AuthorityConverter {

    private final SecurityAccessProperties properties;

    /**
     * DB 원시 권한 → 최종 MenuAuthority Set 변환
     * 1. 메뉴 권한 변환 (WRITE → READ + WRITE)
     * 2. YAML 기반 리소스 권한 파생
     */
    public Set<MenuAuthority> convert(List<MenuPermission> permissions) {
        return permissions.stream()
                .flatMap(p -> Stream.concat(
                        toMenuAuthorities(p),
                        toDerivedAuthorities(p)
                ))
                .collect(Collectors.toUnmodifiableSet());
    }

    private Stream<MenuAuthority> toMenuAuthorities(MenuPermission p) {
        if ("W".equals(p.authCode())) {
            return Stream.of(
                new MenuAuthority(p.menuId(), MenuAccessLevel.READ),
                new MenuAuthority(p.menuId(), MenuAccessLevel.WRITE)
            );
        }
        return Stream.of(new MenuAuthority(p.menuId(), MenuAccessLevel.READ));
    }

    private Stream<MenuAuthority> toDerivedAuthorities(MenuPermission p) {
        return properties.getDerivedAuthorities(p.menuId()).stream();
    }
}
```

---

## 7. 인가 모델

### 7.1 @EnableMethodSecurity

`SecurityConfig`에 선언하여 `@PreAuthorize`를 활성화한다.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    // ...
}
```

### 7.2 Authority 형식

`CustomUserDetails.getAuthorities()`가 반환하는 `GrantedAuthority` 형식:

```
{MENU_ID}:{LEVEL}
```

| 예시 | 의미 |
|------|------|
| `USER:READ` | USER 메뉴 읽기 |
| `USER:WRITE` | USER 메뉴 쓰기 |
| `RES_WAS_INSTANCE:READ` | WAS 인스턴스 리소스 참조 (파생) |

`AuthorityConverter`가 WRITE → READ + WRITE로 확장하므로, WRITE 권한자는 READ 체크도 통과한다.

### 7.3 @PreAuthorize 패턴

| 용도 | 어노테이션 |
|------|-----------|
| 단일 메뉴 읽기 | `@PreAuthorize("hasAuthority('USER:READ')")` |
| 단일 메뉴 쓰기 | `@PreAuthorize("hasAuthority('USER:WRITE')")` |
| 크로스 리소스 읽기 (OR) | `@PreAuthorize("hasAnyAuthority('WAS_INSTANCE:READ', 'RES_WAS_INSTANCE:READ')")` |

- `hasAuthority`: 정확히 일치하는 권한이 있는지 확인
- `hasAnyAuthority`: OR 조건 — 하나라도 매칭되면 통과
- `hasRole()` 사용 금지 — 메뉴 기반 모델과 맞지 않음

### 7.4 전체 흐름 예시

```
[로그인 시]
  DB: user1 → BATCH_APP (W)
  변환 → GrantedAuthority: {"BATCH_APP:READ", "BATCH_APP:WRITE", "RES_WAS_INSTANCE:READ"}

[GET /api/was/instances 요청]
  @PreAuthorize("hasAnyAuthority('WAS_INSTANCE:READ', 'RES_WAS_INSTANCE:READ')")
    → WAS_INSTANCE:READ? NO
    → RES_WAS_INSTANCE:READ? YES → 허용

[POST /api/was/instances 요청]
  @PreAuthorize("hasAuthority('WAS_INSTANCE:WRITE')")
    → WAS_INSTANCE:WRITE? NO → 403 (참조만 허용, CRUD 차단)
```

---

## 8. Controller 패턴

### 8.1 기본: 클래스 READ + 메서드 WRITE

```java
@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasAuthority('USER:READ')")                   // 클래스: READ
public class UserController {

    @GetMapping                                               // READ 상속
    public ResponseEntity<...> list() { ... }

    @PostMapping
    @PreAuthorize("hasAuthority('USER:WRITE')")               // WRITE 오버라이드
    public ResponseEntity<...> create() { ... }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<...> update() { ... }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER:WRITE')")
    public ResponseEntity<...> delete() { ... }
}
```

### 8.2 크로스 리소스: 조회에 리소스 OR 추가

```java
@RestController
@RequestMapping("/api/was/instances")
public class WasInstanceController {

    @GetMapping                                               // 관리자 + 참조자 모두 허용
    @PreAuthorize("hasAnyAuthority('WAS_INSTANCE:READ', 'RES_WAS_INSTANCE:READ')")
    public ResponseEntity<...> list() { ... }

    @PostMapping                                              // 관리자만
    @PreAuthorize("hasAuthority('WAS_INSTANCE:WRITE')")
    public ResponseEntity<...> create() { ... }
}
```

### 8.3 규칙 정리

| 규칙 | 설명 |
|------|------|
| 모든 `@RestController`에 `@PreAuthorize` 필수 | 누락 = 무방비 |
| GET → `hasAuthority('{MENU}:READ')` | 읽기 권한 |
| POST/PUT/DELETE → `hasAuthority('{MENU}:WRITE')` | 쓰기 권한 |
| 크로스 리소스 조회 → `hasAnyAuthority` OR | `'{본래메뉴}:READ', 'RES_{리소스}:READ'` |
| CUD는 본래 메뉴만 허용 | 리소스 참조 권한으로 수정 불가 |

---

## 9. Principal 설계

### 9.1 AuthenticatedUser / MenuAuthority

```java
public record AuthenticatedUser(
    String userId,
    String password,
    UserState userState,
    int loginFailCount,
    Set<MenuAuthority> authorities      // 메뉴 권한 + 파생 리소스 권한
) {}

public record MenuAuthority(
    String menuId,
    MenuAccessLevel level               // READ 또는 WRITE
) {}

public enum MenuAccessLevel {
    READ("R"), WRITE("W");
    // WRITE는 READ를 포함
}
```

### 9.2 CustomUserDetails

```java
@Getter
public class CustomUserDetails implements UserDetails {

    private final AuthenticatedUser user;

    @Override public String getUsername() { return user.userId(); }
    @Override public String getPassword() { return user.password(); }

    @Override
    public boolean isEnabled() {
        return user.userState() == UserState.NORMAL;
    }

    @Override
    public boolean isAccountNonLocked() {
        final int MAX_LOGIN_FAIL_COUNT = 5; // TODO: 설정에서 주입받도록 변경
        return user.loginFailCount() < MAX_LOGIN_FAIL_COUNT;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.authorities().stream()
            .map(a -> new SimpleGrantedAuthority(a.menuId() + ":" + a.level().name()))
            .toList();
    }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
```

---

## 10. SecurityUtil

```java
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SecurityUtil {

    public static final String SYSTEM_USER_ID = "SYSTEM";

    public static CustomUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails details) {
            return details;
        }
        return null;
    }

    public static String getCurrentUserId() {
        CustomUserDetails user = getCurrentUser();
        return (user != null) ? user.getUsername() : null;
    }

    public static String getCurrentUserIdOrSystem() {
        String userId = getCurrentUserId();
        return (userId != null) ? userId : SYSTEM_USER_ID;
    }
}
```

`SecurityContextHolder` 접근은 **이 클래스에만** 허용한다.

---

## 11. 계정 보호

### 11.1 계정 상태

| 상태 | `isEnabled()` | `isAccountNonLocked()` | 로그인 |
|------|---------------|------------------------|--------|
| 정상 | `true` | `failCount < MAX` | 가능 |
| 삭제 | `false` | — | 불가 |
| 정지 | `false` | — | 불가 |
| 잠금 (실패 초과) | `true` | `false` | 불가 |

- MAX 값은 설정으로 관리 (기본: 5)
- 잠금 해제는 관리자가 `LOGIN_FAIL_CNT`를 초기화
- 시간 기반 자동 해제는 선택 사항

### 11.2 에러 메시지 (User Enumeration 방지)

```java
private String mapMessage(AuthenticationException exception) {
    if (exception instanceof BadCredentialsException)
        return "아이디 또는 비밀번호가 올바르지 않습니다.";
    if (exception instanceof LockedException)
        return "로그인 실패 횟수 초과로 계정이 잠겼습니다. 관리자에게 문의하세요.";
    if (exception instanceof DisabledException)
        return "비활성화된 계정입니다.";
    return "로그인에 실패했습니다.";
}
```

"사용자 없음"과 "비밀번호 오류"를 **구분하지 않는다.**

---

## 12. 테스트

### 12.1 인가 테스트 예시

```java
@WebMvcTest(WasInstanceController.class)
class WasInstanceControllerAuthTest {

    @Autowired MockMvc mockMvc;
    @MockBean  WasInstanceService service;

    @Test
    @DisplayName("미인증 → 401")
    void unauthenticated() throws Exception {
        mockMvc.perform(get("/api/was/instances"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(authorities = {"BATCH_APP:READ", "RES_WAS_INSTANCE:READ"})
    @DisplayName("파생 리소스 권한으로 조회 → 200")
    void derivedResource_read() throws Exception {
        mockMvc.perform(get("/api/was/instances"))
            .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = {"BATCH_APP:READ", "RES_WAS_INSTANCE:READ"})
    @DisplayName("리소스 참조 권한으로 생성 시도 → 403")
    void derivedResource_cannotWrite() throws Exception {
        mockMvc.perform(post("/api/was/instances")
                .contentType(APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = {"WAS_INSTANCE:READ", "WAS_INSTANCE:WRITE"})
    @DisplayName("관리 메뉴 WRITE 권한으로 생성 → 201")
    void directMenu_write() throws Exception {
        mockMvc.perform(post("/api/was/instances")
                .contentType(APPLICATION_JSON).content("{}"))
            .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(authorities = {"MESSAGE:READ"})
    @DisplayName("무관한 메뉴 권한 → 403")
    void noAccess() throws Exception {
        mockMvc.perform(get("/api/was/instances"))
            .andExpect(status().isForbidden());
    }
}
```

### 12.2 테스트 체크리스트

```
인가:
- [ ] 미인증 → 401
- [ ] 관리 메뉴 READ + GET → 200
- [ ] 관리 메뉴 READ + POST → 403
- [ ] 관리 메뉴 WRITE + POST → 201
- [ ] 파생 리소스 READ + GET → 200
- [ ] 파생 리소스 READ + POST → 403
- [ ] 무관한 메뉴 권한 → 403

계정 보호:
- [ ] 비활성/잠금 계정 로그인 → 실패
- [ ] 로그인 실패 시 failCount 증가, MAX 초과 시 잠금
- [ ] 로그인 성공 시 failCount 초기화

권한 소스 전환:
- [ ] authority-source: USER_MENU → USER_MENU 테이블에서 로드
- [ ] authority-source: ROLE_MENU → ROLE_MENU 테이블에서 로드
- [ ] YAML 전환 후 재시작 시 정상 동작

리소스 파생:
- [ ] YAML에 의존성 추가 후 재시작 → 파생 권한 반영
- [ ] YAML에서 의존성 제거 후 재시작 → 파생 권한 소멸
```

---

## 13. 파일 구조

```
src/main/java/{base-package}/
│
├── global/security/
│   ├── SecurityConfig.java                    # @EnableMethodSecurity + FilterChain
│   ├── CustomUserDetailsService.java          # UserDetailsService 구현
│   ├── CustomUserDetails.java                 # UserDetails 구현 (Principal)
│   ├── CustomAuthenticationSuccessHandler.java
│   ├── CustomAuthenticationFailureHandler.java
│   │
│   ├── config/
│   │   └── SecurityAccessProperties.java      # YAML 바인딩
│   │
│   ├── constant/
│   │   └── MenuAccessLevel.java               # READ, WRITE
│   │
│   ├── converter/
│   │   └── AuthorityConverter.java            # 메뉴 권한 + 리소스 파생 변환
│   │
│   ├── dto/
│   │   ├── AuthenticatedUser.java             # 인증된 사용자 record
│   │   ├── MenuAuthority.java                 # 메뉴 권한 단위 record
│   │   └── MenuPermission.java                # DB 원시 권한 record
│   │
│   ├── loader/
│   │   ├── MenuAuthorityLoader.java           # 전략 인터페이스
│   │   ├── UserMenuAuthorityLoader.java       # USER_MENU 구현체
│   │   └── RoleMenuAuthorityLoader.java       # ROLE_MENU 구현체
│   │
│   └── service/
│       ├── AuthenticatedUserFinder.java        # 인터페이스
│       └── AuthenticatedUserFinderImpl.java    # 구현체
│
├── global/util/
│   └── SecurityUtil.java                       # SecurityContext 접근 유틸
│
src/main/resources/
│   ├── application.yml
│   └── security-access.yml                     # 권한 소스 + 리소스 의존성
```

---

## 14. 체크리스트

```
□ 1. security-access.yml 작성
      → authority-source 설정
      → resource-dependencies 정의

□ 2. SecurityAccessProperties 생성
      → @ConfigurationProperties(prefix = "security.access")

□ 3. MenuAuthorityLoader 인터페이스 + 구현체 2개 생성
      → UserMenuAuthorityLoader (@ConditionalOnProperty: USER_MENU)
      → RoleMenuAuthorityLoader (@ConditionalOnProperty: ROLE_MENU)

□ 4. AuthorityConverter 구현
      → 메뉴 권한 변환 + YAML 기반 리소스 파생

□ 5. SecurityConfig에 @EnableMethodSecurity 선언
      → @PreAuthorize 활성화

□ 6. 모든 @RestController에 @PreAuthorize 부여
      → 클래스 레벨 hasAuthority(READ), 쓰기 메서드 hasAuthority(WRITE)
      → 크로스 리소스 API는 hasAnyAuthority OR 적용

□ 7. 테스트 작성 (12절 체크리스트)
      → 인가, 계정 보호, 권한 소스 전환, 리소스 파생
```

---

*Last updated: 2026-02-24*
