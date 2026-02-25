# Test Strategy

## 1. 개요

테스트 계층별 커버리지 목표, 도구, 검증 항목을 정의한다.

| 포함 | 제외 |
|------|------|
| 계층별 테스트 전략 및 커버리지 목표 | CI 파이프라인 구성 (CI Strategy 문서) |
| CI 커버리지 임계값 | Postman 컬렉션 작성법 (Postman 문서) |

---

## 2. 테스트 계층 및 커버리지 목표

### 2.1 계층별 전략

| 계층 | 커버리지 목표 | 도구 | 핵심 |
|------|-------------|------|------|
| **Security / Logging / Util** | 100% | @SpringBootTest, @WithMockUser, JUnit5, Mockito, AssertJ | 공통 모듈 안정성 확보 |
| **Mapper** | 100% | Testcontainers, @SpringBootTest, AssertJ | DB 정합성 확보 (통합 테스트 중심) |
| **Controller** | 70% | @WebMvcTest, MockMvc, Mockito-BDD, @WithMockUser, AssertJ | HTTP 계약과 보안 정책 검증 |
| **Service** | 50% | JUnit5, Mockito, AssertJ | 실제 비즈니스 규칙 검증 |
| **E2E** | — | Playwright, Postman | 핵심 사용자 흐름 정상 동작 확인 |

### 2.2 계층별 검증 범위

**[1] Security / Logging / Util — 100%**

- 인증·인가 및 메서드 보안 검증
- SecurityFilterChain 통합 테스트
- 로깅 설정별 출력 대상/레벨 검증
- Configuration 바인딩 검증
- 공통 Util 단위 테스트 전수

**[2] Mapper — 100%**

- Vendor별 SQL 동작 검증 (Oracle + MySQL)
- CRUD, 동적 SQL, 페이징 전수 테스트
- 실제 DB 기반 통합 테스트만 수행 — **Mock 금지**
- DB 초기화: Testcontainers 기반 개별 컨테이너 사용

**[3] Controller — 70%**

- HTTP 계약(status, header, body) 검증
- Validation 및 에러 응답 구조 확인
- 권한별 접근 제어(401/403) 검증
- Service 호출 여부 및 파라미터 검증

**[4] Service — 50%**

- 단순 Mapper 위임 로직 제외
- 생성/수정/삭제 등 실질적 비즈니스 로직 중심 테스트
- 핵심 규칙 및 상태 전이 검증

**[5] E2E**

- 사용자 핵심 시나리오 기반 스모크 수준 검증
- 배포 후 주요 흐름 정상 동작 확인 목적

---

## 3. CI 커버리지 정책

CI에서 커버리지 미달 시 빌드를 실패시킨다.

| 대상 | 최소 커버리지 | 비고 |
|------|-------------|------|
| Security | 100% | 보안 로직은 전수 테스트 |
| Util | 100% | 재사용성 높아 전수 테스트 |
| Controller | 60% | HTTP 계약 중심 (여유 기준) |
| Service | 35% | 비즈니스 로직만 최소 확인 |
| **전체 프로젝트** | **55%** | 전사 품질 신호용 기준 |

---

## 4. 검증 항목

### 4.1 Controller 테스트

**CRUD 공통 패턴:**

| 테스트 | HTTP | 검증 포인트 |
|--------|------|------------|
| 목록 조회 | `GET /api/{resource}` | 페이징 파라미터, 정렬, 검색 필터 |
| 단건 조회 | `GET /api/{resource}/{id}` | 존재 → 200, 미존재 → 404 |
| 생성 | `POST /api/{resource}` | @Valid → 400, 중복 → 409, 성공 → 201 |
| 수정 | `PUT /api/{resource}/{id}` | 존재 확인, 변경 값 반영 → 200 |
| 삭제 | `DELETE /api/{resource}/{id}` | 존재 확인, 참조 무결성 → 204 |

**응답 구조 검증:**

| 케이스 | 검증 항목 |
|--------|----------|
| 성공 | `success: true`, `data` 존재 |
| 에러 | `success: false`, `error.code`, `error.message`, `error.traceId` 존재 |
| 검증 에러 | `error.code: "INVALID_INPUT"`, `error.fields[]` 배열 존재 |

**ErrorType별 검증:**

| ErrorType | 트리거 | 기대 응답 |
|-----------|--------|----------|
| `RESOURCE_NOT_FOUND` | 존재하지 않는 ID | 404 |
| `DUPLICATE_RESOURCE` | 중복 키 생성 | 409 |
| `INVALID_INPUT` | `@Valid` 실패 | 400 + `fields[]` |
| `BUSINESS_RULE_VIOLATED` | 상태 전이 위반 | 409/422 |
| `FORBIDDEN` | 권한 없음 | 403 |
| `EXTERNAL_SERVICE_ERROR` | 외부 API 실패 | 502 |

