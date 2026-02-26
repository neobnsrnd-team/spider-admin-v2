package org.example.springadminv2.domain.property.service;

import java.util.List;

import org.example.springadminv2.domain.property.dto.PropertyBackupRequest;
import org.example.springadminv2.domain.property.dto.PropertyGroupCreateRequest;
import org.example.springadminv2.domain.property.dto.PropertyGroupResponse;
import org.example.springadminv2.domain.property.dto.PropertyGroupSearchRequest;
import org.example.springadminv2.domain.property.dto.PropertyItemRequest;
import org.example.springadminv2.domain.property.dto.PropertyResponse;
import org.example.springadminv2.domain.property.dto.PropertySaveRequest;
import org.example.springadminv2.domain.property.mapper.PropertyMapper;
import org.example.springadminv2.domain.wasproperty.mapper.WasPropertyMapper;
import org.example.springadminv2.global.dto.PageResponse;
import org.example.springadminv2.global.exception.BaseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PropertyServiceTest {

    @Mock
    PropertyMapper propertyMapper;

    @Mock
    WasPropertyMapper wasPropertyMapper;

    @InjectMocks
    PropertyService propertyService;

    // ── getPropertyGroups ──

    @Nested
    @DisplayName("getPropertyGroups")
    class GetPropertyGroups {

        @Test
        @DisplayName("페이징 검색 정상")
        void returns_paged_result() {
            PropertyGroupSearchRequest req = new PropertyGroupSearchRequest(null, null, 0, 20, null, null);
            given(propertyMapper.countPropertyGroups(any())).willReturn(2L);
            given(propertyMapper.selectPropertyGroups(any()))
                    .willReturn(List.of(
                            new PropertyGroupResponse("FW_CORE", "코어", 5),
                            new PropertyGroupResponse("FW_LOG", "로그", 3)));

            PageResponse<PropertyGroupResponse> result = propertyService.getPropertyGroups(req);

            assertThat(result.totalElements()).isEqualTo(2);
            assertThat(result.content()).hasSize(2);
            assertThat(result.content().get(0).groupId()).isEqualTo("FW_CORE");
        }
    }

    // ── createPropertyGroup ──

    @Nested
    @DisplayName("createPropertyGroup")
    class CreatePropertyGroup {

        @Test
        @DisplayName("정상 생성")
        void creates_group_with_properties() {
            given(propertyMapper.existsPropertyGroup("NEW_GRP")).willReturn(0);

            List<PropertyItemRequest> items =
                    List.of(new PropertyItemRequest("p1", "name1", "desc1", "C", null, "val", null, "C"));
            PropertyGroupCreateRequest req = new PropertyGroupCreateRequest("NEW_GRP", "New Group", items);

            propertyService.createPropertyGroup(req, "admin");

            then(propertyMapper).should().insertProperty(eq(items.get(0)), eq("NEW_GRP"), eq("admin"), anyString());
        }

        @Test
        @DisplayName("중복 → DUPLICATE_RESOURCE")
        void throws_when_duplicate() {
            given(propertyMapper.existsPropertyGroup("EXISTING")).willReturn(1);

            PropertyGroupCreateRequest req = new PropertyGroupCreateRequest("EXISTING", "Exists", List.of());

            assertThatThrownBy(() -> propertyService.createPropertyGroup(req, "admin"))
                    .isInstanceOf(BaseException.class)
                    .hasMessageContaining("EXISTING");
        }
    }

    // ── deletePropertyGroup ──

    @Nested
    @DisplayName("deletePropertyGroup")
    class DeletePropertyGroup {

        @Test
        @DisplayName("정상 삭제 — cascade")
        void deletes_with_cascade() {
            given(propertyMapper.existsPropertyGroup("FW_CORE")).willReturn(1);

            propertyService.deletePropertyGroup("FW_CORE");

            then(wasPropertyMapper).should().deleteWasPropertiesByGroupId("FW_CORE");
            then(propertyMapper).should().deletePropertiesByGroupId("FW_CORE");
        }

        @Test
        @DisplayName("미존재 → RESOURCE_NOT_FOUND")
        void throws_when_not_found() {
            given(propertyMapper.existsPropertyGroup("UNKNOWN")).willReturn(0);

            assertThatThrownBy(() -> propertyService.deletePropertyGroup("UNKNOWN"))
                    .isInstanceOf(BaseException.class)
                    .hasMessageContaining("UNKNOWN");
        }
    }

    // ── saveProperties ──

    @Nested
    @DisplayName("saveProperties")
    class SaveProperties {

        @Test
        @DisplayName("C/U/D 각각 올바른 mapper 호출")
        void dispatches_crud_correctly() {
            List<PropertyItemRequest> items = List.of(
                    new PropertyItemRequest("p_new", "New", "New desc", "C", null, "val", null, "C"),
                    new PropertyItemRequest("p_upd", "Upd", "Upd desc", "C", null, "val2", null, "U"),
                    new PropertyItemRequest("p_del", "Del", "Del desc", "C", null, "val3", null, "D"));
            PropertySaveRequest req = new PropertySaveRequest("FW_CORE", items);

            propertyService.saveProperties(req, "admin");

            then(propertyMapper).should().insertProperty(eq(items.get(0)), eq("FW_CORE"), eq("admin"), anyString());
            then(propertyMapper).should().updateProperty(eq(items.get(1)), eq("FW_CORE"), eq("admin"), anyString());
            then(propertyMapper).should().deleteProperty("FW_CORE", "p_del");
        }
    }

    // ── backupPropertyGroup ──

    @Nested
    @DisplayName("backupPropertyGroup")
    class BackupPropertyGroup {

        @Test
        @DisplayName("maxVersion + 1로 히스토리 INSERT")
        void increments_version_and_inserts() {
            given(propertyMapper.selectMaxVersion("FW_CORE")).willReturn(3);

            propertyService.backupPropertyGroup("FW_CORE", new PropertyBackupRequest("test"), "admin");

            then(propertyMapper)
                    .should()
                    .insertBatchHistory(eq("FW_CORE"), eq(4), eq("test"), eq("admin"), anyString());
        }
    }

    // ── restorePropertyGroup ──

    @Nested
    @DisplayName("restorePropertyGroup")
    class RestorePropertyGroup {

        @Test
        @DisplayName("현재 삭제 → 히스토리 복원")
        void deletes_current_then_restores() {
            propertyService.restorePropertyGroup("FW_CORE", 2, "admin");

            then(propertyMapper).should().deletePropertiesByGroupId("FW_CORE");
            then(propertyMapper).should().restoreFromHistory(eq("FW_CORE"), eq(2), eq("admin"), anyString());
        }
    }

    // ── getPropertiesByGroup ──

    @Nested
    @DisplayName("getPropertiesByGroup")
    class GetPropertiesByGroup {

        @Test
        @DisplayName("그룹 내 속성 조회")
        void returns_properties() {
            List<PropertyResponse> data = List.of(
                    new PropertyResponse("FW_CORE", "timeout", "타임아웃", "설명", "N", "1~60", "30", "admin", "20260227"));
            given(propertyMapper.selectPropertiesByGroupId("FW_CORE", null, null))
                    .willReturn(data);

            List<PropertyResponse> result = propertyService.getPropertiesByGroup("FW_CORE", null, null);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).propertyId()).isEqualTo("timeout");
        }
    }
}
