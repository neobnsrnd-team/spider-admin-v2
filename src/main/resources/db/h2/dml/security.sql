-- Security seed data for H2 (local development)

-- Roles
INSERT INTO FWK_ROLE (ROLE_ID, ROLE_NAME, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('ADMIN', '관리자', 'Y', '20240101000000', 'system');
INSERT INTO FWK_ROLE (ROLE_ID, ROLE_NAME, USE_YN, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID)
VALUES ('USER', '일반 사용자', 'Y', '20240101000000', 'system');

-- Users (guest01 / 1q2w3e4r!@)
INSERT INTO FWK_USER (USER_ID, USER_NAME, PASSWORD, ROLE_ID, USER_STATE_CODE, LOGIN_FAIL_COUNT,
                      LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, PASSWD)
VALUES ('guest01', '게스트 관리자', '1q2w3e4r!@', 'ADMIN', '1', 0,
        '20240101000000', 'system', '$2a$10$qZK/xCgH0cu7VlrsWni3beu.k35Kt6AllRVxqnM01Ju67PtVOBSL6');

-- ============================================================================
-- Menus (full hierarchy with category parents and leaf menus)
-- ============================================================================

-- Category: 시스템 관리
INSERT INTO FWK_MENU VALUES ('system', 'ROOT', 1, '시스템 관리', '', 'settings', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_menu_manage', 'system', 1, '메뉴 관리', '/system/menu', 'list', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_role_manage', 'system', 2, '역할 관리', '/system/role', 'people', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_user_manage', 'system', 3, '사용자 관리', '/system/user', 'person', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_neb_code_group_manage', 'system', 4, '코드그룹 관리', '/system/code-group', 'tag', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_code_manage', 'system', 5, '코드 관리', '/system/code', 'tag', 'Y', 'Y', '20240101000000', 'system');

-- Category: 인프라 관리
INSERT INTO FWK_MENU VALUES ('infra', 'ROOT', 2, '인프라 관리', '', 'server', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_was_group_manage', 'infra', 1, 'WAS 그룹 관리', '/infra/was-group', 'folder', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_was_instance', 'infra', 2, 'WAS 인스턴스', '/infra/was-instance', 'computer', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_property_db_manage', 'infra', 3, 'DB 속성 관리', '/infra/property-db', 'database', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_xml_property_manage', 'infra', 4, 'XML 속성 관리', '/infra/xml-property', 'document', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_system_oper_manage', 'infra', 5, '시스템 운영', '/infra/system-oper', 'tools', 'Y', 'Y', '20240101000000', 'system');

-- Category: 연계 관리
INSERT INTO FWK_MENU VALUES ('integration', 'ROOT', 3, '연계 관리', '', 'link', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_connect_org_manage', 'integration', 1, '연계기관 관리', '/integration/connect-org', 'org', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_code_mapping_manage', 'integration', 2, '코드 매핑 관리', '/integration/code-mapping', 'map', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_gw_manage', 'integration', 3, '게이트웨이 관리', '/integration/gateway', 'cloud', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_org_trans_manage', 'integration', 4, '기관별 거래 관리', '/integration/org-trans', 'swap', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_app_mapping_manage', 'integration', 5, '앱 매핑 관리', '/integration/app-mapping', 'app', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_listener_connector_manage', 'integration', 6, '리스너/커넥터 관리', '/integration/listener-connector', 'plug', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_msg_handle_manage', 'integration', 7, '메시지핸들러 관리', '/integration/msg-handle', 'mail', 'Y', 'Y', '20240101000000', 'system');

-- Category: 거래/전문 관리
INSERT INTO FWK_MENU VALUES ('transaction', 'ROOT', 4, '거래/전문 관리', '', 'document', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_message_manage', 'transaction', 1, '전문 관리', '/transaction/message', 'mail', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_msg_trx_manage', 'transaction', 2, '전문거래 관리', '/transaction/msg-trx', 'swap', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_trx_validator', 'transaction', 3, '거래 검증', '/transaction/trx-validator', 'check', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_trx', 'transaction', 4, '거래 관리', '/transaction/trx', 'list', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_req_message_test', 'transaction', 5, '전문 테스트', '/transaction/req-message-test', 'play', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_proxy_testdata', 'transaction', 6, '프록시 테스트', '/transaction/proxy-testdata', 'data', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_db_log', 'transaction', 7, 'DB 로그', '/transaction/db-log', 'database', 'Y', 'Y', '20240101000000', 'system');

-- Category: 배치 관리
INSERT INTO FWK_MENU VALUES ('batch', 'ROOT', 5, '배치 관리', '', 'timer', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_batch_app_manage', 'batch', 1, '배치 앱 관리', '/batch/batch-app', 'app', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_batch_his_list', 'batch', 2, '배치 이력', '/batch/batch-his', 'history', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_executing_batch', 'batch', 3, '실행 중 배치', '/batch/executing-batch', 'play', 'Y', 'Y', '20240101000000', 'system');

-- Category: 오류 관리
INSERT INTO FWK_MENU VALUES ('error', 'ROOT', 6, '오류 관리', '', 'warning', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_error_code', 'error', 1, '오류 코드', '/error/error-code', 'code', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_error_cause_his', 'error', 2, '오류 원인 이력', '/error/error-cause-his', 'history', 'Y', 'Y', '20240101000000', 'system');

-- Category: 모니터링
INSERT INTO FWK_MENU VALUES ('monitor', 'ROOT', 7, '모니터링', '', 'chart', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_system_biz_reg', 'monitor', 1, '업무시스템 등록', '/monitor/system-biz-reg', 'settings', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_was_status', 'monitor', 2, 'WAS 상태', '/monitor/was-status', 'activity', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_was_status_monitor', 'monitor', 3, 'WAS 모니터링', '/monitor/was-status-monitor', 'monitor', 'Y', 'Y', '20240101000000', 'system');

-- Category: 이력/감사
INSERT INTO FWK_MENU VALUES ('audit', 'ROOT', 8, '이력/감사', '', 'history', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_user_page_log', 'audit', 1, '사용자 접근 이력', '/audit/user-page-log', 'person', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_trx_stop_his', 'audit', 2, '거래 중지 이력', '/audit/trx-stop-his', 'stop', 'Y', 'Y', '20240101000000', 'system');

-- Category: 게시판
INSERT INTO FWK_MENU VALUES ('board', 'ROOT', 9, '게시판', '', 'edit', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_board_manage', 'board', 1, '게시판 관리', '/board/board-manage', 'settings', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_BOARD_AUTH', 'board', 2, '게시판 권한', '/board/board-auth', 'lock', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_FWKB_NOTICE_BOARD', 'board', 3, '공지사항', '/board/notice', 'notice', 'Y', 'Y', '20240101000000', 'system');
INSERT INTO FWK_MENU VALUES ('v3_FWKB_TEST_BOARD', 'board', 4, '테스트 게시판', '/board/test-board', 'edit', 'Y', 'Y', '20240101000000', 'system');

-- ============================================================================
-- User-Menu permissions (guest01: WRITE on all leaf menus)
-- ============================================================================
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_menu_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_role_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_user_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_neb_code_group_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_code_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_was_group_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_was_instance', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_property_db_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_xml_property_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_system_oper_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_connect_org_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_code_mapping_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_gw_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_org_trans_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_app_mapping_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_listener_connector_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_msg_handle_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_message_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_msg_trx_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_trx_validator', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_trx', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_req_message_test', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_proxy_testdata', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_db_log', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_batch_app_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_batch_his_list', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_executing_batch', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_error_code', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_error_cause_his', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_system_biz_reg', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_was_status', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_was_status_monitor', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_user_page_log', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_trx_stop_his', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_board_manage', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_BOARD_AUTH', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_FWKB_NOTICE_BOARD', 'W', '20240101000000', 'system', 0);
INSERT INTO FWK_USER_MENU (USER_ID, MENU_ID, AUTH_CODE, LAST_UPDATE_DTIME, LAST_UPDATE_USER_ID, FAVOR_MENU_ORDER)
VALUES ('guest01', 'v3_FWKB_TEST_BOARD', 'W', '20240101000000', 'system', 0);

-- ============================================================================
-- Role-Menu permissions (ADMIN: WRITE on all, USER: READ on system menus)
-- ============================================================================

-- ADMIN role: WRITE on all leaf menus
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_menu_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_role_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_user_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_neb_code_group_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_code_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_group_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_instance', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_property_db_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_xml_property_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_system_oper_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_connect_org_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_code_mapping_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_gw_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_org_trans_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_app_mapping_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_listener_connector_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_msg_handle_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_message_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_msg_trx_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trx_validator', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trx', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_req_message_test', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_proxy_testdata', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_db_log', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_batch_app_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_batch_his_list', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_executing_batch', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_error_code', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_error_cause_his', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_system_biz_reg', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_status', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_was_status_monitor', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_user_page_log', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_trx_stop_his', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_board_manage', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_BOARD_AUTH', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_FWKB_NOTICE_BOARD', 'W');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('ADMIN', 'v3_FWKB_TEST_BOARD', 'W');

-- USER role: READ on system menus only
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('USER', 'v3_menu_manage', 'R');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('USER', 'v3_role_manage', 'R');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('USER', 'v3_user_manage', 'R');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('USER', 'v3_neb_code_group_manage', 'R');
INSERT INTO FWK_ROLE_MENU (ROLE_ID, MENU_ID, AUTH_CODE) VALUES ('USER', 'v3_code_manage', 'R');
