# Package Structure

## 1. 개요

이 문서는 `spring-admin-v2` 프로젝트의 Java 패키지 구조를 정의한다. 모든 도메인은 `domain/{name}` 하위에 controller / service / mapper / dto 레이어로 수직 분할하며, 도메인 유형에 따라 세 가지 카테고리로 분류한다.

| 카테고리 | 설명 | 패키지 수 | Mapper |
|----------|------|-----------|--------|
| A. DDL-Backed | DB 테이블 기반 CRUD 도메인 | 33 | O |
| B. Runtime API | 파일/외부 API 기반 도메인 (DDL 없음) | 4 | X |
| C. 모니터링/통계 | 타 도메인 집계 전용 (DDL 없음) | 1 | X |
| **합계** | | **38** | |

### 1.1 전체 도메인 패키지 목록

| # | 패키지 | 영역 | 유형 | DDL 테이블 |
|---|--------|------|------|-----------|
| 1 | user | 시스템 관리 | A | FWK_USER |
| 2 | menu | 시스템 관리 | A | FWK_MENU, FWK_USER_MENU |
| 3 | role | 시스템 관리 | A | FWK_ROLE, FWK_ROLE_MENU |
| 4 | codegroup | 코드 관리 | A | FWK_CODE_GROUP |
| 5 | code | 코드 관리 | A | FWK_CODE |
| 6 | bizgroup | 인프라 관리 | A | FWK_BIZ_GROUP |
| 7 | wasgroup | 인프라 관리 | A | FWK_WAS_GROUP, FWK_WAS_GROUP_INSTANCE |
| 8 | wasinstance | 인프라 관리 | A | FWK_WAS_INSTANCE |
| 9 | property | 인프라 관리 | A | FWK_PROPERTY, FWK_PROPERTY_HISTORY |
| 10 | wasproperty | 인프라 관리 | A | FWK_WAS_PROPERTY, FWK_WAS_PROPERTY_HISTORY |
| 11 | org | 연계 관리 | A | FWK_ORG |
| 12 | gateway | 연계 관리 | A | FWK_GATEWAY |
| 13 | gwsystem | 연계 관리 | A | FWK_SYSTEM |
| 14 | transport | 연계 관리 | A | FWK_TRANSPORT |
| 15 | listener | 연계 관리 | A | FWK_WAS_LISTENER |
| 16 | listenertrx | 연계 관리 | A | FWK_LISTENER_TRX_MESSAGE, FWK_LISTENER_CONNECTOR_MAPPING |
| 17 | messagehandler | 전문 관리 | A | FWK_MESSAGE_HANDLER |
| 18 | message | 전문 관리 | A | FWK_MESSAGE |
| 19 | messagefield | 전문 관리 | A | FWK_MESSAGE_FIELD, FWK_MESSAGE_FIELD_HISTORY |
| 20 | transaction | 거래 관리 | A | FWK_TRX, FWK_TRX_HISTORY, FWK_TRX_STOP_HISTORY |
| 21 | trxmessage | 거래 관리 | A | FWK_TRX_MESSAGE, FWK_TRX_MESSAGE_HISTORY |
| 22 | validator | 거래 관리 | A | FWK_VALIDATOR |
| 23 | messagetest | 거래 관리 | A | FWK_MESSAGE_TEST |
| 24 | messageinstance | 거래 관리 | A | FWK_MESSAGE_INSTANCE |
| 25 | batch | 배치 관리 | A | FWK_BATCH_APP, FWK_BATCH_HIS, FWK_WAS_EXEC_BATCH |
| 26 | errorcode | 에러 관리 | A | FWK_ERROR, FWK_ERROR_DESC |
| 27 | errorhandle | 에러 관리 | A | FWK_ERROR_HANDLE_APP, FWK_HANDLE_APP, FWK_ERROR_HANDLE_HIS |
| 28 | errorhistory | 에러 관리 | A | FWK_ERROR_HIS |
| 29 | monitor | 모니터링 | A | FWK_MONITOR |
| 30 | adminhistory | 이력/로그 | A | — |
| 31 | transdata | 전송 데이터 | A | FWK_TRANS_DATA_HIS, FWK_TRANS_DATA_TIMES |
| 32 | board | 게시판 | A | FWK_BOARD, FWK_BOARD_AUTH, FWK_BOARD_CATEGORY |
| 33 | article | 게시판 | A | FWK_ARTICLE, FWK_ARTICLE_USER |
| 34 | xmlproperty | 인프라 관리 | B | — |
| 35 | reload | 인프라 관리 | B | — |
| 36 | messageparsing | 개발 관리 | B | — |
| 37 | proxyresponse | 거래 관리 | B | — |
| 38 | dashboard | 홈 | C | — |

