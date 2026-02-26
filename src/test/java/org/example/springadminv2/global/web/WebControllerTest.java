package org.example.springadminv2.global.web;

import java.util.Set;

import org.example.springadminv2.domain.menu.controller.MenuPageController;
import org.example.springadminv2.global.security.CustomUserDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class WebControllerTest {

    // ─── BaseViewController 테스트용 구체 클래스 ─────────────────────
    static class TestableViewController extends BaseViewController {

        public boolean callIsAjaxRequest(MockHttpServletRequest request) {
            return isAjaxRequest(request);
        }

        public String callDetermineAccessLevel(CustomUserDetails user, String resource) {
            return determineAccessLevel(user, resource);
        }
    }

    /** standalone MockMvc에서 circular view path 오류를 방지하기 위한 ViewResolver */
    static InternalResourceViewResolver viewResolver() {
        InternalResourceViewResolver resolver = new InternalResourceViewResolver();
        resolver.setPrefix("/WEB-INF/views/");
        resolver.setSuffix(".html");
        return resolver;
    }

    // ═════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("BaseViewController 단위 테스트")
    class BaseViewControllerTest {

        private TestableViewController controller;

        @BeforeEach
        void setUp() {
            controller = new TestableViewController();
        }

        // ─── isAjaxRequest ──────────────────────────────────────────

        @Test
        @DisplayName("X-Requested-With: XMLHttpRequest 헤더가 있으면 AJAX 요청으로 판단한다")
        void isAjaxRequest_xmlHttpRequest_returnsTrue() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Requested-With", "XMLHttpRequest");

            // when & then
            assertThat(controller.callIsAjaxRequest(request)).isTrue();
        }

        @Test
        @DisplayName("X-Tab-Request: true 헤더가 있으면 AJAX 요청으로 판단한다")
        void isAjaxRequest_tabRequest_returnsTrue() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.addHeader("X-Tab-Request", "true");

            // when & then
            assertThat(controller.callIsAjaxRequest(request)).isTrue();
        }

        @Test
        @DisplayName("AJAX 관련 헤더가 없으면 일반 요청으로 판단한다")
        void isAjaxRequest_noHeader_returnsFalse() {
            // given
            MockHttpServletRequest request = new MockHttpServletRequest();

            // when & then
            assertThat(controller.callIsAjaxRequest(request)).isFalse();
        }

        // ─── determineAccessLevel ───────────────────────────────────

        @Test
        @DisplayName("사용자가 RESOURCE:W 권한을 가지면 'W'를 반환한다")
        void determineAccessLevel_withWrite_returnsW() {
            // given
            Set<GrantedAuthority> authorities =
                    Set.of(new SimpleGrantedAuthority("MENU:R"), new SimpleGrantedAuthority("MENU:W"));
            CustomUserDetails user = new CustomUserDetails("testUser", "pwd", "ROLE01", "1", 0, authorities);

            // when
            String level = controller.callDetermineAccessLevel(user, "MENU");

            // then
            assertThat(level).isEqualTo("W");
        }

        @Test
        @DisplayName("사용자가 RESOURCE:R 권한만 가지면 'R'을 반환한다")
        void determineAccessLevel_withReadOnly_returnsR() {
            // given
            Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("MENU:R"));
            CustomUserDetails user = new CustomUserDetails("testUser", "pwd", "ROLE01", "1", 0, authorities);

            // when
            String level = controller.callDetermineAccessLevel(user, "MENU");

            // then
            assertThat(level).isEqualTo("R");
        }

        @Test
        @DisplayName("사용자가 null이면 'NONE'을 반환한다")
        void determineAccessLevel_nullUser_returnsNone() {
            // when
            String level = controller.callDetermineAccessLevel(null, "MENU");

            // then
            assertThat(level).isEqualTo("NONE");
        }

        @Test
        @DisplayName("사용자가 해당 리소스에 대한 권한이 없으면 'NONE'을 반환한다")
        void determineAccessLevel_noMatchingAuthority_returnsNone() {
            // given
            Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("OTHER:W"));
            CustomUserDetails user = new CustomUserDetails("testUser", "pwd", "ROLE01", "1", 0, authorities);

            // when
            String level = controller.callDetermineAccessLevel(user, "MENU");

            // then
            assertThat(level).isEqualTo("NONE");
        }
    }

    // ═════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("LoginController 테스트")
    class LoginControllerTest {

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
            mockMvc = MockMvcBuilders.standaloneSetup(new LoginController())
                    .setViewResolvers(viewResolver())
                    .build();
        }

        @Test
        @DisplayName("GET /login 요청 시 200 상태와 'login' 뷰를 반환한다")
        void loginPage_returns200AndLoginView() throws Exception {
            mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
        }
    }

    // ═════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("MenuPageController 테스트")
    class MenuPageControllerTest {

        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
            mockMvc = MockMvcBuilders.standaloneSetup(new MenuPageController())
                    .setViewResolvers(viewResolver())
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .build();
        }

        @Test
        @DisplayName("AJAX 요청 시 fragment 뷰 이름을 반환한다")
        void menuManage_ajaxRequest_returnsFragment() throws Exception {
            mockMvc.perform(get("/system/menu").header("X-Tab-Request", "true"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("fragments/pages/system/menu-manage :: content"));
        }

        @Test
        @DisplayName("일반 요청 시 루트로 리다이렉트한다")
        void menuManage_regularRequest_redirectsToRoot() throws Exception {
            mockMvc.perform(get("/system/menu"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"));
        }
    }
}
