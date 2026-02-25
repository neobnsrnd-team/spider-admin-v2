# Postman

## 1. 개요

Postman 컬렉션 작성 표준을 정의한다. 컬렉션은 두 가지 용도로 사용된다.

| 용도 | 설명 |
|------|------|
| 개발자 인터랙티브 테스트 | Postman GUI에서 API를 수동 호출·검증 |
| CI 자동 회귀 테스트 | Newman CLI로 PR마다 자동 실행 |

> Newman CI 실행 구성은 CI Strategy 5.3절 참고. 이 문서는 **컬렉션 작성** 표준에 집중한다.

---

## 2. 디렉터리 구조

### 2.1 컬렉션 파일 구조

```
postman/
├── {domain}/
│   └── {domain}.postman_collection.json
├── environments/
│   ├── local-oracle.postman_environment.json
│   ├── local-mysql.postman_environment.json
│   └── ci.postman_environment.json
└── README.md                                    # (선택) 로컬 실행 안내
```

도메인 디렉터리명은 Java 패키지명과 일치시킨다.

```
postman/user/user.postman_collection.json     → domain.user
postman/role/role.postman_collection.json     → domain.role
```

### 2.2 환경 파일

| 환경 | 파일 | 용도 |
|------|------|------|
| `local-oracle` | 로컬 Oracle 개발 환경 | 개발자 수동 테스트 |
| `local-mysql` | 로컬 MySQL 개발 환경 | 개발자 수동 테스트 |
| `ci` | CI 파이프라인 환경 | Newman 자동 실행 |

**표준 변수:**

| 변수 | 설명 | 예시 |
|------|------|------|
| `baseUrl` | 서버 주소 | `http://localhost:8080` |
| `loginId` | 테스트 계정 ID | `admin` |
| `loginPassword` | 테스트 계정 비밀번호 | (환경별 설정) |

---

## 3. 컬렉션 설계

### 3.1 네이밍 규칙

| 대상 | 패턴 | 예시 |
|------|------|------|
| 컬렉션 | `{Domain} API Tests` | `User API Tests` |
| 폴더 | 실행 순서 기반 | `Setup`, `Create`, `Read`, `Update`, `Delete`, `Cleanup` |

### 3.2 폴더 구조

Newman은 컬렉션 내 요청을 **순차적으로** 실행한다. 폴더 순서가 곧 실행 순서다.

```
User API Tests/
├── Setup/              # 인증, 선행 데이터 생성
├── Create/             # POST — 정상 + 에러 케이스
├── Read/               # GET — 목록, 상세, 검색
├── Update/             # PUT — 정상 + 에러 케이스
├── Delete/             # DELETE — 정상 + 에러 케이스
└── Cleanup/            # 테스트 데이터 정리
```

### 3.3 요청 네이밍

패턴: **`{HTTP method} {설명} - {예상 상태 코드}`**

```
POST Create User - 201
POST Create Duplicate User - 409
GET  Get User List - 200
GET  Get User Detail - 200
GET  Get Non-existent User - 404
PUT  Update User - 200
DELETE Delete User - 204
```

---

## 4. 인증 처리

### 4.1 CSRF + Session 인증 흐름

> Security 3.3절 (CSRF), Auth & Auth 4절 (인증 흐름) 참고.

Spring Security Form Login 환경에서 Postman 인증 흐름:

```
1. GET  /login           → CSRF 토큰 추출 (HTML meta 또는 쿠키)
2. POST /login           → userId + password + CSRF → JSESSIONID 쿠키 획득
3. 후속 요청             → JSESSIONID + CSRF 토큰 자동 전달
```

### 4.2 Pre-request Script

컬렉션 레벨에 인증 스크립트를 설정하여 모든 요청에 자동 적용한다.

```javascript
// Collection Pre-request Script
const loginUrl = pm.environment.get("baseUrl") + "/login";
const loginId = pm.environment.get("loginId");
const loginPassword = pm.environment.get("loginPassword");

// 세션이 없거나 만료된 경우에만 로그인
if (!pm.cookies.has("JSESSIONID")) {
    // 1단계: CSRF 토큰 획득
    pm.sendRequest(loginUrl, (err, res) => {
        const csrfToken = res.headers.get("X-CSRF-TOKEN")
            || extractCsrfFromCookie(res);

        // 2단계: 로그인
        pm.sendRequest({
            url: loginUrl,
            method: "POST",
            header: { "Content-Type": "application/x-www-form-urlencoded" },
            body: {
                mode: "urlencoded",
                urlencoded: [
                    { key: "userId", value: loginId },
                    { key: "password", value: loginPassword },
                    { key: "_csrf", value: csrfToken }
                ]
            }
        });
    });
}
```

