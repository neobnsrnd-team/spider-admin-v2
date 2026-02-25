# Test Strategy

## 1. 개요

테스트 계층, 우선순위, 검증 항목을 정의한다. API 테스트, 보안 테스트, 데이터 테스트, E2E 시나리오를 포함하며, 단위 테스트부터 E2E까지 계층별 전략을 수립한다.

**적용 범위:**

| 포함 | 제외 |
|------|------|
| 테스트 계층 및 비중, 우선순위 분류 | 성능테스트 계획 (performance-test-plan 문서) |
| API·보안·데이터·E2E 테스트 전략 | CI 파이프라인 구성 (CI Strategy 문서) |
| 테스트 데이터 기준, 체크리스트 | Postman 컬렉션 작성법 (Postman 문서) |

---

## 2. 테스트 계층 및 범위

### 2.1 테스트 피라미드

```
          ┌─────────────┐
          │   E2E 테스트  │  ← 핵심 시나리오만 (비용 높음)
         ─┼─────────────┼─
        ┌─┼─────────────┼─┐
        │ │ 통합 테스트    │ │  ← API + DB + Security (주력)
       ─┼─┼─────────────┼─┼─
      ┌─┼─┼─────────────┼─┼─┐
      │ │ │ 단위 테스트    │ │ │  ← Service, Util (기반)
      └─┼─┼─────────────┼─┼─┘
        └─┼─────────────┼─┘
          └─────────────┘
```

### 2.2 계층별 전략

| 계층 | 대상 | 도구 | 비중 |
|------|------|------|------|
| **단위 테스트** | Service, Util | JUnit 5 + Mockito | 40% |
| **통합 테스트** | API 엔드포인트 (Controller → DB) | SpringBootTest + TestRestTemplate | 50% |
| **E2E 테스트** | 핵심 업무 시나리오 | Postman (Newman) / 부하 발생기 | 10% |

> 단위 테스트 패턴(Mockito + Guard Clause 검증)은 Exception Handling 5.3절, 10.1절을 참고하세요.
> Postman 컬렉션 구성은 Postman 문서 참고.

---

## 3. 테스트 우선순위 분류

### 3.1 위험도 기반 분류

도메인별 테스트 우선순위는 다음 기준으로 판단한다.

**[P1] Critical - 반드시 테스트**

| 유형 | 사유 | 핵심 테스트 포인트 |
|------|------|-------------------|
| 인증/인가 | 전 시스템 영향, 보안 사고 직결 | 로그인, 비밀번호 변경, 권한 할당 |
| 핵심 업무 | 가장 복잡한 비즈니스 로직 | CRUD, 연관 데이터 정합성, 상태 전환 |
| 대용량 조회 | 성능 병목 발생 가능성 높음 | 페이징 성능, 날짜 범위 검색 |

**[P2] High - 우선 테스트**

| 유형 | 사유 | 핵심 테스트 포인트 |
|------|------|-------------------|
| 외부 연동 | 장애 전파 위험 | 타임아웃, 연결 실패, 재시도 |
| 배치/스케줄링 | 실행 오류 시 데이터 정합성 훼손 | 동시 실행 방지, 이력 관리 |
| 설정 관리 | 잘못된 설정이 전체 시스템 영향 | 설정 CRUD, 변경 이력 |

**[P3] Medium - 기본 테스트**

| 유형 | 사유 | 핵심 테스트 포인트 |
|------|------|-------------------|
| 공통 코드 | 다른 도메인에서 참조 | CRUD, 중복 체크 |
| 모니터링/로그 | 장애 원인 분석 기반 | 조회, 페이징 |

**[P4] Low - 최소 테스트**

| 유형 | 사유 | 핵심 테스트 포인트 |
|------|------|-------------------|
| 게시판, 공지사항 | 핵심 업무 아님 | 기본 CRUD |
| 단순 마스터 관리 | 복잡도 낮음 | 기본 CRUD |

---

## 4. API 테스트 전략

### 4.1 CRUD 공통 테스트 패턴

> URL 설계 및 HTTP 메서드 매핑은 API Design 2~3절 참고.

모든 REST API에 적용하는 표준 테스트 케이스:

