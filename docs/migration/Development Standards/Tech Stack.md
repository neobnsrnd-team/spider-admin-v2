# Tech Stack

## 1. 개요

프로젝트에서 사용하는 기술 스택과 버전을 정의한다. 버전의 권위 소스(single source of truth)는 `pom.xml`과 `package.json`이며, 이 문서는 빠른 참조용이다.

---

## 2. 코어 프레임워크

| 기술 | 버전 | 비고 |
|------|------|------|
| Java (OpenJDK) | 17 | LTS |
| Spring Boot | 3.2.1 | `spring-boot-starter-parent` |
| Spring Framework | 6.1.x | Spring Boot가 관리 |
| Spring Security | 6.2.x | Spring Boot가 관리 |
| Spring AOP | 6.1.x | Spring Boot가 관리 |

---

## 3. 데이터

| 기술 | 버전 | 비고 |
|------|------|------|
| Oracle | 19c+ | 주 데이터베이스, ojdbc11 |
| MySQL | 8.0+ | 대체 데이터베이스 (Multi-DB 참고) |
| MyBatis Spring Boot Starter | 3.0.3 | SQL 매핑 |
| Caffeine | Spring Boot 관리 | 인메모리 캐시 (Caching 참고) |
| H2 | Spring Boot 관리 | DBAppender 로그 저장 |

---

## 4. 웹·문서화

| 기술 | 버전 | 비고 |
|------|------|------|
| Thymeleaf | 3.1.x | Spring Boot가 관리, 서버 사이드 렌더링 |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI, API 문서 자동 생성 |

---

## 5. 프론트엔드

| 기술 | 버전 | 비고 |
|------|------|------|
| Tailwind CSS | 3.4.x | 유틸리티 클래스 기반 스타일링, Frontend Code Convention §2 참고 |
| jQuery | 3.7.x | DOM 조작, AJAX (`fetch` 대신 `$.ajax` 사용), Frontend Code Convention §3 참고 |

> 프론트엔드 빌드: `tailwindcss` CLI (PostCSS). 전 환경에서 PostCSS 빌드 사용. CDN은 CSP(`style-src 'self'`) 정책으로 차단되므로 사용하지 않는다.

---

## 6. 유틸리티

| 기술 | 버전 | 비고 |
|------|------|------|
| Lombok | 1.18.34 | 보일러플레이트 제거, `provided` 스코프 |
| Logstash Logback Encoder | 8.0 | ELK 연동 JSON 로그 |
| Logback Classic DB | 1.2.11.1 | DB 로그 어펜더 |
| Apache POI (poi-ooxml) | 5.2.5 | Excel 파일 생성 |

---

## 7. 빌드

| 기술 | 버전 | 비고 |
|------|------|------|
| Maven | 3.x | Java 빌드 도구 |
| Maven Compiler Plugin | 3.11.0 | `release=17` |
| Spotless | 2.44.0 | Palantir Java Format, Java 코드 포맷 강제 |
| ESLint | 9.x | JavaScript 코드 품질 강제, Frontend Code Convention §4 참고 |
| HTMLHint | 1.x | HTML 구조 검사, Thymeleaf 템플릿 오용 탐지 보조 |
| Husky | 9.x | Pre-commit 훅, 금지 패턴 커밋 시점 차단, Frontend Code Convention §5 참고 |

---

## 8. 테스트·CI

| 기술 | 버전 | 비고 |
|------|------|------|
| JUnit 5 | Spring Boot 관리 | 유닛 테스트 |
| Spring Security Test | Spring Boot 관리 | 보안 테스트 |
| MyBatis Starter Test | 3.0.3 | Mapper 테스트 |
| Testcontainers | Spring Boot 관리 | DB 통합 테스트 (Oracle/MySQL 컨테이너) |
| ArchUnit | 1.3.0 | 아키텍처 규칙 테스트 |
| Node.js | 20.x | ESLint, Playwright, Newman 런타임 |
| Playwright | 1.50.x | E2E UI 테스트 |
| Newman | 6.2.x | Postman API 테스트 |
| GitHub Actions | — | CI 파이프라인 |
| SonarCloud | — | 코드 품질 + SAST 보안 분석 |

---

## 9. 버전 관리 원칙

| 원칙 | 설명 |
|------|------|
| Spring Boot 관리 의존성 | 버전을 명시하지 않는다. `spring-boot-starter-parent`가 호환 버전을 결정한다. |
| 외부 의존성 | `pom.xml` `<properties>`에 버전을 선언하고 `${}` 플레이스홀더로 참조한다. |
| 버전 업그레이드 시 | `pom.xml` 변경과 함께 이 문서를 갱신한다. |

---

*Last updated: 2026-02-26*
