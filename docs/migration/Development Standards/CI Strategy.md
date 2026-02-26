# CI Strategy

## 1. 개요

`main` 브랜치는 직접 push를 허용하지 않는다. 모든 변경은 Pull Request를 통해서만 병합되며, PR이 열리거나 업데이트될 때마다 CI 파이프라인이 자동 실행된다. **모든 Job이 통과해야 merge가 가능하다.**

테스트는 **Oracle과 MySQL 각각에 대해 독립적으로 실행**한다. 두 DB 모두에서 통과해야 한다.

```
feature/* ──PR──► main
                   │
                   ├── Dependabot       (주간 의존성 취약점 스캔 — 별도 워크플로)
                   │
                   └── CI Pipeline (.github/workflows/ci.yml)
                         ├── build              (빌드 + 유닛 테스트 + SonarCloud 분석)
                         ├── playwright-oracle  (E2E UI 테스트 / Oracle)   ┐ build 완료 후
                         ├── playwright-mysql   (E2E UI 테스트 / MySQL)    │
                         ├── newman-oracle      (API 테스트 / Oracle)      │ 병렬 실행
                         └── newman-mysql       (API 테스트 / MySQL)       ┘
```

---

## 2. GitHub Actions

CI 플랫폼으로 **GitHub Actions**를 사용한다.

- 워크플로 파일: `.github/workflows/ci.yml`
- 트리거: `main` 브랜치 대상 `pull_request` 및 `push`
- 실행 환경: `ubuntu-latest`

```yaml
on:
  push:
    branches: [main]
  pull_request:
    branches: [main]
```

PR 코멘트 게시 및 Check 상태 업데이트에 필요한 권한을 워크플로 수준에서 명시한다.

```yaml
permissions:
  contents: read
  checks: write
  pull-requests: write
```

### 2.1 Concurrency 제어

동일 PR에 커밋이 연속으로 push되면 이전 실행을 즉시 취소하고 최신 실행만 유지한다. Runner 낭비와 PR 코멘트 중복 게시를 방지한다.

```yaml
concurrency:
  group: ci-${{ github.ref }}
  cancel-in-progress: true
```

### 2.2 의존성 캐싱

빌드 속도와 Runner 비용 절감을 위해 의존성을 캐싱한다. `setup-java`의 내장 캐시 기능을 사용하며, 캐시 키는 `pom.xml` 해시 기반으로 자동 생성된다.

| 대상 | 캐시 키 기반 | 설정 방법 |
|------|-------------|-----------|
| Maven (`~/.m2/repository`) | `pom.xml` 해시 | `actions/setup-java` → `cache: maven` |
| npm (`~/.npm`) | `package-lock.json` 해시 | `actions/setup-node` → `cache: npm` |
| Playwright 브라우저 바이너리 | Playwright 버전 | `actions/cache` (별도 지정) |

```yaml
# Playwright 브라우저 캐싱 예시
- uses: actions/cache@v4
  with:
    path: ~/.cache/ms-playwright
    key: playwright-${{ hashFiles('package-lock.json') }}
```

---

## 3. 인프라 (Docker 컨테이너)

### 3.1 방침

DB를 포함한 모든 인프라 의존성은 **Docker 컨테이너**로 격리하여 실행한다. 외부 서버나 공유 환경에 의존하지 않으며, Job 시작 시 컨테이너를 기동하고 종료 시 자동으로 정리된다.

모든 테스트는 **Oracle과 MySQL 각각에 대해 독립 Job으로 실행**한다. DB별로 별도의 Job이 기동·테스트·리포트의 전 과정을 독립적으로 수행하며, 두 Job 모두 통과해야 한다.

컨테이너 이미지는 **패치 버전까지 고정**한다. Floating 태그(`mysql:8`)는 업스트림 변경으로 동일 워크플로가 다른 결과를 낼 수 있다.

GitHub Actions도 동일 원칙을 적용한다. 문서 예시는 `@v4` 형태로 표기하지만, 실제 워크플로에서는 패치 버전까지 고정(`@v4.x.x`)하고 Dependabot `github-actions` 에코시스템이 자동으로 갱신한다. 보안 민감한 Action(시크릿 접근 등)은 SHA 핀으로 고정한다.

| 인프라 | 이미지 (고정 예시) | 비고 |
|--------|-------------------|------|
| Oracle | `gvenzl/oracle-xe:21.3.0-slim` | 운영 환경 동일 엔진 |
| MySQL  | `mysql:8.0.41`                 | 멀티 DB 호환성 보장 |

