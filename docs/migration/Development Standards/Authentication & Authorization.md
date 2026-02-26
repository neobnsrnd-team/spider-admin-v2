# Authentication & Authorization

## 1. 개요

Spring Security 기반 메뉴 단위 접근 제어 표준이다. 메뉴-리소스 매핑을 YAML(`menu-resource-permissions.yml`)로 선언적으로 관리하며, 권한 소스(USER_MENU / ROLE_MENU)를 설정으로 교체할 수 있다.

### 1.1 아키텍처

```
┌─────────────────────────────────────────────────────────────┐
│  Authentication                                             │
│                                                             │
│  POST /login (userId, password)                             │
│    → DaoAuthenticationProvider                              │
│      → CustomUserDetailsService                             │
│        → AuthorityMapper.selectUserById()                   │
│        → AuthorityProvider (설정에 따라 구현체 교체)          │
│          → AuthorityConverter (리소스 파생 포함)             │
│            → CustomUserDetails (Principal)                  │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│  Authorization (@EnableMethodSecurity)                       │
│                                                             │
│  @PreAuthorize 선언은 가능하나, 현재 도메인 컨트롤러 미존재  │
│    → Principal.getAuthorities()에서 매칭                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 1.2 핵심 원칙

| 원칙 | 설명 |
|------|------|
| **리소스 단위 보호** | `@PreAuthorize("hasAuthority('RESOURCE:R')")` 형식 사용 |
| **DB가 유일한 판단 기준** | 권한 테이블에 레코드 없으면 403 |
| **리소스 의존성은 YAML** | 메뉴-리소스 매핑을 `menu-resource-permissions.yml`에서 선언적으로 관리 |
| **권한 소스 교체 가능** | USER_MENU / ROLE_MENU 중 YAML 설정으로 전환 |
| **WRITE가 READ를 포함** | WRITE 권한 보유 시 READ 리소스도 함께 파생 |

### 1.3 금지 사항

| 금지 항목 | 대안 |
|-----------|------|
| `@PreAuthorize("hasRole(…)")` | `@PreAuthorize("hasAuthority(…)")` |
| `@PreAuthorize` 없는 `@RestController` | 모든 컨트롤러에 필수 부여 |
| 에러 메시지에서 "사용자 없음"/"비밀번호 틀림" 구분 | 동일 메시지 (User Enumeration 방지) |

---

## 2. DB Schema

```
FWK_USER                         FWK_ROLE
┌──────────────────┐             ┌──────────────────┐
│ USER_ID (PK)     │             │ ROLE_ID (PK)     │
│ PASSWD           │             │ ROLE_NAME        │
│ ROLE_ID (FK) ────│────────────→│ USE_YN           │
│ USER_STATE_CODE  │             └──────────────────┘
│ LOGIN_FAIL_COUNT │                      │ 1:N
└──────────────────┘             ┌──────────────────┐
         │                       │ FWK_ROLE_MENU    │
         │                       │ ROLE_ID (PK,FK)  │
         │                       │ MENU_ID (PK,FK)  │
         │                       │ AUTH_CODE (R/W)  │
         │ 1:N                   └──────────────────┘
┌──────────────────┐             ┌──────────────────┐
│ FWK_USER_MENU    │             │ FWK_MENU         │
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
| `FWK_USER_MENU` | 사용자별 개별 메뉴 권한 (세밀한 제어) |
| `FWK_ROLE_MENU` | 역할별 메뉴 권한 템플릿 (일괄 제어) |
| `FWK_MENU` | 메뉴 정의 (계층 구조, 표시 여부) |

두 테이블은 **동일한 구조**(MENU_ID + AUTH_CODE)를 가지므로 설정으로 교체 가능하다.

---

## 3. 설정 파일

### 3.1 menu-resource-permissions.yml

메뉴별 리소스 권한 매핑을 정의하는 YAML 파일이다. `menu-resource.permissions` 접두사로 바인딩된다.

