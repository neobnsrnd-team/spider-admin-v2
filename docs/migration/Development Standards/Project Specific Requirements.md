# Project Specific Requirements

## 1. 개요

다른 개발 표준 문서에서 다루지 않는 이 프로젝트 고유의 DB 스키마, 도메인 규칙, 설정 규약을 정의한다.

| 항목 | 관련 문서 |
|------|----------|
| 메뉴 기반 인가 (MenuGuard, @PreAuthorize) | Authentication & Authorization 5-6절 |
| DB Schema (USER_MENU, ROLE_MENU, MENU) | Authentication & Authorization 2절 |
| DB별 SQL 분기 (databaseId) | Multi-DB 3절 |
| 예외 처리 (ErrorType, BusinessException) | Exception Handling 2절 |
| 보안 설정 (CSRF, 세션, 비밀번호) | Security |

이 문서는 위 문서들이 다루는 범용 패턴이 아닌, **프로젝트 특수 구성**에 집중한다.

---

## 2. 메뉴 계층 구조

> 메뉴 기반 인가(MenuGuard, @PreAuthorize)는 Authentication & Authorization 5-6절 참고.
> DB Schema(USER_MENU, ROLE_MENU, MENU)는 Authentication & Authorization 2절 참고.

### 2.1 MENU 트리 규칙

`MENU` 테이블은 자기참조 계층 구조를 가진다.

| 컬럼 | 역할 | 규칙 |
|------|------|------|
| `MENU_ID` | 메뉴 고유 ID | PK |
| `PRIOR_MENU_ID` | 부모 메뉴 ID | `'*'` = 루트 메뉴 (최상위) |
| `USE_YN` | 사용 여부 | `'Y'`인 메뉴만 트리에 포함 |
| `DISPLAY_YN` | 표시 여부 | `'Y'`인 메뉴만 화면에 렌더링 |
| `SORT_ORDER` | 형제 간 정렬 순서 | 숫자 오름차순 |

### 2.2 계층 조회 SQL

Oracle 환경에서는 `CONNECT BY`로 트리를 탐색한다.

```sql
SELECT MENU_ID, MENU_NAME, PRIOR_MENU_ID, LEVEL AS LEV,
       SUBSTR(SYS_CONNECT_BY_PATH(MENU_NAME, ' > '), 4) AS MENU_PATH
FROM MENU
START WITH PRIOR_MENU_ID = '*'
CONNECT BY PRIOR MENU_ID = PRIOR_MENU_ID
AND USE_YN = 'Y'
ORDER SIBLINGS BY SORT_ORDER
```

> MySQL에서는 `WITH RECURSIVE` CTE로 대체한다. DB별 SQL 분기는 Multi-DB 3절 참고.

### 2.3 LEAF_YN 판별

리프 노드(자식이 없는 메뉴)는 동적으로 판별한다.

```sql
NVL((SELECT 'N' FROM MENU B
     WHERE A.MENU_ID = B.PRIOR_MENU_ID AND ROWNUM = 1), 'Y') AS LEAF_YN
```

- 자식이 있으면 `'N'`, 없으면 `'Y'`

### 2.4 즐겨찾기 메뉴

`USER_MENU.FAVOR_MENU_ORDER` 컬럼으로 즐겨찾기 순서를 관리한다.

| 규칙 | 설명 |
|------|------|
| `FAVOR_MENU_ORDER > 0` | 즐겨찾기 등록된 메뉴 |
| `FAVOR_MENU_ORDER = 0` 또는 NULL | 즐겨찾기 아님 |
| 정렬 | `ORDER BY FAVOR_MENU_ORDER ASC` |

---

## 3. 타임스탬프 규약

### 3.1 표준 형식