| 순번 | 테스트 | HTTP | 검증 포인트 |
|------|--------|------|------------|
| 1 | 목록 조회 | `GET /api/{resource}` | 페이징 파라미터, 정렬, 검색 필터 |
| 2 | 단건 조회 | `GET /api/{resource}/{id}` | 존재하는 ID → 200, 존재하지 않는 ID → 404 |
| 3 | 생성 | `POST /api/{resource}` | 필수 값 검증 (`@Valid`) → 400, 중복 체크 → 409, 성공 → 201 |
| 4 | 수정 | `PUT /api/{resource}/{id}` | 존재 확인, 변경 값 반영 → 200 |
| 5 | 삭제 | `DELETE /api/{resource}/{id}` | 존재 확인, 참조 무결성 → 204 |

### 4.2 공통 응답 구조 검증

> 응답 구조(`ApiResponse<T>`, `ErrorDetail`)는 API Design 4절 참고.

모든 API 응답이 `ApiResponse<T>` 래핑을 준수하는지 검증한다:

| 케이스 | 검증 항목 |
|--------|----------|
| 성공 응답 | `success: true`, `data` 존재 |
| 에러 응답 | `success: false`, `error.code`, `error.message`, `error.traceId` 존재 |
| 검증 에러 | `error.code: "INVALID_INPUT"`, `error.fields[]` 배열 존재 |

### 4.3 MyBatis 동적 쿼리 무결성 테스트

> MyBatis XML 매핑 규칙은 SQL 문서 참고.

MyBatis `<if>`, `<choose>` 등 동적 SQL 조건이 누락될 경우 의도하지 않은 전체 테이블 영향이 발생할 수 있다. 다음을 반드시 검증한다:

| 테스트 | 검증 포인트 |
|--------|------------|
| 검색 조건 전체 NULL | WHERE 절 없이 전체 조회 방지 (의도된 동작인지 확인) |
| UPDATE 조건 NULL | `<if>` 누락 시 전체 행 UPDATE 방지 (WHERE 절 필수 존재 확인) |
| DELETE 조건 NULL | 조건 없는 DELETE 방지 |
| `<foreach>` 빈 리스트 | `collection`이 빈 리스트일 때 SQL 문법 오류 없음 확인 |
| `<choose>` 기본값 | 모든 `<when>` 불일치 시 `<otherwise>` 동작 확인 |

> **주의:** `UPDATE ... SET col = #{value}` 에서 WHERE 절이 동적 조건에만 의존하는 경우,
> 모든 조건이 NULL이면 **테이블 전체 행이 변경**되는 치명적 사고가 발생한다.

### 4.4 페이징 테스트 표준

> `PageRequest`/`PageResponse` 구조는 API Design 7절 참고.

```
테스트 항목:
├── 기본 조회 (page=1, size=20)
├── 빈 결과 (존재하지 않는 검색 조건)
├── 경계값 (page=0, size=0, size=MAX)
├── 정렬 (sortBy=필드명, sortDirection=ASC/DESC)
└── 검색 필터 조합 (각 필터 단독 + 복합)
```

### 4.5 외부 연동 API 테스트

> 외부 시스템 예외 처리 패턴은 Exception Handling 6절 참고.

외부 시스템을 호출하는 API는 추가로 다음을 검증한다:

| 테스트 | 검증 포인트 |
|--------|------------|
| 정상 응답 | 외부 시스템 정상 응답 시 결과 처리 |
| 타임아웃 | Connect/Read Timeout 초과 시 `EXTERNAL_SERVICE_ERROR` 응답 |
| 연결 실패 | 외부 시스템 미기동 시 `EXTERNAL_SERVICE_ERROR` 응답 |
| 다중 호출 | 여러 인스턴스 순차 호출 시 부분 실패 처리 |

---

## 5. 보안 테스트 전략

> 인증·인가 아키텍처는 Authentication & Authorization 문서 참고.
> SecurityFilterChain 설정은 Security 문서 참고.

### 5.1 인증 테스트

| 테스트 | 검증 포인트 |
|--------|------------|
| 로그인 성공 | 올바른 ID/PW → 세션 생성, 리다이렉트 |
| 로그인 실패 | 잘못된 PW → 에러 메시지, 계정 잠금 (임계치 초과 시) |
| 미인증 접근 | API 직접 호출 → 401 또는 로그인 리다이렉트 |
| 세션 만료 | 세션 타임아웃 후 API 호출 → 401 |
| 동시 세션 | 다른 브라우저 로그인 → 이전 세션 만료 (maxSession 정책에 따름) |

### 5.2 인가 테스트