```yaml
# src/main/resources/menu-resource-permissions.yml

menu-resource:
  permissions:

    # ── 시스템 관리 ──
    v3_role_manage:
      R: ROLE:R, MENU:R
      W: ROLE:W, MENU:R

    v3_user_manage:
      R: USER:R, ROLE:R
      W: USER:W, ROLE:R

    # ── 인프라 관리 ──
    v3_was_instance:
      R: WASINSTANCE:R, WASGROUP:R
      W: WASINSTANCE:W, WASGROUP:R

    # ── 연계 관리 ──
    v3_app_mapping_manage:
      R: TRANSPORT:R, TRANSACTION:R, WASINSTANCE:R, GATEWAY:R
      W: TRANSPORT:W, TRANSACTION:R, WASINSTANCE:R, GATEWAY:R, RELOAD:W
```

- 키(1depth): `menu-resource.permissions.<MENU_ID>`
- 키(2depth): `R` = 조회, `W` = 등록/수정/삭제
- 값: 최종 `GrantedAuthority` (콤마 구분), 형식 `{RESOURCE}:{R or W}`
- `W` 미정의 = 조회 전용 화면

### 3.2 application-base.yml

```yaml
# src/main/resources/application-base.yml

spring:
  config:
    import: classpath:menu-resource-permissions.yml

security:
  access:
    authority-source: ${AUTHORITY_SOURCE:USER_MENU}
```

- `spring.config.import`로 `menu-resource-permissions.yml`을 로드한다.
- `authority-source`는 환경 변수 `AUTHORITY_SOURCE`로 오버라이드 가능하며, 기본값은 `USER_MENU`이다.

### 3.3 SecurityAccessProperties

```java
@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "security.access")
public class SecurityAccessProperties {

    @NotNull
    private String authoritySource;
}
```

`authority-source` 값만 바인딩한다. 리소스 의존성 정의는 별도의 `MenuResourcePermissions`에서 관리한다.

### 3.4 MenuResourcePermissions

```java
@ConfigurationProperties(prefix = "menu-resource")
public class MenuResourcePermissions {

    private Map<String, Map<String, String>> permissions = Collections.emptyMap();

    public Set<String> getDerivedResourceAuthorities(String menuId, MenuAccessLevel level) {
        Map<String, String> entry = permissions.getOrDefault(menuId, Collections.emptyMap());

        Set<String> authorities = new LinkedHashSet<>();
        authorities.addAll(splitAuthorities(entry.getOrDefault("R", "")));
        if (level == MenuAccessLevel.WRITE) {
            authorities.addAll(splitAuthorities(entry.getOrDefault("W", "")));
        }

        return authorities;
    }

    private Set<String> splitAuthorities(String value) { ... }
}
```

- `permissions` 맵은 `{menuId → {R → "ROLE:R, MENU:R", W → "ROLE:W, MENU:R"}}` 구조이다.
- `getDerivedResourceAuthorities()`는 `READ` 레벨이면 `R` 값만, `WRITE` 레벨이면 `R` + `W` 값을 모두 반환한다.

---

## 4. 인증 흐름

```
POST /login (userId, password)
  │
  ├─ DaoAuthenticationProvider
  │     ├─ CustomUserDetailsService.loadUserByUsername(userId)
  │     │     ├─ AuthorityMapper.selectUserById(userId)
  │     │     ├─ AuthorityProvider.getAuthorities(userId, roleId)
  │     │     │     ├─ [USER_MENU] AuthorityMapper.selectMenuPermissionsByUserId(userId)
  │     │     │     └─ [ROLE_MENU] AuthorityMapper.selectMenuPermissionsByRoleId(roleId)
  │     │     └─ AuthorityConverter.convert(permissions)
  │     │           └─ MenuResourcePermissions.getDerivedResourceAuthorities()
  │     │
  │     ├─ PasswordEncoder.matches(raw, stored)
  │     └─ UserDetails 상태 검증
  │           ├─ isEnabled()          → "1".equals(userStateCode)
  │           └─ isAccountNonLocked() → loginFailCount < 5
  │
  ├─ 성공 → defaultSuccessUrl("/", true)
  │
  └─ 실패 → 로그인 페이지 리다이렉트
```

