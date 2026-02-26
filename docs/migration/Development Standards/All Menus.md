# V3 전체 메뉴 URL 목록

## 인증 (1건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_my_info_manage | 개인정보수정 | `/users/profile` | Personal information modification |

## 시스템 관리 (5건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_user_manage | 내부 사용자 관리 | `/users` | User management |
| v3_menu_manage | 메뉴 관리 | `/menus` | Menu management |
| v3_role_manage | 역할 관리 | `/roles` | Role management |
| v3_neb_code_group_manage | 코드그룹 관리 | `/code-groups` | Code group management |
| v3_code_manage | 코드 관리 | `/codes` | Code management |

## 인프라 관리 (5건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_system_oper_manage | 운영정보Reload | `/reload` | Operation info manage(reload) |
| v3_property_db_manage | 프로퍼티 DB 관리 | `/properties` | Property DB management |
| v3_xml_property_manage | XML Property 관리 | `/xml-properties` | XML Property Management |
| v3_was_group_manage | Was 그룹 관리 | `/was-groups` | WAS group management |
| v3_was_instance | Was 인스턴스 | `/was-instances` | Was instance management |

## 연계 관리 (7건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_connect_org_manage | 연계기관 관리 | `/orgs` | Organization management |
| v3_gw_manage | Gateway 관리 | `/gateways` | Gateway management |
| v3_org_trans_manage | 기관통신 Gateway맵핑 관리 | `/gw-systems` | Org-Gateway mapping management |
| v3_msg_handle_manage | 전문처리핸들러 관리 | `/message-handlers` | Message handler management |
| v3_listener_connector_manage | 리스너 응답커넥터 맵핑관리 | `/listener-trxs` | Listener-Connector mapping management |
| v3_app_mapping_manage | 요청처리 App맵핑 관리 | `/transports` | Application mapping management |
| v3_code_mapping_manage | 코드맵핑 관리 | `/codes/org-codes` | Code mapping management |

## 거래/전문 관리 (7건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_msg_trx_manage | 거래 관리 | `/transactions` | Transaction management |
| v3_message_manage | 전문 관리 | `/messages` | Message management |
| v3_trx_validator | 거래 validator | `/validators` | 거래 validator |
| v3_trx | 거래중지 | `/transactions/trx-stop` | Transection stop |
| v3_req_message_test | 전문 테스트 | `/message-tests` | Message test |
| v3_proxy_testdata | 당발 대응답 관리 | `/proxy-responses` | Outward proxy msg management |
| v3_db_log | 거래추적로그조회(DB) | `/message-instances` | Search message log(DB) |

## 배치 관리 (3건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_batch_app_manage | 배치 App 관리 | `/batches/apps` | Batch app management |
| v3_batch_his_list | 배치 수행내역 | `/batches/history` | Batch history list |
| v3_executing_batch | 실행중인 배치/스케쥴 | `/batches/executing` | Executing batch |

## 오류 관리 (2건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_error_cause_his | 오류발생 현황 | `/error-histories` | error_cause_his |
| v3_error_code | 오류코드 관리 | `/error-codes` | Error code management |

## 모니터링 (3건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_system_biz_reg | 현황판 등록 | `/monitors` | Create status plate |
| v3_was_status | WAS별 Gateway정보 | `/was-gateway-statuses` | Gateway info per WAS |
| v3_was_status_monitor | WAS별 Gateway모니터링 | `/was-status-monitors` | WAS Status Monitor |

## 이력/감사 (2건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_user_page_log | 관리자 작업이력 로그 | `/admin-histories` | Maneger Log |
| v3_trx_stop_his | 거래중지이력 | `/admin-histories/trx-stop-history` | Transaction history |

## 게시판 (4건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_board_manage | 게시판 관리 | `/boards` | Board |
| v3_BOARD_AUTH | 게시판 권한 관리 | `/boards/auth` | Board authorization management |
| v3_FWKB_NOTICE_BOARD | 공지사항 | `/articles/notice-board` | — |
| v3_FWKB_TEST_BOARD | 게시판 | `/articles/test-board` | — |

## 이행 데이터 (3건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_trans_data_popup | 이행데이터 생성 | `/trans-data/generation` | Create transfer data |
| v3_trans_data_exec | 이행데이터 반영 | `/trans-data` | Apply transfer data |
| v3_trans_data_list | 이행데이터 조회 | `/trans-data/files` | Search transfer data |

## 개발 관리 (2건)

| MENU_ID | 메뉴명 | URL | 영문명 |
|---------|--------|-----|--------|
| v3_message_parsing_test | 전문로그파싱 | `/message-parsing` | Parsing message log |
| v3_message_parsing_json | 전문로그파싱(JSON) | `/message-parsing/json` | Parsing message log to JSON |