> `@PreAuthorize` 패턴은 Authentication & Authorization 8절 참고.

| 테스트 | 검증 포인트 |
|--------|------------|
| READ 권한으로 GET | 조회 성공 (200) |
| READ 권한으로 POST/PUT/DELETE | 403 Forbidden |
| WRITE 권한으로 모든 HTTP Method | 성공 |
| 메뉴/권한 미할당 사용자 | 403 Forbidden |
| 관리자 역할 사용자 | 전체 접근 가능 |

**수평 권한 상승 (Horizontal Privilege Escalation) 테스트:**

API-level 인가(메뉴/역할)와 별개로, 데이터-level 접근 제어를 검증한다.

| 테스트 | 검증 포인트 |
|--------|------------|
| 타인 리소스 조회 | URL의 `{id}`를 다른 사용자의 리소스 ID로 변경 → 403 또는 데이터 필터링 |
| 타인 리소스 수정/삭제 | 본인 소유가 아닌 리소스에 PUT/DELETE → 403 |
| ID 열거 공격 | 순차적 ID 변경으로 타인 데이터 접근 시도 → 차단 확인 |

> **주의:** 메뉴 접근 권한이 있더라도 다른 사용자의 데이터에 접근 가능한지 반드시 확인해야 한다.
> 관리자 시스템은 특성상 데이터 범위 제한이 필요한 경우 (부서별, 담당자별 등) 이 테스트가 핵심이다.

### 5.3 보안 취약점 테스트

> CSRF, CORS, 보안 헤더 설정은 Security 문서 참고.

| 항목 | 테스트 방법 |
|------|------------|
| SQL Injection | 검색 파라미터에 `' OR 1=1 --` 주입 (MyBatis `#{}` 바인딩 확인) |
| XSS | 입력 필드에 `<script>alert(1)</script>` 주입 |
| CSRF | API 호출 시 CSRF 토큰 없이 요청 (웹 폼) |
| 경로 탐색 | `../../etc/passwd` 등 파일 경로 주입 (파일 다운로드 기능) |
| 권한 상승 | 일반 사용자로 관리자 전용 API 직접 호출 |

---

## 6. 에러 처리 테스트 전략

> `ErrorType` enum 정의 및 `BusinessException` 패턴은 Exception Handling 2~5절 참고.

### 6.1 ErrorType별 테스트

| ErrorType | 트리거 | 기대 응답 |
|-----------|--------|----------|
| `RESOURCE_NOT_FOUND` | 존재하지 않는 ID 조회 | 404 + `error.code: "RESOURCE_NOT_FOUND"` |
| `DUPLICATE_RESOURCE` | 중복 키로 생성 | 409 + `error.code: "DUPLICATE_RESOURCE"` |
| `INVALID_INPUT` | `@Valid` 검증 실패 | 400 + `error.code: "INVALID_INPUT"` + `error.fields[]` |
| `BUSINESS_RULE_VIOLATED` | 상태 전이 규칙 위반 | 409/422 + `error.code: "BUSINESS_RULE_VIOLATED"` |
| `FORBIDDEN` | 권한 없는 API 접근 | 403 + `error.code: "FORBIDDEN"` |
| `EXTERNAL_SERVICE_ERROR` | 외부 API 호출 실패 | 502 + `error.code: "EXTERNAL_SERVICE_ERROR"` |

### 6.2 응답 보안 검증

> 응답 보안 규칙은 API Design 4.4절 참고.

에러 응답에 다음 항목이 포함되지 않는지 확인한다:

| 금지 항목 | 확인 방법 |
|-----------|----------|
| Stack trace | `error.message`에 패키지명·클래스명 미포함 |
| DB 오류 메시지 | `ORA-`, `MySQL error` 등 원본 메시지 미노출 |
| 내부 파일 경로 | 서버 디렉토리 구조 미노출 |
| `detail` 파라미터 | `BusinessException`의 식별자가 클라이언트에 미노출 (로그에만 기록) |

---

## 7. 데이터 테스트 전략

### 7.1 대용량 테이블 성능 기준

이력성 테이블은 데이터가 누적되므로 운영 수준의 데이터량으로 테스트해야 한다.

