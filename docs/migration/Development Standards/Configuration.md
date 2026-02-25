# Configuration

## 1. 개요

[12-Factor App](https://12factor.net/config)을 따른다.

- **코드와 설정을 분리한다.** 환경마다 달라지는 값은 코드에 포함하지 않는다.
- **민감 정보는 환경 변수로만 주입한다.** YAML 파일에 비밀번호·토큰·접속 정보를 직접 기재하지 않는다.
- **모든 YAML 파일은 Git으로 추적한다.** YAML에 민감 정보가 없으므로 gitignore 대상이 없다. gitignore 대상은 `.env` 하나뿐이다.
- **설정 누락은 기동 시 즉시 실패한다.** 필수값이 없으면 런타임 오류 전에 기동 단계에서 명확한 메시지로 종료된다.

---

## 2. 파일 구조

파일 수는 프로젝트마다 다르지만 역할 분리 원칙은 동일하다.

```
src/main/resources/
├── application.yml           # 프로파일 그룹 정의 + spring.config.import
├── application-base.yml      # 환경·인프라 무관 공통 비민감 설정
├── application-{infra}.yml   # 특정 인프라 전용 설정
│                               예) application-oracle.yml, application-redis.yml
└── application-{env}.yml     # 특정 실행 환경 오버라이드
                                예) application-local.yml, application-ci.yml, application-staging.yml

src/test/resources/
└── application.yml           # 유닛 테스트 전용 (외부 의존성 없음)

.env                          # 로컬 실제 값 — gitignore
.env.example                  # 변수 목록과 설명 — tracked
```

### 2.1 각 파일의 역할

| 파일 | 담아야 할 것 | 담지 말아야 할 것 |
|------|-------------|-----------------|
| `application.yml` | 프로파일 그룹, `spring.config.import` | 실제 설정값 |
| `application-base.yml` | 환경 무관 비민감 설정 | DB 설정, 자격 증명 |
| `application-{infra}.yml` | 해당 인프라 접속 설정 (`${ENV_VAR}` 참조) | 하드코딩된 접속 정보 |
| `application-{env}.yml` | base·인프라 프로파일 대비 **차이점만** | 전체 설정 재기술 |
| `src/test/.../application.yml` | 인메모리·목(Mock) 기반 설정 | 실제 인프라 접속 정보 |

---

## 3. 프로파일 전략

### 3.1 네이밍 규칙

프로파일은 **단일 관심사**를 표현한다. 두 관심사를 한 파일에 섞지 않는다.

| 종류 | 규칙 | 예시 |
|------|------|------|
| 공통 | `base` 고정 | `base` |
| 인프라 프로파일 | 인프라명 소문자 단수 | `oracle`, `mysql`, `redis` |
| 환경 프로파일 | 환경명 소문자 | `ci`, `staging`, `prod` |
| 프로파일 그룹 | `{사용목적}` 또는 `{사용목적}-{주인프라}` | `local-oracle`, `ci-mysql` |

```
❌ application-ci-oracle.yml   # 두 관심사(CI 환경 + Oracle 인프라)가 한 파일에
✅ application-ci.yml          # CI 환경 관심사만
✅ application-oracle.yml      # Oracle 관심사만
```

### 3.2 Profile Groups

분리된 단위 프로파일을 그룹으로 조합한다.

```yaml
# application.yml
spring:
  config:
    import: optional:file:./.env[.properties]
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local-oracle}
    group:
      local-oracle: "base,oracle,local"   # 환경 프로파일(local)은 항상 마지막
      local-mysql:  "base,mysql,local"
      ci-oracle:    "base,oracle,ci"      # 환경 프로파일(ci)은 항상 마지막
      ci-mysql:     "base,mysql,ci"
```

**그룹 내 순서가 곧 우선순위다.** 나중에 나열된 프로파일이 앞선 프로파일을 덮어쓴다. 환경 프로파일은 항상 마지막에 위치시켜 최우선 오버라이드를 보장한다.

### 3.3 로드 순서

```
application.yml              (프로파일 그룹 정의)
    ↓
application-base.yml         (공통 기본값)
    ↓
application-{infra}.yml      (인프라 설정 — base 오버라이드)
    ↓
application-{env}.yml        (환경 오버라이드 — 최우선)
    ↓
환경 변수 / .env              (절대 최우선)
```

### 3.4 새 환경 또는 인프라 추가

1. `application-{이름}.yml`을 추가한다.
2. `application.yml`의 `group`에 필요한 조합을 등록한다.
3. 기존 파일은 수정하지 않는다.

---

## 4. 새 설정 추가 절차

### 4.1 민감 여부 판단

파일 선택보다 먼저 결정한다.

| 민감 정보 → 환경 변수로 주입 | 비민감 정보 → YAML 직접 기재 가능 |
|-----------------------------|---------------------------------|
| 비밀번호, API 키, 토큰 | 포트, 파일 크기 한도, 타임아웃 |
| DB / 외부 서비스 접속 정보 | 로그 레벨, cron 표현식 |
| 암호화 키, 서비스 계정 | 기능 플래그 (on/off) |

민감 정보는 YAML에 `${VAR}` 또는 `${VAR:default}` 형태로만 기재한다.

```yaml
some-feature:
  api-key: ${MY_API_KEY}         # 민감 — 환경 변수 참조
  timeout: 30000                 # 비민감 — 직접 기재
  retry-count: ${RETRY_COUNT:3}  # 민감하지 않지만 환경마다 다를 수 있는 경우
```

### 4.2 파일 선택

```
새 설정값
    │
    ├── 모든 환경·인프라에서 동일한가?
    │       └── Yes → application-base.yml
    │
    ├── 특정 인프라에 종속적인가?
    │       └── Yes → application-{해당 인프라}.yml
    │               (여러 인프라를 지원하면 각각의 파일에 모두 추가)
    │
    ├── 특정 실행 환경에서만 다른가?
    │       └── Yes → application-{해당 환경}.yml  (차이점만 기재)
    │
    └── 유닛 테스트에서만 다른가?
            └── Yes → src/test/resources/application.yml
```

### 4.3 .env.example 업데이트

환경 변수를 추가했다면 반드시 `.env.example`에도 추가한다.

---

## 5. 프로퍼티 네이밍 규칙

### 5.1 YAML 키 형식

Spring Boot 표준에 따라 **kebab-case**를 사용한다.

```yaml
# ❌
uploadDir: /uploads
maxFileSize: 50MB

# ✅
upload-dir: /uploads
max-file-size: 50MB
```

### 5.2 커스텀 프로퍼티 prefix

단일 도메인 명사를 prefix로 사용한다.

```yaml
# ❌ Spring Boot 예약 네임스페이스 오염
spring:
  upload-dir: /uploads

# ✅ 명확한 커스텀 도메인 prefix
file:
  upload-dir: /uploads

notification:
  slack-webhook-url: ${SLACK_WEBHOOK_URL}
```

Spring Boot 예약 네임스페이스(`spring.*`, `server.*`, `management.*`, `logging.*`)를 커스텀 프로퍼티 prefix로 사용하지 않는다.

---

## 6. ConfigurationProperties — 기동 시 유효성 검사

### 6.1 적용 범위

`@ConfigurationProperties`는 **우리가 직접 정의한 커스텀 프로퍼티**에만 적용한다.
`spring.*`, `server.*` 등 Spring Boot 표준 프로퍼티는 Spring Boot가 자체 바인딩하므로 별도 클래스가 필요 없다.

```
spring.datasource.url    → Spring Boot 자체 처리  (@ConfigurationProperties 불필요)
file.upload-dir          → 우리 코드에서 처리     (@ConfigurationProperties 필요)
```

### 6.2 규칙

커스텀 프로퍼티는 반드시 `@ConfigurationProperties` + `@Validated`로 바인딩한다. `@Value`를 직접 사용하지 않는다.

```java
// ❌ 사용하지 않는다
@Value("${file.upload-dir}")
private String uploadDir;

// ✅ 사용한다
@ConfigurationProperties(prefix = "file")
@Validated
@Getter @Setter
public class FileProperties {

    @NotBlank(message = "file.upload-dir must be set")
    private String uploadDir;

    @Positive
    private int maxFileSizeMb = 50;   // 비민감 기본값은 코드에 정의
}
```

### 6.3 등록

`@ConfigurationProperties` 클래스는 `@ConfigurationPropertiesScan` 없이는 동작하지 않는다.

```java
@SpringBootApplication
@ConfigurationPropertiesScan   // 없으면 @ConfigurationProperties 클래스가 무시된다
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 6.4 누락 시 출력

```
APPLICATION FAILED TO START

  Property: file.upload-dir
  Value:    ""
  Reason:   file.upload-dir must be set
```

### 6.5 pom.xml 의존성

```xml
<!-- IDE 자동완성 및 타입 검증을 위한 메타데이터 생성 (빌드 산출물 미포함) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>
```

`mvn compile` 후 IntelliJ·VS Code에서 커스텀 프로퍼티 자동완성과 타입 오류 하이라이팅이 동작한다.

---

## 7. .env 연동

**Spring Boot는 `.env` 파일을 기본적으로 로드하지 않는다.** `application.yml`에 `spring.config.import`를 추가하여 명시적으로 연결한다.

```yaml
# application.yml
spring:
  config:
    import: optional:file:./.env[.properties]
```

이 한 줄로 `.env`의 모든 `KEY=VALUE` 항목이 Spring 프로퍼티로 주입된다. `optional:`은 `.env`가 없어도 기동이 중단되지 않도록 한다 (CI·운영 환경에서 `.env`가 존재하지 않아도 됨).

### 7.1 프로파일 활성화의 예외

`spring.config.import`로 읽은 값은 이미 결정된 프로파일을 변경하지 못한다. 프로파일을 전환할 때는 OS 환경 변수로 설정한다.

```bash
# 프로파일 전환
SPRING_PROFILES_ACTIVE=local-mysql mvn spring-boot:run
```

기본 프로파일은 `application.yml`의 fallback으로 커버하므로, 대부분의 경우 별도 설정이 필요 없다.

```yaml
active: ${SPRING_PROFILES_ACTIVE:local-oracle}   # 환경 변수 없으면 local-oracle
```

### 7.2 .env.example 작성 규칙

`.env.example`이 이 프로젝트의 유일한 설정 템플릿이다. 아래 형식을 준수한다.

```bash
# ================================================================
# 환경 변수 템플릿
# 사용법: cp .env.example .env → 실제 값 입력
# ================================================================

# [섹션명] ────────────────────────────────────────────────────────
REQUIRED_VAR=              # 설명: 이 변수가 무엇인지, 어디서 얻는지
# OPTIONAL_VAR=default     # (선택) 기본값이 있는 경우 주석 처리 후 기본값 표기
```

규칙:
- 모든 항목에 한 줄 설명 주석을 단다.
- 필수값은 빈 값(`VAR=`), 선택값은 주석 처리(`# VAR=default`)로 구분한다.
- 실제 값 예시는 절대 기재하지 않는다.

---

## 8. 환경별 공급 방식

| 환경 | 방식 | 비고 |
|------|------|------|
| 로컬 개발 | `.env` → `spring.config.import`로 주입 | gitignore |
| CI | GitHub Secrets → workflow `env:` 블록으로 주입 | CI Strategy 8절 (시크릿 관리) 참고 |
| 운영 | 서버 OS 환경 변수 또는 Secret Manager | 인프라 담당자 관리 |

---

## 9. 로컬 개발 초기 설정

```bash
# 1. 환경 변수 파일 생성 후 실제 값 입력
cp .env.example .env

# 2. 실행
mvn spring-boot:run
# spring.config.import 가 .env 를 자동으로 읽는다
```

프로파일을 전환하려면:

```bash
SPRING_PROFILES_ACTIVE=local-mysql mvn spring-boot:run
```

---

## 부록 — 이 프로젝트 설정 레퍼런스

> 이 프로젝트에 특정한 설정 정보다. 컨벤션(섹션 1~9)은 프로젝트에 무관하게 적용된다.

> CI 파이프라인 관련 설정은 CI Strategy 문서 참고.

### 프로파일 구성

| 단위 프로파일 | 역할 |
|-------------|------|
| `base` | 공통 비민감 설정 (Thymeleaf, 파일 업로드, 스케줄러 기본값 등) |
| `oracle` | Oracle Datasource + MyBatis oracle 매퍼 |
| `mysql` | MySQL Datasource + MyBatis mysql 매퍼 |
| `local` | 로컬 개발 오버라이드 (DEBUG 로그, devtools, 느슨한 캐시 설정 등) |
| `ci` | CI 오버라이드 (스케줄러 off, 로그 축소, 임시 경로) |

> 프로파일 그룹 YAML은 3.2절 참고.

### .env.example

```bash
# ================================================================
# 환경 변수 템플릿
# 사용법: cp .env.example .env → 실제 값 입력
# Playwright 1.40+: .env 자동 로드
# ================================================================

# [프로파일] ──────────────────────────────────────────────────────
# SPRING_PROFILES_ACTIVE=local-oracle   # (선택) 기본값: local-oracle | local-mysql

# [DB 공통] ───────────────────────────────────────────────────────
DB_HOST=          # DB 서버 호스트
DB_USERNAME=      # DB 접속 사용자
DB_PASSWORD=      # DB 접속 비밀번호
# DB_PORT=        # (선택) Oracle 기본: 1521, MySQL 기본: 3306

# [Oracle 전용] ───────────────────────────────────────────────────
DB_SID=           # Oracle SID (local-oracle 프로파일 시 필수)
DB_SCHEMA=        # Oracle 스키마 (HikariCP CURRENT_SCHEMA 설정에 사용)

# [MySQL 전용] ────────────────────────────────────────────────────
# DB_NAME=        # MySQL 데이터베이스 이름 (local-mysql 프로파일 시 필수)

# [경로] ──────────────────────────────────────────────────────────
# FILE_UPLOAD_DIR=    # (선택) 기본: {프로젝트루트}/uploads
# XML_PROPERTY_DIR=   # (선택) 기본: {프로젝트루트}/xml-properties

# [테스트 계정] ───────────────────────────────────────────────────
CI_TEST_USER_ID=      # E2E·API 테스트 로그인 계정 ID
CI_TEST_PASSWORD=     # E2E·API 테스트 로그인 계정 비밀번호
```

### CI 환경 변수 목록

CI에서는 DB별로 별도 Secret을 사용하고, GitHub Actions workflow의 `env:` 블록에서 공통 변수명(`DB_PASSWORD`)으로 매핑하여 주입한다.

```yaml
# ci.yml — Oracle Job
env:
  SPRING_PROFILES_ACTIVE: ci-oracle
  DB_HOST: localhost
  DB_USERNAME: system
  DB_PASSWORD: ${{ secrets.CI_ORACLE_PASSWORD }}
  DB_SID: XEPDB1
  DB_SCHEMA: spider_admin

# ci.yml — MySQL Job
env:
  SPRING_PROFILES_ACTIVE: ci-mysql
  DB_HOST: localhost
  DB_USERNAME: root
  DB_PASSWORD: ${{ secrets.CI_MYSQL_PASSWORD }}
  DB_NAME: spider_admin
```

| Secret | 용도 |
|--------|------|
| `CI_ORACLE_PASSWORD` | Oracle 컨테이너 비밀번호 |
| `CI_MYSQL_PASSWORD` | MySQL 컨테이너 비밀번호 |
| `CI_TEST_USER_ID` | 테스트 계정 ID |
| `CI_TEST_PASSWORD` | 테스트 계정 비밀번호 |
| `SLACK_WEBHOOK_URL` | CI 실패 알림 Webhook |

---

*Last updated: 2026-02-23*
