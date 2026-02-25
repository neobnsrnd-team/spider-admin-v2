src/main/java/org/example/springadminv2/                                                                                                                              
├── SpringAdminV2Application.java
│
├── domain/
│   │
│   │  ┌─────────────────────────────────────┐
│   │  │  A. DDL-Backed 도메인 (35개)         │
│   │  └─────────────────────────────────────┘
│   │
│   │  ── 시스템 관리 ──────────────────────────
│   │
│   ├── user/                                   ← FWK_USER
│   │   ├── controller/
│   │   │   ├── UserController.java
│   │   │   └── UserPageController.java
│   │   ├── service/
│   │   │   └── UserService.java
│   │   ├── mapper/
│   │   │   └── UserMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── UserSearchRequestDTO.java
│   │       │   ├── UserCreateRequestDTO.java
│   │       │   └── UserUpdateRequestDTO.java
│   │       └── response/
│   │           ├── UserResponseDTO.java
│   │           └── UserDetailResponseDTO.java
│   │
│   ├── menu/                                   ← FWK_MENU, FWK_USER_MENU
│   │   ├── controller/
│   │   │   ├── MenuController.java
│   │   │   └── MenuPageController.java
│   │   ├── service/
│   │   │   └── MenuService.java
│   │   ├── mapper/
│   │   │   └── MenuMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── MenuSearchRequestDTO.java
│   │       │   ├── MenuCreateRequestDTO.java
│   │       │   └── MenuUpdateRequestDTO.java
│   │       └── response/
│   │           ├── MenuResponseDTO.java
│   │           └── MenuTreeResponseDTO.java
│   │
│   ├── role/                                   ← FWK_ROLE, FWK_ROLE_MENU
│   │   ├── controller/
│   │   │   ├── RoleController.java
│   │   │   └── RolePageController.java
│   │   ├── service/
│   │   │   └── RoleService.java
│   │   ├── mapper/
│   │   │   └── RoleMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── RoleSearchRequestDTO.java
│   │       │   ├── RoleCreateRequestDTO.java
│   │       │   └── RoleUpdateRequestDTO.java
│   │       └── response/
│   │           ├── RoleResponseDTO.java
│   │           └── RoleDetailResponseDTO.java
│   │
│   │  ── 코드 관리 ──────────────────────────
│   │
│   ├── codegroup/                              ← FWK_CODE_GROUP
│   │   ├── controller/
│   │   │   ├── CodeGroupController.java
│   │   │   └── CodeGroupPageController.java
│   │   ├── service/
│   │   │   └── CodeGroupService.java
│   │   ├── mapper/
│   │   │   └── CodeGroupMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── CodeGroupSearchRequestDTO.java
│   │       │   ├── CodeGroupCreateRequestDTO.java
│   │       │   └── CodeGroupUpdateRequestDTO.java
│   │       └── response/
│   │           └── CodeGroupResponseDTO.java
│   │
│   ├── code/                                   ← FWK_CODE
│   │   ├── controller/
│   │   │   ├── CodeController.java
│   │   │   └── CodePageController.java
│   │   ├── service/
│   │   │   └── CodeService.java
│   │   ├── mapper/
│   │   │   └── CodeMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── CodeSearchRequestDTO.java
│   │       │   ├── CodeCreateRequestDTO.java
│   │       │   └── CodeUpdateRequestDTO.java
│   │       └── response/
│   │           └── CodeResponseDTO.java
│   │
│   │  ── 인프라 관리 ──────────────────────────
│   │
│   ├── bizgroup/                               ← FWK_BIZ_GROUP
│   │   ├── controller/
│   │   │   ├── BizGroupController.java
│   │   │   └── BizGroupPageController.java
│   │   ├── service/
│   │   │   └── BizGroupService.java
│   │   ├── mapper/
│   │   │   └── BizGroupMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── BizGroupSearchRequestDTO.java
│   │       │   ├── BizGroupCreateRequestDTO.java
│   │       │   └── BizGroupUpdateRequestDTO.java
│   │       └── response/
│   │           └── BizGroupResponseDTO.java
│   │
│   ├── wasgroup/                               ← FWK_WAS_GROUP, FWK_WAS_GROUP_INSTANCE
│   │   ├── controller/
│   │   │   ├── WasGroupController.java
│   │   │   └── WasGroupPageController.java
│   │   ├── service/
│   │   │   └── WasGroupService.java
│   │   ├── mapper/
│   │   │   └── WasGroupMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── WasGroupSearchRequestDTO.java
│   │       │   ├── WasGroupCreateRequestDTO.java
│   │       │   └── WasGroupUpdateRequestDTO.java
│   │       └── response/
│   │           ├── WasGroupResponseDTO.java
│   │           └── WasGroupDetailResponseDTO.java
│   │
│   ├── wasinstance/                            ← FWK_WAS_INSTANCE
│   │   ├── controller/
│   │   │   ├── WasInstanceController.java
│   │   │   └── WasInstancePageController.java
│   │   ├── service/
│   │   │   └── WasInstanceService.java
│   │   ├── mapper/
│   │   │   └── WasInstanceMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── WasInstanceSearchRequestDTO.java
│   │       │   ├── WasInstanceCreateRequestDTO.java
│   │       │   └── WasInstanceUpdateRequestDTO.java
│   │       └── response/
│   │           ├── WasInstanceResponseDTO.java
│   │           └── WasInstanceDetailResponseDTO.java
│   │
│   ├── property/                               ← FWK_PROPERTY, FWK_PROPERTY_HISTORY
│   │   ├── controller/
│   │   │   ├── PropertyController.java
│   │   │   └── PropertyPageController.java
│   │   ├── service/
│   │   │   └── PropertyService.java
│   │   ├── mapper/
│   │   │   └── PropertyMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── PropertySearchRequestDTO.java
│   │       │   ├── PropertyCreateRequestDTO.java
│   │       │   └── PropertyUpdateRequestDTO.java
│   │       └── response/
│   │           ├── PropertyResponseDTO.java
│   │           └── PropertyHistoryResponseDTO.java
│   │
│   ├── wasproperty/                            ← FWK_WAS_PROPERTY, FWK_WAS_PROPERTY_HISTORY
│   │   ├── controller/
│   │   │   ├── WasPropertyController.java
│   │   │   └── WasPropertyPageController.java
│   │   ├── service/
│   │   │   └── WasPropertyService.java
│   │   ├── mapper/
│   │   │   └── WasPropertyMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── WasPropertySearchRequestDTO.java
│   │       │   ├── WasPropertyCreateRequestDTO.java
│   │       │   └── WasPropertyUpdateRequestDTO.java
│   │       └── response/
│   │           ├── WasPropertyResponseDTO.java
│   │           └── WasPropertyHistoryResponseDTO.java
│   │
│   │  ── 대외기관 / 채널 관리 ──────────────────
│   │
│   ├── org/                                    ← FWK_ORG
│   │   ├── controller/
│   │   │   ├── OrgController.java
│   │   │   └── OrgPageController.java
│   │   ├── service/
│   │   │   └── OrgService.java
│   │   ├── mapper/
│   │   │   └── OrgMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── OrgSearchRequestDTO.java
│   │       │   ├── OrgCreateRequestDTO.java
│   │       │   └── OrgUpdateRequestDTO.java
│   │       └── response/
│   │           └── OrgResponseDTO.java
│   │
│   ├── gateway/                                ← FWK_GATEWAY
│   │   ├── controller/
│   │   │   ├── GatewayController.java
│   │   │   └── GatewayPageController.java
│   │   ├── service/
│   │   │   └── GatewayService.java
│   │   ├── mapper/
│   │   │   └── GatewayMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── GatewaySearchRequestDTO.java
│   │       │   ├── GatewayCreateRequestDTO.java
│   │       │   └── GatewayUpdateRequestDTO.java
│   │       └── response/
│   │           └── GatewayResponseDTO.java
│   │
│   ├── gwsystem/                               ← FWK_SYSTEM
│   │   ├── controller/
│   │   │   ├── GwSystemController.java
│   │   │   └── GwSystemPageController.java
│   │   ├── service/
│   │   │   └── GwSystemService.java
│   │   ├── mapper/
│   │   │   └── GwSystemMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── GwSystemSearchRequestDTO.java
│   │       │   ├── GwSystemCreateRequestDTO.java
│   │       │   └── GwSystemUpdateRequestDTO.java
│   │       └── response/
│   │           └── GwSystemResponseDTO.java
│   │
│   ├── transport/                              ← FWK_TRANSPORT
│   │   ├── controller/
│   │   │   ├── TransportController.java
│   │   │   └── TransportPageController.java
│   │   ├── service/
│   │   │   └── TransportService.java
│   │   ├── mapper/
│   │   │   └── TransportMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── TransportSearchRequestDTO.java
│   │       │   ├── TransportCreateRequestDTO.java
│   │       │   └── TransportUpdateRequestDTO.java
│   │       └── response/
│   │           └── TransportResponseDTO.java
│   │
│   ├── listener/                               ← FWK_WAS_LISTENER
│   │   ├── controller/
│   │   │   ├── ListenerController.java
│   │   │   └── ListenerPageController.java
│   │   ├── service/
│   │   │   └── ListenerService.java
│   │   ├── mapper/
│   │   │   └── ListenerMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── ListenerSearchRequestDTO.java
│   │       │   ├── ListenerCreateRequestDTO.java
│   │       │   └── ListenerUpdateRequestDTO.java
│   │       └── response/
│   │           └── ListenerResponseDTO.java
│   │
│   ├── listenertrx/                            ← FWK_LISTENER_TRX_MESSAGE, FWK_LISTENER_CONNECTOR_MAPPING
│   │   ├── controller/
│   │   │   ├── ListenerTrxController.java
│   │   │   └── ListenerTrxPageController.java
│   │   ├── service/
│   │   │   └── ListenerTrxService.java
│   │   ├── mapper/
│   │   │   └── ListenerTrxMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── ListenerTrxSearchRequestDTO.java
│   │       │   ├── ListenerTrxCreateRequestDTO.java
│   │       │   └── ListenerTrxUpdateRequestDTO.java
│   │       └── response/
│   │           └── ListenerTrxResponseDTO.java
│   │
│   │  ── 전문 관리 ──────────────────────────
│   │
│   ├── messagehandler/                         ← FWK_MESSAGE_HANDLER
│   │   ├── controller/
│   │   │   ├── MessageHandlerController.java
│   │   │   └── MessageHandlerPageController.java
│   │   ├── service/
│   │   │   └── MessageHandlerService.java
│   │   ├── mapper/
│   │   │   └── MessageHandlerMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── MessageHandlerSearchRequestDTO.java
│   │       │   ├── MessageHandlerCreateRequestDTO.java
│   │       │   └── MessageHandlerUpdateRequestDTO.java
│   │       └── response/
│   │           └── MessageHandlerResponseDTO.java
│   │
│   ├── message/                                ← FWK_MESSAGE
│   │   ├── controller/
│   │   │   ├── MessageController.java
│   │   │   └── MessagePageController.java
│   │   ├── service/
│   │   │   └── MessageService.java
│   │   ├── mapper/
│   │   │   └── MessageMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── MessageSearchRequestDTO.java
│   │       │   ├── MessageCreateRequestDTO.java
│   │       │   └── MessageUpdateRequestDTO.java
│   │       └── response/
│   │           ├── MessageResponseDTO.java
│   │           └── MessageDetailResponseDTO.java
│   │
│   ├── messagefield/                           ← FWK_MESSAGE_FIELD, FWK_MESSAGE_FIELD_HISTORY
│   │   ├── controller/
│   │   │   ├── MessageFieldController.java
│   │   │   └── MessageFieldPageController.java
│   │   ├── service/
│   │   │   └── MessageFieldService.java
│   │   ├── mapper/
│   │   │   └── MessageFieldMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── MessageFieldSearchRequestDTO.java
│   │       │   ├── MessageFieldCreateRequestDTO.java
│   │       │   └── MessageFieldUpdateRequestDTO.java
│   │       └── response/
│   │           ├── MessageFieldResponseDTO.java
│   │           └── MessageFieldHistoryResponseDTO.java
│   │
│   │  ── 거래 관리 ──────────────────────────
│   │
│   ├── transaction/                            ← FWK_TRX, FWK_TRX_HISTORY, FWK_TRX_STOP_HISTORY
│   │   ├── controller/
│   │   │   ├── TransactionController.java
│   │   │   └── TransactionPageController.java
│   │   ├── service/
│   │   │   └── TransactionService.java
│   │   ├── mapper/
│   │   │   └── TransactionMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── TransactionSearchRequestDTO.java
│   │       │   ├── TransactionCreateRequestDTO.java
│   │       │   └── TransactionUpdateRequestDTO.java
│   │       └── response/
│   │           ├── TransactionResponseDTO.java
│   │           ├── TransactionDetailResponseDTO.java
│   │           └── TransactionHistoryResponseDTO.java
│   │
│   ├── trxmessage/                             ← FWK_TRX_MESSAGE, FWK_TRX_MESSAGE_HISTORY
│   │   ├── controller/
│   │   │   ├── TrxMessageController.java
│   │   │   └── TrxMessagePageController.java
│   │   ├── service/
│   │   │   └── TrxMessageService.java
│   │   ├── mapper/
│   │   │   └── TrxMessageMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── TrxMessageSearchRequestDTO.java
│   │       │   ├── TrxMessageCreateRequestDTO.java
│   │       │   └── TrxMessageUpdateRequestDTO.java
│   │       └── response/
│   │           ├── TrxMessageResponseDTO.java
│   │           └── TrxMessageHistoryResponseDTO.java
│   │
│   ├── validator/                              ← FWK_VALIDATOR
│   │   ├── controller/
│   │   │   ├── ValidatorController.java
│   │   │   └── ValidatorPageController.java
│   │   ├── service/
│   │   │   └── ValidatorService.java
│   │   ├── mapper/
│   │   │   └── ValidatorMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── ValidatorSearchRequestDTO.java
│   │       │   ├── ValidatorCreateRequestDTO.java
│   │       │   └── ValidatorUpdateRequestDTO.java
│   │       └── response/
│   │           └── ValidatorResponseDTO.java
│   │
│   ├── messagetest/                            ← FWK_MESSAGE_TEST
│   │   ├── controller/
│   │   │   ├── MessageTestController.java
│   │   │   └── MessageTestPageController.java
│   │   ├── service/
│   │   │   └── MessageTestService.java
│   │   ├── mapper/
│   │   │   └── MessageTestMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── MessageTestSearchRequestDTO.java
│   │       │   └── MessageTestCreateRequestDTO.java
│   │       └── response/
│   │           └── MessageTestResponseDTO.java
│   │
│   ├── messageinstance/                        ← FWK_MESSAGE_INSTANCE
│   │   ├── controller/
│   │   │   ├── MessageInstanceController.java
│   │   │   └── MessageInstancePageController.java
│   │   ├── service/
│   │   │   └── MessageInstanceService.java
│   │   ├── mapper/
│   │   │   └── MessageInstanceMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   └── MessageInstanceSearchRequestDTO.java
│   │       └── response/
│   │           └── MessageInstanceResponseDTO.java
│   │
│   │  ── 배치 관리 ──────────────────────────
│   │
│   ├── batch/                                  ← FWK_BATCH_APP, FWK_BATCH_HIS, FWK_WAS_EXEC_BATCH
│   │   ├── controller/
│   │   │   ├── BatchController.java
│   │   │   └── BatchPageController.java
│   │   ├── service/
│   │   │   └── BatchService.java
│   │   ├── mapper/
│   │   │   └── BatchMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── BatchSearchRequestDTO.java
│   │       │   ├── BatchCreateRequestDTO.java
│   │       │   └── BatchUpdateRequestDTO.java
│   │       └── response/
│   │           ├── BatchResponseDTO.java
│   │           └── BatchHistoryResponseDTO.java
│   │
│   │  ── 에러 관리 ──────────────────────────
│   │
│   ├── errorcode/                              ← FWK_ERROR, FWK_ERROR_DESC
│   │   ├── controller/
│   │   │   ├── ErrorCodeController.java
│   │   │   └── ErrorCodePageController.java
│   │   ├── service/
│   │   │   └── ErrorCodeService.java
│   │   ├── mapper/
│   │   │   └── ErrorCodeMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── ErrorCodeSearchRequestDTO.java
│   │       │   ├── ErrorCodeCreateRequestDTO.java
│   │       │   └── ErrorCodeUpdateRequestDTO.java
│   │       └── response/
│   │           ├── ErrorCodeResponseDTO.java
│   │           └── ErrorCodeDetailResponseDTO.java
│   │
│   ├── errorhandle/                            ← FWK_ERROR_HANDLE_APP, FWK_HANDLE_APP, FWK_ERROR_HANDLE_HIS
│   │   ├── controller/
│   │   │   ├── ErrorHandleController.java
│   │   │   └── ErrorHandlePageController.java
│   │   ├── service/
│   │   │   └── ErrorHandleService.java
│   │   ├── mapper/
│   │   │   └── ErrorHandleMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── ErrorHandleSearchRequestDTO.java
│   │       │   ├── ErrorHandleCreateRequestDTO.java
│   │       │   └── ErrorHandleUpdateRequestDTO.java
│   │       └── response/
│   │           ├── ErrorHandleResponseDTO.java
│   │           └── ErrorHandleHistoryResponseDTO.java
│   │
│   ├── errorhistory/                           ← FWK_ERROR_HIS
│   │   ├── controller/
│   │   │   ├── ErrorHistoryController.java
│   │   │   └── ErrorHistoryPageController.java
│   │   ├── service/
│   │   │   └── ErrorHistoryService.java
│   │   ├── mapper/
│   │   │   └── ErrorHistoryMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   └── ErrorHistorySearchRequestDTO.java
│   │       └── response/
│   │           └── ErrorHistoryResponseDTO.java
│   │
│   │  ── 모니터링 ──────────────────────────
│   │
│   ├── monitor/                                ← FWK_MONITOR
│   │   ├── controller/
│   │   │   ├── MonitorController.java
│   │   │   └── MonitorPageController.java
│   │   ├── service/
│   │   │   └── MonitorService.java
│   │   ├── mapper/
│   │   │   └── MonitorMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── MonitorSearchRequestDTO.java
│   │       │   ├── MonitorCreateRequestDTO.java
│   │       │   └── MonitorUpdateRequestDTO.java
│   │       └── response/
│   │           └── MonitorResponseDTO.java
│   │
│   │  ── 이력 / 로그 ──────────────────────────
│   │
│   ├── adminhistory/                           
│   │   ├── controller/
│   │   │   ├── AdminHistoryController.java
│   │   │   └── AdminHistoryPageController.java
│   │   ├── service/
│   │   │   └── AdminHistoryService.java
│   │   ├── mapper/
│   │   │   └── AdminHistoryMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   └── AdminHistorySearchRequestDTO.java
│   │       └── response/
│   │           └── AdminHistoryResponseDTO.java
│   │
│   ├── transdata/                                ← FWK_TRANS_DATA_HIS, FWK_TRANS_DATA_TIMES //이행데이터
│   │   ├── controller/
│   │   │   ├── TransDataController.java
│   │   │   └── TransDataPageController.java
│   │   ├── service/
│   │   │   └── TransDataService.java
│   │   ├── mapper/
│   │   │   └── TransDataMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   └── TransDataSearchRequestDTO.java
│   │       └── response/
│   │           ├── TransDataResponseDTO.java
│   │           └── TransDataDetailResponseDTO.java
│   │
│   │  ── 게시판 ──────────────────────────
│   │
│   ├── board/                                  ← FWK_BOARD, FWK_BOARD_AUTH, FWK_BOARD_CATEGORY
│   │   ├── controller/
│   │   │   ├── BoardController.java
│   │   │   └── BoardPageController.java
│   │   ├── service/
│   │   │   └── BoardService.java
│   │   ├── mapper/
│   │   │   └── BoardMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── BoardSearchRequestDTO.java
│   │       │   ├── BoardCreateRequestDTO.java
│   │       │   └── BoardUpdateRequestDTO.java
│   │       └── response/
│   │           ├── BoardResponseDTO.java
│   │           └── BoardDetailResponseDTO.java
│   │
│   ├── article/                                ← FWK_ARTICLE, FWK_ARTICLE_USER
│   │   ├── controller/
│   │   │   ├── ArticleController.java
│   │   │   └── ArticlePageController.java
│   │   ├── service/
│   │   │   └── ArticleService.java
│   │   ├── mapper/
│   │   │   └── ArticleMapper.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── ArticleSearchRequestDTO.java
│   │       │   ├── ArticleCreateRequestDTO.java
│   │       │   └── ArticleUpdateRequestDTO.java
│   │       └── response/
│   │           ├── ArticleResponseDTO.java
│   │           └── ArticleDetailResponseDTO.java
│   │
│   │
│   │  ┌─────────────────────────────────────┐
│   │  │  B. Runtime API 도메인 (7개)                    │
│   │  │  DDL 없음 → Mapper 없음                         │
│   │  └─────────────────────────────────────┘
│   │
│   ├── xmlproperty/                            ← 파일 기반 XML 속성
│   │   ├── controller/
│   │   │   ├── XmlPropertyController.java
│   │   │   └── XmlPropertyPageController.java
│   │   ├── service/
│   │   │   └── XmlPropertyService.java
│   │   └── dto/
│   │       ├── request/
│   │       │   └── XmlPropertyUpdateRequestDTO.java
│   │       └── response/
│   │           └── XmlPropertyResponseDTO.java
│   │
│   ├── reload/                                 ← WAS 캐시/설정 재로드
│   │   ├── controller/
│   │   │   └── ReloadController.java
│   │   ├── service/
│   │   │   └── ReloadService.java
│   │   └── dto/
│   │       ├── request/
│   │       │   └── ReloadRequestDTO.java
│   │       └── response/
│   │           └── ReloadResponseDTO.java
│   │
│   ├── messageparsing/                         ← 전문 파싱/조립 테스트
│   │   ├── controller/
│   │   │   ├── MessageParsingController.java
│   │   │   └── MessageParsingPageController.java
│   │   ├── service/
│   │   │   └── MessageParsingService.java
│   │   └── dto/
│   │       ├── request/
│   │       │   └── MessageParsingRequestDTO.java
│   │       └── response/
│   │           └── MessageParsingResponseDTO.java
│   │
│   ├── proxyresponse/                          ← 대응답 대행
│   │   ├── controller/
│   │   │   ├── ProxyResponseController.java
│   │   │   └── ProxyResponsePageController.java
│   │   ├── service/
│   │   │   └── ProxyResponseService.java
│   │   └── dto/
│   │       ├── request/
│   │       │   ├── ProxyResponseCreateRequestDTO.java
│   │       │   └── ProxyResponseUpdateRequestDTO.java
│   │       └── response/
│   │           └── ProxyResponseResponseDTO.java
│   │
│   │  ┌─────────────────────────────────────┐
│   │  │  C. 모니터링/통계 도메인 (5개)        │
│   │  │  DDL 없음 → Mapper 없음              │
│   │  │  타 도메인 Service 참조로 데이터 집계  │
│   │  └─────────────────────────────────────┘
│   │
│   ├── dashboard/                              ← 대시보드 집계 // 중요도 : LOW
│   │   ├── controller/
│   │   │   ├── DashboardController.java
│   │   │   └── DashboardPageController.java
│   │   ├── service/
│   │   │   └── DashboardService.java
│   │   └── dto/
│   │       └── response/
│   │           └── DashboardResponseDTO.java
│   │
├── global/
│   ├── config/                                 ← @Configuration
│   ├── exception/                              ← 예외 핸들러
│   ├── dto/                                    ← ApiResponse, PageRequest 등
│   ├── security/                               ← 인증/인가 (로그인 API 포함)
│   ├── filter/                                 ← 서블릿 필터
│   ├── interceptor/                            ← 인터셉터
│   ├── typehandler/                            ← MyBatis TypeHandler
│   └── util/                                   ← 유틸리티
│
└── resources/
└── mapper/                                 ← MyBatis XML (DDL-Backed 35개만)
├── user/
│   └── UserMapper.xml
├── menu/
│   └── MenuMapper.xml
├── role/
│   └── RoleMapper.xml
├── codegroup/
│   └── CodeGroupMapper.xml
├── code/
│   └── CodeMapper.xml
├── bizgroup/
│   └── BizGroupMapper.xml
├── wasgroup/
│   └── WasGroupMapper.xml
├── wasinstance/
│   └── WasInstanceMapper.xml
├── property/
│   └── PropertyMapper.xml
├── wasproperty/
│   └── WasPropertyMapper.xml
├── org/
│   └── OrgMapper.xml
├── gateway/
│   └── GatewayMapper.xml
├── gwsystem/
│   └── GwSystemMapper.xml
├── transport/
│   └── TransportMapper.xml
├── listener/
│   └── ListenerMapper.xml
├── listenertrx/
│   └── ListenerTrxMapper.xml
├── messagehandler/
│   └── MessageHandlerMapper.xml
├── message/
│   └── MessageMapper.xml
├── messagefield/
│   └── MessageFieldMapper.xml
├── transaction/
│   └── TransactionMapper.xml
├── trxmessage/
│   └── TrxMessageMapper.xml
├── validator/
│   └── ValidatorMapper.xml
├── messagetest/
│   └── MessageTestMapper.xml
├── messageinstance/
│   └── MessageInstanceMapper.xml
├── batch/
│   └── BatchMapper.xml
├── errorcode/
│   └── ErrorCodeMapper.xml
├── errorhandle/
│   └── ErrorHandleMapper.xml
├── errorhistory/
│   └── ErrorHistoryMapper.xml
├── monitor/
│   └── MonitorMapper.xml
├── adminhistory/
│   └── AdminHistoryMapper.xml
├── transdata/
│   └── TransDataMapper.xml
├── board/
│   └── BoardMapper.xml
├── article/
└── ArticleMapper.xml

