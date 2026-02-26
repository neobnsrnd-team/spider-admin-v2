package org.example.springadminv2.security;

import org.example.springadminv2.global.log.adapter.CompositeLogEventAdapter;
import org.example.springadminv2.global.security.SecurityConfig;
import org.example.springadminv2.global.security.handler.CustomAccessDeniedHandler;
import org.example.springadminv2.global.security.handler.CustomAuthenticationEntryPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = SampleSecuredController.class)
@Import({SecurityConfig.class, CustomAuthenticationEntryPoint.class, CustomAccessDeniedHandler.class})
class PreAuthorizeSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CompositeLogEventAdapter logEventAdapter;

    @Test
    @DisplayName("인증 없음 → 로그인 리다이렉트")
    @WithAnonymousUser
    void unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/test/menu001")).andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("READ 권한 → GET 성공")
    @WithMockUser(authorities = "MENU001:R")
    void readAuthority_getSuccess() throws Exception {
        mockMvc.perform(get("/test/menu001")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("READ 권한 → POST 403")
    @WithMockUser(authorities = "MENU001:R")
    void readAuthority_postForbidden() throws Exception {
        mockMvc.perform(post("/test/menu001").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("READ 권한 → PUT 403")
    @WithMockUser(authorities = "MENU001:R")
    void readAuthority_putForbidden() throws Exception {
        mockMvc.perform(put("/test/menu001").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("READ 권한 → DELETE 403")
    @WithMockUser(authorities = "MENU001:R")
    void readAuthority_deleteForbidden() throws Exception {
        mockMvc.perform(delete("/test/menu001").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("WRITE 권한 → GET 성공")
    @WithMockUser(authorities = {"MENU001:R", "MENU001:W"})
    void writeAuthority_getSuccess() throws Exception {
        mockMvc.perform(get("/test/menu001")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("WRITE 권한 → POST 성공")
    @WithMockUser(authorities = {"MENU001:R", "MENU001:W"})
    void writeAuthority_postSuccess() throws Exception {
        mockMvc.perform(post("/test/menu001").with(csrf())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("WRITE 권한 → PUT 성공")
    @WithMockUser(authorities = {"MENU001:R", "MENU001:W"})
    void writeAuthority_putSuccess() throws Exception {
        mockMvc.perform(put("/test/menu001").with(csrf())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("WRITE 권한 → DELETE 성공")
    @WithMockUser(authorities = {"MENU001:R", "MENU001:W"})
    void writeAuthority_deleteSuccess() throws Exception {
        mockMvc.perform(delete("/test/menu001").with(csrf())).andExpect(status().isOk());
    }

    @Test
    @DisplayName("다른 메뉴 권한 → 403")
    @WithMockUser(authorities = "MENU999:R")
    void wrongMenuAuthority_forbidden() throws Exception {
        mockMvc.perform(get("/test/menu001")).andExpect(status().isForbidden());
    }
}
