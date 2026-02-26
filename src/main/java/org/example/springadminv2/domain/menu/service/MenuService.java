package org.example.springadminv2.domain.menu.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.springadminv2.domain.menu.dto.MenuCreateRequest;
import org.example.springadminv2.domain.menu.dto.MenuResponse;
import org.example.springadminv2.domain.menu.dto.MenuTreeNode;
import org.example.springadminv2.domain.menu.dto.MenuUpdateRequest;
import org.example.springadminv2.domain.menu.dto.UserMenuRow;
import org.example.springadminv2.domain.menu.mapper.MenuMapper;
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
        return menuMapper.selectMenuById(menuId);
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
            throw new IllegalStateException("하위 메뉴가 존재하여 삭제할 수 없습니다. (children=" + childCount + ")");
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
    public List<UserMenuRow> getAuthorizedMenuTree(String userId, String roleId) {
        if ("ROLE_MENU".equals(securityAccessProperties.getAuthoritySource())) {
            return menuMapper.selectRoleMenuTree(roleId);
        }
        return menuMapper.selectUserMenuTree(userId);
    }

    // ── 트리 빌드 ──────────────────────────────────────────

    private List<MenuTreeNode> buildTree(List<MenuResponse> allMenus) {
        // priorMenuId 기준으로 그룹핑 (순서 유지)
        Map<String, List<MenuResponse>> childrenMap = allMenus.stream()
                .collect(Collectors.groupingBy(
                        m -> m.priorMenuId() == null ? "" : m.priorMenuId(), LinkedHashMap::new, Collectors.toList()));

        // ROOT 레벨 메뉴 (priorMenuId가 'ROOT' 이거나 null/"")
        List<MenuResponse> roots = new ArrayList<>();
        roots.addAll(childrenMap.getOrDefault("ROOT", List.of()));
        roots.addAll(childrenMap.getOrDefault("", List.of()));

        // 각 루트 노드에 대해 재귀적으로 트리 노드 생성
        return roots.stream().map(r -> toTreeNode(r, childrenMap)).toList();
    }

    private MenuTreeNode toTreeNode(MenuResponse menu, Map<String, List<MenuResponse>> childrenMap) {
        List<MenuResponse> childMenus = childrenMap.getOrDefault(menu.menuId(), List.of());
        List<MenuTreeNode> children =
                childMenus.stream().map(c -> toTreeNode(c, childrenMap)).toList();
        return MenuTreeNode.of(menu, children.isEmpty() ? null : children);
    }
}
