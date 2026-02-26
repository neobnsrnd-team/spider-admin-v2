package org.example.springadminv2.global.web;

import org.example.springadminv2.global.security.CustomUserDetails;
import org.springframework.security.core.GrantedAuthority;

import jakarta.servlet.http.HttpServletRequest;

/**
 * View Controller 공통 유틸리티.
 * AJAX 요청은 fragment만, 직접 URL 접근은 전체 셸을 반환한다.
 */
public abstract class BaseViewController {

    protected boolean isAjaxRequest(HttpServletRequest request) {
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"))
                || "true".equals(request.getHeader("X-Tab-Request"));
    }

    /**
     * 특정 리소스에 대한 사용자의 접근 수준을 결정한다.
     * authorities에 "RESOURCE:W"가 있으면 "W", "RESOURCE:R"이 있으면 "R", 없으면 "NONE".
     * @param user 인증된 사용자
     * @param resource 리소스 코드
     * @return "W", "R", or "NONE"
     */
    protected String determineAccessLevel(CustomUserDetails user, String resource) {
        if (user == null) {
            return "NONE";
        }
        return user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith(resource + ":"))
                .map(a -> a.substring(a.indexOf(':') + 1))
                .reduce((a, b) -> "W".equals(a) || "W".equals(b) ? "W" : "R")
                .orElse("NONE");
    }
}