| 테이블 유형 | 예상 데이터량 | 응답시간 목표 |
|-------------|-------------|-------------|
| 트랜잭션 이력 (거래 로그) | 수백만 건 | 페이징 조회 3초 이내 |
| 접근 이력 (감사 로그) | 수십만 건/월 | 페이징 조회 3초 이내 |
| 배치 실행 이력 | 수만 건/월 | 페이징 조회 2초 이내 |
| 에러 발생 이력 | 수만 건/월 | 페이징 조회 2초 이내 |

### 7.2 외부 연동 성능

| 항목 | 기준 | 테스트 포인트 |
|------|------|-------------|
| Connect Timeout | 5초 이내 | 타임아웃 초과 시 `BusinessException(EXTERNAL_SERVICE_ERROR)` 확인 |
| Read Timeout | 10초 이내 | 타임아웃 초과 시 `BusinessException(EXTERNAL_SERVICE_ERROR)` 확인 |
| 다중 인스턴스 호출 | 순차/병렬 여부 확인 | 부분 실패 시 전체 롤백 또는 부분 성공 정책 |

### 7.3 DB별 특화 테스트

> Multi-DB 아키텍처(`databaseId` 기반 SQL 분기)는 Multi-DB 문서 참고.

**Oracle 특화:**

| 항목 | 테스트 포인트 |
|------|-------------|
| Batch Insert | `databaseId="oracle"` SQL에서 DUAL + UNION ALL 패턴 정상 동작 (ORA-00933 방지) |
| Pagination | PageHelper `autoDialect`에 의한 ROWNUM 기반 페이징 정확성 |
| 시퀀스 정합성 | 시퀀스 기반 PK 생성 시 채번 누락/중복 없음 확인 (RAC 환경 포함) |
| 한글 바이트 길이 | VARCHAR2(N **BYTE**) 컬럼에 한글 입력 시 ORA-12899 발생 여부 |
| DataIntegrity 분기 | `ORA-00001` (unique constraint) → `DUPLICATE_RESOURCE` 응답 확인 |

**MySQL 특화:**

| 항목 | 테스트 포인트 |
|------|-------------|
| Batch Insert | `databaseId="mysql"` SQL에서 표준 `VALUES (...), (...)` 정상 동작 |
| Pagination | PageHelper `autoDialect`에 의한 LIMIT/OFFSET 기반 페이징 정확성 |
| AUTO_INCREMENT | PK 자동 채번 정합성 |
| 문자셋 | `utf8mb4` 환경에서 이모지 등 4바이트 문자 저장 확인 |
| DataIntegrity 분기 | `Duplicate entry` → `DUPLICATE_RESOURCE` 응답 확인 |

**공통 (양쪽 DB에서 모두 검증):**

| 항목 | 테스트 포인트 |
|------|-------------|
| Composite Key | 복합 PK 테이블의 생성/수정/삭제 정합성 |
| 문자열 길이 | 최대 길이 초과 데이터 truncation 처리 |
| 한글 바이트 | 한글 + 영문 + 특수문자 혼합 입력 시 저장 정확성 |
| `<foreach>` 빈 리스트 | 빈 리스트 전달 시 SQL 문법 오류 없음 확인 |

> **한글 바이트 이슈 (Oracle AL32UTF8):** 한글 1글자 = 3바이트. `VARCHAR2(100 BYTE)` 컬럼에 한글 34자(= 102바이트) 입력 시 `ORA-12899` 발생. 애플리케이션 레벨에서 바이트 단위 검증을 수행하거나 DDL을 `CHAR` 시맨틱으로 통일한다.

### 7.4 트랜잭션 테스트

| 항목 | 테스트 포인트 |
|------|-------------|
| 읽기 전용 | `@Transactional(readOnly = true)` 서비스에서 쓰기 시도 → 예외 |
| 롤백 | 중간 실패 시 전체 롤백 확인 (특히 batch insert) |
| 동시성 | 같은 레코드 동시 수정 → 낙관적 잠금 예외 발생 |

---

## 8. 비동기/큐 테스트 전략

> 비동기 예외 처리 패턴(`@Async`, `@Scheduled`)은 Exception Handling 7절 참고.

비동기 큐(BlockingQueue 등)를 사용하는 기능에 대한 테스트 표준:

