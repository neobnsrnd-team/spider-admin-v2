package org.example.springadminv2.domain.wasproperty.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.example.springadminv2.domain.wasproperty.dto.WasPropertyBackupRequest;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyHistoryResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyHistoryVersionResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertySaveRequest;
import org.example.springadminv2.domain.wasproperty.mapper.WasPropertyMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WasPropertyService {

    private static final DateTimeFormatter TIMESTAMP_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final WasPropertyMapper wasPropertyMapper;

    @Transactional(readOnly = true)
    public List<WasPropertyResponse> getWasProperties(String groupId, String propertyId) {
        return wasPropertyMapper.selectWasProperties(groupId, propertyId);
    }

    @Transactional
    public void saveWasProperties(List<WasPropertySaveRequest> requests) {
        for (WasPropertySaveRequest req : requests) {
            wasPropertyMapper.mergeWasProperty(
                    req.instanceId(), req.groupId(), req.propertyId(), req.value(), req.desc());
        }
    }

    @Transactional
    public void backupWasProperties(String groupId, WasPropertyBackupRequest request, String userId) {
        String now = LocalDateTime.now().format(TIMESTAMP_FMT);
        for (String instanceId : request.instanceIds()) {
            int maxVersion = wasPropertyMapper.selectWasMaxVersion(groupId, instanceId);
            wasPropertyMapper.insertBatchWasHistory(groupId, instanceId, maxVersion + 1, request.reason(), userId, now);
        }
    }

    @Transactional(readOnly = true)
    public List<WasPropertyHistoryVersionResponse> getWasHistoryVersions(String groupId, String instanceId) {
        return wasPropertyMapper.selectWasHistoryVersions(groupId, instanceId);
    }

    @Transactional(readOnly = true)
    public List<WasPropertyHistoryResponse> getWasHistoryByVersion(String groupId, String instanceId, int version) {
        return wasPropertyMapper.selectWasHistoryByVersion(groupId, instanceId, version);
    }

    @Transactional(readOnly = true)
    public List<WasPropertyResponse> getCurrentWasProperties(String groupId, String instanceId) {
        return wasPropertyMapper.selectCurrentWasProperties(groupId, instanceId);
    }

    @Transactional
    public void restoreWasProperties(String groupId, String instanceId, int version) {
        wasPropertyMapper.deleteWasPropertiesByInstanceAndGroup(groupId, instanceId);
        wasPropertyMapper.restoreWasFromHistory(groupId, instanceId, version, null, null);
    }

    @Transactional
    public void deleteByGroupId(String groupId) {
        wasPropertyMapper.deleteWasPropertiesByGroupId(groupId);
    }
}
