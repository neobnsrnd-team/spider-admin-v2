# Postman Test Criteria

## 1. 개요

본 문서는 AS-IS 소스코드 분석을 기반으로 도메인별 API 엔드포인트와 테스트 시나리오를 정의한다.
개발자와 AI가 이 문서만 보고 Postman 테스트 컬렉션을 작성할 수 있는 **검증 기준서(WHAT)** 역할을 한다.

| 문서 | 역할 | 위치 |
|------|------|------|
| `Postman.md` | 컬렉션 작성 규칙 (HOW) | `Development Standards/Postman.md` |
| **본 문서** | **도메인별 검증 기준 (WHAT)** | `Postman Test Criteria.md` |

---

### 1.1 문서 구조

도메인별로 다음 3가지를 정의한다:

1. **엔드포인트 테이블** — HTTP 메서드, 경로, 예상 상태 코드, 권한 수준
2. **테스트 시나리오** — Setup → Create → Read → Update → Delete → Cleanup 순서
3. **에러 검증 기준** — 도메인별 ErrorCode와 검증 포인트

### 1.2 공통 검증 규칙

모든 요청에 적용되는 공통 검증 사항이다.

| 검증 항목 | 기준 |
|-----------|------|
| 상태 코드 | 모든 요청에 `pm.response.to.have.status()` 포함 |
| 응답 시간 | `pm.response.responseTime < 3000ms` |
| 성공 응답 구조 | `{ success: true, data: ... }` |
| 에러 응답 구조 | `{ success: false, error: { code, message, traceId } }` |
| Validation 에러 | `error.code = "INVALID_INPUT"`, `error.fields[]` 존재 |
| 인증 | 모든 API는 JSESSIONID + CSRF 토큰 필요 (Collection Pre-request Script) |

### 1.3 컬렉션-시나리오 매핑 원칙

| 원칙 | 설명 |
|------|------|
| 1 컬렉션 = 1 도메인 | `postman/user/user.postman_collection.json` |
| 실행 순서 = 폴더 순서 | Setup → Create → Read → Update → Delete → Cleanup |
| 변수 체이닝 | Create에서 ID 추출 → Read/Update/Delete에서 사용 → Cleanup에서 unset |
| 에러 케이스 포함 | 각 CRUD 폴더에 정상 + 에러 케이스 병행 |

### 1.4 에러 코드 범위 규칙

| 코드 범위 | HTTP 상태 | 의미 |
|-----------|-----------|------|
| `{DOMAIN}_001` ~ `099` | 404 Not Found | 리소스 미존재 |
| `{DOMAIN}_101` ~ `199` | 409 Conflict | 중복 리소스 |
| `{DOMAIN}_201` ~ `299` | 400 Bad Request | 입력값 오류 |
| `{DOMAIN}_801` ~ `899` | 403 Forbidden | 접근 권한 없음 |
| `{DOMAIN}_901` ~ `999` | 500 Internal Error | 서버 오류 |

---

## 2. 인증 (Auth)

### 2.1 엔드포인트

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| POST | `/api/auth/login` | 로그인 | 200 |
| POST | `/api/auth/logout` | 로그아웃 | 200 |
| POST | `/api/auth/validate` | 인증 검증 | 200 |
| GET | `/api/auth/permission/menu` | 메뉴 권한 확인 | 200 |

> Auth는 별도 컬렉션 대신 각 도메인 컬렉션의 **Setup** 폴더에서 처리한다.

### 2.2 테스트 시나리오

| 순서 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| 1 | `POST Login - 200` | JSESSIONID 쿠키 설정, CSRF 토큰 획득 |
| 2 | `POST Login Invalid Password - 401` | `success: false` |
| 3 | `POST Validate Credentials - 200` | 인증 유효 확인 |
| 4 | `GET Check Menu Permission - 200` | 권한 조회 결과 확인 |
| 5 | `POST Logout - 200` | 세션 만료 확인 |

---

## 3. 사용자 관리 (User)

### 3.1 엔드포인트

**UserController** — `/api/users` | `@RequireMenuAccess(menuId="USER")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/users` | 전체 사용자 목록 | 200 | READ |
| GET | `/api/users/page` | 페이징 사용자 목록 | 200 | READ |
| GET | `/api/users/{userId}` | 사용자 상세 | 200 | READ |
| GET | `/api/users/check/username` | 사용자명 중복 확인 | 200 | READ |
| GET | `/api/users/check/email` | 이메일 중복 확인 | 200 | READ |
| POST | `/api/users` | 사용자 생성 | 201 | WRITE |
| PUT | `/api/users/{userId}` | 사용자 수정 | 200 | WRITE |
| PUT | `/api/users/{userId}/error-count/reset` | 로그인 오류 횟수 초기화 | 200 | WRITE |
| DELETE | `/api/users/{userId}` | 사용자 삭제 | 200 | WRITE |

**UserMenuController** — `/api/user-menus`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/user-menus/user/{userId}` | 사용자 메뉴 권한 조회 | 200 | READ |
| POST | `/api/user-menus` | 사용자 메뉴 권한 생성 | 201 | WRITE |
| PUT | `/api/user-menus/{userId}/batch` | 사용자 메뉴 권한 일괄 저장 | 200 | WRITE |
| DELETE | `/api/user-menus/{userId}` | 사용자 메뉴 권한 전체 삭제 | 200 | WRITE |

**ProfileController** — `/api/profile` | `@RequireMenuAccess(menuId="PROFILE")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/profile` | 내 프로필 조회 | 200 | READ |
| PUT | `/api/profile` | 내 프로필 수정 | 200 | WRITE |

### 3.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| USER_001 | 404 | 사용자를 찾을 수 없습니다 |
| USER_002 | 404 | 역할을 찾을 수 없습니다 |
| USER_101 | 409 | 이미 존재하는 사용자명입니다 |
| USER_102 | 409 | 이미 존재하는 이메일입니다 |
| USER_201 | 400 | 유효하지 않은 사용자 상태입니다 |
| USER_202 | 400 | 새 비밀번호와 비밀번호 확인이 일치하지 않습니다 |
| USER_203 | 400 | 비밀번호 형식 오류 (8~50자, 영문+숫자+특수문자) |

### 3.3 주요 Validation 규칙

| 필드 | 규칙 |
|------|------|
| `userName` | `@NotBlank`, `@Size(max=50)` |
| `email` | `@Email`, `@Size(max=100)` |
| `password` | `@Size(max=50)`, 서비스에서 형식 검증 |
| `authCode` (UserMenu) | `@Pattern(regexp="^[RrWw]$")` |

### 3.4 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create User - 201` | `data.userId` 존재, 변수 저장 |
| | `POST Create User Missing UserName - 400` | `error.fields[].field = "userName"` |
| | `POST Create User Invalid Email - 400` | `error.fields[].field = "email"` |
| | `POST Create Duplicate UserName - 409` | `error.code = "USER_101"` |
| | `POST Create Duplicate Email - 409` | `error.code = "USER_102"` |
| **Read** | `GET Get User List - 200` | `data` 배열 존재 |
| | `GET Get User Page - 200` | `data.content`, `data.totalElements` 존재 |
| | `GET Get User Detail - 200` | `data.userId = {{createdUserId}}` |
| | `GET Get Non-existent User - 404` | `error.code = "USER_001"` |
| | `GET Check Username Exists - 200` | `data = true` |
| | `GET Check Email Exists - 200` | `data = true` |
| **Update** | `PUT Update User - 200` | 수정된 필드 값 확인 |
| | `PUT Update Non-existent User - 404` | `error.code = "USER_001"` |
| | `PUT Reset Login Error Count - 200` | 오류 횟수 초기화 확인 |
| **Delete** | `DELETE Delete User - 200` | 삭제 성공 |
| | `DELETE Delete Non-existent User - 404` | `error.code = "USER_001"` |
| **Cleanup** | Cleanup Variables | `createdUserId` unset |

---

## 4. 메뉴 관리 (Menu)

### 4.1 엔드포인트

**MenuController** — `/api/menus`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/menus` | 전체 메뉴 목록 | 200 |
| GET | `/api/menus/active` | 활성 메뉴 목록 | 200 |
| GET | `/api/menus/tree` | 메뉴 트리 구조 | 200 |
| GET | `/api/menus/root` | 루트 메뉴 목록 | 200 |
| GET | `/api/menus/children/{priorMenuId}` | 하위 메뉴 목록 | 200 |
| GET | `/api/menus/page` | 페이징 메뉴 목록 | 200 |
| GET | `/api/menus/{menuId}` | 메뉴 상세 | 200 |
| GET | `/api/menus/{menuId}/with-children` | 메뉴 + 하위 포함 | 200 |
| GET | `/api/menus/selectable-parents` | 선택 가능 상위 메뉴 | 200 |
| GET | `/api/menus/check/name` | 메뉴명 중복 확인 | 200 |
| POST | `/api/menus` | 메뉴 생성 | 201 |
| PUT | `/api/menus/{menuId}` | 메뉴 수정 | 200 |
| DELETE | `/api/menus/{menuId}` | 메뉴 삭제 | 200 |

**MenuApiController** — `/api/user-menus`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/user-menus` | 현재 사용자 메뉴 (계층) | 200 |
| GET | `/api/user-menus/by-role/{roleId}` | 역할별 메뉴 조회 | 200 |

### 4.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| MENU_001 | 404 | 메뉴를 찾을 수 없습니다 |

