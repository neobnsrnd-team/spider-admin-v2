# Security

## 1. 개요

보안 설정은 `SecurityFilterChain` **한 곳**에서 선언적으로 구성한다.
보안 로직을 Controller, Service, 커스텀 Filter에 분산시키지 않는다.

```
Request
  │
  ├─ Spring Security FilterChain   ← 인증 · CSRF · 헤더 · CORS
  │
  ├─ @PreAuthorize                 ← Method Security (메뉴 기반 인가)
  │
  └─ Controller
```

**설계 원칙:**

| 원칙 | 적용 |
|------|------|
| **Secure by Default** | 전체 차단 후 명시적 허용. `anyRequest().authenticated()` 기본 |
| **Convention over Configuration** | Spring Security 내장 메커니즘 우선. 커스텀 구현 최소화 |
| **단일 설정 지점** | `SecurityConfig` 한 파일에서만 보안 설정 |
| **Defense in Depth** | FilterChain(인증) + @PreAuthorize Method Security(인가) + Service(검증) |

---

## 2. 금지 사항

> 민감 정보 로그 마스킹은 Logging 8절 참고.

위반 시 코드 리뷰에서 **즉시 반려**한다.

| 금지 항목 | 이유 |
|-----------|------|
| SHA-256 / MD5 / 평문 비밀번호 | 솔트 없음, 레인보우 테이블 취약 |
| Controller/Service에서 `SecurityContextHolder` 직접 조작 | Spring Security 생명주기 파괴 |
| 커스텀 Filter로 인증 구현 | 표준 AuthenticationProvider 우회 |
| `.csrf(csrf -> csrf.disable())` 전역 적용 | CSRF 방어선 전면 제거 |
| `permitAll()` 남발 | 의도치 않은 경로 노출. 추가 시 PR에 사유 필수 |
| 하드코딩된 비밀번호 / 시크릿 / 토큰 | 형상 관리 유출 시 즉시 피해 |
| `HttpSecurity.and()` 체이닝 | Spring Security 6.x에서 deprecated |
| Security 헤더 기본값 약화 | X-Content-Type-Options, X-Frame-Options 등 |
| 운영 환경에서 CORS 와일드카드 origin | 모든 도메인 허용 = CORS 무력화 |

---

## 3. SecurityFilterChain

> 메뉴 기반 인가는 Authentication & Authorization 7절 참고.

### 3.1 경로 분류

```java
private static final String[] PUBLIC_PATHS = {
    "/login", "/error",
    "/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico",
    "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html",
    "/actuator/health"
};
```

- 인증 없이 접근해야 하는 경로**만** 추가한다.
- 추가 시 **PR에 사유를 기록**한다.

### 3.2 구성 원칙

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http

            // ─── CSRF ─────────────────────────────────────────────
            // 렌더링 방식에 따라 전략 결정 (3.3절 참조)

            // ─── Headers ──────────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp ->
                    csp.policyDirectives("default-src 'self'; script-src 'self'")))

            // ─── Authorization Rules ──────────────────────────────
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .anyRequest().authenticated())

            // ─── 인증 방식 ────────────────────────────────────────
            // 프로젝트에 맞는 인증 방식 선택 (4절 참조)

            // ─── Exception Handling ───────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(...)
                .accessDeniedHandler(...))

            .build();
    }
}
```

**규칙:**
- 이 파일 외에 보안 설정을 담는 `@Configuration` 클래스를 만들지 않는다.
- `@EnableMethodSecurity`를 선언하여 `@PreAuthorize`를 활성화한다 (Authentication & Authorization 7절).

### 3.3 CSRF 전략

렌더링 방식에 따라 결정한다.

| 방식 | CSRF 전략 | 이유 |
|------|-----------|------|
| **서버 렌더링** (Thymeleaf 등) | 활성 (기본값 유지) | 브라우저 폼 CSRF 방어 |
| **SPA + 세션 쿠키** | `CookieCsrfTokenRepository` | JS에서 토큰 읽기 필요 |
| **SPA + Bearer 토큰** | 비활성 가능 | 쿠키 미사용 → CSRF 불필요 |
| **Stateless API** | 비활성 가능 | 쿠키 미사용 → CSRF 불필요 |

```java
// 서버 렌더링: 기본값 유지 (별도 설정 불필요)

// SPA + 세션 쿠키:
.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))