GitHub Actions `services` 블록을 사용하여 Job과 생명주기를 함께한다.

**Oracle Job 예시:**
```yaml
services:
  oracle:
    image: gvenzl/oracle-xe:21.3.0-slim
    env:
      ORACLE_PASSWORD: ${{ secrets.CI_ORACLE_PASSWORD }}
    ports:
      - 1521:1521
    options: >-
      --health-cmd healthcheck.sh
      --health-interval 10s
      --health-timeout 5s
      --health-retries 20
```

**MySQL Job 예시:**
```yaml
services:
  mysql:
    image: mysql:8.0.41
    env:
      MYSQL_ROOT_PASSWORD: ${{ secrets.CI_MYSQL_PASSWORD }}
      MYSQL_DATABASE: spider_admin
    ports:
      - 3306:3306
    options: >-
      --health-cmd "mysqladmin ping -h localhost"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 10
```

### 3.2 DDL / DML 관리

테스트 환경 초기화 스크립트는 `src/test/resources/db/` 디렉터리에서 DB별로 구분하여 버전 관리한다.

```
src/test/resources/db/
├── oracle/
│   ├── ddl/    # Oracle용 CREATE 스크립트 (시퀀스, 파티션 등 Oracle 전용 구문 포함)
│   └── dml/    # Oracle용 테스트 픽스처
└── mysql/
    ├── ddl/    # MySQL용 CREATE 스크립트 (AUTO_INCREMENT 등 MySQL 전용 구문 포함)
    └── dml/    # MySQL용 테스트 픽스처
```

**관리 원칙:**

- 스키마 변경 시 Oracle·MySQL 양쪽 스크립트를 함께 수정한다. 한쪽만 수정하면 CI에서 즉시 검출된다.
- DML은 **멱등성**을 보장하도록 Oracle은 `MERGE`, MySQL은 `INSERT ... ON DUPLICATE KEY UPDATE` 패턴으로 작성한다.
- CI Job에서 컨테이너 헬스체크 통과 직후 `ddl/ → dml/` 순서로 스크립트를 실행하여 초기 상태를 구성한다.

---

## 4. 의존성 보안 (Dependabot)

**Dependabot**을 사용하여 서드파티 라이브러리의 알려진 CVE를 자동으로 탐지하고 수정 PR을 생성한다. SonarCloud가 *작성한 코드*의 품질과 보안 취약점을 분석한다면, Dependabot은 *사용하는 라이브러리*의 취약점을 관리한다.

설정 파일 위치: `.github/dependabot.yml`

```yaml
version: 2
updates:
  # Maven 의존성 (pom.xml)
  - package-ecosystem: "maven"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
    open-pull-requests-limit: 10

  # npm 의존성 (package.json — Playwright, Newman)
  - package-ecosystem: "npm"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
    open-pull-requests-limit: 10

  # GitHub Actions 버전 (workflows/*.yml)
  - package-ecosystem: "github-actions"
    directory: "/"
    schedule:
      interval: "weekly"
      day: "monday"
    open-pull-requests-limit: 5
```

- 취약점이 발견되면 Dependabot이 자동으로 PR을 생성하고, GitHub Security 탭에 알림이 표시된다.
- 생성된 PR도 CI 파이프라인을 통과해야 merge된다.
- `github-actions` 에코시스템을 포함하여 워크플로에서 사용하는 Action 버전도 자동으로 관리한다.

---

## 5. CI Job 구성

`build` Job이 생성한 JAR을 아티팩트로 공유하고, 후속 Job들이 이를 내려받아 재사용한다. 빌드를 한 번만 수행하여 모든 테스트가 동일한 바이너리를 검증한다.

모든 Job에 `timeout-minutes`를 설정한다. 컨테이너 헬스체크 실패나 테스트 무한 대기 상황에서 Job이 GitHub Actions 기본 한도(6시간)까지 Runner를 점유하는 것을 방지한다.

```
[ci.yml]
build ──┬──► playwright-oracle  ┐
        ├──► playwright-mysql   │ 모두 병렬 실행
        ├──► newman-oracle      │
        └──► newman-mysql       ┘
```

### 5.1 build — 빌드 + 유닛 테스트 + SonarCloud 분석

| 항목 | 내용 |
|------|------|
| 환경 | JDK 17 (Temurin), Maven, Node.js 20 |
| 타임아웃 | 15분 |
| 단계 | `npm ci` → `npm run lint` → `compile` → `test` → `package -DskipTests` → `SonarCloud scan` |
| 산출물 | `target/*.jar` (아티팩트 보관 1일) |
| 리포트 | Surefire XML → PR Check + 코멘트 / ESLint·HTMLHint 위반 시 빌드 실패로 Check 차단 / SonarCloud → PR Decoration + SonarCloud 대시보드 |