### 4.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Menu - 201` | `data.menuId` 존재, 변수 저장 |
| | `POST Create Menu Missing Fields - 400` | Validation 에러 |
| **Read** | `GET Get All Menus - 200` | `data` 배열 존재 |
| | `GET Get Menu Tree - 200` | 트리 구조 검증 (children 필드) |
| | `GET Get Menu Detail - 200` | `data.menuId = {{createdMenuId}}` |
| | `GET Get Non-existent Menu - 404` | `error.code = "MENU_001"` |
| | `GET Check Menu Name Exists - 200` | `data = true` |
| | `GET Get Selectable Parents - 200` | 배열 반환 확인 |
| **Update** | `PUT Update Menu - 200` | 수정된 필드 확인 |
| **Delete** | `DELETE Delete Menu - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdMenuId` unset |

---

## 5. 역할 관리 (Role)

### 5.1 엔드포인트

**RoleController** — `/api/roles` | `@RequireMenuAccess(menuId="ROLE")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/roles` | 전체 역할 목록 | 200 | READ |
| GET | `/api/roles/active` | 활성 역할 목록 | 200 | READ |
| GET | `/api/roles/page` | 페이징 역할 목록 | 200 | READ |
| GET | `/api/roles/{roleId}` | 역할 상세 | 200 | READ |
| GET | `/api/roles/check/name` | 역할명 중복 확인 | 200 | READ |
| POST | `/api/roles` | 역할 생성 | 201 | WRITE |
| POST | `/api/roles/batch` | 역할 일괄 처리 | 200 | WRITE |
| PUT | `/api/roles/{roleId}` | 역할 수정 | 200 | WRITE |
| DELETE | `/api/roles/{roleId}` | 역할 삭제 | 200 | WRITE |

**RoleMenuController** — `/api/role-menus` | `@RequireMenuAccess(menuId="ROLE")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/role-menus/{roleId}` | 역할별 메뉴 권한 조회 | 200 | READ |
| POST | `/api/role-menus/{roleId}` | 역할별 메뉴 권한 수정 | 200 | WRITE |

### 5.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| ROLE_001 | 404 | 역할을 찾을 수 없습니다 |
| ROLE_101 | 409 | 이미 존재하는 역할명입니다 |
| ROLE_201 | 400 | 해당 역할을 사용하는 사용자가 존재합니다 |

### 5.3 주요 Validation 규칙

| 필드 | 규칙 |
|------|------|
| `roleId` | `@NotBlank`, `@Size(max=10)` |
| `roleName` | `@NotBlank`, `@Size(max=50)` |
| `menuPermissions` (RoleMenu) | `@NotNull`, 각 항목에 `menuId @NotBlank` |

### 5.4 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Role - 201` | `data.roleId` 존재, 변수 저장 |
| | `POST Create Role Missing Name - 400` | `error.fields[].field = "roleName"` |
| | `POST Create Duplicate Role Name - 409` | `error.code = "ROLE_101"` |
| **Read** | `GET Get All Roles - 200` | 배열 반환 |
| | `GET Get Role Page - 200` | 페이징 구조 확인 |
| | `GET Get Role Detail - 200` | `data.roleId = {{createdRoleId}}` |
| | `GET Get Non-existent Role - 404` | `error.code = "ROLE_001"` |
| | `GET Get Role Menu Permissions - 200` | 메뉴 권한 목록 반환 |
| **Update** | `PUT Update Role - 200` | 수정된 필드 확인 |
| | `POST Update Role Menu Permissions - 200` | 권한 변경 확인 |
| **Delete** | `DELETE Delete Role In Use - 400` | `error.code = "ROLE_201"` (사용자 존재 시) |
| | `DELETE Delete Role - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdRoleId` unset |

---

## 6. 코드 관리 (Code)

### 6.1 엔드포인트

**CodeGroupController** — `/api/code-groups`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/code-groups` | 전체 코드그룹 목록 | 200 |
| GET | `/api/code-groups/with-codes` | 코드 포함 전체 목록 | 200 |
| GET | `/api/code-groups/page` | 페이징 코드그룹 목록 | 200 |
| GET | `/api/code-groups/{codeGroupId}` | 코드그룹 상세 | 200 |
| GET | `/api/code-groups/{codeGroupId}/with-codes` | 코드 포함 상세 | 200 |
| GET | `/api/code-groups/search` | 코드그룹 검색 | 200 |
| GET | `/api/code-groups/check/name` | 코드그룹명 중복 확인 | 200 |
| GET | `/api/code-groups/count` | 코드그룹 총 건수 | 200 |
| POST | `/api/code-groups` | 코드그룹 생성 | 201 |
| POST | `/api/code-groups/with-codes` | 코드 포함 코드그룹 생성 | 201 |
| PUT | `/api/code-groups/{codeGroupId}` | 코드그룹 수정 | 200 |
| PUT | `/api/code-groups/{codeGroupId}/with-codes` | 코드 포함 코드그룹 수정 | 200 |
| DELETE | `/api/code-groups/{codeGroupId}` | 코드그룹 삭제 | 200 |
| DELETE | `/api/code-groups/{codeGroupId}/with-codes` | 코드 포함 코드그룹 삭제 | 200 |

**CodeController** — `/api/codes`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/codes` | 전체 코드 목록 | 200 |
| GET | `/api/codes/page` | 페이징 코드 목록 | 200 |
| GET | `/api/codes/{codeGroupId}/{code}` | 코드 상세 | 200 |
| GET | `/api/codes/by-group/{codeGroupId}` | 그룹별 코드 목록 | 200 |
| GET | `/api/codes/by-group/{codeGroupId}/active` | 그룹별 활성 코드 | 200 |
| GET | `/api/codes/search` | 코드 검색 | 200 |
| GET | `/api/codes/check/exists` | 코드 존재 확인 | 200 |
| GET | `/api/codes/count/{codeGroupId}` | 그룹별 코드 건수 | 200 |
| POST | `/api/codes` | 코드 생성 | 201 |
| PUT | `/api/codes/{codeGroupId}/{code}` | 코드 수정 | 200 |
| DELETE | `/api/codes/{codeGroupId}/{code}` | 코드 삭제 | 200 |
| DELETE | `/api/codes/by-group/{codeGroupId}` | 그룹별 코드 전체 삭제 | 200 |
| DELETE | `/api/codes/batch` | 코드 일괄 삭제 | 200 |

### 6.2 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create CodeGroup With Codes - 201` | `data.codeGroupId` 존재, 변수 저장 |
| | `POST Create Code - 201` | `data` 확인, 변수 저장 |
| | `POST Create Duplicate CodeGroup - 409` | 중복 에러 |
| **Read** | `GET Get All CodeGroups - 200` | 배열 반환 |
| | `GET Get CodeGroup With Codes - 200` | 코드 목록 포함 |
| | `GET Get Code Detail - 200` | 코드 상세 필드 확인 |
| | `GET Search Codes By Name - 200` | 검색 결과 배열 |
| | `GET Check Code Exists - 200` | `data = true` |
| **Update** | `PUT Update CodeGroup - 200` | 수정 확인 |
| | `PUT Update Code - 200` | 수정 확인 |
| **Delete** | `DELETE Delete Code - 200` | 개별 삭제 |
| | `DELETE Delete CodeGroup With Codes - 200` | 그룹+코드 삭제 |
| **Cleanup** | Cleanup Variables | `createdCodeGroupId`, `createdCode` unset |

---

## 7. WAS 관리 (WAS)

### 7.1 엔드포인트

**WasGroupController** — `/api/was/group`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/was/group` | 전체 WAS 그룹 | 200 |
| GET | `/api/was/group/page` | 페이징 WAS 그룹 | 200 |
| GET | `/api/was/group/{wasGroupId}` | WAS 그룹 상세 | 200 |
| GET | `/api/was/group/{wasGroupId}/instances` | 그룹 내 인스턴스 ID 목록 | 200 |
| GET | `/api/was/group/{wasGroupId}/instances/details` | 그룹 내 인스턴스 상세 | 200 |
| POST | `/api/was/group` | WAS 그룹 생성 | 201 |
| POST | `/api/was/group/{wasGroupId}/instances` | 그룹에 인스턴스 추가 | 200 |
| PUT | `/api/was/group/{wasGroupId}` | WAS 그룹 수정 | 200 |
| DELETE | `/api/was/group/{wasGroupId}` | WAS 그룹 삭제 | 200 |
| DELETE | `/api/was/group/{wasGroupId}/instances` | 그룹 내 인스턴스 전체 제거 | 200 |

**WasInstanceController** — `/api/was/instance`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/was/instance` | 전체 인스턴스 | 200 |
| GET | `/api/was/instance/page` | 페이징 인스턴스 | 200 |
| GET | `/api/was/instance/{instanceId}` | 인스턴스 상세 | 200 |
| POST | `/api/was/instance` | 인스턴스 생성 | 201 |
| PUT | `/api/was/instance/{instanceId}` | 인스턴스 수정 | 200 |
| DELETE | `/api/was/instance/{instanceId}` | 인스턴스 삭제 | 200 |

**WasGatewayStatusController** — `/api/was/gateway-status` | `@RequireMenuAccess(menuId="WAS_GATEWAY_STATUS")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/was/gateway-status/page` | Gateway 상태 페이징 | 200 | READ |
| GET | `/api/was/gateway-status/options` | 필터 옵션 | 200 | READ |
| POST | `/api/was/gateway-status/{instanceId}/{gwId}/{systemId}/test` | 연결 테스트 | 200 | WRITE |

