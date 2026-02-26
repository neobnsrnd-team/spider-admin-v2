package org.example.springadminv2.domain.menu.controller;

import java.util.List;
import java.util.Set;

import org.example.springadminv2.domain.menu.dto.MenuCreateRequest;
import org.example.springadminv2.domain.menu.dto.MenuResponse;
import org.example.springadminv2.domain.menu.dto.MenuSortUpdateRequest;
import org.example.springadminv2.domain.menu.dto.MenuTreeNode;
import org.example.springadminv2.domain.menu.dto.MenuUpdateRequest;
import org.example.springadminv2.domain.menu.service.MenuService;
import org.example.springadminv2.global.exception.BusinessException;
import org.example.springadminv2.global.exception.ErrorType;
import org.example.springadminv2.global.exception.GlobalExceptionHandler;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = MenuController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    SecurityLogEventListener.class,
    GlobalExceptionHandler.class
})
class MenuControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MenuService menuService;

    // ── 테스트 픽스처 ──────────────────────────────────────────

    private static CustomUserDetails mockUser(String... authorities) {
        Set<SimpleGrantedAuthority> auths = new java.util.HashSet<>();
        for (String a : authorities) {
            auths.add(new SimpleGrantedAuthority(a));
        }
        return new CustomUserDetails("testuser", "password", "ROLE01", "1", 0, Set.copyOf(auths));
    }

    private static final CustomUserDetails RW_USER = mockUser("MENU:R", "MENU:W");
    private static final CustomUserDetails READ_ONLY_USER = mockUser("MENU:R");

    private static MenuResponse sampleMenu() {
        return new MenuResponse(
                "MENU001", "ROOT", 1, "시스템관리", "/system", "icon-system", "Y", "Y", "20260226120000", "admin");
    }

    private static MenuTreeNode sampleTreeNode() {
        MenuResponse menu = sampleMenu();
        MenuTreeNode child = new MenuTreeNode(
                "MENU002",
                "MENU001",
                1,
                "메뉴관리",
                "/system/menu",
                "icon-menu",
                "Y",
                "Y",
                "20260226120000",
                "admin",
                null);
        return MenuTreeNode.of(menu, List.of(child));
    }

    // ── 메뉴 트리 조회 ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/menus/tree - 메뉴 트리 조회")
    class GetMenuTree {

        @Test
        @DisplayName("트리 목록을 정상 반환한다")
        @WithMockUser(authorities = {"MENU:R", "MENU:W"})
        void returns_menu_tree() throws Exception {
            // given
            given(menuService.getMenuTree()).willReturn(List.of(sampleTreeNode()));

            // when & then
            mockMvc.perform(get("/api/menus/tree"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].menuId").value("MENU001"))
                    .andExpect(jsonPath("$.data[0].children[0].menuId").value("MENU002"));
        }
    }

    // ── 메뉴 상세 조회 ─────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/menus/{menuId} - 메뉴 상세 조회")
    class GetMenuDetail {

        @Test
        @DisplayName("존재하는 메뉴를 정상 반환한다")
        @WithMockUser(authorities = {"MENU:R", "MENU:W"})
        void returns_menu_detail() throws Exception {
            // given
            given(menuService.getMenuDetail("MENU001")).willReturn(sampleMenu());

            // when & then
            mockMvc.perform(get("/api/menus/MENU001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.menuId").value("MENU001"))
                    .andExpect(jsonPath("$.data.menuName").value("시스템관리"));
        }

        @Test
        @DisplayName("존재하지 않는 메뉴 → 404 RESOURCE_NOT_FOUND")
        @WithMockUser(authorities = {"MENU:R", "MENU:W"})
        void returns_404_when_not_found() throws Exception {
            // given
            given(menuService.getMenuDetail("UNKNOWN"))
                    .willThrow(new BusinessException(ErrorType.RESOURCE_NOT_FOUND, "menuId=UNKNOWN"));

            // when & then
            mockMvc.perform(get("/api/menus/UNKNOWN"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    // ── 메뉴 생성 ──────────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/menus - 메뉴 생성")
    class CreateMenu {

        @Test
        @DisplayName("메뉴를 생성하면 201을 반환한다")
        void creates_menu_returns_201() throws Exception {
            // given
            willDoNothing().given(menuService).createMenu(any(MenuCreateRequest.class), anyString());

            MenuCreateRequest request =
                    new MenuCreateRequest("MENU003", "대시보드", "ROOT", "/dashboard", "icon-dashboard", 3, "Y", "Y");

            // when & then
            mockMvc.perform(post("/api/menus")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("displayYn·useYn이 null이면 기본값 'Y'로 설정된다")
        void null_displayYn_useYn_defaults_to_Y() throws Exception {
            // given
            willDoNothing().given(menuService).createMenu(any(MenuCreateRequest.class), anyString());

            // displayYn, useYn을 null로 전달
            String json =
                    """
                    {"menuId":"MENU004","menuName":"테스트","priorMenuId":"ROOT",
                     "menuUrl":"/test","menuImage":"icon","sortOrder":1,
                     "displayYn":null,"useYn":null}""";

            // when & then
            mockMvc.perform(post("/api/menus")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── 메뉴 수정 ──────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/menus/{menuId} - 메뉴 수정")
    class UpdateMenu {

        @Test
        @DisplayName("메뉴를 수정하면 200을 반환한다")
        void updates_menu_returns_200() throws Exception {
            // given
            willDoNothing().given(menuService).updateMenu(eq("MENU001"), any(MenuUpdateRequest.class), anyString());

            MenuUpdateRequest request =
                    new MenuUpdateRequest("시스템관리(수정)", "ROOT", "/system", "icon-system", 1, "Y", "Y");

            // when & then
            mockMvc.perform(put("/api/menus/MENU001")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── 메뉴 삭제 ──────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /api/menus/{menuId} - 메뉴 삭제")
    class DeleteMenu {

        @Test
        @DisplayName("하위 메뉴가 없으면 삭제 성공 200")
        @WithMockUser(authorities = {"MENU:R", "MENU:W"})
        void deletes_menu_returns_200() throws Exception {
            // given
            willDoNothing().given(menuService).deleteMenu("MENU001");

            // when & then
            mockMvc.perform(delete("/api/menus/MENU001").with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("하위 메뉴가 존재하면 409 BUSINESS_RULE_VIOLATED")
        @WithMockUser(authorities = {"MENU:R", "MENU:W"})
        void returns_409_when_has_children() throws Exception {
            // given
            willThrow(new BusinessException(ErrorType.INVALID_STATE, "menuId=MENU001, children=2"))
                    .given(menuService)
                    .deleteMenu("MENU001");

            // when & then
            mockMvc.perform(delete("/api/menus/MENU001").with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("BUSINESS_RULE_VIOLATED"));
        }
    }

    // ── 메뉴 순서 변경 ─────────────────────────────────────────

    @Nested
    @DisplayName("PUT /api/menus/{menuId}/sort - 메뉴 순서 변경")
    class UpdateSortOrder {

        @Test
        @DisplayName("순서를 변경하면 200을 반환한다")
        void updates_sort_order_returns_200() throws Exception {
            // given
            willDoNothing().given(menuService).updateSortOrder(eq("MENU001"), anyInt(), anyString(), anyString());

            MenuSortUpdateRequest request = new MenuSortUpdateRequest(2, "ROOT");

            // when & then
            mockMvc.perform(put("/api/menus/MENU001/sort")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── 권한 검증 ──────────────────────────────────────────────

    @Nested
    @DisplayName("권한 부족 시 접근 거부")
    class PermissionDenied {

        @Test
        @DisplayName("READ 권한만 보유 → POST 403")
        void read_only_user_cannot_create_menu() throws Exception {
            // given - MENU:R 권한만 보유 (MENU:W 없음)

            MenuCreateRequest request =
                    new MenuCreateRequest("MENU003", "대시보드", "ROOT", "/dashboard", "icon-dashboard", 3, "Y", "Y");

            // when & then
            mockMvc.perform(post("/api/menus")
                            .with(user(READ_ONLY_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }
}