- 유닛 테스트는 외부 의존성(DB, 네트워크) 없이 실행되어야 한다. DB가 필요한 테스트는 통합 테스트로 분리한다.
- `compile` 단계에서 빌드 실패 시 이후 모든 Job이 실행되지 않는다.
- `npm run lint` (`eslint` + `htmlhint`)는 Java 빌드 전에 실행하여 프론트엔드 금지 패턴을 조기 차단한다. ESLint 규칙과 HTMLHint 규칙은 Frontend Code Convention 참고.

**코드 품질 + SAST 분석 — SonarCloud:**

**SonarCloud**(SonarQube Cloud)를 사용하여 코드 품질 분석과 SAST 보안 분석을 통합 수행한다. Public GitHub 레포지토리에서 무료로 사용 가능하다.

| 분석 영역 | SonarCloud 기능 |
|-----------|----------------|
| **버그 탐지** | Bug 룰 (null 역참조, 리소스 누수 등) |
| **코드 품질** | Code Smell 룰 (복잡도, 미사용 변수, 유지보수성) |
| **코드 중복** | Duplications 탐지 |
| **보안 취약점** | SAST Taint Analysis (SQL Injection, XSS, Path Traversal 등) |
| **보안 핫스팟** | Security Hotspot 리뷰 |
| **코드 커버리지** | JaCoCo 연동 (추후 설정) |

SonarCloud는 `build` Job의 마지막 단계에서 실행된다. `mvn verify` 완료 후 별도 step으로 `mvn sonar:sonar`를 실행하여 컴파일된 바이트코드와 테스트 리포트를 함께 분석한다.

**pom.xml 설정:**

```xml
<!-- pom.xml properties -->
<sonar.organization>neobnsrnd-team</sonar.organization>
<sonar.host.url>https://sonarcloud.io</sonar.host.url>
```

**ci.yml SonarCloud step:**

```yaml
- name: SonarCloud Scan
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: mvn sonar:sonar -B
```

**PR Decoration:**
- SonarCloud가 PR에 직접 코멘트를 게시한다 (품질 게이트 통과 여부, 신규 이슈 수).
- Quality Gate 실패 시 PR Check가 `failure`로 표시되어 merge를 블로킹한다.
- 결과는 SonarCloud 대시보드에서 누적 관리된다.

### 5.2 playwright — E2E UI 테스트

| 항목 | 내용 |
|------|------|
| 환경 | JDK 17 + Node.js 20 + Chromium |
| 타임아웃 | 30분 |
| 의존 | `build` Job 완료 |
| 대상 | `playwright/**/*.spec.ts` |
| 리포트 | JUnit XML → PR Check + 코멘트, HTML 리포트 아티팩트 (보관 7일) |

동일한 테스트 스펙을 Oracle / MySQL 각각의 컨테이너를 띄워 독립적으로 실행한다. `build` Job 아티팩트(JAR)를 내려받아 `SPRING_PROFILES_ACTIVE=ci-oracle` 또는 `ci-mysql`로 Spring Boot를 백그라운드 기동한다. 서버 헬스체크(최대 120초, `/login` 응답 확인) 통과 후 테스트를 실행한다. 실패 시 Spring Boot 로그를 아티팩트로 업로드한다 (보관 3일).

### 5.3 newman — API 테스트

| 항목 | 내용 |
|------|------|
| 환경 | JDK 17 + Node.js 20 + Newman CLI |
| 타임아웃 | 20분 |
| 의존 | `build` Job 완료 (`playwright-*`와 병렬) |
| 대상 | `postman/**/*.postman_collection.json` |
| 리포트 | JUnit XML → PR Check + 코멘트, XML 결과 아티팩트 (보관 7일) |

실행 구조는 5.2절 playwright와 동일하다 (Oracle/MySQL 독립 Job, JAR 재사용, 백그라운드 기동). Postman 컬렉션은 `postman/{domain}/` 디렉터리에서 도메인별로 관리한다. Spring Security CSRF 구조상 `curl`로 직접 인증하여 `JSESSIONID`를 추출한 뒤 Newman 환경 변수로 전달한다.

> 컬렉션 작성 표준은 Postman 문서 참고.

### 5.4 SonarCloud Quality Gate 설정

SonarCloud의 Quality Gate는 PR merge 조건으로 사용된다. **Sonar Way** (기본 프로파일)를 사용하며, 주요 조건:

