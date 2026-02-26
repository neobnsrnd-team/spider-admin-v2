package org.example.springadminv2.global.web;

import java.util.List;
import java.util.stream.Collectors;

import org.example.springadminv2.domain.menu.dto.UserMenuRow;
import org.example.springadminv2.domain.menu.service.MenuService;
import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.security.CustomUserDetails;
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

    private final MenuService menuService;

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
        model.addAttribute("menuTree", menuService.getAuthorizedMenuTree(user.getUserId(), user.getRoleId()));
        model.addAttribute("initialTab", null);
        return "layout";
    }

    /**
     * 메뉴 트리 API. 사이드바에서 호출 (새로고침용).
     * 사용자에게 권한이 있는 메뉴만 트리 구조로 반환한다.
     */
    @GetMapping("/api/user-menus/tree")
    @ResponseBody
    public ApiResponse<List<UserMenuRow>> menuTree(@AuthenticationPrincipal CustomUserDetails user) {
        return ApiResponse.success(menuService.getAuthorizedMenuTree(user.getUserId(), user.getRoleId()));
    }
}
