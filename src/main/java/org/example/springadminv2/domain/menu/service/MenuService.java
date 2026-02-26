package org.example.springadminv2.domain.menu.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.springadminv2.domain.menu.dto.MenuCreateRequest;
import org.example.springadminv2.domain.menu.dto.MenuResponse;
import org.example.springadminv2.domain.menu.dto.MenuTreeNode;
import org.example.springadminv2.domain.menu.dto.MenuUpdateRequest;
import org.example.springadminv2.domain.menu.dto.UserMenuRow;
import org.example.springadminv2.domain.menu.dto.UserMenuTreeNode;
import org.example.springadminv2.domain.menu.mapper.MenuMapper;
import org.example.springadminv2.global.exception.BaseException;
import org.example.springadminv2.global.exception.ErrorType;
import org.example.springadminv2.global.security.config.SecurityAccessProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MenuService {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final MenuMapper menuMapper;
    private final SecurityAccessProperties securityAccessProperties;

    /**
     * 전체 메뉴를 조회하여 트리 구조로 반환한다.
     * flat list -> priorMenuId 기준 grouping -> 재귀 빌드
     */
    @Transactional(readOnly = true)
    public List<MenuTreeNode> getMenuTree() {
        List<MenuResponse> allMenus = menuMapper.selectAllMenus();
        return buildTree(allMenus);
    }

    /**
     * 단건 메뉴 상세 조회.
     */
    @Transactional(readOnly = true)
    public MenuResponse getMenuDetail(String menuId) {
        MenuResponse menu = menuMapper.selectMenuById(menuId);
        if (menu == null) {
            throw new BaseException(ErrorType.RESOURCE_NOT_FOUND, "menuId=" + menuId);
        }
        return menu;
    }

    /**
     * 메뉴 생성.
     */
    @Transactional
    public void createMenu(MenuCreateRequest request, String userId) {
        String now = LocalDateTime.now().format(TIMESTAMP_FMT);
        menuMapper.insertMenu(request, userId, now);
    }

    /**
     * 메뉴 수정.
     */
    @Transactional
    public void updateMenu(String menuId, MenuUpdateRequest request, String userId) {
        String now = LocalDateTime.now().format(TIMESTAMP_FMT);
        menuMapper.updateMenu(menuId, request, userId, now);
    }

    /**
     * 메뉴 삭제. 하위 메뉴가 존재하면 예외를 던진다.
     */
    @Transactional
    public void deleteMenu(String menuId) {
        int childCount = menuMapper.countChildMenus(menuId);
        if (childCount > 0) {
            throw new BaseException(ErrorType.INVALID_STATE, "menuId=" + menuId + ", children=" + childCount);
        }
        menuMapper.deleteMenu(menuId);
    }

    /**
     * 메뉴 순서(정렬) 변경.
     */
    @Transactional
    public void updateSortOrder(String menuId, int sortOrder, String priorMenuId, String userId) {
        String now = LocalDateTime.now().format(TIMESTAMP_FMT);
        menuMapper.updateSortOrder(menuId, sortOrder, priorMenuId, userId, now);
    }

    /**
     * 사용자 권한 기반 메뉴 트리 조회.
     * authority-source 설정에 따라 FWK_USER_MENU 또는 FWK_ROLE_MENU에서 조회한다.
     * 계층 조회 + 권한 필터링은 SQL에서 처리하고, Service는 결과를 그대로 전달한다.
     */
    @Transactional(readOnly = true)
    public List<UserMenuTreeNode> getAuthorizedMenuTree(String userId, String roleId) {
        List<UserMenuRow> rows;
        if ("ROLE_MENU".equals(securityAccessProperties.getAuthoritySource())) {
            rows = menuMapper.selectRoleMenuTree(roleId);
        } else {
            rows = menuMapper.selectUserMenuTree(userId);
        }
        return buildUserTree(rows);
    }

    // ── 트리 빌드 ──────────────────────────────────────────

    private List<MenuTreeNode> buildTree(List<MenuResponse> allMenus) {
        Set<String> allMenuIds = allMenus.stream().map(MenuResponse::menuId).collect(Collectors.toSet());

        // priorMenuId 기준으로 그룹핑 (순서 유지)
        Map<String, List<MenuResponse>> childrenMap = allMenus.stream()
                .collect(Collectors.groupingBy(
                        m -> m.priorMenuId() == null ? "" : m.priorMenuId(), LinkedHashMap::new, Collectors.toList()));

        // 루트 = 결과 셋 내에서 부모(priorMenuId)가 존재하지 않는 메뉴
        List<MenuResponse> roots = allMenus.stream()
                .filter(m -> !allMenuIds.contains(m.priorMenuId()))
                .toList();

        return roots.stream().map(r -> toTreeNode(r, childrenMap)).toList();
    }

    private MenuTreeNode toTreeNode(MenuResponse menu, Map<String, List<MenuResponse>> childrenMap) {
        List<MenuResponse> childMenus = childrenMap.getOrDefault(menu.menuId(), List.of());
        List<MenuTreeNode> children =
                childMenus.stream().map(c -> toTreeNode(c, childrenMap)).toList();
        return MenuTreeNode.of(menu, children.isEmpty() ? null : children);
    }

    // ── 사용자 권한 메뉴 트리 빌드 ──────────────────────────────

    private List<UserMenuTreeNode> buildUserTree(List<UserMenuRow> allRows) {
        Set<String> allMenuIds = allRows.stream().map(UserMenuRow::menuId).collect(Collectors.toSet());

        Map<String, List<UserMenuRow>> childrenMap = allRows.stream()
                .collect(Collectors.groupingBy(
                        r -> r.priorMenuId() == null ? "" : r.priorMenuId(), LinkedHashMap::new, Collectors.toList()));

        // 루트 = 결과 셋 내에서 부모(priorMenuId)가 존재하지 않는 메뉴
        List<UserMenuRow> roots = allRows.stream()
                .filter(r -> !allMenuIds.contains(r.priorMenuId()))
                .toList();

        return roots.stream().map(r -> toUserTreeNode(r, childrenMap)).toList();
    }

    private UserMenuTreeNode toUserTreeNode(UserMenuRow row, Map<String, List<UserMenuRow>> childrenMap) {
        List<UserMenuRow> childRows = childrenMap.getOrDefault(row.menuId(), List.of());
        List<UserMenuTreeNode> children =
                childRows.stream().map(c -> toUserTreeNode(c, childrenMap)).toList();
        return UserMenuTreeNode.of(row, children.isEmpty() ? null : children);
    }
}