### 4.1 CustomUserDetailsService

```java
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthorityProvider authorityProvider;
    private final AuthorityMapper authorityMapper;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        AuthenticatedUser user = authorityMapper.selectUserById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + userId);
        }

        Set<GrantedAuthority> authorities = authorityProvider.getAuthorities(user.userId(), user.roleId());

        return new CustomUserDetails(
                user.userId(), user.password(), user.userStateCode(), user.loginFailCount(), authorities);
    }
}
```

- `AuthorityMapper`로 직접 사용자 정보를 조회한다.
- `AuthorityProvider`에 위임하여 권한 소스에 따른 권한 목록을 가져온다.
- 5개 파라미터를 개별적으로 전달하여 `CustomUserDetails`를 생성한다.

### 4.2 AuthenticatedUser

```java
public record AuthenticatedUser(
        String userId, String password, String roleId, String userStateCode, int loginFailCount) {}
```

DB 조회 결과를 담는 record이다. `FWK_USER` 테이블의 `PASSWD` 컬럼은 `password`로 alias된다.

---

## 5. 권한 소스 교체 (USER_MENU / ROLE_MENU)

### 5.1 공통 인터페이스

```java
public interface AuthorityProvider {
    Set<GrantedAuthority> getAuthorities(String userId, String roleId);
}
```

```java
public record MenuPermission(String menuId, String authCode) {}
```

- `AuthorityProvider`는 `Set<GrantedAuthority>`를 반환한다.
- `MenuPermission`은 `FWK_USER_MENU` / `FWK_ROLE_MENU` 모두 동일한 구조(menuId + authCode)이다.

### 5.2 UserMenuAuthorityProvider

```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.access.authority-source", havingValue = "USER_MENU")
public class UserMenuAuthorityProvider implements AuthorityProvider {

    private final AuthorityMapper authorityMapper;
    private final AuthorityConverter authorityConverter;

    @Override
    public Set<GrantedAuthority> getAuthorities(String userId, String roleId) {
        List<MenuPermission> permissions = authorityMapper.selectMenuPermissionsByUserId(userId);
        return authorityConverter.convert(permissions);
    }
}
```

```sql
<!-- AuthorityMapper.xml -->
<select id="selectMenuPermissionsByUserId" resultType="MenuPermission">
    SELECT MENU_ID, AUTH_CODE
      FROM FWK_USER_MENU
     WHERE USER_ID = #{userId}
</select>
```

### 5.3 RoleMenuAuthorityProvider

```java
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.access.authority-source", havingValue = "ROLE_MENU")
public class RoleMenuAuthorityProvider implements AuthorityProvider {

    private final AuthorityMapper authorityMapper;
    private final AuthorityConverter authorityConverter;

    @Override
    public Set<GrantedAuthority> getAuthorities(String userId, String roleId) {
        List<MenuPermission> permissions = authorityMapper.selectMenuPermissionsByRoleId(roleId);
        return authorityConverter.convert(permissions);
    }
}
```

```sql
<!-- AuthorityMapper.xml -->
<select id="selectMenuPermissionsByRoleId" resultType="MenuPermission">
    SELECT MENU_ID, AUTH_CODE
      FROM FWK_ROLE_MENU
     WHERE ROLE_ID = #{roleId}
</select>
```

### 5.4 AuthorityMapper (단일 Mapper)

```java
@Mapper
public interface AuthorityMapper {

    List<MenuPermission> selectMenuPermissionsByUserId(String userId);

    List<MenuPermission> selectMenuPermissionsByRoleId(String roleId);

    AuthenticatedUser selectUserById(String userId);
}
```

사용자 조회와 권한 조회를 하나의 Mapper에서 통합 관리한다.

### 5.5 전환 방법

```yaml
# application-base.yml

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

### 5.6 비교

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
  사용자 권한: v3_batch_app_manage만 보유
  → WASINSTANCE:R 권한 없음 → 403
```