### 7.2 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create WAS Group - 201` | 변수 저장 |
| | `POST Create WAS Instance - 201` | 변수 저장 |
| | `POST Add Instance To Group - 200` | 그룹-인스턴스 연결 |
| **Read** | `GET Get All WAS Groups - 200` | 배열 반환 |
| | `GET Get WAS Group Detail - 200` | 상세 필드 확인 |
| | `GET Get Group Instances - 200` | 인스턴스 포함 확인 |
| | `GET Get All Instances - 200` | 배열 반환 |
| | `GET Get Instance Detail - 200` | 상세 필드 확인 |
| | `GET Get Gateway Status Page - 200` | 페이징 구조 확인 |
| **Update** | `PUT Update WAS Group - 200` | 수정 확인 |
| | `PUT Update WAS Instance - 200` | 수정 확인 |
| **Delete** | `DELETE Remove Instance From Group - 200` | 연결 해제 |
| | `DELETE Delete WAS Instance - 200` | 인스턴스 삭제 |
| | `DELETE Delete WAS Group - 200` | 그룹 삭제 |
| **Cleanup** | Cleanup Variables | `createdWasGroupId`, `createdInstanceId` unset |

---

## 8. 프로퍼티 관리 (Property)

### 8.1 엔드포인트

**PropertyController** — `/api/properties` | `@RequireMenuAccess(menuId="PROPERTY_DB")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/properties/groups/page` | 프로퍼티 그룹 페이징 | 200 | READ |
| GET | `/api/properties/groups` | 전체 프로퍼티 그룹 | 200 | READ |
| GET | `/api/properties/groups/{id}/properties` | 그룹별 프로퍼티 목록 | 200 | READ |
| GET | `/api/properties/groups/{id}/exists` | 그룹 존재 확인 | 200 | READ |
| GET | `/api/properties/groups/{id}/properties/{propId}/was` | WAS별 프로퍼티 | 200 | READ |
| POST | `/api/properties/groups` | 프로퍼티 그룹 생성 | 201 | WRITE |
| POST | `/api/properties/save` | 프로퍼티 저장 | 200 | WRITE |
| POST | `/api/properties/was/save` | WAS 프로퍼티 저장 | 200 | WRITE |
| DELETE | `/api/properties/groups/{id}` | 프로퍼티 그룹 삭제 | 200 | WRITE |

**PropertyHistoryController** — `/api/properties` | `@RequireMenuAccess(menuId="PROPERTY_DB")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/properties/groups/{id}/history/versions` | 이력 버전 목록 | 200 | READ |
| GET | `/api/properties/groups/{id}/history/{ver}` | 특정 버전 이력 | 200 | READ |
| GET | `/api/properties/was/instances` | WAS 인스턴스 목록 | 200 | READ |
| POST | `/api/properties/groups/{id}/backup` | 프로퍼티 백업 | 200 | WRITE |
| POST | `/api/properties/groups/{id}/restore/{ver}` | 프로퍼티 복원 | 200 | WRITE |

### 8.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| PROP_001 | 404 | 프로퍼티 그룹을 찾을 수 없습니다 |
| PROP_002 | 404 | 프로퍼티를 찾을 수 없습니다 |
| PROP_101 | 409 | 이미 존재하는 프로퍼티 그룹입니다 |
| PROP_201 | 400 | 백업할 프로퍼티가 없습니다 |

### 8.3 주요 Validation 규칙

| 필드 | 규칙 |
|------|------|
| `propertyGroupId` | `@NotBlank`, `@Size(max=100)` |
| `propertyGroupName` | `@NotBlank`, `@Size(max=200)` |
| `properties` | `@NotEmpty`, `@Valid` (중첩 검증) |

### 8.4 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Property Group - 201` | `data.propertyGroupId` 존재, 변수 저장 |
| | `POST Create Duplicate Group - 409` | `error.code = "PROP_101"` |
| | `POST Create Group Empty Properties - 400` | `@NotEmpty` 검증 |
| **Read** | `GET Get Property Groups Page - 200` | 페이징 확인 |
| | `GET Get Properties By Group - 200` | 프로퍼티 목록 |
| | `GET Get Non-existent Group - 404` | `error.code = "PROP_001"` |
| | `GET Get History Versions - 200` | 버전 목록 |
| **Update** | `POST Save Properties - 200` | 프로퍼티 저장 확인 |
| | `POST Backup Property Group - 200` | 백업 성공 |
| | `POST Restore Property Group - 200` | 복원 성공 |
| **Delete** | `DELETE Delete Property Group - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdPropertyGroupId` unset |

---

## 9. XML Property 관리 (XmlProperty)

### 9.1 엔드포인트

**XmlPropertyController** — `/api/xml-property` | `@RequireMenuAccess(menuId="XML_PROPERTY")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/xml-property/files` | XML 파일 목록 | 200 | READ |
| GET | `/api/xml-property/files/{fileName}` | XML 파일 상세 | 200 | READ |
| POST | `/api/xml-property/files` | XML 파일 생성 | 201 | WRITE |
| PUT | `/api/xml-property/files/{fileName}/entries` | 항목 저장 | 200 | WRITE |
| DELETE | `/api/xml-property/files/{fileName}` | XML 파일 삭제 | 200 | WRITE |
| POST | `/api/xml-property/reload` | 파일별 Reload | 200 | WRITE |
| POST | `/api/xml-property/reload/all` | 전체 Reload | 200 | WRITE |

### 9.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| XMLPROP_001 | 404 | XML Property 파일을 찾을 수 없습니다 |
| XMLPROP_101 | 409 | 이미 존재하는 파일입니다 |
| XMLPROP_201 | 400 | 유효하지 않은 파일명입니다 |
| XMLPROP_202 | 400 | 중복된 key가 존재합니다 |

### 9.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create XML File - 201` | `data.fileName` 존재, 변수 저장 |
| | `POST Create Duplicate File - 409` | `error.code = "XMLPROP_101"` |
| | `POST Create Invalid FileName - 400` | `error.code = "XMLPROP_201"` |
| **Read** | `GET Get XML File List - 200` | 배열, `entryCount` 필드 확인 |
| | `GET Get XML File Detail - 200` | `entries` 배열 확인 |
| | `GET Get Non-existent File - 404` | `error.code = "XMLPROP_001"` |
| **Update** | `PUT Save Entries - 200` | 항목 저장 확인 |
| | `PUT Save Entries Duplicate Key - 400` | `error.code = "XMLPROP_202"` |
| | `POST Reload File - 200` | Reload 성공 확인 |
| **Delete** | `DELETE Delete XML File - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdFileName` unset |

---

## 10. Reload 관리 (Reload)

### 10.1 엔드포인트

**ReloadController** — `/api/reload` | `@RequireMenuAccess(menuId="RELOAD")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/reload/types` | Reload 타입 목록 | 200 | READ |
| GET | `/api/reload/groups` | WAS 그룹 목록 | 200 | READ |
| GET | `/api/reload/instances` | 인스턴스 목록 | 200 | READ |
| POST | `/api/reload/execute` | Reload 실행 | 200 | WRITE |

### 10.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| RELOAD_001 | 404 | WAS 인스턴스를 찾을 수 없습니다 |
| RELOAD_201 | 400 | 유효하지 않은 Reload 타입입니다 |
| RELOAD_202 | 400 | Reload할 WAS 인스턴스를 선택해주세요 |

### 10.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Read** | `GET Get Reload Types - 200` | 타입 목록 반환 |
| | `GET Get WAS Groups - 200` | 그룹 목록 반환 |
| | `GET Get Instances - 200` | 인스턴스 목록 반환 |
| **Execute** | `POST Execute Reload - 200` | `data.results` 배열, 각 `success` 필드 확인 |
| | `POST Execute Reload No Instance - 400` | `error.code = "RELOAD_202"` |
| | `POST Execute Reload Invalid Type - 400` | `error.code = "RELOAD_201"` |

---

## 11. 기관 관리 (Org / Interface)

### 11.1 엔드포인트

**InterfaceOrgController** — `/api/interface/orgs` | `@RequireMenuAccess(menuId="ORG")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/interface/orgs` | 전체 기관 목록 | 200 | READ |
| GET | `/api/interface/orgs/page` | 페이징 기관 목록 | 200 | READ |
| POST | `/api/interface/orgs/batch` | 기관 일괄 저장 | 200 | WRITE |

### 11.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| IFM_001 | 404 | 기관 정보를 찾을 수 없습니다 |

### 11.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Batch Save Orgs - 200` | 일괄 저장 성공, 변수 저장 |
| | `POST Batch Save Invalid StartTime - 400` | `@Size(min=4, max=4)` 검증 |
| **Read** | `GET Get All Orgs - 200` | 배열 반환 |
| | `GET Get Orgs Page - 200` | 페이징 구조 확인 |
| **Cleanup** | Cleanup Variables | `createdOrgId` unset |

---

## 12. Gateway 관리 (Interface Gateway)

### 12.1 엔드포인트

**InterfaceGatewayController** — `/api/interface/gateways` | `@RequireMenuAccess(menuId="GATEWAY_MANAGEMENT")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/interface/gateways/page` | 페이징 Gateway 목록 | 200 | READ |
| GET | `/api/interface/gateways/{gwId}` | Gateway 상세 | 200 | READ |
| GET | `/api/interface/gateways/{gwId}/transports` | 관련 Transport 목록 | 200 | READ |
| POST | `/api/interface/gateways/batch` | Gateway 일괄 저장 | 200 | WRITE |
| POST | `/api/interface/gateways/{gwId}/systems/batch` | System 일괄 저장 | 200 | WRITE |
| POST | `/api/interface/gateways/with-systems` | Gateway+System 동시 저장 | 200 | WRITE |

### 12.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| IFM_004 | 404 | Gateway 정보를 찾을 수 없습니다 |
| IFM_005 | 404 | Gateway System 정보를 찾을 수 없습니다 |

### 12.3 주요 Validation 규칙

