package org.example.springadminv2.domain.wasproperty;

import java.util.List;

import org.example.springadminv2.domain.wasproperty.dto.WasPropertyBackupRequest;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertySaveRequest;
import org.example.springadminv2.domain.wasproperty.mapper.WasPropertyMapper;
import org.example.springadminv2.domain.wasproperty.service.WasPropertyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class WasPropertyServiceTest {

    @Mock
    WasPropertyMapper wasPropertyMapper;

    @InjectMocks
    WasPropertyService wasPropertyService;

    @Nested
    @DisplayName("getWasProperties")
    class GetWasProperties {

        @Test
        @DisplayName("인스턴스별 속성값 조회")
        void returns_was_properties() {
            List<WasPropertyResponse> data = List.of(
                    new WasPropertyResponse("0001", "WAS1", "val1", "desc1"),
                    new WasPropertyResponse("0002", "WAS2", null, null));
            given(wasPropertyMapper.selectWasProperties("FW_CORE", "timeout")).willReturn(data);

            List<WasPropertyResponse> result = wasPropertyService.getWasProperties("FW_CORE", "timeout");

            assertThat(result).hasSize(2);
            assertThat(result.get(0).propertyValue()).isEqualTo("val1");
        }
    }

    @Nested
    @DisplayName("saveWasProperties")
    class SaveWasProperties {

        @Test
        @DisplayName("MERGE 일괄 저장")
        void merges_all() {
            List<WasPropertySaveRequest> requests = List.of(
                    new WasPropertySaveRequest("0001", "FW_CORE", "timeout", "30", "prod"),
                    new WasPropertySaveRequest("0002", "FW_CORE", "timeout", "60", "stg"));

            wasPropertyService.saveWasProperties(requests);

            then(wasPropertyMapper).should().mergeWasProperty("0001", "FW_CORE", "timeout", "30", "prod");
            then(wasPropertyMapper).should().mergeWasProperty("0002", "FW_CORE", "timeout", "60", "stg");
        }
    }

    @Nested
    @DisplayName("backupWasProperties")
    class BackupWasProperties {

        @Test
        @DisplayName("선택된 인스턴스에 대해 maxVersion + 1 백업")
        void backs_up_selected_instances() {
            given(wasPropertyMapper.selectWasMaxVersion("FW_CORE", "0001")).willReturn(2);
            given(wasPropertyMapper.selectWasMaxVersion("FW_CORE", "0002")).willReturn(0);

            WasPropertyBackupRequest req = new WasPropertyBackupRequest(List.of("0001", "0002"), "test");
            wasPropertyService.backupWasProperties("FW_CORE", req, "admin");

            then(wasPropertyMapper)
                    .should()
                    .insertBatchWasHistory(eq("FW_CORE"), eq("0001"), eq(3), eq("test"), eq("admin"), anyString());
            then(wasPropertyMapper)
                    .should()
                    .insertBatchWasHistory(eq("FW_CORE"), eq("0002"), eq(1), eq("test"), eq("admin"), anyString());
        }
    }

    @Nested
    @DisplayName("restoreWasProperties")
    class RestoreWasProperties {

        @Test
        @DisplayName("현재 삭제 → 히스토리 복원")
        void deletes_then_restores() {
            wasPropertyService.restoreWasProperties("FW_CORE", "0001", 2);

            then(wasPropertyMapper).should().deleteWasPropertiesByInstanceAndGroup("FW_CORE", "0001");
            then(wasPropertyMapper).should().restoreWasFromHistory("FW_CORE", "0001", 2, null, null);
        }
    }
}