### 6.2 해결: YAML 기반 리소스 파생

로그인 시 메뉴 권한을 변환할 때, YAML에 정의된 리소스 권한을 자동으로 추가한다.

```
FWK_USER_MENU: user1 → v3_batch_app_manage (W)

AuthorityConverter.convert()
  └─ MenuResourcePermissions.getDerivedResourceAuthorities("v3_batch_app_manage", WRITE)
     → R 값: BATCH:R, WASINSTANCE:R
     → W 값: BATCH:W, WASINSTANCE:R
     → 합집합: {BATCH:R, BATCH:W, WASINSTANCE:R}

최종 GrantedAuthority: {BATCH:R, BATCH:W, WASINSTANCE:R}
```

### 6.3 AuthorityConverter

```java
@Component
@RequiredArgsConstructor
public class AuthorityConverter {

    private final MenuResourcePermissions menuResourcePermissions;

    public Set<GrantedAuthority> convert(List<MenuPermission> permissions) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (MenuPermission permission : permissions) {
            MenuAccessLevel level = MenuAccessLevel.fromCode(permission.authCode());
            String menuId = permission.menuId();

            Set<String> derivedResources = menuResourcePermissions.getDerivedResourceAuthorities(menuId, level);
            for (String resource : derivedResources) {
                authorities.add(new SimpleGrantedAuthority(resource));
            }
        }

        return authorities;
    }
}
```

- `MenuPermission`의 `authCode`를 `MenuAccessLevel`로 변환한다.
- `MenuResourcePermissions`에서 해당 메뉴의 파생 리소스 권한을 조회한다.
- 각 리소스 문자열을 `SimpleGrantedAuthority`로 변환하여 `Set`에 추가한다.

### 6.4 MenuAccessLevel

```java
@Getter
@RequiredArgsConstructor
public enum MenuAccessLevel {
    READ("R"),
    WRITE("W");

    private final String code;

    public static MenuAccessLevel fromCode(String code) {
        for (MenuAccessLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown MenuAccessLevel code: " + code);
    }
}
```

---

## 7. 인가 모델

### 7.1 @EnableMethodSecurity

`SecurityConfig`에 선언하여 `@PreAuthorize`를 활성화한다. 현재 도메인 컨트롤러가 존재하지 않으므로 실제 적용된 `@PreAuthorize`는 없다.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    // ...
}
```

### 7.2 Authority 형식

`CustomUserDetails.getAuthorities()`가 반환하는 `GrantedAuthority` 형식:

```
{RESOURCE}:{R or W}
```

| 예시 | 의미 |
|------|------|
| `ROLE:R` | ROLE 리소스 읽기 |
| `ROLE:W` | ROLE 리소스 쓰기 |
| `WASINSTANCE:R` | WAS 인스턴스 리소스 읽기 (파생 포함) |
| `MENU:R` | MENU 리소스 읽기 |

`MenuResourcePermissions`가 WRITE 레벨일 때 R + W 값을 모두 반환하므로, WRITE 권한자는 READ 리소스도 보유한다.

### 7.3 @PreAuthorize 패턴 (향후 컨트롤러 적용 시)

| 용도 | 어노테이션 |
|------|-----------|
| 단일 리소스 읽기 | `@PreAuthorize("hasAuthority('USER:R')")` |
| 단일 리소스 쓰기 | `@PreAuthorize("hasAuthority('USER:W')")` |
| 복수 리소스 OR | `@PreAuthorize("hasAnyAuthority('WASINSTANCE:R', 'BATCH:R')")` |

- `hasAuthority`: 정확히 일치하는 권한이 있는지 확인
- `hasAnyAuthority`: OR 조건 -- 하나라도 매칭되면 통과
- `hasRole()` 사용 금지 -- 리소스 기반 모델과 맞지 않음

### 7.4 전체 흐름 예시

```
[로그인 시]
  DB: user1 → v3_batch_app_manage (W)
  MenuResourcePermissions 파생:
    R: BATCH:R, WASINSTANCE:R
    W: BATCH:W, WASINSTANCE:R
  최종 GrantedAuthority: {BATCH:R, BATCH:W, WASINSTANCE:R}