| 필드 | 규칙 |
|------|------|
| `gwId` | `@NotBlank`, `@Size(max=20)`, `@Pattern(regexp="^[A-Za-z0-9_]+$")` |
| `gwName` | `@NotBlank`, `@Size(max=200)` |
| `threadCount` | `@NotNull`, `@Min(1)`, `@Max(999)` |

### 12.4 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Save Gateways Batch - 200` | 일괄 저장 성공, 변수 저장 |
| | `POST Save Gateway Invalid GwId Pattern - 400` | `@Pattern` 검증 |
| | `POST Save Gateway ThreadCount Out of Range - 400` | `@Min(1)`, `@Max(999)` 검증 |
| **Read** | `GET Get Gateways Page - 200` | 페이징 구조 확인 |
| | `GET Get Gateway Detail - 200` | 상세 필드 확인 |
| | `GET Get Related Transports - 200` | Transport 배열 확인 |
| **Delete** | (일괄 저장에서 삭제 항목 포함) | 삭제 반영 확인 |
| **Cleanup** | Cleanup Variables | `createdGwId` unset |

---

## 13. Gateway 맵핑 관리 (Interface Transport)

### 13.1 엔드포인트

**InterfaceTransportController** | `@RequireMenuAccess(menuId="GATEWAY_MAPPING")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/interface/transports` | 전체 Transport 목록 | 200 | READ |
| GET | `/api/interface/orgs/{orgId}/gateways` | 기관별 Gateway 맵핑 | 200 | READ |
| POST | `/api/interface/orgs/{orgId}/gateways/batch` | 맵핑 일괄 저장 | 200 | WRITE |

### 13.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| IFM_002 | 404 | 기관통신 Gateway 맵핑 정보를 찾을 수 없습니다 |

---

## 14. 전문처리 핸들러 관리 (Message Handler)

### 14.1 엔드포인트

**InterfaceMessageHandlerController** — `/api/interface/orgs` | `@RequireMenuAccess(menuId="MESSAGE_HANDLER")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/interface/orgs/{orgId}/handlers` | 기관별 핸들러 목록 | 200 | READ |
| POST | `/api/interface/orgs/{orgId}/handlers/batch` | 핸들러 일괄 저장 | 200 | WRITE |

**InterfaceMessageHandlerQueryController** — `/api/interface/handlers` | `@RequireMenuAccess(menuId="MESSAGE_HANDLER")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/interface/handlers` | 전체 핸들러 조회 | 200 | READ |

### 14.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| IFM_003 | 404 | 전문처리 핸들러 정보를 찾을 수 없습니다 |

---

## 15. 리스너-커넥터 맵핑 관리 (Listener Connector Mapping)

### 15.1 엔드포인트

**ListenerConnectorMappingController** — `/api/interface-mnt/listener-connector-mappings` | `@RequireMenuAccess(menuId="LISTENER_CONNECTOR_MAPPING")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/interface-mnt/listener-connector-mappings` | 맵핑 검색 | 200 | READ |
| GET | `/api/interface-mnt/listener-connector-mappings/{gwId}/{systemId}/{id}` | 맵핑 상세 | 200 | READ |
| POST | `/api/interface-mnt/listener-connector-mappings` | 맵핑 생성 | 201 | WRITE |
| POST | `/api/interface-mnt/listener-connector-mappings/batch` | 맵핑 일괄 저장 | 200 | WRITE |
| PUT | `/api/interface-mnt/listener-connector-mappings/{gwId}/{systemId}/{id}` | 맵핑 수정 | 200 | WRITE |
| DELETE | `/api/interface-mnt/listener-connector-mappings/{gwId}/{systemId}/{id}` | 맵핑 삭제 | 200 | WRITE |

### 15.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| IFM_006 | 404 | 리스너-커넥터 매핑 정보를 찾을 수 없습니다 |
| IFM_101 | 409 | 이미 존재하는 리스너-커넥터 매핑입니다 |
| IFM_202 | 400 | 유효하지 않은 리스너 Gateway/System입니다 |
| IFM_203 | 400 | 유효하지 않은 커넥터 Gateway/System입니다 |

---

## 16. 요청처리 APP 맵핑 관리 (App Mapping)

### 16.1 엔드포인트

**AppMappingController** — `/api/interface-mnt/app-mappings` | `@RequireMenuAccess(menuId="APP_MAPPING")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/interface-mnt/app-mappings` | 맵핑 검색 | 200 | READ |
| GET | `/api/interface-mnt/app-mappings/{gwId}/{reqIdCode}` | 맵핑 상세 | 200 | READ |
| POST | `/api/interface-mnt/app-mappings` | 맵핑 생성 | 201 | WRITE |
| PUT | `/api/interface-mnt/app-mappings/{gwId}/{reqIdCode}` | 맵핑 수정 | 200 | WRITE |
| DELETE | `/api/interface-mnt/app-mappings/{gwId}/{reqIdCode}` | 맵핑 삭제 | 200 | WRITE |

### 16.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| IFM_007 | 404 | 요청처리 App 맵핑 정보를 찾을 수 없습니다 |
| IFM_102 | 409 | 이미 존재하는 요청처리 App 맵핑입니다 |

---

## 17. 인터페이스 관리 (Interface)

### 17.1 엔드포인트

**InterfaceController** — `/api/interfaces`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/interfaces` | 전체 인터페이스 | 200 |
| GET | `/api/interfaces/page` | 페이징 인터페이스 | 200 |
| GET | `/api/interfaces/{interfaceId}` | 인터페이스 상세 | 200 |
| GET | `/api/interfaces/check/classname` | 클래스명 중복 확인 | 200 |
| POST | `/api/interfaces` | 인터페이스 생성 | 201 |
| PUT | `/api/interfaces/{interfaceId}` | 인터페이스 수정 | 200 |
| DELETE | `/api/interfaces/{interfaceId}` | 인터페이스 삭제 | 200 |
| DELETE | `/api/interfaces/batch` | 일괄 삭제 | 200 |

### 17.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| INTERFACE_001 | 404 | 인터페이스를 찾을 수 없습니다 |
| INTERFACE_101 | 409 | 이미 존재하는 클래스명입니다 |
| INTERFACE_102 | 409 | 이미 존재하는 인터페이스 ID입니다 |

### 17.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Interface - 201` | 변수 저장 |
| | `POST Create Duplicate InterfaceId - 409` | `error.code = "INTERFACE_102"` |
| | `POST Create Duplicate ClassName - 409` | `error.code = "INTERFACE_101"` |
| **Read** | `GET Get All Interfaces - 200` | 배열 반환 |
| | `GET Get Interface Page - 200` | 페이징 구조 확인 |
| | `GET Get Interface Detail - 200` | 상세 필드 확인 |
| | `GET Get Non-existent Interface - 404` | `error.code = "INTERFACE_001"` |
| **Update** | `PUT Update Interface - 200` | 수정 확인 |
| **Delete** | `DELETE Delete Interface - 200` | 삭제 성공 |
| | `DELETE Batch Delete Interfaces - 200` | 일괄 삭제 |
| **Cleanup** | Cleanup Variables | `createdInterfaceId` unset |

---

## 18. 전문 관리 (Message)

### 18.1 엔드포인트

**MessageController** — `/api/messages` | `@RequireMenuAccess(menuId="MESSAGE")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/messages/{messageId}` | 전문 조회 | 200 | READ |
| GET | `/api/messages/{messageId}/detail` | 전문 상세 (필드 포함) | 200 | READ |
| GET | `/api/messages/page` | 페이징 전문 목록 | 200 | READ |
| POST | `/api/messages` | 전문 생성 | 201 | WRITE |
| PUT | `/api/messages/{messageId}` | 전문 수정 | 200 | WRITE |
| DELETE | `/api/messages/{messageId}` | 전문 삭제 | 200 | WRITE |

**FieldController** — `/api/messages/{messageId}/fields`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| POST | `/api/messages/{messageId}/fields` | 필드 생성 | 201 |
| POST | `/api/messages/{messageId}/fields/batch` | 필드 일괄 생성 | 201 |
| PUT | `/api/messages/{messageId}/fields/{fieldId}` | 필드 수정 | 200 |
| DELETE | `/api/messages/{messageId}/fields/{fieldId}` | 필드 삭제 | 200 |

### 18.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| MSG_002 | 404 | 전문(Message)을 찾을 수 없습니다 |
| MSG_003 | 404 | 필드를 찾을 수 없습니다 |
| MSG_103 | 409 | 이미 존재하는 전문 ID입니다 |
| MSG_104 | 409 | 이미 존재하는 필드입니다 |
| MSG_206 | 400 | 전문 파싱에 실패했습니다 |

### 18.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Message - 201` | `data.messageId` 저장 |
| | `POST Create Duplicate Message - 409` | `error.code = "MSG_103"` |
| | `POST Create Field - 201` | `data.messageFieldId` 저장 |
| | `POST Create Field Batch - 201` | 일괄 생성 확인 |
| | `POST Create Duplicate Field - 409` | `error.code = "MSG_104"` |
| **Read** | `GET Get Messages Page - 200` | 페이징 구조 확인 |
| | `GET Get Message Detail - 200` | `data.fields` 배열 확인 |
| | `GET Get Non-existent Message - 404` | `error.code = "MSG_002"` |
| **Update** | `PUT Update Message - 200` | 수정 확인 |
| | `PUT Update Field - 200` | 필드 수정 확인 |
| **Delete** | `DELETE Delete Field - 200` | 필드 삭제 |
| | `DELETE Delete Message - 200` | 전문 삭제 |
| **Cleanup** | Cleanup Variables | `createdMessageId`, `createdFieldId` unset |

---

## 19. 거래 관리 (Trx)

### 19.1 엔드포인트