### 1.2 공통 모듈

| 패키지 | 설명 |
|--------|------|
| global/config | @Configuration 클래스 (MyBatisConfig, AsyncConfig) |
| global/exception | 예외 핸들러 |
| global/dto | ApiResponse, ErrorDetail, DateRangeResult, ExcelColumnDefinition 등 공통 DTO |
| global/security | 인증/인가 (로그인 API 포함) |
| global/log | 로그 이벤트 (event, listener, mapper) |
| global/util | TraceIdUtil, ExcelExportUtil, DateRangeUtil, ScreenDateType 등 유틸리티 |
| global/filter | 서블릿 필터 |
| global/interceptor | 인터셉터 |
| global/typehandler | MyBatis TypeHandler |

---

## 2. 도메인 유형별 표준 구조

### 2.1 DDL-Backed 도메인 (A)

```
domain/{name}/
├── controller/
│   ├── {Name}Controller.java            ← REST API
│   └── {Name}PageController.java        ← Thymeleaf 뷰
├── service/
│   └── {Name}Service.java
├── mapper/
│   └── {Name}Mapper.java               ← MyBatis Mapper 인터페이스
└── dto/
    ├── request/
    │   ├── {Name}SearchRequestDTO.java
    │   ├── {Name}CreateRequestDTO.java
    │   └── {Name}UpdateRequestDTO.java
    └── response/
        ├── {Name}ResponseDTO.java
        ├── {Name}DetailResponseDTO.java     (선택)
        └── {Name}HistoryResponseDTO.java    (선택)

resources/mapper/{name}/
└── {Name}Mapper.xml                     ← MyBatis SQL XML
```

### 2.2 Runtime API 도메인 (B)

```
domain/{name}/
├── controller/
│   ├── {Name}Controller.java
│   └── {Name}PageController.java        (선택)
├── service/
│   └── {Name}Service.java
└── dto/
    ├── request/
    │   └── {Name}RequestDTO.java
    └── response/
        └── {Name}ResponseDTO.java
```

- DDL 없음 → `mapper/` 없음, Mapper XML 없음

### 2.3 모니터링/통계 도메인 (C)

```
domain/{name}/
├── controller/
│   ├── {Name}Controller.java
│   └── {Name}PageController.java
├── service/
│   └── {Name}Service.java               ← 타 도메인 Service 참조로 집계
└── dto/
    └── response/
        └── {Name}ResponseDTO.java
```

- DDL 없음 → `mapper/` 없음, Request DTO 없음

---

## 3. 파일 구조

### 3.1 도메인 패키지

> 기본 경로: `src/main/java/org/example/springadminv2/domain/`