---

## 5. 테스트 스크립트

### 5.1 기본 검증 패턴

**모든 요청**에 상태 코드와 응답 시간 검증을 포함한다.

```javascript
// 상태 코드 검증
pm.test("Status code is 200", () => {
    pm.response.to.have.status(200);
});

// 응답 시간 검증
pm.test("Response time < 3000ms", () => {
    pm.expect(pm.response.responseTime).to.be.below(3000);
});
```

### 5.2 응답 구조 검증

> ApiResponse JSON 형식은 API Design 4.3절 참고.

```javascript
// 성공 응답 구조 검증
pm.test("Response has ApiResponse structure", () => {
    const json = pm.response.json();
    pm.expect(json).to.have.property("success", true);
    pm.expect(json).to.have.property("data");
});

// 에러 응답 구조 검증
pm.test("Error response structure", () => {
    const json = pm.response.json();
    pm.expect(json.success).to.be.false;
    pm.expect(json.error).to.have.property("code");
    pm.expect(json.error).to.have.property("message");
    pm.expect(json.error).to.have.property("traceId");
});

// 검증 에러 필드 검증
pm.test("Validation error has fields", () => {
    const json = pm.response.json();
    pm.expect(json.error.code).to.eql("INVALID_INPUT");
    pm.expect(json.error.fields).to.be.an("array").that.is.not.empty;
});
```

### 5.3 변수 체이닝

생성 응답에서 ID를 추출하여 후속 요청에서 사용한다.

```javascript
// Create 응답에서 ID 추출
pm.test("Extract created user ID", () => {
    const json = pm.response.json();
    pm.collectionVariables.set("createdUserId", json.data.userId);
});

// 후속 요청 URL에서 사용
// GET {{baseUrl}}/api/users/{{createdUserId}}

// Cleanup에서 변수 정리
pm.collectionVariables.unset("createdUserId");
```

---

## 6. 변수 관리

### 6.1 환경 변수

환경에 따라 달라지는 값은 환경 변수로 관리한다.

| 변수 | 타입 | 설명 |
|------|------|------|
| `baseUrl` | default | 서버 주소 |
| `loginId` | default | 테스트 계정 ID |
| `loginPassword` | **secret** | 테스트 계정 비밀번호 |

> `secret` 타입은 Postman UI에서 마스킹되며, 내보내기 시 값이 포함되지 않는다.

### 6.2 컬렉션 변수

테스트 간 데이터 전달용 일시 변수는 컬렉션 변수를 사용한다.

| 네이밍 | 예시 | 설명 |
|--------|------|------|
| `{entity}Id` | `createdUserId` | 생성된 리소스 ID |
| `{entity}Name` | `createdUserName` | 생성된 리소스 이름 |

Cleanup 폴더에서 사용한 컬렉션 변수를 `unset`한다.

---

## 7. 금지 사항

| 금지 항목 | 이유 |
|-----------|------|
| 하드코딩된 인증 정보 | 환경 변수로 관리. Git에 비밀번호 커밋 금지 |
| 상태 코드 검증 누락 | 모든 요청에 `pm.response.to.have.status()` 필수 |
| 빈 `pm.test()` 본문 | 검증 없는 테스트는 CI에서 의미 없음 |
| Postman 클라우드 동기화 | 컬렉션은 Git으로만 버전 관리 |
| SSL 검증 비활성화 | `pm.sendRequest`에서 `rejectUnauthorized: false` 금지 |

---

## 8. 체크리스트

```
□ 1. 컬렉션 파일이 postman/{domain}/ 디렉터리에 위치하는가

□ 2. 폴더 순서: Setup → Create → Read → Update → Delete → Cleanup

□ 3. 요청 이름이 "{HTTP method} {설명} - {상태 코드}" 패턴인가

□ 4. 모든 요청에 상태 코드 + 응답 시간 검증이 있는가

□ 5. 성공 응답에서 ApiResponse 구조(success, data)를 검증하는가

□ 6. 에러 케이스에서 error.code, error.message를 검증하는가

□ 7. 변수 체이닝: 생성 ID → 후속 요청에서 사용 → Cleanup에서 unset

□ 8. 환경 변수에 비밀번호를 secret 타입으로 설정했는가

□ 9. 컬렉션을 Newman으로 로컬 실행하여 전체 통과를 확인했는가
```

---

*Last updated: 2026-02-23*