**TrxManagementController** — `/api/management/trx` | `@RequireMenuAccess(menuId="TRX")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/management/trx/page` | 거래 검색 | 200 | READ |
| GET | `/api/management/trx/{trxId}` | 거래 상세 | 200 | READ |
| POST | `/api/management/trx` | 거래 생성 | 201 | WRITE |
| PUT | `/api/management/trx/{trxId}` | 거래 수정 | 200 | WRITE |
| DELETE | `/api/management/trx/{trxId}` | 거래 삭제 | 200 | WRITE |
| GET | `/api/management/trx/{trxId}/messages/{ioType}` | 거래-전문 맵핑 조회 | 200 | READ |
| PUT | `/api/management/trx/{trxId}/messages/{ioType}` | 거래-전문 맵핑 수정 | 200 | WRITE |

**TrxMessageController** — `/api/trx-messages` | `@RequireMenuAccess(menuIds={"TRX","TRX_TEST"})`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/trx-messages/page` | 거래-전문 페이징 | 200 | READ |
| GET | `/api/trx-messages/{trxId}/{ioType}` | 거래-전문 상세 | 200 | READ |
| POST | `/api/trx-messages` | 거래-전문 맵핑 생성 | 201 | WRITE |
| POST | `/api/trx-messages/batch` | 거래-전문 일괄 처리 | 200 | WRITE |
| PUT | `/api/trx-messages/{trxId}/{ioType}/status` | 거래-전문 상태 수정 | 200 | WRITE |
| DELETE | `/api/trx-messages/{trxId}/{orgId}/{ioType}` | 거래-전문 삭제 | 200 | WRITE |

### 19.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| MSG_001 | 404 | 거래(TRX)를 찾을 수 없습니다 |
| MSG_004 | 404 | 거래-전문 매핑을 찾을 수 없습니다 |
| MSG_101 | 409 | 이미 존재하는 TRX ID입니다 |
| MSG_201 | 400 | 유효하지 않은 I/O 타입입니다 |
| MSG_202 | 400 | 유효하지 않은 운영 모드 타입입니다 |

### 19.3 주요 Validation 규칙

| 필드 | 규칙 |
|------|------|
| `trxId` | `@NotBlank`, `@Size(max=40)` |
| `trxType` | `@NotBlank`, `@Size(max=1)` |
| `retryTrxYn` | `@NotBlank`, `@Size(max=1)` |
| `maxRetryCount` | `@NotNull` (Integer) |

### 19.4 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Trx - 201` | `data.trxId` 저장 |
| | `POST Create Duplicate Trx - 409` | `error.code = "MSG_101"` |
| | `POST Create Trx Missing Fields - 400` | `@NotBlank` 검증 |
| | `POST Create Trx Message Mapping - 201` | 맵핑 생성 확인 |
| **Read** | `GET Get Trx Page - 200` | 페이징 구조 확인 |
| | `GET Get Trx Detail - 200` | 상세 필드 확인 |
| | `GET Get Non-existent Trx - 404` | `error.code = "MSG_001"` |
| | `GET Get Trx Messages - 200` | 맵핑 정보 확인 |
| **Update** | `PUT Update Trx - 200` | 수정 확인 |
| | `PUT Update Trx Message Status - 200` | 상태 수정 확인 |
| **Delete** | `DELETE Delete Trx Message - 200` | 맵핑 삭제 |
| | `DELETE Delete Trx - 200` | 거래 삭제 |
| **Cleanup** | Cleanup Variables | `createdTrxId` unset |

---

## 20. 거래 중지 관리 (TrxStop)

### 20.1 엔드포인트

**TrxStopController** — `/api/trx-stop` | `@RequireMenuAccess(menuId="TRX_STOP")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/trx-stop/page` | 거래 중지 검색 | 200 | READ |
| PUT | `/api/trx-stop/batch` | 거래 중지 일괄 수정 | 200 | WRITE |
| PUT | `/api/trx-stop/batch-oper-mode` | 운영모드 일괄 수정 | 200 | WRITE |

### 20.2 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Read** | `GET Search Trx Stop - 200` | 페이징 구조 확인 |
| **Update** | `PUT Batch Update Trx Stop - 200` | 중지/해제 결과 확인 |
| | `PUT Batch Update Oper Mode - 200` | 운영모드 변경 확인 |

---

## 21. 전문 테스트 (Message Test)

### 21.1 엔드포인트

**MessageTestController** — `/api/message-test` | `@RequireMenuAccess(menuId="TRX_TEST")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/message-test` | 내 테스트 케이스 | 200 | READ |
| GET | `/api/message-test/search` | 테스트 케이스 검색 | 200 | READ |
| GET | `/api/message-test/fields` | 테스트용 필드 목록 | 200 | READ |
| GET | `/api/message-test/by-trx/{trxId}` | 거래별 테스트 케이스 | 200 | READ |
| GET | `/api/message-test/{testSno}` | 테스트 케이스 상세 | 200 | READ |
| POST | `/api/message-test` | 테스트 케이스 생성 | 201 | WRITE |
| PUT | `/api/message-test/{testSno}` | 테스트 케이스 수정 | 200 | WRITE |
| DELETE | `/api/message-test/{testSno}` | 테스트 케이스 삭제 | 200 | WRITE |
| POST | `/api/message-test/simulate` | 테스트 실행(시뮬레이션) | 200 | READ |

### 21.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| MSG_TEST_001 | 404 | 테스트 케이스를 찾을 수 없습니다 |
| MSG_TEST_101 | 409 | 이미 존재하는 테스트 케이스명입니다 |
| MSG_TEST_201 | 400 | 테스트 데이터 형식이 올바르지 않습니다 |
| MSG_TEST_801 | 403 | 테스트 케이스에 접근 권한이 없습니다 |

### 21.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보, 선행 거래 데이터 확인 |
| **Create** | `POST Create Test Case - 201` | `data.testSno` 저장 |
| | `POST Create Duplicate Test Name - 409` | `error.code = "MSG_TEST_101"` |
| **Read** | `GET Get My Test Cases - 200` | 배열 반환 |
| | `GET Get Test Case Detail - 200` | 상세 필드 확인 |
| | `GET Get Test Cases By Trx - 200` | 거래별 필터링 확인 |
| | `GET Get Non-existent Test - 404` | `error.code = "MSG_TEST_001"` |
| **Execute** | `POST Simulate Test - 200` | 시뮬레이션 결과 확인 |
| **Update** | `PUT Update Test Case - 200` | 수정 확인 |
| **Delete** | `DELETE Delete Test Case - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdTestSno` unset |

---

## 22. Proxy 응답 관리 (Proxy Testdata)

### 22.1 엔드포인트

**ProxyTestdataController** — `/api/proxy-testdata` | `@RequireMenuAccess(menuId="PROXY_TESTDATA")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/proxy-testdata/page` | 페이징 목록 | 200 | READ |
| GET | `/api/proxy-testdata/{testSno}` | 상세 조회 | 200 | READ |
| GET | `/api/proxy-testdata/test-fields` | 테스트 필드 목록 | 200 | READ |
| GET | `/api/proxy-testdata/group-ids` | 그룹 ID 목록 | 200 | READ |
| POST | `/api/proxy-testdata` | Proxy 데이터 생성 | 201 | WRITE |
| PUT | `/api/proxy-testdata/{testSno}` | Proxy 데이터 수정 | 200 | WRITE |
| DELETE | `/api/proxy-testdata/{testSno}` | Proxy 데이터 삭제 | 200 | WRITE |
| GET | `/api/proxy-testdata/proxy-settings` | Proxy 설정 조회 | 200 | READ |
| PUT | `/api/proxy-testdata/proxy-settings/default-proxy/set` | 기본 Proxy 설정 | 200 | WRITE |
| PUT | `/api/proxy-testdata/proxy-settings/default-proxy/clear` | 기본 Proxy 해제 | 200 | WRITE |

### 22.2 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보, 선행 거래/전문 데이터 확인 |
| **Create** | `POST Create Proxy Testdata - 201` | `data.testSno` 저장 |
| **Read** | `GET Get Proxy Testdata Page - 200` | 페이징 구조 확인 |
| | `GET Get Proxy Testdata Detail - 200` | 상세 필드 확인 |
| | `GET Get Proxy Settings - 200` | 설정 정보 확인 |
| **Update** | `PUT Update Proxy Testdata - 200` | 수정 확인 |
| | `PUT Set Default Proxy - 200` | 기본 Proxy 설정 확인 |
| **Delete** | `DELETE Delete Proxy Testdata - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdProxyTestSno` unset |

---

## 23. 전문 내역 조회 (Message Instance)

### 23.1 엔드포인트

**MessageInstanceController** — `/api/message-instances` | `@RequireMenuAccess(menuId="MESSAGE_INSTANCE")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/message-instances/page` | 전문 내역 검색 | 200 | READ |
| GET | `/api/message-instances/{messageSno}` | 전문 내역 상세 | 200 | READ |
| GET | `/api/message-instances/tracking/{trxTrackingNo}` | 추적번호로 조회 | 200 | READ |
| DELETE | `/api/message-instances/{messageSno}` | 전문 내역 삭제 | 200 | WRITE |

### 23.2 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Read** | `GET Search Message Instances - 200` | 페이징 구조 확인 |
| | `GET Get Instance Detail - 200` | 상세 필드 확인 |
| | `GET Get Non-existent Instance - 404` | `error.code = "MSG_INST_001"` |
| **Delete** | `DELETE Delete Message Instance - 200` | 삭제 성공 |

---

## 24. 전문 파싱 (Message Parsing)

### 24.1 엔드포인트

