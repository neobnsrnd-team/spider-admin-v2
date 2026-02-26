package org.example.springadminv2.domain.property.controller;

import java.util.List;
import java.util.Set;

import org.example.springadminv2.domain.property.dto.PropertyGroupResponse;
import org.example.springadminv2.domain.property.dto.PropertyHistoryVersionResponse;
import org.example.springadminv2.domain.property.dto.PropertyResponse;
import org.example.springadminv2.domain.property.service.PropertyService;
import org.example.springadminv2.domain.wasinstance.service.WasInstanceService;
import org.example.springadminv2.domain.wasproperty.service.WasPropertyService;
import org.example.springadminv2.global.dto.PageResponse;
import org.example.springadminv2.global.exception.BaseException;
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
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PropertyController.class)
@Import({
    SecurityConfig.class,
    CustomAuthenticationEntryPoint.class,
    CustomAccessDeniedHandler.class,
    SecurityLogEventListener.class,
    GlobalExceptionHandler.class
})
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PropertyService propertyService;

    @MockBean
    private WasPropertyService wasPropertyService;

    @MockBean
    private WasInstanceService wasInstanceService;

    private static CustomUserDetails mockUser(String... authorities) {
        Set<SimpleGrantedAuthority> auths = new java.util.HashSet<>();
        for (String a : authorities) {
            auths.add(new SimpleGrantedAuthority(a));
        }
        return new CustomUserDetails("testuser", "password", "ROLE01", "1", 0, Set.copyOf(auths));
    }

    private static final CustomUserDetails RW_USER = mockUser("PROPERTY:R", "PROPERTY:W");
    private static final CustomUserDetails READ_ONLY_USER = mockUser("PROPERTY:R");

    // ── 그룹 목록 ──

    @Nested
    @DisplayName("GET /api/properties/groups/page - 그룹 페이징 검색")
    class GetPropertyGroups {

        @Test
        @DisplayName("정상 조회 200")
        void returns_paged_groups() throws Exception {
            List<PropertyGroupResponse> content = List.of(new PropertyGroupResponse("FW_CORE", "코어 설정", 5));
            PageResponse<PropertyGroupResponse> page = PageResponse.of(content, 1, 0, 20);
            given(propertyService.getPropertyGroups(any())).willReturn(page);

            mockMvc.perform(get("/api/properties/groups/page").with(user(RW_USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content[0].groupId").value("FW_CORE"))
                    .andExpect(jsonPath("$.data.totalElements").value(1));
        }

        @Test
        @DisplayName("빈 결과 200")
        void returns_empty_page() throws Exception {
            given(propertyService.getPropertyGroups(any())).willReturn(PageResponse.of(List.of(), 0, 0, 20));

            mockMvc.perform(get("/api/properties/groups/page").with(user(RW_USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content").isEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(0));
        }
    }

    // ── 그룹 생성 ──

    @Nested
    @DisplayName("POST /api/properties/groups - 그룹 생성")
    class CreatePropertyGroup {

        @Test
        @DisplayName("정상 생성 201")
        void creates_group_returns_201() throws Exception {
            willDoNothing().given(propertyService).createPropertyGroup(any(), anyString());

            String json =
                    """
                    {"groupId":"NEW_GRP","groupName":"New Group","properties":[
                      {"propertyId":"p1","propertyName":"name1","propertyDesc":"desc1","dataType":"C"}
                    ]}""";

            mockMvc.perform(post("/api/properties/groups")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("중복 → 409")
        void duplicate_returns_409() throws Exception {
            willThrow(new BaseException(ErrorType.DUPLICATE_RESOURCE, "groupId=EXISTING"))
                    .given(propertyService)
                    .createPropertyGroup(any(), anyString());

            String json =
                    """
                    {"groupId":"EXISTING","groupName":"Exists","properties":[
                      {"propertyId":"p1","propertyName":"n","propertyDesc":"d","dataType":"C"}
                    ]}""";

            mockMvc.perform(post("/api/properties/groups")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("DUPLICATE_RESOURCE"));
        }
    }

    // ── 그룹 삭제 ──

    @Nested
    @DisplayName("DELETE /api/properties/groups/{groupId} - 그룹 삭제")
    class DeletePropertyGroup {

        @Test
        @DisplayName("정상 삭제 200")
        void deletes_group_returns_200() throws Exception {
            willDoNothing().given(propertyService).deletePropertyGroup("FW_CORE");

            mockMvc.perform(delete("/api/properties/groups/FW_CORE")
                            .with(user(RW_USER))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("미존재 → 404")
        void not_found_returns_404() throws Exception {
            willThrow(new BaseException(ErrorType.RESOURCE_NOT_FOUND, "groupId=UNKNOWN"))
                    .given(propertyService)
                    .deletePropertyGroup("UNKNOWN");

            mockMvc.perform(delete("/api/properties/groups/UNKNOWN")
                            .with(user(RW_USER))
                            .with(csrf()))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
        }
    }

    // ── 속성 목록 ──

    @Nested
    @DisplayName("GET /api/properties/groups/{groupId}/properties")
    class GetProperties {

        @Test
        @DisplayName("정상 조회 200")
        void returns_properties() throws Exception {
            List<PropertyResponse> data = List.of(new PropertyResponse(
                    "FW_CORE", "timeout", "타임아웃", "연결 타임아웃", "N", "1~60", "30", "admin", "20260227"));
            given(propertyService.getPropertiesByGroup("FW_CORE", null, null)).willReturn(data);

            mockMvc.perform(get("/api/properties/groups/FW_CORE/properties").with(user(RW_USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].propertyId").value("timeout"));
        }
    }

    // ── 속성 저장 ──

    @Nested
    @DisplayName("POST /api/properties/save")
    class SaveProperties {

        @Test
        @DisplayName("정상 저장 200")
        void saves_properties() throws Exception {
            willDoNothing().given(propertyService).saveProperties(any(), anyString());

            String json =
                    """
                    {"groupId":"FW_CORE","items":[
                      {"propertyId":"p1","propertyName":"n","propertyDesc":"d","dataType":"C","crud":"U"}
                    ]}""";

            mockMvc.perform(post("/api/properties/save")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── 백업/복원 ──

    @Nested
    @DisplayName("히스토리 백업/복원")
    class BackupRestore {

        @Test
        @DisplayName("백업 200")
        void backup_returns_200() throws Exception {
            willDoNothing().given(propertyService).backupPropertyGroup(anyString(), any(), anyString());

            mockMvc.perform(post("/api/properties/groups/FW_CORE/backup")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"reason\":\"test backup\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("버전 목록 200")
        void versions_returns_200() throws Exception {
            given(propertyService.getHistoryVersions("FW_CORE"))
                    .willReturn(List.of(new PropertyHistoryVersionResponse(1, "backup1", "admin", "20260227")));

            mockMvc.perform(get("/api/properties/groups/FW_CORE/history/versions")
                            .with(user(RW_USER)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].version").value(1));
        }

        @Test
        @DisplayName("복원 200")
        void restore_returns_200() throws Exception {
            willDoNothing().given(propertyService).restorePropertyGroup(anyString(), any(int.class), anyString());

            mockMvc.perform(post("/api/properties/groups/FW_CORE/restore/1")
                            .with(user(RW_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }
    }

    // ── 권한 검증 ──

    @Nested
    @DisplayName("권한 검증")
    class PermissionDenied {

        @Test
        @DisplayName("READ만 → POST 403")
        void read_only_cannot_create() throws Exception {
            String json =
                    """
                    {"groupId":"GRP","groupName":"Name","properties":[
                      {"propertyId":"p","propertyName":"n","propertyDesc":"d","dataType":"C"}
                    ]}""";

            mockMvc.perform(post("/api/properties/groups")
                            .with(user(READ_ONLY_USER))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("READ만 → DELETE 403")
        void read_only_cannot_delete() throws Exception {
            mockMvc.perform(delete("/api/properties/groups/FW_CORE")
                            .with(user(READ_ONLY_USER))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }
    }
}