[GET /api/was/instances 요청]
  @PreAuthorize("hasAuthority('WASINSTANCE:R')")
    → WASINSTANCE:R? YES → 허용

[POST /api/was/instances 요청]
  @PreAuthorize("hasAuthority('WASINSTANCE:W')")
    → WASINSTANCE:W? NO → 403 (참조만 허용, CUD 차단)
```

---

## 8. SecurityConfig

### 8.1 Form Login 설정

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomAuthenticationEntryPoint authenticationEntryPoint;
    private final CustomAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/images/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .sessionManagement(session -> session
                        .sessionFixation().migrateSession());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
```

- 로그인 성공/실패 핸들러는 별도 커스텀 클래스 없이 **인라인 설정**(`defaultSuccessUrl`)을 사용한다.
- 예외 처리는 `CustomAuthenticationEntryPoint`(미인증)와 `CustomAccessDeniedHandler`(권한 없음)가 담당한다.
- 세션 고정 공격 방지를 위해 `migrateSession`을 설정한다.

### 8.2 예외 핸들러

`CustomAuthenticationEntryPoint`과 `CustomAccessDeniedHandler`는 AJAX 요청과 일반 요청을 구분하여 처리한다.

| 핸들러 | AJAX 요청 | 일반 요청 |
|--------|-----------|-----------|
| `CustomAuthenticationEntryPoint` | 401 JSON 응답 | `/login`으로 리다이렉트 |
| `CustomAccessDeniedHandler` | 403 JSON 응답 | 403 에러 페이지 |

두 핸들러 모두 `ApplicationEventPublisher`를 통해 `SecurityLogEvent`를 발행한다.

```java
// AJAX 판별
private boolean isAjaxRequest(HttpServletRequest request) {
    return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
}
```

---

## 9. Principal 설계

### 9.1 CustomUserDetails

```java
@Getter
public class CustomUserDetails implements UserDetails {

    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    private final String userId;
    private final String password;
    private final String userStateCode;
    private final int loginFailCount;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(
            String userId, String password, String userStateCode,
            int loginFailCount, Set<GrantedAuthority> authorities) {
        this.userId = userId;
        this.password = password;
        this.userStateCode = userStateCode;
        this.loginFailCount = loginFailCount;
        this.authorities = authorities;
    }

    @Override public String getUsername()  { return userId; }
    @Override public String getPassword()  { return password; }

    @Override
    public boolean isEnabled() {
        return "1".equals(userStateCode);
    }

    @Override
    public boolean isAccountNonLocked() {
        return loginFailCount < MAX_LOGIN_FAIL_COUNT;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override public boolean isAccountNonExpired()    { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
}
```

- 5개 파라미터를 개별적으로 받는 생성자를 사용한다.
- `isEnabled()`: `userStateCode`가 `"1"`이면 활성 상태이다.
- `isAccountNonLocked()`: `loginFailCount`가 `MAX_LOGIN_FAIL_COUNT`(5) 미만이면 잠금 해제 상태이다.
- `getAuthorities()`: `Set<GrantedAuthority>`를 그대로 반환한다 (변환 없음).

### 9.2 계정 상태

| 상태 | `isEnabled()` | `isAccountNonLocked()` | 로그인 |
|------|---------------|------------------------|--------|
| 정상 (`userStateCode = "1"`) | `true` | `failCount < 5` | 가능 |
| 비활성 (`userStateCode != "1"`) | `false` | -- | 불가 |
| 잠금 (실패 5회 이상) | `true` | `false` | 불가 |

- 잠금 해제는 관리자가 `LOGIN_FAIL_COUNT`를 초기화
- `MAX_LOGIN_FAIL_COUNT`는 클래스 상수로 5 고정

---

## 10. 테스트

### 10.1 테스트 체크리스트