**MessageParsingController** — `/api/telegram` | `@RequireMenuAccess(menuId="MESSAGE_PARSING")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| POST | `/api/telegram/parse` | 전문 파싱 | 200 | READ |
| GET | `/api/telegram/orgs` | 기관 목록 | 200 | READ |
| GET | `/api/telegram/messages` | 전문 검색 | 200 | READ |

### 24.2 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Read** | `GET Get Org List - 200` | 기관 배열 반환 |
| | `GET Search Messages - 200` | 검색 결과 반환 |
| **Execute** | `POST Parse Message - 200` | 파싱 결과 필드 확인 |
| | `POST Parse Invalid Message - 400` | `error.code = "MSG_206"` |

---

## 25. Validator 관리 (Validator)

### 25.1 엔드포인트

**ValidatorController** — `/api/validators` | `@RequireMenuAccess(menuId="VALIDATOR")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/validators/page` | Validator 페이징 | 200 | READ |
| GET | `/api/validators/{validatorId}` | Validator 상세 | 200 | READ |
| POST | `/api/validators` | Validator 생성 | 201 | WRITE |
| PUT | `/api/validators/{validatorId}` | Validator 수정 | 200 | WRITE |
| DELETE | `/api/validators/{validatorId}` | Validator 삭제 | 200 | WRITE |

### 25.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| VALIDATOR_001 | 404 | Validator를 찾을 수 없습니다 |
| VALIDATOR_101 | 409 | 이미 존재하는 Validator ID입니다 |

### 25.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Validator - 201` | `data.validatorId` 저장 |
| | `POST Create Duplicate Validator - 409` | `error.code = "VALIDATOR_101"` |
| **Read** | `GET Get Validators Page - 200` | 페이징 구조 확인 |
| | `GET Get Validator Detail - 200` | 상세 필드 확인 |
| | `GET Get Non-existent Validator - 404` | `error.code = "VALIDATOR_001"` |
| **Update** | `PUT Update Validator - 200` | 수정 확인 |
| **Delete** | `DELETE Delete Validator - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdValidatorId` unset |

---

## 26. 배치 관리 (Batch)

### 26.1 엔드포인트

**BatchAppController** — `/api/batch/apps` | `@RequireMenuAccess(menuId="BATCH_APP")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/batch/apps/list` | 전체 배치 앱 | 200 | READ |
| GET | `/api/batch/apps/page` | 페이징 배치 앱 | 200 | READ |
| GET | `/api/batch/apps/{batchAppId}` | 배치 앱 상세 | 200 | READ |
| GET | `/api/batch/apps/check/id` | 배치 앱 ID 중복 확인 | 200 | READ |
| POST | `/api/batch/apps` | 배치 앱 생성 | 201 | WRITE |
| PUT | `/api/batch/apps/{batchAppId}` | 배치 앱 수정 | 200 | WRITE |
| DELETE | `/api/batch/apps/{batchAppId}` | 배치 앱 삭제 | 200 | WRITE |
| POST | `/api/batch/apps/{id}/was/instance` | WAS 인스턴스 할당 | 200 | WRITE |
| DELETE | `/api/batch/apps/{id}/was/instance/{instanceId}` | WAS 인스턴스 해제 | 200 | WRITE |

**BatchHisController** — `/api/batch/history` | `@RequireMenuAccess(menuId="BATCH_HISTORY")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/batch/history/page` | 배치 이력 페이징 | 200 | READ |

**BatchExecController** — `/api/batch/exec`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| POST | `/api/batch/exec` | 배치 수동 실행 | 200 | WRITE |

**ExecutingBatchController** — `/api/batch/executing` | `@RequireMenuAccess(menuId="BATCH_EXECUTING")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/batch/executing/page` | 실행 중 배치 목록 | 200 | READ |

**BatchHisCleanupController** — `/api/batch/history/cleanup` | `@RequireMenuAccess(menuId="BATCH_HISTORY")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/batch/history/cleanup/preview` | 정리 대상 미리보기 | 200 | READ |
| POST | `/api/batch/history/cleanup/execute` | 정리 실행 | 200 | WRITE |

### 26.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| BATCH_001 | 404 | 배치 앱을 찾을 수 없습니다 |
| BATCH_101 | 409 | 이미 존재하는 배치 앱 ID입니다 |
| BATCH_102 | 409 | 이미 존재하는 배치 앱명입니다 |
| BATCH_103 | 409 | 이미 할당된 WAS 인스턴스입니다 |
| BATCH_201 | 400 | 유효하지 않은 배치 주기입니다 |
| BATCH_202 | 400 | 유효하지 않은 크론 표현식입니다 |

### 26.3 주요 Validation 규칙

| 필드 | 규칙 |
|------|------|
| `batchAppId` | `@NotBlank`, `@Size(max=50)` |
| `batchAppFileName` | `@NotBlank`, `@Size(max=200)` |

### 26.4 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Batch App - 201` | `data.batchAppId` 저장 |
| | `POST Create Duplicate Batch App - 409` | `error.code = "BATCH_101"` |
| | `POST Assign WAS Instance - 200` | 인스턴스 할당 확인 |
| | `POST Assign Duplicate Instance - 409` | `error.code = "BATCH_103"` |
| **Read** | `GET Get Batch Apps Page - 200` | 페이징 구조 확인 |
| | `GET Get Batch App Detail - 200` | `assignedInstances` 확인 |
| | `GET Get Non-existent Batch App - 404` | `error.code = "BATCH_001"` |
| | `GET Get Batch History - 200` | 이력 페이징 확인 |
| | `GET Get Executing Batches - 200` | 실행 중 배치 확인 |
| **Update** | `PUT Update Batch App - 200` | 수정 확인 |
| **Delete** | `DELETE Remove WAS Instance - 200` | 인스턴스 해제 |
| | `DELETE Delete Batch App - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdBatchAppId` unset |

---

## 27. 오류 관리 (Error)

### 27.1 엔드포인트

**ErrorController** — `/api/errors`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/errors/page` | 오류코드 페이징 | 200 |
| GET | `/api/errors/{errorCode}` | 오류코드 상세 | 200 |
| POST | `/api/errors` | 오류코드 생성 | 201 |
| PUT | `/api/errors/{errorCode}` | 오류코드 수정 | 200 |
| DELETE | `/api/errors/{errorCode}` | 오류코드 삭제 | 200 |
| GET | `/api/errors/{errorCode}/handle-apps` | 오류-처리APP 맵핑 조회 | 200 |
| PUT | `/api/errors/{errorCode}/handle-apps` | 오류-처리APP 맵핑 저장 | 200 |

**ErrorDescController** — `/api/error-descs` | `@RequireMenuAccess(menuId="ERROR_CODE")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/error-descs` | 오류 설명 조회 | 200 | READ |
| GET | `/api/error-descs/{errorCode}/{localeCode}` | 오류 설명 상세 | 200 | READ |
| POST | `/api/error-descs` | 오류 설명 생성 | 201 | WRITE |
| PUT | `/api/error-descs/{errorCode}/{localeCode}` | 오류 설명 수정 | 200 | WRITE |
| DELETE | `/api/error-descs/{errorCode}/{localeCode}` | 오류 설명 삭제 | 200 | WRITE |

**HandleAppController** — `/api/handle-apps`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/handle-apps` | 전체 처리 APP | 200 |
| GET | `/api/handle-apps/{handleAppId}` | 처리 APP 상세 | 200 |

**ErrorHandleAppController** — `/api/error-handle-apps` | `@RequireMenuAccess(menuId="ERROR_CODE")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/error-handle-apps` | 오류-처리APP 맵핑 조회 | 200 | READ |
| POST | `/api/error-handle-apps` | 맵핑 생성 | 201 | WRITE |
| PUT | `/api/error-handle-apps/{errorCode}/{handleAppId}` | 맵핑 수정 | 200 | WRITE |
| DELETE | `/api/error-handle-apps/{errorCode}/{handleAppId}` | 맵핑 삭제 | 200 | WRITE |

**ErrorHisController** — `/api/error-histories`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/error-histories/page` | 오류 이력 페이징 | 200 |
| GET | `/api/error-histories/{errorCode}/{errorSerNo}` | 오류 이력 상세 | 200 |
| POST | `/api/error-histories` | 오류 이력 등록 | 201 |
| DELETE | `/api/error-histories/{errorCode}/{errorSerNo}` | 오류 이력 삭제 | 200 |

**ErrorHandleHisController** — `/api/error-handle-histories`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/error-handle-histories/page` | 처리 이력 페이징 | 200 |
| GET | `/api/error-handle-histories/{errorCode}/{handleAppId}/{errorSerNo}` | 처리 이력 상세 | 200 |
| POST | `/api/error-handle-histories` | 처리 이력 등록 | 201 |
| DELETE | `/api/error-handle-histories/{errorCode}/{handleAppId}/{errorSerNo}` | 처리 이력 삭제 | 200 |

### 27.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| ERROR_001 | 404 | 오류코드를 찾을 수 없습니다 |
| ERROR_002 | 404 | 처리 APP을 찾을 수 없습니다 |
| ERROR_003 | 404 | 오류코드 설명을 찾을 수 없습니다 |
| ERROR_101 | 409 | 이미 존재하는 오류코드입니다 |
| ERROR_102 | 409 | 이미 존재하는 처리 APP ID입니다 |
| ERROR_103 | 409 | 이미 존재하는 오류코드 설명입니다 |
| ERROR_104 | 409 | 이미 존재하는 오류코드-처리APP 매핑입니다 |