// Bearer 토큰 / Stateless API:
.csrf(csrf -> csrf.disable())
```

**전역 `csrf.disable()` 금지.** 비활성화 시 반드시 "쿠키 기반 인증을 사용하지 않음"을 확인한다.

---

## 4. 인증 방식

프로젝트 특성에 맞게 선택한다. **혼용 금지.**

| 방식 | 적합한 경우 | Spring Security 설정 |
|------|------------|---------------------|
| **Form Login + Session** | 서버 렌더링, 내부 시스템 | `.formLogin()` + `.sessionManagement()` |
| **JWT (Stateless)** | SPA + API, 모바일 클라이언트 | `.oauth2ResourceServer(jwt -> ...)` |
| **OAuth2 / OIDC** | SSO, 외부 IdP 연동 | `.oauth2Login()` |

선택 후 해당 방식의 설정만 `SecurityFilterChain`에 포함한다.

### 4.1 Session 사용 시 필수 설정

```java
.sessionManagement(session -> session
    .sessionFixation().changeSessionId())       // 세션 고정 공격 방지 — 필수
```

| 옵션 | 허용 |
|------|------|
| `changeSessionId()` | **필수** |
| `none()` | 금지 — 세션 고정 공격 취약 |

동시 세션 수(`maximumSessions`)는 서비스 요구사항에 따라 결정한다.

### 4.2 JWT 사용 시 필수 설정

```java
.sessionManagement(session -> session
    .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
.oauth2ResourceServer(oauth2 -> oauth2
    .jwt(jwt -> jwt.decoder(jwtDecoder())))
```

- Access Token 만료 시간은 **15분 이하**로 설정한다.
- Refresh Token은 **HTTP-Only 쿠키** 또는 **서버 사이드 저장소**에 보관한다.
- 토큰에 민감 정보(비밀번호, 주민번호 등)를 포함하지 않는다.

---

## 5. Password Encoding

> 인증 흐름 내 PasswordEncoder 사용은 Authentication & Authorization 4절 참고.

### 5.1 알고리즘 선택

| 알고리즘 | 권장도 | 비고 |
|----------|--------|------|
| **Argon2id** | OWASP 1순위 권장 | `Argon2PasswordEncoder` |
| **BCrypt** | 허용 (strength ≥ 10) | `BCryptPasswordEncoder(10)` |
| **SCrypt** | 허용 | `SCryptPasswordEncoder` |
| SHA-256 / MD5 / 평문 | **금지** | 솔트 없음, 취약 |

```java
// Argon2id (권장)
@Bean
public PasswordEncoder passwordEncoder() {
    return Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();
}

// BCrypt (허용)
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
}
```

### 5.2 규칙

| 항목 | 규칙 |
|------|------|
| 비교 방법 | `PasswordEncoder.matches()` **만** 사용. 직접 해시 비교 금지 |
| DB 컬럼 | 해시 길이 수용: `VARCHAR(100)` 이상 |
| 타이밍 공격 | Spring Security가 `UsernameNotFoundException`에도 dummy matches 수행. 우회 금지 |

---

## 6. HTTPS / TLS

| 항목 | 규칙 |
|------|------|
| 운영 환경 | **HTTPS 필수**. HTTP 접근 시 301 리다이렉트 |
| HSTS | `Strict-Transport-Security: max-age=31536000; includeSubDomains` |
| TLS 버전 | **1.2 이상**. 1.0/1.1 금지 |
| 인증서 | 공인 CA 발급. 자체 서명 인증서 운영 사용 금지 |

```java
// Spring Boot
server.ssl.enabled=true
server.ssl.protocol=TLS
server.ssl.enabled-protocols=TLSv1.2,TLSv1.3

// 또는 리버스 프록시(Nginx, ALB)에서 TLS 종단 처리
```

개발 환경에서는 HTTP 허용하되, **운영 프로필에서 반드시 HTTPS를 강제**한다.

---

## 7. CORS

### 7.1 구성

```java
@Bean
public CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));
    config.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

### 7.2 규칙

- Origin은 **환경별 프로퍼티**로 관리한다. 코드에 하드코딩 금지.
- **운영에서 와일드카드 origin 금지**: `setAllowedOrigins(List.of("*"))` 금지.
- `allowCredentials(true)` 사용 시 반드시 **명시적 origin** 지정.

---

## 8. Security Headers

Spring Security 기본 헤더를 유지한다. **약화 금지.**