```
인가:
- [ ] 미인증 → 401 (AJAX) 또는 /login 리다이렉트
- [ ] 권한 보유 + GET → 200
- [ ] 권한 미보유 + GET → 403
- [ ] WRITE 리소스 보유 + POST → 정상
- [ ] READ 리소스만 보유 + POST → 403

계정 보호:
- [ ] 비활성 계정 (userStateCode != "1") 로그인 → 실패
- [ ] 로그인 실패 5회 이상 → 잠금

권한 소스 전환:
- [ ] authority-source: USER_MENU → FWK_USER_MENU에서 로드
- [ ] authority-source: ROLE_MENU → FWK_ROLE_MENU에서 로드
- [ ] YAML 전환 후 재시작 시 정상 동작

리소스 파생:
- [ ] YAML에 의존성 추가 후 재시작 → 파생 권한 반영
- [ ] YAML에서 의존성 제거 후 재시작 → 파생 권한 소멸
- [ ] WRITE 레벨 시 R + W 값 모두 파생
- [ ] READ 레벨 시 R 값만 파생
```

---

## 11. 파일 구조

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
│   │   ├── SecurityAccessProperties.java      # authority-source YAML 바인딩
│   │   └── MenuResourcePermissions.java       # menu-resource.permissions YAML 바인딩
│   │
│   ├── constant/
│   │   └── MenuAccessLevel.java               # READ, WRITE enum
│   │
│   ├── converter/
│   │   └── AuthorityConverter.java            # MenuPermission → GrantedAuthority 변환
│   │
│   ├── dto/
│   │   ├── AuthenticatedUser.java             # 인증된 사용자 record
│   │   └── MenuPermission.java                # DB 원시 권한 record
│   │
│   ├── handler/
│   │   ├── CustomAccessDeniedHandler.java     # 403 처리 (AJAX/일반 분기)
│   │   └── CustomAuthenticationEntryPoint.java # 401 처리 (AJAX/일반 분기)
│   │
│   ├── mapper/
│   │   └── AuthorityMapper.java               # 단일 Mapper (사용자 조회 + 권한 조회)
│   │
│   └── provider/
│       ├── AuthorityProvider.java             # 전략 인터페이스
│       ├── UserMenuAuthorityProvider.java     # USER_MENU 구현체
│       └── RoleMenuAuthorityProvider.java     # ROLE_MENU 구현체
│
src/main/resources/
│   ├── application-base.yml                   # security.access + config import
│   └── menu-resource-permissions.yml          # 메뉴별 리소스 권한 매핑
│
│   └── mapper/{db}/security/
│       └── AuthorityMapper.xml                # SQL 매핑
```

---

## 12. 체크리스트

- [ ] `menu-resource-permissions.yml` 작성 -- 메뉴별 리소스 권한 매핑 정의
- [ ] `application-base.yml`에서 `classpath:menu-resource-permissions.yml` import 및 `authority-source` 설정
- [ ] `SecurityAccessProperties` 생성 -- `@ConfigurationProperties(prefix = "security.access")`
- [ ] `MenuResourcePermissions` 생성 -- `@ConfigurationProperties(prefix = "menu-resource")`
- [ ] `AuthorityProvider` 인터페이스 + 구현체 2개 생성
  - [ ] `UserMenuAuthorityProvider` (`@ConditionalOnProperty`: USER_MENU)
  - [ ] `RoleMenuAuthorityProvider` (`@ConditionalOnProperty`: ROLE_MENU)
- [ ] `AuthorityConverter` 구현 -- `MenuResourcePermissions` 기반 리소스 파생
- [ ] `AuthorityMapper` 구현 -- 사용자 조회 + 메뉴 권한 조회 통합
- [ ] `SecurityConfig`에 `@EnableMethodSecurity` 선언
- [ ] `CustomAccessDeniedHandler` / `CustomAuthenticationEntryPoint` 구현
- [ ] 도메인 컨트롤러 작성 시 `@PreAuthorize` 부여

---

*Last updated: 2026-02-26*
