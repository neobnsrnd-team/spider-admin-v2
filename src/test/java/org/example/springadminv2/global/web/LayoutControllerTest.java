package org.example.springadminv2.global.web;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.example.springadminv2.global.log.listener.SecurityLogEventListener;
import org.example.springadminv2.global.security.CustomUserDetails;
import org.example.springadminv2.global.security.SecurityConfig;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.handler.CustomAccessDeniedHandler;
import org.example.springadminv2.global.security.handler.CustomAuthenticationEntryPoint;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
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
    private AuthorityMapper authorityMapper;

    private static CustomUserDetails mockUser() {
        return new CustomUserDetails(
                "testUser",
                "pwd",
                "1",
                0,
                Set.of(new SimpleGrantedAuthority("MENU:R"), new SimpleGrantedAuthority("MENU:W")));
    }

    private static Map<String, Object> menu(String menuId, String parentId, String name, String url, String image) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("menuId", menuId);
        m.put("priorMenuId", parentId);
        m.put("menuName", name);
        m.put("menuUrl", url);
        m.put("menuImage", image);
        return m;
    }

    @Nested
    @DisplayName("GET / (메인 셸)")
    class Index {

        @Test
        @DisplayName("인증된 사용자 → layout 뷰 반환, 모델에 userId/authorities/menuTree 포함")
        void returns_layout_with_model() throws Exception {
            // given
            CustomUserDetails user = mockUser();
            given(authorityMapper.selectMenuPermissionsByUserId("testUser"))
                    .willReturn(List.of(new MenuPermission("v3_menu_manage", "RW")));
            given(authorityMapper.selectAllMenus())
                    .willReturn(List.of(
                            menu("system", "ROOT", "시스템 관리", null, "settings"),
                            menu("v3_menu_manage", "system", "메뉴 관리", "/system/menu", "list")));

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
    @DisplayName("GET /api/menus/tree (메뉴 트리 API)")
    class MenuTree {

        @Test
        @DisplayName("권한 있는 리프 메뉴만 트리에 포함")
        @SuppressWarnings("unchecked")
        void returns_accessible_leaf_menus_only() throws Exception {
            // given
            CustomUserDetails user = mockUser();
            given(authorityMapper.selectMenuPermissionsByUserId("testUser"))
                    .willReturn(List.of(new MenuPermission("v3_menu_manage", "RW")));
            given(authorityMapper.selectAllMenus())
                    .willReturn(List.of(
                            menu("system", "ROOT", "시스템 관리", null, "settings"),
                            menu("v3_menu_manage", "system", "메뉴 관리", "/system/menu", "list"),
                            menu("v3_role_manage", "system", "역할 관리", "/system/role", "people")));

            // when & then – v3_menu_manage 만 권한 보유
            mockMvc.perform(get("/api/menus/tree").with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data[0].menuId").value("system"))
                    .andExpect(jsonPath("$.data[0].children[0].menuId").value("v3_menu_manage"))
                    .andExpect(jsonPath("$.data[0].children.length()").value(1));
        }

        @Test
        @DisplayName("접근 가능한 리프가 없는 카테고리는 제외")
        void excludes_empty_categories() throws Exception {
            // given
            CustomUserDetails user = mockUser();
            given(authorityMapper.selectMenuPermissionsByUserId("testUser")).willReturn(List.of());
            given(authorityMapper.selectAllMenus())
                    .willReturn(List.of(
                            menu("system", "ROOT", "시스템 관리", null, "settings"),
                            menu("v3_menu_manage", "system", "메뉴 관리", "/system/menu", "list")));

            // when & then – 권한 없으면 빈 트리
            mockMvc.perform(get("/api/menus/tree").with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.length()").value(0));
        }

        @Test
        @DisplayName("priorMenuId가 null이거나 빈 문자열인 메뉴를 ROOT로 처리")
        void null_and_empty_parent_treated_as_root() throws Exception {
            // given
            CustomUserDetails user = mockUser();
            given(authorityMapper.selectMenuPermissionsByUserId("testUser"))
                    .willReturn(List.of(new MenuPermission("leaf1", "R"), new MenuPermission("leaf2", "R")));
            given(authorityMapper.selectAllMenus())
                    .willReturn(List.of(
                            menu("cat1", null, "카테고리1", null, null),
                            menu("cat2", "", "카테고리2", null, null),
                            menu("leaf1", "cat1", "리프1", "/page1", null),
                            menu("leaf2", "cat2", "리프2", "/page2", null)));

            // when & then – 두 카테고리 모두 ROOT 레벨로 나타남
            mockMvc.perform(get("/api/menus/tree").with(user(user)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].menuId").value("cat1"))
                    .andExpect(jsonPath("$.data[1].menuId").value("cat2"));
        }
    }
}