```
domain/
│
│  ── A. DDL-Backed 도메인 (33) ─────────
│
│  ── 시스템 관리 ───────────────────────
│
├── user/                                   ← FWK_USER
│   ├── controller/
│   │   ├── UserController.java
│   │   └── UserPageController.java
│   ├── service/
│   │   └── UserService.java
│   ├── mapper/
│   │   └── UserMapper.java
│   └── dto/
│       ├── request/
│       │   ├── UserSearchRequestDTO.java
│       │   ├── UserCreateRequestDTO.java
│       │   └── UserUpdateRequestDTO.java
│       └── response/
│           ├── UserResponseDTO.java
│           └── UserDetailResponseDTO.java
│
├── menu/                                   ← FWK_MENU, FWK_USER_MENU
│   ├── controller/
│   │   ├── MenuController.java
│   │   └── MenuPageController.java
│   ├── service/
│   │   └── MenuService.java
│   ├── mapper/
│   │   └── MenuMapper.java
│   └── dto/
│       ├── request/
│       │   ├── MenuSearchRequestDTO.java
│       │   ├── MenuCreateRequestDTO.java
│       │   └── MenuUpdateRequestDTO.java
│       └── response/
│           ├── MenuResponseDTO.java
│           └── MenuTreeResponseDTO.java
│
├── role/                                   ← FWK_ROLE, FWK_ROLE_MENU
│   ├── controller/
│   │   ├── RoleController.java
│   │   └── RolePageController.java
│   ├── service/
│   │   └── RoleService.java
│   ├── mapper/
│   │   └── RoleMapper.java
│   └── dto/
│       ├── request/
│       │   ├── RoleSearchRequestDTO.java
│       │   ├── RoleCreateRequestDTO.java
│       │   └── RoleUpdateRequestDTO.java
│       └── response/
│           ├── RoleResponseDTO.java
│           └── RoleDetailResponseDTO.java
│
│  ── 코드 관리 ─────────────────────────
│
├── codegroup/                              ← FWK_CODE_GROUP
│   ├── controller/
│   │   ├── CodeGroupController.java
│   │   └── CodeGroupPageController.java
│   ├── service/
│   │   └── CodeGroupService.java
│   ├── mapper/
│   │   └── CodeGroupMapper.java
│   └── dto/
│       ├── request/
│       │   ├── CodeGroupSearchRequestDTO.java
│       │   ├── CodeGroupCreateRequestDTO.java
│       │   └── CodeGroupUpdateRequestDTO.java
│       └── response/
│           └── CodeGroupResponseDTO.java
│
├── code/                                   ← FWK_CODE
│   ├── controller/
│   │   ├── CodeController.java
│   │   └── CodePageController.java
│   ├── service/
│   │   └── CodeService.java
│   ├── mapper/
│   │   └── CodeMapper.java
│   └── dto/
│       ├── request/
│       │   ├── CodeSearchRequestDTO.java
│       │   ├── CodeCreateRequestDTO.java
│       │   └── CodeUpdateRequestDTO.java
│       └── response/
│           └── CodeResponseDTO.java
│
│  ── 인프라 관리 ───────────────────────
│
├── bizgroup/                               ← FWK_BIZ_GROUP
│   ├── controller/
│   │   ├── BizGroupController.java
│   │   └── BizGroupPageController.java
│   ├── service/
│   │   └── BizGroupService.java
│   ├── mapper/
│   │   └── BizGroupMapper.java
│   └── dto/
│       ├── request/
│       │   ├── BizGroupSearchRequestDTO.java
│       │   ├── BizGroupCreateRequestDTO.java
│       │   └── BizGroupUpdateRequestDTO.java
│       └── response/
│           └── BizGroupResponseDTO.java
│
├── wasgroup/                               ← FWK_WAS_GROUP, FWK_WAS_GROUP_INSTANCE
│   ├── controller/
│   │   ├── WasGroupController.java
│   │   └── WasGroupPageController.java
│   ├── service/
│   │   └── WasGroupService.java
│   ├── mapper/
│   │   └── WasGroupMapper.java
│   └── dto/
│       ├── request/
│       │   ├── WasGroupSearchRequestDTO.java
│       │   ├── WasGroupCreateRequestDTO.java
│       │   └── WasGroupUpdateRequestDTO.java
│       └── response/
│           ├── WasGroupResponseDTO.java
│           └── WasGroupDetailResponseDTO.java
│
├── wasinstance/                            ← FWK_WAS_INSTANCE
│   ├── controller/
│   │   ├── WasInstanceController.java
│   │   └── WasInstancePageController.java
│   ├── service/
│   │   └── WasInstanceService.java
│   ├── mapper/
│   │   └── WasInstanceMapper.java
│   └── dto/
│       ├── request/
│       │   ├── WasInstanceSearchRequestDTO.java
│       │   ├── WasInstanceCreateRequestDTO.java
│       │   └── WasInstanceUpdateRequestDTO.java
│       └── response/
│           ├── WasInstanceResponseDTO.java
│           └── WasInstanceDetailResponseDTO.java
│
├── property/                               ← FWK_PROPERTY, FWK_PROPERTY_HISTORY
│   ├── controller/
│   │   ├── PropertyController.java
│   │   └── PropertyPageController.java
│   ├── service/
│   │   └── PropertyService.java
│   ├── mapper/
│   │   └── PropertyMapper.java
│   └── dto/
│       ├── request/
│       │   ├── PropertySearchRequestDTO.java
│       │   ├── PropertyCreateRequestDTO.java
│       │   └── PropertyUpdateRequestDTO.java
│       └── response/
│           ├── PropertyResponseDTO.java
│           └── PropertyHistoryResponseDTO.java
│
├── wasproperty/                            ← FWK_WAS_PROPERTY, FWK_WAS_PROPERTY_HISTORY
│   ├── controller/
│   │   ├── WasPropertyController.java
│   │   └── WasPropertyPageController.java
│   ├── service/
│   │   └── WasPropertyService.java
│   ├── mapper/
│   │   └── WasPropertyMapper.java
│   └── dto/
│       ├── request/
│       │   ├── WasPropertySearchRequestDTO.java
│       │   ├── WasPropertyCreateRequestDTO.java
│       │   └── WasPropertyUpdateRequestDTO.java
│       └── response/
│           ├── WasPropertyResponseDTO.java
│           └── WasPropertyHistoryResponseDTO.java
│
│  ── 연계 관리 ─────────────────────────
│
├── org/                                    ← FWK_ORG
│   ├── controller/
│   │   ├── OrgController.java
│   │   └── OrgPageController.java
│   ├── service/
│   │   └── OrgService.java
│   ├── mapper/
│   │   └── OrgMapper.java
│   └── dto/
│       ├── request/
│       │   ├── OrgSearchRequestDTO.java
│       │   ├── OrgCreateRequestDTO.java
│       │   └── OrgUpdateRequestDTO.java
│       └── response/
│           └── OrgResponseDTO.java
│
├── gateway/                                ← FWK_GATEWAY
│   ├── controller/
│   │   ├── GatewayController.java
│   │   └── GatewayPageController.java
│   ├── service/
│   │   └── GatewayService.java
│   ├── mapper/
│   │   └── GatewayMapper.java
│   └── dto/
│       ├── request/
│       │   ├── GatewaySearchRequestDTO.java
│       │   ├── GatewayCreateRequestDTO.java
│       │   └── GatewayUpdateRequestDTO.java
│       └── response/
│           └── GatewayResponseDTO.java
│
├── gwsystem/                               ← FWK_SYSTEM
│   ├── controller/
│   │   ├── GwSystemController.java
│   │   └── GwSystemPageController.java
│   ├── service/
│   │   └── GwSystemService.java
│   ├── mapper/
│   │   └── GwSystemMapper.java
│   └── dto/
│       ├── request/
│       │   ├── GwSystemSearchRequestDTO.java
│       │   ├── GwSystemCreateRequestDTO.java
│       │   └── GwSystemUpdateRequestDTO.java
│       └── response/
│           └── GwSystemResponseDTO.java
│
├── transport/                              ← FWK_TRANSPORT
│   ├── controller/
│   │   ├── TransportController.java
│   │   └── TransportPageController.java
│   ├── service/
│   │   └── TransportService.java
│   ├── mapper/
│   │   └── TransportMapper.java
│   └── dto/
│       ├── request/
│       │   ├── TransportSearchRequestDTO.java
│       │   ├── TransportCreateRequestDTO.java
│       │   └── TransportUpdateRequestDTO.java
│       └── response/
│           └── TransportResponseDTO.java
│
├── listener/                               ← FWK_WAS_LISTENER
│   ├── controller/
│   │   ├── ListenerController.java
│   │   └── ListenerPageController.java
│   ├── service/
│   │   └── ListenerService.java
│   ├── mapper/
│   │   └── ListenerMapper.java
│   └── dto/
│       ├── request/
│       │   ├── ListenerSearchRequestDTO.java
│       │   ├── ListenerCreateRequestDTO.java
│       │   └── ListenerUpdateRequestDTO.java
│       └── response/
│           └── ListenerResponseDTO.java
│
├── listenertrx/                            ← FWK_LISTENER_TRX_MESSAGE, FWK_LISTENER_CONNECTOR_MAPPING
│   ├── controller/
│   │   ├── ListenerTrxController.java
│   │   └── ListenerTrxPageController.java
│   ├── service/
│   │   └── ListenerTrxService.java
│   ├── mapper/
│   │   └── ListenerTrxMapper.java
│   └── dto/
│       ├── request/
│       │   ├── ListenerTrxSearchRequestDTO.java
│       │   ├── ListenerTrxCreateRequestDTO.java
│       │   └── ListenerTrxUpdateRequestDTO.java
│       └── response/
│           └── ListenerTrxResponseDTO.java
│
│  ── 전문 관리 ─────────────────────────
│
├── messagehandler/                         ← FWK_MESSAGE_HANDLER
│   ├── controller/
│   │   ├── MessageHandlerController.java
│   │   └── MessageHandlerPageController.java
│   ├── service/
│   │   └── MessageHandlerService.java
│   ├── mapper/
│   │   └── MessageHandlerMapper.java
│   └── dto/
│       ├── request/
│       │   ├── MessageHandlerSearchRequestDTO.java
│       │   ├── MessageHandlerCreateRequestDTO.java
│       │   └── MessageHandlerUpdateRequestDTO.java
│       └── response/
│           └── MessageHandlerResponseDTO.java
│
├── message/                                ← FWK_MESSAGE
│   ├── controller/
│   │   ├── MessageController.java
│   │   └── MessagePageController.java
│   ├── service/
│   │   └── MessageService.java
│   ├── mapper/
│   │   └── MessageMapper.java
│   └── dto/
│       ├── request/
│       │   ├── MessageSearchRequestDTO.java
│       │   ├── MessageCreateRequestDTO.java
│       │   └── MessageUpdateRequestDTO.java
│       └── response/
│           ├── MessageResponseDTO.java
│           └── MessageDetailResponseDTO.java
│
├── messagefield/                           ← FWK_MESSAGE_FIELD, FWK_MESSAGE_FIELD_HISTORY
│   ├── controller/
│   │   ├── MessageFieldController.java
│   │   └── MessageFieldPageController.java
│   ├── service/
│   │   └── MessageFieldService.java
│   ├── mapper/
│   │   └── MessageFieldMapper.java
│   └── dto/
│       ├── request/
│       │   ├── MessageFieldSearchRequestDTO.java
│       │   ├── MessageFieldCreateRequestDTO.java
│       │   └── MessageFieldUpdateRequestDTO.java
│       └── response/
│           ├── MessageFieldResponseDTO.java
│           └── MessageFieldHistoryResponseDTO.java
│
│  ── 거래 관리 ─────────────────────────
│
├── transaction/                            ← FWK_TRX, FWK_TRX_HISTORY, FWK_TRX_STOP_HISTORY
│   ├── controller/
│   │   ├── TransactionController.java
│   │   └── TransactionPageController.java
│   ├── service/
│   │   └── TransactionService.java
│   ├── mapper/
│   │   └── TransactionMapper.java
│   └── dto/
│       ├── request/
│       │   ├── TransactionSearchRequestDTO.java
│       │   ├── TransactionCreateRequestDTO.java
│       │   └── TransactionUpdateRequestDTO.java
│       └── response/
│           ├── TransactionResponseDTO.java
│           ├── TransactionDetailResponseDTO.java
│           └── TransactionHistoryResponseDTO.java
│
├── trxmessage/                             ← FWK_TRX_MESSAGE, FWK_TRX_MESSAGE_HISTORY
│   ├── controller/
│   │   ├── TrxMessageController.java
│   │   └── TrxMessagePageController.java
│   ├── service/
│   │   └── TrxMessageService.java
│   ├── mapper/
│   │   └── TrxMessageMapper.java
│   └── dto/
│       ├── request/
│       │   ├── TrxMessageSearchRequestDTO.java
│       │   ├── TrxMessageCreateRequestDTO.java
│       │   └── TrxMessageUpdateRequestDTO.java
│       └── response/
│           ├── TrxMessageResponseDTO.java
│           └── TrxMessageHistoryResponseDTO.java
│
├── validator/                              ← FWK_VALIDATOR
│   ├── controller/
│   │   ├── ValidatorController.java
│   │   └── ValidatorPageController.java
│   ├── service/
│   │   └── ValidatorService.java
│   ├── mapper/
│   │   └── ValidatorMapper.java
│   └── dto/
│       ├── request/
│       │   ├── ValidatorSearchRequestDTO.java
│       │   ├── ValidatorCreateRequestDTO.java
│       │   └── ValidatorUpdateRequestDTO.java
│       └── response/
│           └── ValidatorResponseDTO.java
│
├── messagetest/                            ← FWK_MESSAGE_TEST
│   ├── controller/
│   │   ├── MessageTestController.java
│   │   └── MessageTestPageController.java
│   ├── service/
│   │   └── MessageTestService.java
│   ├── mapper/
│   │   └── MessageTestMapper.java
│   └── dto/
│       ├── request/
│       │   ├── MessageTestSearchRequestDTO.java
│       │   └── MessageTestCreateRequestDTO.java
│       └── response/
│           └── MessageTestResponseDTO.java
│
├── messageinstance/                        ← FWK_MESSAGE_INSTANCE
│   ├── controller/
│   │   ├── MessageInstanceController.java
│   │   └── MessageInstancePageController.java
│   ├── service/
│   │   └── MessageInstanceService.java
│   ├── mapper/
│   │   └── MessageInstanceMapper.java
│   └── dto/
│       ├── request/
│       │   └── MessageInstanceSearchRequestDTO.java
│       └── response/
│           └── MessageInstanceResponseDTO.java
│
│  ── 배치 관리 ─────────────────────────
│
├── batch/                                  ← FWK_BATCH_APP, FWK_BATCH_HIS, FWK_WAS_EXEC_BATCH
│   ├── controller/
│   │   ├── BatchController.java
│   │   └── BatchPageController.java
│   ├── service/
│   │   └── BatchService.java
│   ├── mapper/
│   │   └── BatchMapper.java
│   └── dto/
│       ├── request/
│       │   ├── BatchSearchRequestDTO.java
│       │   ├── BatchCreateRequestDTO.java
│       │   └── BatchUpdateRequestDTO.java
│       └── response/
│           ├── BatchResponseDTO.java
│           └── BatchHistoryResponseDTO.java
│
│  ── 에러 관리 ─────────────────────────
│
├── errorcode/                              ← FWK_ERROR, FWK_ERROR_DESC
│   ├── controller/
│   │   ├── ErrorCodeController.java
│   │   └── ErrorCodePageController.java
│   ├── service/
│   │   └── ErrorCodeService.java
│   ├── mapper/
│   │   └── ErrorCodeMapper.java
│   └── dto/
│       ├── request/
│       │   ├── ErrorCodeSearchRequestDTO.java
│       │   ├── ErrorCodeCreateRequestDTO.java
│       │   └── ErrorCodeUpdateRequestDTO.java
│       └── response/
│           ├── ErrorCodeResponseDTO.java
│           └── ErrorCodeDetailResponseDTO.java
│
├── errorhandle/                            ← FWK_ERROR_HANDLE_APP, FWK_HANDLE_APP, FWK_ERROR_HANDLE_HIS
│   ├── controller/
│   │   ├── ErrorHandleController.java
│   │   └── ErrorHandlePageController.java
│   ├── service/
│   │   └── ErrorHandleService.java
│   ├── mapper/
│   │   └── ErrorHandleMapper.java
│   └── dto/
│       ├── request/
│       │   ├── ErrorHandleSearchRequestDTO.java
│       │   ├── ErrorHandleCreateRequestDTO.java
│       │   └── ErrorHandleUpdateRequestDTO.java
│       └── response/
│           ├── ErrorHandleResponseDTO.java
│           └── ErrorHandleHistoryResponseDTO.java
│
├── errorhistory/                           ← FWK_ERROR_HIS
│   ├── controller/
│   │   ├── ErrorHistoryController.java
│   │   └── ErrorHistoryPageController.java
│   ├── service/
│   │   └── ErrorHistoryService.java
│   ├── mapper/
│   │   └── ErrorHistoryMapper.java
│   └── dto/
│       ├── request/
│       │   └── ErrorHistorySearchRequestDTO.java
│       └── response/
│           └── ErrorHistoryResponseDTO.java
│
│  ── 모니터링 ──────────────────────────
│
├── monitor/                                ← FWK_MONITOR
│   ├── controller/
│   │   ├── MonitorController.java
│   │   └── MonitorPageController.java
│   ├── service/
│   │   └── MonitorService.java
│   ├── mapper/
│   │   └── MonitorMapper.java
│   └── dto/
│       ├── request/
│       │   ├── MonitorSearchRequestDTO.java
│       │   ├── MonitorCreateRequestDTO.java
│       │   └── MonitorUpdateRequestDTO.java
│       └── response/
│           └── MonitorResponseDTO.java
│
│  ── 이력/로그 ─────────────────────────
│
├── adminhistory/
│   ├── controller/
│   │   ├── AdminHistoryController.java
│   │   └── AdminHistoryPageController.java
│   ├── service/
│   │   └── AdminHistoryService.java
│   ├── mapper/
│   │   └── AdminHistoryMapper.java
│   └── dto/
│       ├── request/
│       │   └── AdminHistorySearchRequestDTO.java
│       └── response/
│           └── AdminHistoryResponseDTO.java
│
│  ── 전송 데이터 ───────────────────────
│
├── transdata/                              ← FWK_TRANS_DATA_HIS, FWK_TRANS_DATA_TIMES
│   ├── controller/
│   │   ├── TransDataController.java
│   │   └── TransDataPageController.java
│   ├── service/
│   │   └── TransDataService.java
│   ├── mapper/
│   │   └── TransDataMapper.java
│   └── dto/
│       ├── request/
│       │   └── TransDataSearchRequestDTO.java
│       └── response/
│           ├── TransDataResponseDTO.java
│           └── TransDataDetailResponseDTO.java
│
│  ── 게시판 ────────────────────────────
│
├── board/                                  ← FWK_BOARD, FWK_BOARD_AUTH, FWK_BOARD_CATEGORY
│   ├── controller/
│   │   ├── BoardController.java
│   │   └── BoardPageController.java
│   ├── service/
│   │   └── BoardService.java
│   ├── mapper/
│   │   └── BoardMapper.java
│   └── dto/
│       ├── request/
│       │   ├── BoardSearchRequestDTO.java
│       │   ├── BoardCreateRequestDTO.java
│       │   └── BoardUpdateRequestDTO.java
│       └── response/
│           ├── BoardResponseDTO.java
│           └── BoardDetailResponseDTO.java
│
├── article/                                ← FWK_ARTICLE, FWK_ARTICLE_USER
│   ├── controller/
│   │   ├── ArticleController.java
│   │   └── ArticlePageController.java
│   ├── service/
│   │   └── ArticleService.java
│   ├── mapper/
│   │   └── ArticleMapper.java
│   └── dto/
│       ├── request/
│       │   ├── ArticleSearchRequestDTO.java
│       │   ├── ArticleCreateRequestDTO.java
│       │   └── ArticleUpdateRequestDTO.java
│       └── response/
│           ├── ArticleResponseDTO.java
│           └── ArticleDetailResponseDTO.java
│
│  ── B. Runtime API 도메인 (4) ─────────
│
├── xmlproperty/                            ← 파일 기반 XML 속성
│   ├── controller/
│   │   ├── XmlPropertyController.java
│   │   └── XmlPropertyPageController.java
│   ├── service/
│   │   └── XmlPropertyService.java
│   └── dto/
│       ├── request/
│       │   └── XmlPropertyUpdateRequestDTO.java
│       └── response/
│           └── XmlPropertyResponseDTO.java
│
├── reload/                                 ← WAS 캐시/설정 재로드
│   ├── controller/
│   │   └── ReloadController.java
│   ├── service/
│   │   └── ReloadService.java
│   └── dto/
│       ├── request/
│       │   └── ReloadRequestDTO.java
│       └── response/
│           └── ReloadResponseDTO.java
│
├── messageparsing/                         ← 전문 파싱/조립 테스트
│   ├── controller/
│   │   ├── MessageParsingController.java
│   │   └── MessageParsingPageController.java
│   ├── service/
│   │   └── MessageParsingService.java
│   └── dto/
│       ├── request/
│       │   └── MessageParsingRequestDTO.java
│       └── response/
│           └── MessageParsingResponseDTO.java
│
├── proxyresponse/                          ← 대응답 대행
│   ├── controller/
│   │   ├── ProxyResponseController.java
│   │   └── ProxyResponsePageController.java
│   ├── service/
│   │   └── ProxyResponseService.java
│   └── dto/
│       ├── request/
│       │   ├── ProxyResponseCreateRequestDTO.java
│       │   └── ProxyResponseUpdateRequestDTO.java
│       └── response/
│           └── ProxyResponseResponseDTO.java
│
│  ── C. 모니터링/통계 도메인 (1) ───────
│
└── dashboard/                              ← 대시보드 집계
    ├── controller/
    │   ├── DashboardController.java
    │   └── DashboardPageController.java
    ├── service/
    │   └── DashboardService.java
    └── dto/
        └── response/
            └── DashboardResponseDTO.java
```

