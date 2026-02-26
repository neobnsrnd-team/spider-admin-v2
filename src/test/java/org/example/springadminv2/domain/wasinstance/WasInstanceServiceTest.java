package org.example.springadminv2.domain.wasinstance;

import java.util.List;

import org.example.springadminv2.domain.wasinstance.dto.WasInstanceResponse;
import org.example.springadminv2.domain.wasinstance.mapper.WasInstanceMapper;
import org.example.springadminv2.domain.wasinstance.service.WasInstanceService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class WasInstanceServiceTest {

    @Mock
    WasInstanceMapper wasInstanceMapper;

    @InjectMocks
    WasInstanceService wasInstanceService;

    @Test
    @DisplayName("전체 인스턴스 목록 조회")
    void returns_all_instances() {
        List<WasInstanceResponse> data = List.of(
                new WasInstanceResponse("0001", "WAS1", "Production WAS", "192.168.1.1", "8080"),
                new WasInstanceResponse("0002", "WAS2", "Staging WAS", "192.168.1.2", "8080"));
        given(wasInstanceMapper.selectAllInstances()).willReturn(data);

        List<WasInstanceResponse> result = wasInstanceService.getAllInstances();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).instanceId()).isEqualTo("0001");
    }
}