| 테스트 | 방법 | 검증 포인트 |
|--------|------|------------|
| 정상 저장 | API 호출 후 저장 테이블 확인 | 큐 → DB 데이터 정합성 |
| 큐 용량 초과 | 설정 용량 이상 동시 요청 | 초과 건 유실 + 경고 로그 발생 여부 |
| 데이터 절단 | DB 컬럼 최대 길이 초과 데이터 | 인코딩 인식 truncation 정상 동작 |
| 종료 처리 | 애플리케이션 종료 중 큐 잔여 건 | Graceful Shutdown 시간 내 처리 완료 |

---

## 9. E2E 시나리오 설계 가이드

> Postman 컬렉션 구성(Setup → Create → Read → Update → Delete → Cleanup)은 Postman 문서 참고.

### 9.1 시나리오 유형

E2E 테스트는 다음 4가지 유형의 핵심 업무 흐름을 대상으로 한다:

| 유형 | 설명 | 예시 |
|------|------|------|
| **마스터 등록 → 배포** | 마스터 데이터 생성 후 운영 반영 | 설정 등록 → 인스턴스 반영 → 결과 확인 |
| **사용자 관리 → 권한 검증** | 사용자/역할 설정 후 접근 제어 확인 | 사용자 생성 → 권한 할당 → 접근 성공/실패 |
| **실행 → 이력 → 정리** | 작업 실행 후 이력 확인 및 정리 | 배치 실행 → 이력 조회 → 클린업 |
| **상태 전환 → 접근 제어** | 상태 변경 후 접근 정책 검증 | 기능 중지 → 제한 접근 → 기능 재개 |

### 9.2 시나리오 작성 원칙

```
1. 로그인으로 시작
2. 핵심 업무 흐름을 순서대로 수행 (3~8단계)
3. 각 단계에서 응답 코드 및 ApiResponse 구조 검증
4. 권한별 접근 성공/실패 검증 포함
5. 로그아웃 또는 정리 작업으로 종료
```

---

## 10. 테스트 데이터 기준

### 10.1 데이터량 가이드

| 테이블 유형 | 최소 데이터량 | 용도 |
|-------------|-------------|------|
| 이력/로그 (대용량) | 100만 건 | 대용량 페이징 성능 검증 |
| 이력/로그 (중간) | 10~50만 건 | 조회/집계 성능 검증 |
| 마스터 데이터 | 500~1,000건 | 목록 조회, 검색 필터 |
| 사용자/권한 | 100건 | 인증/인가 테스트 |

### 10.2 테스트 계정 가이드

| 역할 | 수량 | 용도 |
|------|------|------|
| 관리자 (전체 권한) | 5개 | 관리자 시나리오 |
| 일반 사용자 (제한 권한) | 30개 | 권한 테스트, 동시 접속 |
| 읽기 전용 사용자 | 10개 | READ 권한 테스트 |
| 권한 미할당 사용자 | 5개 | 인가 실패 테스트 |

---

## 11. 체크리스트

```
□ 1. CRUD 기본 동작 (생성 201/조회 200/수정 200/삭제 204)

□ 2. 페이징 정상 동작 (page, size, sortBy, sortDirection)
      → PageHelper + PageResponse 구조 준수

□ 3. @Valid 유효성 검증
      → 400 + INVALID_INPUT + error.fields[]

□ 4. 존재하지 않는 리소스
      → 404 + RESOURCE_NOT_FOUND

□ 5. 중복 리소스
      → 409 + DUPLICATE_RESOURCE

□ 6. 미인증 접근
      → 401 또는 로그인 리다이렉트

□ 7. 미인가 접근 (READ/WRITE)
      → 403 + FORBIDDEN
      → @PreAuthorize 어노테이션 설정 확인

□ 8. 수평 권한 상승 (타인 리소스 접근)
      → 403 또는 데이터 필터링

□ 9. ApiResponse 구조 준수
      → success, data/error 필드 존재
      → error.traceId 존재

□ 10. 에러 응답 보안
       → stack trace, DB 오류, 내부 경로 미포함

□ 11. MyBatis 동적 조건 NULL 시 안전성
       → WHERE 절 누락으로 전체 행 변경 방지

□ 12. Multi-DB 동작 확인
       → Oracle + MySQL 양쪽에서 동일 테스트 통과

□ 13. 한글 입력 바이트 길이 검증
       → Oracle BYTE 시맨틱, MySQL utf8mb4

□ 14. 감사 로그 기록 확인
       → 누가, 언제, 무엇을 변경했는지

□ 15. Soft Delete 시 조회 API에서 삭제 데이터 노출 여부 확인
```

---

*Last updated: 2026-02-24*