| 구분 | 형식 | 길이 | 예시 |
|------|------|------|------|
| DB 컬럼 | `VARCHAR(14)` | 14자 | `20260224153045` |
| Java 포맷 | `yyyyMMddHHmmss` | — | `DateTimeFormatter.ofPattern("yyyyMMddHHmmss")` |
| Oracle SQL | `YYYYMMDDHH24MISS` | — | `TO_CHAR(SYSDATE, 'YYYYMMDDHH24MISS')` |

### 3.2 적용 범위

모든 `*_DTIME` 컬럼(예: `LAST_UPDATE_DTIME`, `CREATED_DTIME`)에 이 형식을 사용한다.

`"yyyy-MM-dd HH:mm:ss"` 등 다른 형식을 사용하지 않는다.

---

## 4. Reload 타입

시스템 설정 갱신을 위한 8종 Reload 타입이다.

| 타입 | 코드 | 설명 |
|------|------|------|
| TRX | `trx` | 거래 정보 |
| MENU | `menu` | 메뉴 정보 |
| REQUEST_APP_MAPPING | `request_app_mapping` | 요청처리 APP 맵핑 정보 |
| MANAGE_MONITOR | `manage_monitor` | 관리자 모니터링 정보 |
| CODE | `code` | 코드 정보 |
| BATCH | `batch_reload` | 배치 정보 |
| SERVICE | `service_reload_all` | 서비스 정보 |
| ERROR | `error` | 오류 코드 정보 |

---

## 5. 배치 설정 규칙

`BATCH_APP` 테이블의 주요 설정 항목이다.

| 항목 | 설명 | DB 컬럼 |
|------|------|---------|
| CRON_TEXT | 스케줄 표현식 | `BATCH_APP.CRON_TEXT` |
| RETRYABLE_YN | 재시도 가능 여부 (Y/N) | `BATCH_APP.RETRYABLE_YN` |
| PER_WAS_YN | WAS 인스턴스별 개별 실행 여부 | `BATCH_APP.PER_WAS_YN` |
| IMPORTANT_TYPE | 중요도 분류 | `BATCH_APP.IMPORTANT_TYPE` |
| PRE_BATCH_APP_ID | 선행 배치 의존성 | `BATCH_APP.PRE_BATCH_APP_ID` |

---

## 6. 코드 그룹 핵심 ID

| 코드 그룹 ID | 설명 |
|-------------|------|
| FR20001 | 언어코드 (Language Code) |
| FR90009 | 오류레벨 코드 |
| FR00001 | 인스턴스코드 (WAS 인스턴스 ID 매핑) |

---

## 7. 에러 코드 접두사 (레거시)

레거시 에러 테이블의 기존 에러 코드 접두사 체계이다.

> 새 코드에서는 ErrorType enum으로 예외를 처리한다. Exception Handling 2절 참고.

| 접두사 | 범주 | 설명 |
|--------|------|------|
| FRM | Framework Message | 통신, 파싱, 전문 변환 오류 |
| FRU | Framework UI/Web | 웹 메시지 처리 오류 |
| FRC | Framework Cross | 크로스 시스템 검증 오류 |
| FRX | Framework Exception | 보안/예외 오류 |

---

## 8. 체크리스트

```
□ 1. 타임스탬프 컬럼이 VARCHAR(14) + yyyyMMddHHmmss 형식인지 확인
      → "yyyy-MM-dd HH:mm:ss" 등 다른 형식 미사용

□ 2. 메뉴 추가 시 MENU 테이블에 PRIOR_MENU_ID, USE_YN, DISPLAY_YN, SORT_ORDER 설정
      → PRIOR_MENU_ID = '*'이면 루트 메뉴

□ 3. Reload 타입 추가 시 ReloadType enum 업데이트

□ 4. 배치 등록 시 BATCH_APP 필수 항목 확인
      → CRON_TEXT, RETRYABLE_YN, PER_WAS_YN

□ 5. 레거시 에러 코드(FRM/FRU/FRC/FRX)는 레거시 에러 테이블 전용
      → 새 코드에서는 BusinessException + ErrorType 사용
```

---

*Last updated: 2026-02-24*