### 27.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Error Code - 201` | `data.errorCode` 저장 |
| | `POST Create Duplicate Error Code - 409` | `error.code = "ERROR_101"` |
| | `POST Create Error Desc - 201` | 설명 생성 확인 |
| | `POST Create Error Handle App Mapping - 201` | 맵핑 생성 확인 |
| **Read** | `GET Get Errors Page - 200` | 페이징 구조 확인 |
| | `GET Get Error Detail - 200` | 상세 필드 확인 |
| | `GET Get Non-existent Error - 404` | `error.code = "ERROR_001"` |
| | `GET Get Error Descs - 200` | 설명 목록 확인 |
| | `GET Get Handle Apps - 200` | 처리 APP 목록 확인 |
| **Update** | `PUT Update Error Code - 200` | 수정 확인 |
| | `PUT Update Error Desc - 200` | 설명 수정 확인 |
| | `PUT Save Error Handle Apps - 200` | 맵핑 저장 확인 |
| **Delete** | `DELETE Delete Error Handle App - 200` | 맵핑 삭제 |
| | `DELETE Delete Error Desc - 200` | 설명 삭제 |
| | `DELETE Delete Error Code - 200` | 오류코드 삭제 |
| **Cleanup** | Cleanup Variables | `createdErrorCode` unset |

---

## 28. 모니터링 (Monitor)

### 28.1 엔드포인트

**MonitorController** — `/api/monitors`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/monitors` | 전체 모니터 목록 | 200 |
| GET | `/api/monitors/page` | 페이징 모니터 목록 | 200 |
| GET | `/api/monitors/{monitorId}` | 모니터 상세 | 200 |
| POST | `/api/monitors` | 모니터 생성 | 201 |
| PUT | `/api/monitors/{monitorId}` | 모니터 수정 | 200 |
| DELETE | `/api/monitors/{monitorId}` | 모니터 삭제 | 200 |

### 28.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| MONITOR_001 | 404 | 모니터를 찾을 수 없습니다 |
| MONITOR_101 | 409 | 이미 존재하는 모니터 ID입니다 |
| MONITOR_201 | 400 | 유효하지 않은 새로고침 주기입니다 |
| MONITOR_202 | 400 | 유효하지 않은 SQL 쿼리입니다 |
| MONITOR_203 | 400 | SQL 쿼리에 위험한 패턴이 감지되었습니다 |
| MONITOR_204 | 400 | SELECT 쿼리만 허용됩니다 |

### 28.3 주요 Validation 규칙

| 필드 | 규칙 |
|------|------|
| `monitorId` | `@NotBlank`, `@Size(max=20)` |
| `monitorName` | `@NotBlank`, `@Size(max=50)` |
| `refreshTerm` | `@NotBlank`, `@Pattern(regexp="^(1\|5\|10\|30\|60\|180\|360)$")` |
| `monitorQuery` | `@Size(max=2000)`, 서비스에서 SQL Injection 검증 |

### 28.4 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Monitor - 201` | `data.monitorId` 저장 |
| | `POST Create Duplicate Monitor - 409` | `error.code = "MONITOR_101"` |
| | `POST Create Monitor Invalid RefreshTerm - 400` | `@Pattern` 검증 |
| | `POST Create Monitor Dangerous SQL - 400` | `error.code = "MONITOR_203"` |
| **Read** | `GET Get Monitors Page - 200` | 페이징 구조 확인 |
| | `GET Get Monitor Detail - 200` | 상세 필드 확인 |
| | `GET Get Non-existent Monitor - 404` | `error.code = "MONITOR_001"` |
| **Update** | `PUT Update Monitor - 200` | 수정 확인 |
| **Delete** | `DELETE Delete Monitor - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | `createdMonitorId` unset |

---

## 29. 중지거래 접근허용자 관리 (Access User)

### 29.1 엔드포인트

**AccessUserController** — `/api/access-users` | `@RequireMenuAccess(menuId="ACCESS_USER")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/access-users/page` | 접근허용자 페이징 | 200 | READ |
| POST | `/api/access-users` | 접근허용자 생성 | 201 | WRITE |
| PUT | `/api/access-users` | 접근허용자 수정 | 200 | WRITE |
| DELETE | `/api/access-users/{gubunType}/{trxId}/{custUserId}` | 접근허용자 삭제 | 200 | WRITE |

### 29.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| ACCESS_USER_001 | 404 | 중지거래 접근허용자를 찾을 수 없습니다 |
| ACCESS_USER_101 | 409 | 이미 존재하는 중지거래 접근허용자입니다 |
| ACCESS_USER_201 | 400 | 유효하지 않은 구분유형 (T/S만 허용) |
| ACCESS_USER_202 | 400 | 유효하지 않은 사용여부 (Y/N만 허용) |

### 29.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Access User - 201` | 생성 확인, PK 변수 저장 |
| | `POST Create Duplicate Access User - 409` | `error.code = "ACCESS_USER_101"` |
| | `POST Create Invalid GubunType - 400` | `error.code = "ACCESS_USER_201"` |
| **Read** | `GET Get Access Users Page - 200` | 페이징 구조 확인 |
| **Update** | `PUT Update Access User - 200` | 수정 확인 |
| **Delete** | `DELETE Delete Access User - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | 관련 변수 unset |

---

## 30. 이력 관리 (Audit / History)

### 30.1 엔드포인트

**AdminActionLogController** — `/api/admin-action-logs`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/admin-action-logs` | 관리자 작업 이력 검색 | 200 |

**TrxStopHistoryController** — `/api/trx-stop-histories` | `@RequireMenuAccess(menuId="TRX_STOP_HISTORY")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/trx-stop-histories` | 거래 중지 이력 검색 | 200 | READ |
| GET | `/api/trx-stop-histories/trx/{trxId}` | 거래별 중지 이력 | 200 | READ |

### 30.2 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Read** | `GET Search Admin Action Logs - 200` | 페이징 구조 확인 |
| | `GET Search Trx Stop Histories - 200` | 페이징 구조 확인 |
| | `GET Get Trx Stop History By TrxId - 200` | 거래별 이력 확인 |

---

## 31. 게시판 관리 (Board)

### 31.1 엔드포인트

**BoardController** — `/api/boards`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/boards` | 전체 게시판 | 200 |
| GET | `/api/boards/page` | 페이징 게시판 | 200 |
| GET | `/api/boards/{boardId}` | 게시판 상세 | 200 |
| POST | `/api/boards` | 게시판 생성 | 201 |
| PUT | `/api/boards/{boardId}` | 게시판 수정 | 200 |
| DELETE | `/api/boards/{boardId}` | 게시판 삭제 | 200 |

**BoardCategoryController** — `/api/board-categories`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/board-categories/board/{boardId}` | 카테고리 목록 | 200 |
| POST | `/api/board-categories/{boardId}` | 카테고리 생성 | 201 |
| PUT | `/api/board-categories/{boardId}/{categorySeq}` | 카테고리 수정 | 200 |
| DELETE | `/api/board-categories/{boardId}/{categorySeq}` | 카테고리 삭제 | 200 |

**BoardAuthController** — `/api/board-auth`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/board-auth/page` | 권한 페이징 | 200 |
| GET | `/api/board-auth/{userId}/{boardId}` | 권한 상세 | 200 |
| GET | `/api/board-auth/can-write/{userId}/{boardId}` | 쓰기 권한 확인 | 200 |
| POST | `/api/board-auth` | 권한 생성 | 201 |
| PUT | `/api/board-auth/{userId}/{boardId}` | 권한 수정 | 200 |
| DELETE | `/api/board-auth/{userId}/{boardId}` | 권한 삭제 | 200 |

**ArticleController** — `/api/articles`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/articles/board/{boardId}/page` | 게시글 페이징 | 200 |
| GET | `/api/articles/{articleSeq}` | 게시글 상세 | 200 |
| POST | `/api/articles` | 게시글 작성 | 201 |
| PUT | `/api/articles/{articleSeq}` | 게시글 수정 | 200 |
| DELETE | `/api/articles/{articleSeq}` | 게시글 삭제 | 200 |
| POST | `/api/articles/with-files` | 파일 첨부 게시글 작성 | 201 |

### 31.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| BOARD_001 | 404 | 게시판을 찾을 수 없습니다 |
| BOARD_002 | 404 | 게시글을 찾을 수 없습니다 |
| BOARD_003 | 404 | 카테고리를 찾을 수 없습니다 |
| BOARD_101 | 409 | 이미 존재하는 게시판 ID입니다 |
| BOARD_102 | 409 | 이미 권한이 존재합니다 |

### 31.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Create** | `POST Create Board - 201` | `data.boardId` 저장 |
| | `POST Create Duplicate Board - 409` | `error.code = "BOARD_101"` |
| | `POST Create Board Category - 201` | `categorySeq` 저장 |
| | `POST Create Board Auth - 201` | 권한 생성 확인 |
| | `POST Create Article - 201` | `data.articleSeq` 저장 |
| **Read** | `GET Get Boards Page - 200` | 페이징 확인 |
| | `GET Get Board Detail - 200` | 상세 필드 확인 |
| | `GET Get Non-existent Board - 404` | `error.code = "BOARD_001"` |
| | `GET Get Articles Page - 200` | 게시글 페이징 확인 |
| | `GET Get Article Detail - 200` | 게시글 상세 확인 |
| | `GET Check Write Permission - 200` | 쓰기 권한 확인 |
| **Update** | `PUT Update Board - 200` | 수정 확인 |
| | `PUT Update Article - 200` | 수정 확인 |
| | `PUT Update Board Auth - 200` | 권한 수정 확인 |
| **Delete** | `DELETE Delete Article - 200` | 게시글 삭제 |
| | `DELETE Delete Board Auth - 200` | 권한 삭제 |
| | `DELETE Delete Board Category - 200` | 카테고리 삭제 |
| | `DELETE Delete Board - 200` | 게시판 삭제 |
| **Cleanup** | Cleanup Variables | `createdBoardId`, `createdArticleSeq` unset |

