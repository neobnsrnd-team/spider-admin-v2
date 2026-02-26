package org.example.springadminv2.global.web;

import java.util.List;
import java.util.Set;

import org.example.springadminv2.domain.menu.dto.UserMenuRow;
import org.example.springadminv2.domain.menu.service.MenuService;
import org.example.springadminv2.global.log.listener.SecurityLogEventListener;
import org.example.springadminv2.global.security.CustomUserDetails;
import org.example.springadminv2.global.security.SecurityConfig;
import org.example.springadminv2.global.security.handler.CustomAccessDeniedHandler;
import org.example.springadminv2.global.security.handler.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = LayoutController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    SecurityLogEventListener.class
})
class LayoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MenuService menuService;

    private static CustomUserDetails mockUser() {
        return new CustomUserDetails(
                "testUser",
                "pwd",
                "ROLE01",
                "1",
                0,
                Set.of(new SimpleGrantedAuthority("MENU:R"), new SimpleGrantedAuthority("MENU:W")));
    }

    @Nested
    @DisplayName("GET / (메인 셸)")
    class Index {

        @Test
        @DisplayName("인증된 사용자 → layout 뷰 반환, 모델에 userId/authorities/menuTree 포함")
        void returns_layout_with_model() throws Exception {
            // given
            CustomUserDetails user = mockUser();
            List<UserMenuRow> menuTree = List.of(
                    new UserMenuRow("system", "ROOT", 1, "시스템 관리", null, "settings", null),
                    new UserMenuRow("v3_menu_manage", "system", 2, "메뉴 관리", "/system/menu", "list", "RW"));
            given(menuService.getAuthorizedMenuTree("testUser", "ROLE01")).willReturn(menuTree);

            // when & then
            MvcResult result = mockMvc.perform(get("/").with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(view().name("layout"))
                    .andExpect(model().attributeExists("userId", "authorities", "menuTree"))
                    .andReturn();

            assertThat(result.getModelAndView().getModel().get("userId")).isEqualTo("testUser");
        }
    }

    @Nested
    @DisplayName("GET /api/user-menus/tree (메뉴 트리 API)")
    class MenuTree {

        @Test
        @DisplayName("사용자 메뉴 트리를 정상 반환한다")
        void returns_user_menu_tree() throws Exception {
            // given
            CustomUserDetails user = mockUser();
            List<UserMenuRow> menuTree = List.of(
                    new UserMenuRow("system", "ROOT", 1, "시스템 관리", null, "settings", null),
                    new UserMenuRow("v3_menu_manage", "system", 2, "메뉴 관리", "/system/menu", "list", "RW"));
            given(menuService.getAuthorizedMenuTree("testUser", "ROLE01")).willReturn(menuTree);

            // when & then
            mockMvc.perform(get("/api/user-menus/tree").with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].menuId").value("system"))
                    .andExpect(jsonPath("$.data[1].menuId").value("v3_menu_manage"));
        }

        @Test
        @DisplayName("권한이 없으면 빈 리스트를 반환한다")
        void returns_empty_when_no_permissions() throws Exception {
            // given
            CustomUserDetails user = mockUser();
            given(menuService.getAuthorizedMenuTree("testUser", "ROLE01")).willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/user-menus/tree").with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }
    }
}