### 3.2 공통 모듈

> 기본 경로: `src/main/java/org/example/springadminv2/global/`

```
global/
├── config/                                 ← @Configuration (MyBatisConfig, AsyncConfig)
├── exception/                              ← 예외 핸들러
├── dto/                                    ← ApiResponse, ErrorDetail, DateRangeResult, ExcelColumnDefinition
├── security/                               ← 인증/인가
├── log/                                    ← 로그 이벤트
│   ├── event/                              ← LogEvent, AccessLogEvent, AuditLogEvent, SecurityLogEvent
│   ├── listener/                           ← AccessLogEventListener, SecurityLogEventListener
│   └── mapper/                             ← AccessLogMapper
├── util/                                   ← TraceIdUtil, ExcelExportUtil, DateRangeUtil, ScreenDateType
└── typehandler/                            ← (설정 완료, 구현 예정)
```

### 3.3 Mapper XML

> 기본 경로: `src/main/resources/mapper/`
> DB별 디렉터리로 분리되며, `mybatis.mapper-locations`가 프로파일에 따라 경로를 선택한다.
> - Oracle: `classpath:mapper/oracle/**/*.xml`
> - MySQL: `classpath:mapper/mysql/**/*.xml`

```
mapper/
├── oracle/                                 ← Oracle 전용 SQL
│   ├── log/          └── AccessLogMapper.xml
│   ├── security/     └── AuthorityMapper.xml
│   ├── user/         └── UserMapper.xml
│   ├── menu/         └── MenuMapper.xml
│   ├── role/         └── RoleMapper.xml
│   ├── codegroup/    └── CodeGroupMapper.xml
│   ├── code/         └── CodeMapper.xml
│   ├── bizgroup/     └── BizGroupMapper.xml
│   ├── wasgroup/     └── WasGroupMapper.xml
│   ├── wasinstance/  └── WasInstanceMapper.xml
│   ├── property/     └── PropertyMapper.xml
│   ├── wasproperty/  └── WasPropertyMapper.xml
│   ├── org/          └── OrgMapper.xml
│   ├── gateway/      └── GatewayMapper.xml
│   ├── gwsystem/     └── GwSystemMapper.xml
│   ├── transport/    └── TransportMapper.xml
│   ├── listener/     └── ListenerMapper.xml
│   ├── listenertrx/  └── ListenerTrxMapper.xml
│   ├── messagehandler/ └── MessageHandlerMapper.xml
│   ├── message/      └── MessageMapper.xml
│   ├── messagefield/ └── MessageFieldMapper.xml
│   ├── transaction/  └── TransactionMapper.xml
│   ├── trxmessage/   └── TrxMessageMapper.xml
│   ├── validator/    └── ValidatorMapper.xml
│   ├── messagetest/  └── MessageTestMapper.xml
│   ├── messageinstance/ └── MessageInstanceMapper.xml
│   ├── batch/        └── BatchMapper.xml
│   ├── errorcode/    └── ErrorCodeMapper.xml
│   ├── errorhandle/  └── ErrorHandleMapper.xml
│   ├── errorhistory/ └── ErrorHistoryMapper.xml
│   ├── monitor/      └── MonitorMapper.xml
│   ├── adminhistory/ └── AdminHistoryMapper.xml
│   ├── transdata/    └── TransDataMapper.xml
│   ├── board/        └── BoardMapper.xml
│   └── article/      └── ArticleMapper.xml
│
└── mysql/                                  ← MySQL 전용 SQL
    ├── log/          └── AccessLogMapper.xml
    ├── security/     └── AuthorityMapper.xml
    ├── user/         └── UserMapper.xml
    ├── menu/         └── MenuMapper.xml
    ├── role/         └── RoleMapper.xml
    │   ... (oracle과 동일 구조)
    ├── board/        └── BoardMapper.xml
    └── article/      └── ArticleMapper.xml
```

---

*Last updated: 2026-02-26*
