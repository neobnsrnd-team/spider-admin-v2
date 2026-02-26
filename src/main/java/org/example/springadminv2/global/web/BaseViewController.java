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
        return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
    }

    /**
     * 특정 메뉴에 대한 사용자의 접근 수준을 결정한다.
     * authorities에 "RESOURCE:W"가 있으면 "W", "RESOURCE:R"이 있으면 "R", 없으면 null.
     * @param user 인증된 사용자
     * @param menuId 메뉴 ID (화면별 리소스 코드 매핑에 사용)
     * @return "W", "R", or null
     */
    protected String determineAccessLevel(CustomUserDetails user, String menuId) {
        if (user == null || user.getAuthorities() == null) {
            return null;
        }
        // Check if user has any W authority for this screen's resources
        boolean hasWrite = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.endsWith(":W"));
        if (hasWrite) return "W";

        boolean hasRead = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.endsWith(":R"));
        if (hasRead) return "R";

        return null;
    }
}