| 헤더 | 기본값 | 변경 가능 조건 |
|------|--------|----------------|
| `X-Content-Type-Options: nosniff` | 활성 | **변경 금지** |
| `X-Frame-Options: DENY` | 활성 | 특정 경로만 `SAMEORIGIN` 필요 시 별도 FilterChain |
| `Cache-Control: no-cache, no-store` | 활성 | 정적 리소스에 한해 조정 가능 |
| `Strict-Transport-Security` | HTTPS 시 활성 | **운영 필수** |
| `Content-Security-Policy` | 미설정 (수동 추가) | **추가 권장** |

### 8.1 Content Security Policy (CSP)

```java
.headers(headers -> headers
    .contentSecurityPolicy(csp ->
        csp.policyDirectives("default-src 'self'; script-src 'self'; style-src 'self'")))
```

인라인 스크립트(`'unsafe-inline'`)와 `eval()`(`'unsafe-eval'`)은 **최대한 배제**한다.
외부 CDN 사용 시 해당 도메인만 명시적으로 허용한다.

---

## 9. Rate Limiting

인증 엔드포인트에 대한 브루트포스 공격을 방어한다.

| 대상 | 방식 | 권장 |
|------|------|------|
| 로그인 엔드포인트 | IP 기반 요청 제한 | 5회/분 |
| API 전체 | 사용자/IP 기반 | 서비스 요구사항에 따라 |

**구현 위치:** API Gateway, 리버스 프록시(Nginx `limit_req`), 또는 Spring Filter(`bucket4j` 등).
Application 레벨에서 구현 시 `SecurityFilterChain` 앞에 Filter로 배치한다.

---

## 10. 의존성 보안

| 항목 | 규칙 |
|------|------|
| 취약점 스캔 | `dependencyCheck` 또는 Dependabot 활성화 |
| Critical/High 취약점 | **릴리즈 전 해결 필수** |
| Spring Security 버전 | 패치 릴리즈 1주 이내 적용 권장 |
| 미사용 의존성 | 제거. 공격 표면 최소화 |

---

## 11. 환경별 보안 구성

| 항목 | 개발 | 운영 |
|------|------|------|
| HTTPS | HTTP 허용 | **HTTPS 필수** |
| CORS origin | `localhost:*` | **명시적 도메인만** |
| Swagger/API docs | 허용 | **비활성 또는 인증 요구** |
| DB Console (H2 등) | `@Profile`로 허용 | **금지** |
| CSP | `report-only` 가능 | **enforce** |
| 쿠키 `secure` | `false` | **`true`** |
| 디버그 로깅 | 허용 | **금지** — 민감 정보 유출 위험 |

DB 콘솔 사용 시 **반드시 `@Profile`로 제한**한다:

```java
@Profile("local")
@Bean
@Order(0)
public SecurityFilterChain devConsoleFilterChain(HttpSecurity http) throws Exception {
    return http
        .securityMatcher(PathRequest.toH2Console())
        .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .build();
}
```

---

## 12. 체크리스트

```
□ 1. SecurityFilterChain에서만 보안 설정
      → 다른 @Configuration에 보안 로직 없음

□ 2. 비밀번호: Argon2id 또는 BCrypt(strength ≥ 10)
      → PasswordEncoder.matches() 사용, 직접 해시 비교 없음

□ 3. CSRF: 렌더링 방식에 맞는 전략 적용
      → 전역 disable 금지. 비활성화 시 쿠키 미사용 확인

□ 4. HTTPS: 운영 환경 HTTPS 강제 + HSTS 활성
      → TLS 1.2 이상

□ 5. 공개 경로: 명시된 것만 permitAll
      → 추가 시 PR에 사유 기록

□ 6. CORS: 운영에서 명시적 origin만 허용
      → 와일드카드 없음

□ 7. Security Headers: 기본값 유지 + CSP 추가
      → X-Frame-Options, X-Content-Type-Options 약화 없음

□ 8. Session 사용 시: sessionFixation().changeSessionId()
      → 로그인 후 세션 ID 변경 확인

□ 9. 의존성: 취약점 스캔 활성화
      → Critical/High 릴리즈 전 해결

□ 10. 환경 분리: 개발 전용 설정 @Profile 제한
       → 운영에서 DB Console, Swagger 접근 불가
```

---

*Last updated: 2026-02-23*
