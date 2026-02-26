package org.example.springadminv2.security;

import org.example.springadminv2.global.log.listener.SecurityLogEventListener;
import org.example.springadminv2.global.security.SecurityConfig;
import org.example.springadminv2.global.security.handler.CustomAccessDeniedHandler;
import org.example.springadminv2.global.security.handler.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SampleSecuredController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    SecurityLogEventListener.class
})
class PreAuthorizeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Nested
    @DisplayName("메뉴 직접 권한")
    class MenuDirectAuthority {

        @Test
        @DisplayName("인증 없음 → 로그인 리다이렉트")
        @WithAnonymousUser
        void unauthenticated_redirects_to_login() throws Exception {
            // given – 인증되지 않은 사용자

            // when & then
            mockMvc.perform(get("/test/menu001")).andExpect(status().is3xxRedirection());
        }

        @Test
        @DisplayName("READ 권한 → GET 성공")
        @WithMockUser(authorities = "MENU001:R")
        void read_authority_allows_get() throws Exception {
            // given – MENU001:R 권한 보유

            // when & then
            mockMvc.perform(get("/test/menu001")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("READ 권한 → POST 403")
        @WithMockUser(authorities = "MENU001:R")
        void read_authority_denies_post() throws Exception {
            // given – MENU001:R 권한만 보유

            // when & then
            mockMvc.perform(post("/test/menu001").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("READ 권한 → PUT 403")
        @WithMockUser(authorities = "MENU001:R")
        void read_authority_denies_put() throws Exception {
            // given – MENU001:R 권한만 보유

            // when & then
            mockMvc.perform(put("/test/menu001").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("READ 권한 → DELETE 403")
        @WithMockUser(authorities = "MENU001:R")
        void read_authority_denies_delete() throws Exception {
            // given – MENU001:R 권한만 보유

            // when & then
            mockMvc.perform(delete("/test/menu001").with(csrf())).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("WRITE 권한 → GET 성공")
        @WithMockUser(authorities = {"MENU001:R", "MENU001:W"})
        void write_authority_allows_get() throws Exception {
            // given – MENU001:R, MENU001:W 권한 보유

            // when & then
            mockMvc.perform(get("/test/menu001")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("WRITE 권한 → POST 성공")
        @WithMockUser(authorities = {"MENU001:R", "MENU001:W"})
        void write_authority_allows_post() throws Exception {
            // given – MENU001:R, MENU001:W 권한 보유

            // when & then
            mockMvc.perform(post("/test/menu001").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("WRITE 권한 → PUT 성공")
        @WithMockUser(authorities = {"MENU001:R", "MENU001:W"})
        void write_authority_allows_put() throws Exception {
            // given – MENU001:R, MENU001:W 권한 보유

            // when & then
            mockMvc.perform(put("/test/menu001").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("WRITE 권한 → DELETE 성공")
        @WithMockUser(authorities = {"MENU001:R", "MENU001:W"})
        void write_authority_allows_delete() throws Exception {
            // given – MENU001:R, MENU001:W 권한 보유

            // when & then
            mockMvc.perform(delete("/test/menu001").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("다른 메뉴 권한 → 403")
        @WithMockUser(authorities = "MENU999:R")
        void wrong_menu_authority_returns_forbidden() throws Exception {
            // given – 다른 메뉴 권한만 보유

            // when & then
            mockMvc.perform(get("/test/menu001")).andExpect(status().isForbidden());
        }
    }

    @Nested
    @DisplayName("크로스 리소스 권한")
    class CrossResourceAuthority {

        @Test
        @DisplayName("직접 메뉴 권한으로 크로스 리소스 GET 성공")
        @WithMockUser(authorities = "v3_was_instance:R")
        void direct_menu_authority_allows_cross_resource_get() throws Exception {
            // given – v3_was_instance:R 직접 메뉴 권한 보유

            // when & then
            mockMvc.perform(get("/test/cross-resource")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("파생 리소스 권한으로 크로스 리소스 GET 성공")
        @WithMockUser(authorities = "RES_WASINSTANCE:R")
        void derived_resource_authority_allows_cross_resource_get() throws Exception {
            // given – RES_WASINSTANCE:R 파생 리소스 권한 보유

            // when & then
            mockMvc.perform(get("/test/cross-resource")).andExpect(status().isOk());
        }

        @Test
        @DisplayName("파생 리소스 WRITE 권한으로 크로스 리소스 POST 성공")
        @WithMockUser(authorities = "RES_WASINSTANCE:W")
        void derived_resource_write_allows_cross_resource_post() throws Exception {
            // given – RES_WASINSTANCE:W 파생 리소스 권한 보유

            // when & then
            mockMvc.perform(post("/test/cross-resource").with(csrf())).andExpect(status().isOk());
        }

        @Test
        @DisplayName("관련 없는 권한으로 크로스 리소스 GET 403")
        @WithMockUser(authorities = "MENU999:R")
        void unrelated_authority_denies_cross_resource_get() throws Exception {
            // given – 관련 없는 권한만 보유

            // when & then
            mockMvc.perform(get("/test/cross-resource")).andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("READ 리소스 권한으로 크로스 리소스 POST 403")
        @WithMockUser(authorities = "RES_WASINSTANCE:R")
        void read_resource_denies_cross_resource_post() throws Exception {
            // given – RES_WASINSTANCE:R만 보유 (WRITE 없음)

            // when & then
            mockMvc.perform(post("/test/cross-resource").with(csrf())).andExpect(status().isForbidden());
        }
    }
}