**응답 보안:** stack trace, DB 오류 메시지, 내부 파일 경로, `detail` 파라미터가 클라이언트에 미노출.

### 4.2 Security 테스트

**인증:**

| 테스트 | 검증 포인트 |
|--------|------------|
| 로그인 성공 | 세션 생성, 리다이렉트 |
| 로그인 실패 | 에러 메시지, 계정 잠금 |
| 미인증 접근 | 401 또는 로그인 리다이렉트 |
| 세션 만료 | 401 |

**인가:**

| 테스트 | 검증 포인트 |
|--------|------------|
| READ 권한으로 GET | 200 |
| READ 권한으로 POST/PUT/DELETE | 403 |
| WRITE 권한으로 전체 | 성공 |
| 권한 미할당 | 403 |

**수평 권한 상승:** URL의 `{id}`를 타인 리소스로 변경하여 조회/수정/삭제 시도 → 403 또는 데이터 필터링 확인.

**보안 취약점:**

| 항목 | 테스트 방법 |
|------|------------|
| SQL Injection | `' OR 1=1 --` 주입 |
| XSS | `<script>alert(1)</script>` 주입 |
| CSRF | CSRF 토큰 없이 요청 |
| 경로 탐색 | `../../etc/passwd` 주입 |

### 4.3 Mapper 테스트

**동적 SQL 무결성:**

| 테스트 | 검증 포인트 |
|--------|------------|
| 검색 조건 전체 NULL | 의도치 않은 전체 조회 방지 |
| UPDATE/DELETE 조건 NULL | WHERE 절 누락으로 전체 행 변경 방지 |
| `<foreach>` 빈 리스트 | SQL 문법 오류 없음 |
| `<choose>` 기본값 | `<otherwise>` 동작 확인 |

**DB별 특화 테스트:**

| 항목 | Oracle | MySQL |
|------|--------|-------|
| Batch Insert | DUAL + UNION ALL 정상 동작 | `VALUES (...), (...)` 정상 동작 |
| 페이징 | ROWNUM 기반 정확성 | LIMIT/OFFSET 기반 정확성 |
| PK 생성 | 시퀀스 채번 정합성 | AUTO_INCREMENT 정합성 |
| 한글 바이트 | VARCHAR2(N BYTE) — ORA-12899 | utf8mb4 — 4바이트 문자 |
| DataIntegrity | ORA-00001 → DUPLICATE_RESOURCE | Duplicate entry → DUPLICATE_RESOURCE |

**트랜잭션:**

| 항목 | 검증 포인트 |
|------|-------------|
| 읽기 전용 | `readOnly = true`에서 쓰기 → 예외 |
| 롤백 | 중간 실패 시 전체 롤백 (특히 batch insert) |

### 4.4 E2E 시나리오

| 유형 | 예시 |
|------|------|
| 사용자 관리 → 권한 검증 | 사용자 생성 → 권한 할당 → 접근 성공/실패 |
| 마스터 등록 → 배포 | 설정 등록 → 인스턴스 반영 → 결과 확인 |
| 실행 → 이력 → 정리 | 배치 실행 → 이력 조회 → 클린업 |
| 상태 전환 → 접근 제어 | 기능 중지 → 제한 접근 → 기능 재개 |

---

## 5. 테스트 데이터 기준

| 테이블 유형 | 최소 데이터량 | 용도 |
|-------------|-------------|------|
| 이력/로그 (대용량) | 100만 건 | 페이징 성능 검증 |
| 이력/로그 (중간) | 10~50만 건 | 조회/집계 성능 검증 |
| 마스터 데이터 | 500~1,000건 | 목록 조회, 검색 필터 |
| 사용자/권한 | 100건 | 인증/인가 테스트 |

---

## 6. 체크리스트

```
□ 1. CRUD 기본 동작 (201/200/200/204)

□ 2. 페이징·정렬·검색 필터 정상 동작

□ 3. @Valid 유효성 검증 → 400 + INVALID_INPUT + fields[]

□ 4. 미존재 리소스 → 404, 중복 리소스 → 409

□ 5. 미인증 → 401, 미인가 → 403

□ 6. 수평 권한 상승 차단 확인

□ 7. ApiResponse 구조 준수 (success, data/error, traceId)

□ 8. 에러 응답에 stack trace·DB 오류·내부 경로 미포함

□ 9. 동적 SQL 조건 NULL 시 전체 행 변경 방지

□ 10. Oracle + MySQL 양쪽 테스트 통과

□ 11. 한글 바이트 길이 검증

□ 12. CI 커버리지 임계값 충족
```

---

*Last updated: 2026-02-26*
