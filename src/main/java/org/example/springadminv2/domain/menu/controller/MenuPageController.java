package org.example.springadminv2.domain.menu.controller;

import org.example.springadminv2.global.security.CustomUserDetails;
import org.example.springadminv2.global.web.BaseViewController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class MenuPageController extends BaseViewController {

    @GetMapping("/system/menu")
    public String menuManage(HttpServletRequest request, @AuthenticationPrincipal CustomUserDetails user) {
        if (isAjaxRequest(request)) {
            return "fragments/pages/system/menu-manage :: content";
        }
        return "redirect:/";
    }
}
