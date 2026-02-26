package org.example.springadminv2.security;

import org.example.springadminv2.global.log.listener.SecurityLogEventListener;
import org.example.springadminv2.global.security.SecurityConfig;
import org.example.springadminv2.global.security.handler.CustomAccessDeniedHandler;
import org.example.springadminv2.global.security.handler.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SampleSecuredController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    SecurityLogEventListener.class
})
class SecurityExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("미인증 AJAX 요청 → 401 JSON")
    @WithAnonymousUser
    void unauthenticated_ajax_returns_401_json() throws Exception {
        // given – 미인증 AJAX 요청

        // when & then
        mockMvc.perform(get("/test/menu001").header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("미인증 일반 요청 → /login 리다이렉트")
    @WithAnonymousUser
    void unauthenticated_normal_redirects_to_login() throws Exception {
        // given – 미인증 일반 요청

        // when & then
        mockMvc.perform(get("/test/menu001"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("권한 없음 AJAX 요청 → 403 JSON")
    @WithMockUser(authorities = "OTHER:R")
    void forbidden_ajax_returns_403_json() throws Exception {
        // given – 권한 없는 사용자의 AJAX 요청

        // when & then
        mockMvc.perform(post("/test/menu001").with(csrf()).header("X-Requested-With", "XMLHttpRequest"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("권한 없음 일반 요청 → 403 에러")
    @WithMockUser(authorities = "OTHER:R")
    void forbidden_normal_returns_403() throws Exception {
        // given – 권한 없는 사용자의 일반 요청

        // when & then
        mockMvc.perform(post("/test/menu001").with(csrf())).andExpect(status().isForbidden());
    }
}