| 조건 | 기준 |
|------|------|
| 신규 버그 | 0개 |
| 신규 취약점 | 0개 |
| 신규 보안 핫스팟 리뷰율 | 100% |
| 신규 코드 중복률 | 3% 이하 |
| 신규 코드 커버리지 | 80% 이상 (JaCoCo 연동 후) |

- Quality Gate 실패 시 GitHub PR Check가 `failure`로 표시되어 merge가 블로킹된다.
- SonarCloud 대시보드에서 프로젝트 전체 품질 현황을 확인할 수 있다.
- SAST Taint Analysis로 SQL Injection, XSS, Path Traversal 등 CWE 기반 취약점을 탐지한다.

---

## 6. 테스트 결과 리포트 및 PR 코멘트

모든 Job은 성공·실패와 관계없이(`if: always()`) 결과를 수집하고 PR에 코멘트로 게시한다.

### 6.1 사용 도구

| 도구 | 역할 |
|------|------|
| `EnricoMi/publish-unit-test-result-action@v2` | JUnit XML 파싱 → PR Check + 인라인 코멘트 |
| `actions/upload-artifact@v4` | HTML 리포트, 로그 등 원문 산출물 보관 |

### 6.2 PR 코멘트 예시

```
Build — Unit Tests                     ✅  42 passed,  0 failed
SonarCloud — Quality Gate              ✅  Passed (0 bugs, 0 vulnerabilities, 0 hotspots)
Playwright — E2E Tests (Oracle)        ✅  10 passed,  0 failed
Playwright — E2E Tests (MySQL)         ❌  10 passed,  2 failed  ← 실패 케이스명 + 오류 메시지 포함
Newman — API Tests (Oracle)            ✅  36 passed,  0 failed
Newman — API Tests (MySQL)             ✅  36 passed,  0 failed
```

### 6.3 아티팩트 보관 정책

| 아티팩트 | 보관 기간 | 조건 |
|----------|-----------|------|
| `app-jar` | 1일 | 항상 |
| SonarCloud 대시보드 | — (SonarCloud 호스팅) | 항상 |
| `playwright-report-oracle` | 7일 | 항상 |
| `playwright-report-mysql` | 7일 | 항상 |
| `newman-results-oracle` | 7일 | 항상 |
| `newman-results-mysql` | 7일 | 항상 |
| `spring-boot-log-*` | 3일 | 실패 시에만 |

---

## 7. 알림

PR 코멘트는 리뷰어가 직접 확인해야 하므로, 아래 이벤트는 **Slack 또는 이메일로 push 알림**을 별도 설정한다.

| 이벤트 | 이유 |
|--------|------|
| `main` push 후 CI 실패 | merge 직후 파이프라인 깨짐 — 즉시 인지 필요 |
| Dependabot PR 생성 | 신규 취약점 발견 시 빠른 대응 필요 |

```yaml
# main 실패 알림 예시 (Slack)
- name: Notify on main failure
  if: failure() && github.ref == 'refs/heads/main'
  uses: slackapi/slack-github-action@v2
  with:
    payload: '{"text": "CI build failed on main: ${{ github.repository }} — ${{ github.sha }}"}'
  env:
    SLACK_WEBHOOK_URL: ${{ secrets.SLACK_WEBHOOK_URL }}
```

---

## 8. 시크릿 관리

민감 정보는 코드와 설정 파일에 직접 포함하지 않는다. 환경 변수를 통해서만 주입하며, CI에서는 GitHub Secrets를 단일 소스로 사용한다.

| 시크릿 | 용도 |
|--------|------|
| `CI_TEST_USER_ID` | 테스트 계정 ID |
| `CI_TEST_PASSWORD` | 테스트 계정 비밀번호 |
| `CI_ORACLE_PASSWORD` | CI용 Oracle 컨테이너 비밀번호 |
| `CI_MYSQL_PASSWORD` | CI용 MySQL 컨테이너 비밀번호 |
| `SLACK_WEBHOOK_URL` | 알림용 Slack Incoming Webhook |
| `SONAR_TOKEN` | SonarCloud 분석 인증 토큰 |

현재 CI는 외부 의존성 없이 유닛 테스트만 실행하므로 DB 관련 시크릿은 불필요하다. 향후 Docker 기반 통합 테스트를 추가하면 `CI_ORACLE_PASSWORD`, `CI_MYSQL_PASSWORD` 등의 시크릿이 필요하다.

> 로컬 환경 `.env` 관리는 Configuration 7-8절 참고.

---

*Last updated: 2026-02-26*