---

## 32. 이행 데이터 관리 (Trans Data)

### 32.1 엔드포인트

**TransDataController** — `/api/trans` | `@RequireMenuAccess(menuId="TRANS_DATA_EXEC")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/trans/page` | 이행 데이터 페이징 | 200 | READ |
| GET | `/api/trans/{tranSeq}/details` | 이행 데이터 상세 | 200 | READ |
| GET | `/api/trans/{tranSeq}/details/page` | 이행 상세 이력 페이징 | 200 | READ |

**TransDataFileController** — `/api/trans/trans-data-inqlist` | `@RequireMenuAccess(menuId="TRANS_DATA_LIST")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/trans/trans-data-inqlist/page` | 파일 검색 | 200 | READ |
| GET | `/api/trans/trans-data-inqlist/preview` | 파일 미리보기 | 200 | READ |
| POST | `/api/trans/trans-data-inqlist/upload` | SQL 파일 업로드 | 200 | WRITE |
| POST | `/api/trans/trans-data-inqlist/execute-sql` | SQL 실행 | 200 | WRITE |

**TransDataGenerationController** — `/api/trans/generation` | `@RequireMenuAccess(menuId="TRANS_DATA_POPUP")`

| HTTP | Path | 설명 | 상태 코드 | 권한 |
|------|------|------|-----------|------|
| GET | `/api/trans/generation/source` | 소스 데이터 목록 | 200 | READ |
| POST | `/api/trans/generation/execute` | 이행 데이터 생성 실행 | 200 | WRITE |

### 32.2 에러 코드

| 코드 | 상태 | 메시지 |
|------|------|--------|
| TRANS_001 | 404 | 이행 데이터를 찾을 수 없습니다 |

### 32.3 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보 |
| **Read** | `GET Get Trans Data Page - 200` | 페이징 구조 확인 |
| | `GET Get Trans Data Detail - 200` | 상세 확인 |
| | `GET Get Trans Files Page - 200` | 파일 목록 확인 |
| | `GET Preview Trans File - 200` | 파일 내용 확인 |
| | `GET Get Generation Source - 200` | 소스 목록 확인 |
| **Execute** | `POST Upload SQL Files - 200` | 파일 업로드 성공 (multipart) |
| | `POST Execute SQL File - 200` | SQL 실행 결과 확인 |
| | `POST Execute Transfer Generation - 200` | 이행 생성 결과 확인 |

---

## 33. WAS 프로퍼티 관리 (WAS Property)

### 33.1 엔드포인트

**WasPropertyController** — `/api/was/property`

| HTTP | Path | 설명 | 상태 코드 |
|------|------|------|-----------|
| GET | `/api/was/property` | 전체 WAS 프로퍼티 | 200 |
| GET | `/api/was/property/page` | 페이징 목록 | 200 |
| GET | `/api/was/property/{instanceId}/{groupId}/{propId}` | 프로퍼티 상세 | 200 |
| GET | `/api/was/property/instance/{instanceId}` | 인스턴스별 프로퍼티 | 200 |
| GET | `/api/was/property/instance/{instanceId}/export` | Excel 내보내기 | 200 |
| POST | `/api/was/property` | 프로퍼티 생성 | 201 |
| POST | `/api/was/property/compare` | 프로퍼티 비교 | 200 |
| POST | `/api/was/property/copy` | 프로퍼티 복사 | 200 |
| POST | `/api/was/property/batch` | 프로퍼티 일괄 저장 | 200 |
| PUT | `/api/was/property` | 프로퍼티 수정 | 200 |
| DELETE | `/api/was/property/{instanceId}/{groupId}/{propId}` | 프로퍼티 삭제 | 200 |

### 33.2 테스트 시나리오

| 폴더 | 요청 이름 | 검증 포인트 |
|------|-----------|------------|
| **Setup** | `POST Login - 200` | 세션 확보, 선행 인스턴스 확인 |
| **Create** | `POST Create WAS Property - 201` | PK 변수 저장 |
| | `POST Batch Save WAS Properties - 200` | 일괄 저장 확인 |
| **Read** | `GET Get WAS Properties By Instance - 200` | 인스턴스별 조회 |
| | `GET Get WAS Property Detail - 200` | 상세 필드 확인 |
| | `POST Compare WAS Properties - 200` | 비교 결과 확인 |
| | `GET Export Properties To Excel - 200` | Content-Type: application/vnd 확인 |
| **Update** | `PUT Update WAS Property - 200` | 수정 확인, `reason` 파라미터 필수 |
| | `POST Copy Properties - 200` | 복사 결과 확인 |
| **Delete** | `DELETE Delete WAS Property - 200` | 삭제 성공 |
| **Cleanup** | Cleanup Variables | 관련 변수 unset |

---

## 부록 A. 도메인-컬렉션 매핑표

| # | 도메인 | 컬렉션 파일 경로 | 엔드포인트 수 | 템플릿 |
|---|--------|-----------------|--------------|--------|
| 1 | User | `postman/user/` | 16 | A |
| 2 | Menu | `postman/menu/` | 15 | A |
| 3 | Role | `postman/role/` | 11 | A |
| 4 | Code | `postman/code/` | 31 | A |
| 5 | WAS | `postman/was/` | 27 | A |
| 6 | Property | `postman/property/` | 21 | A |
| 7 | XML Property | `postman/xmlproperty/` | 7 | D |
| 8 | Reload | `postman/reload/` | 4 | D |
| 9 | Interface (Org) | `postman/interface-org/` | 3 | A |
| 10 | Interface (Gateway) | `postman/interface-gateway/` | 6 | A |
| 11 | Interface (Transport) | `postman/interface-transport/` | 3 | A |
| 12 | Interface (Handler) | `postman/interface-handler/` | 3 | A |
| 13 | Interface (Listener-Connector) | `postman/interface-listener-connector/` | 6 | A |
| 14 | Interface (App Mapping) | `postman/interface-app-mapping/` | 5 | A |
| 15 | Interface | `postman/interface/` | 8 | A |
| 16 | Message | `postman/message/` | 10 | E |
| 17 | Trx | `postman/trx/` | 13 | E |
| 18 | Trx Stop | `postman/trx-stop/` | 3 | A |
| 19 | Message Test | `postman/message-test/` | 11 | D |
| 20 | Proxy Testdata | `postman/proxy-testdata/` | 16 | A |
| 21 | Message Instance | `postman/message-instance/` | 6 | B |
| 22 | Message Parsing | `postman/message-parsing/` | 3 | D |
| 23 | Validator | `postman/validator/` | 5 | A |
| 24 | Batch | `postman/batch/` | 17 | A |
| 25 | Error | `postman/error/` | 29 | A |
| 26 | Monitor | `postman/monitor/` | 8 | A |
| 27 | Access User | `postman/access-user/` | 4 | A |
| 28 | Audit / History | `postman/audit/` | 3 | B |
| 29 | Board | `postman/board/` | 36 | A+D |
| 30 | Trans Data | `postman/trans/` | 11 | A+D |
| 31 | WAS Property | `postman/was-property/` | 17 | A |

---

## 부록 B. 우선순위 가이드

구현 및 테스트 작성 우선순위는 다음 기준으로 결정한다:

| 우선순위 | 기준 | 도메인 |
|----------|------|--------|
| **P0 (필수)** | 시스템 기반 기능 | User, Role, Menu, Code |
| **P1 (높음)** | 인프라 관리 기능 | WAS, Property, XML Property, Reload |
| **P2 (중간)** | 핵심 업무 기능 | Message, Trx, Interface, Batch, Error |
| **P3 (낮음)** | 부가 기능 | Board, Trans, Monitor, Validator, Audit |

---

## 부록 C. 변수 체이닝 종합표

각 컬렉션에서 사용하는 주요 컬렉션 변수 목록이다.

| 도메인 | 생성 변수 | 설정 시점 | 사용 시점 | 해제 시점 |
|--------|----------|----------|----------|----------|
| User | `createdUserId` | Create User | Read/Update/Delete | Cleanup |
| Menu | `createdMenuId` | Create Menu | Read/Update/Delete | Cleanup |
| Role | `createdRoleId` | Create Role | Read/Update/Delete | Cleanup |
| Code | `createdCodeGroupId`, `createdCode` | Create | Read/Update/Delete | Cleanup |
| WAS | `createdWasGroupId`, `createdInstanceId` | Create | Read/Update/Delete | Cleanup |
| Property | `createdPropertyGroupId` | Create | Read/Update/Delete | Cleanup |
| XML Property | `createdFileName` | Create | Read/Update/Delete | Cleanup |
| Interface | `createdInterfaceId` | Create | Read/Update/Delete | Cleanup |
| Gateway | `createdGwId` | Create | Read/Delete | Cleanup |
| Message | `createdMessageId`, `createdFieldId` | Create | Read/Update/Delete | Cleanup |
| Trx | `createdTrxId` | Create | Read/Update/Delete | Cleanup |
| Batch | `createdBatchAppId` | Create | Read/Update/Delete | Cleanup |
| Error | `createdErrorCode` | Create | Read/Update/Delete | Cleanup |
| Monitor | `createdMonitorId` | Create | Read/Update/Delete | Cleanup |
| Board | `createdBoardId`, `createdArticleSeq` | Create | Read/Update/Delete | Cleanup |
| Validator | `createdValidatorId` | Create | Read/Update/Delete | Cleanup |
| Message Test | `createdTestSno` | Create | Read/Update/Delete | Cleanup |
| Proxy Testdata | `createdProxyTestSno` | Create | Read/Update/Delete | Cleanup |

---

*Last updated: 2026-02-25*
