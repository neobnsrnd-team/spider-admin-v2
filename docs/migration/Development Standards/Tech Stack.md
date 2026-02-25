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
| PageHelper Spring Boot Starter | 2.1.0 | MyBatis 페이징 |
| Caffeine | Spring Boot 관리 | 인메모리 캐시 (Caching 참고) |
| H2 | Spring Boot 관리 | DBAppender 로그 저장 |
| PostgreSQL | Spring Boot 관리 | RAG 전용 |

---

## 4. 웹·문서화

| 기술 | 버전 | 비고 |
|------|------|------|
| Thymeleaf | 3.1.x | Spring Boot가 관리, 서버 사이드 렌더링 |
| SpringDoc OpenAPI | 2.3.0 | Swagger UI, API 문서 자동 생성 |

---

## 5. 유틸리티

| 기술 | 버전 | 비고 |
|------|------|------|
| Lombok | 1.18.34 | 보일러플레이트 제거, `provided` 스코프 |
| Logstash Logback Encoder | 8.0 | ELK 연동 JSON 로그 |
| Logback Classic DB | 1.2.11.1 | DB 로그 어펜더 |

---

## 6. 빌드

| 기술 | 버전 | 비고 |
|------|------|------|
| Maven | 3.x | 빌드 도구 |
| Maven Compiler Plugin | 3.11.0 | `release=17` |
| Spotless | 2.44.0 | Google Java Format (AOSP), 코드 포맷 강제 |

---

## 7. 테스트·CI

| 기술 | 버전 | 비고 |
|------|------|------|
| JUnit 5 | Spring Boot 관리 | 유닛 테스트 |
| Spring Security Test | Spring Boot 관리 | 보안 테스트 |
| MyBatis Starter Test | 3.0.3 | Mapper 테스트 |
| ArchUnit | 1.3.0 | 아키텍처 규칙 테스트 |
| Playwright | 1.50.x | E2E UI 테스트 |
| Newman | 6.2.x | Postman API 테스트 |
| GitHub Actions | — | CI 파이프라인 |
| CodeQL | — | SAST 보안 분석 |

---

## 8. 버전 관리 원칙

| 원칙 | 설명 |
|------|------|
| Spring Boot 관리 의존성 | 버전을 명시하지 않는다. `spring-boot-starter-parent`가 호환 버전을 결정한다. |
| 외부 의존성 | `pom.xml` `<properties>`에 버전을 선언하고 `${}` 플레이스홀더로 참조한다. |
| 버전 업그레이드 시 | `pom.xml` 변경과 함께 이 문서를 갱신한다. |

---

*Last updated: 2026-02-24*
