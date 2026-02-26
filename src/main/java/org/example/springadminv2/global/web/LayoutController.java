package org.example.springadminv2.global.web;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.security.CustomUserDetails;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class LayoutController {

    private final AuthorityMapper authorityMapper;

    /**
     * 메인 셸 진입.
     * 인증된 사용자가 / 접근 시 layout.html을 반환한다.
     */
    @GetMapping("/")
    public String index(@AuthenticationPrincipal CustomUserDetails user, Model model) {
        model.addAttribute("userId", user.getUserId());
        model.addAttribute(
                "authorities",
                user.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toList()));
        model.addAttribute("menuTree", getUserMenuTree(user));
        model.addAttribute("initialTab", null);
        return "layout";
    }

    /**
     * 메뉴 트리 API. 사이드바에서 호출 (새로고침용).
     * 사용자에게 권한이 있는 메뉴만 트리 구조로 반환한다.
     */
    @GetMapping("/api/menus/tree")
    @ResponseBody
    public ApiResponse<List<Map<String, Object>>> menuTree(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success(getUserMenuTree(user));
    }

    private List<Map<String, Object>> getUserMenuTree(CustomUserDetails user) {
        List<MenuPermission> permissions = authorityMapper.selectMenuPermissionsByUserId(user.getUserId());
        Set<String> accessibleMenuIds =
                permissions.stream().map(MenuPermission::menuId).collect(Collectors.toSet());
        List<Map<String, Object>> allMenus = authorityMapper.selectAllMenus();
        Map<String, String> permMap = permissions.stream()
                .collect(Collectors.toMap(MenuPermission::menuId, MenuPermission::authCode, (a, b) -> b));
        return buildTree(allMenus, accessibleMenuIds, permMap);
    }

    private List<Map<String, Object>> buildTree(
            List<Map<String, Object>> allMenus, Set<String> accessibleMenuIds, Map<String, String> permMap) {

        // Group by parent (null and "" are treated as ROOT)
        Map<String, List<Map<String, Object>>> byParent = new LinkedHashMap<>();
        for (Map<String, Object> menu : allMenus) {
            String parentId = (String) menu.get("priorMenuId");
            if (parentId == null || parentId.isEmpty()) parentId = "ROOT";
            byParent.computeIfAbsent(parentId, k -> new ArrayList<>()).add(menu);
        }

        // Recursively build from ROOT
        return buildChildren("ROOT", byParent, accessibleMenuIds, permMap);
    }

    private List<Map<String, Object>> buildChildren(
            String parentId,
            Map<String, List<Map<String, Object>>> byParent,
            Set<String> accessibleMenuIds,
            Map<String, String> permMap) {

        List<Map<String, Object>> children = byParent.getOrDefault(parentId, List.of());
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> menu : children) {
            String menuId = (String) menu.get("menuId");
            String menuUrl = (String) menu.get("menuUrl");

            // Build sub-tree
            List<Map<String, Object>> subChildren = buildChildren(menuId, byParent, accessibleMenuIds, permMap);

            // Include if: has sub-children (category) OR user has access (leaf)
            boolean isLeaf = (menuUrl != null && !menuUrl.isEmpty());
            boolean hasAccess = accessibleMenuIds.contains(menuId);
            boolean hasVisibleChildren = !subChildren.isEmpty();

            if (isLeaf && hasAccess) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("menuId", menuId);
                node.put("menuName", menu.get("menuName"));
                node.put("menuUrl", menuUrl);
                node.put("menuImage", menu.get("menuImage"));
                node.put("authCode", permMap.getOrDefault(menuId, "R"));
                node.put("children", List.of());
                result.add(node);
            } else if (hasVisibleChildren) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("menuId", menuId);
                node.put("menuName", menu.get("menuName"));
                node.put("menuUrl", "");
                node.put("menuImage", menu.get("menuImage"));
                node.put("authCode", null);
                node.put("children", subChildren);
                result.add(node);
            }
        }

        return result;
    }
}
