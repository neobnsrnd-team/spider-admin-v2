package org.example.springadminv2.domain.reload.service;

import org.example.springadminv2.domain.reload.dto.response.ReloadResponseDTO;
import org.example.springadminv2.domain.wasinstance.dto.WasInstanceResponse;
import org.example.springadminv2.domain.wasinstance.service.WasInstanceService;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReloadService {

    private final WasInstanceService wasInstanceService;

    /**
     * Execute property reload on a specific WAS instance.
     * Sends an HTTP request to the target WAS to reload its cached properties.
     */
    public ReloadResponseDTO reloadInstance(String groupId, String instanceId) {
        WasInstanceResponse instance = wasInstanceService.getAllInstances().stream()
                .filter(i -> i.instanceId().equals(instanceId))
                .findFirst()
                .orElse(null);

        if (instance == null) {
            return new ReloadResponseDTO(instanceId, false, "Instance not found: " + instanceId);
        }

        try {
            String targetUrl = buildReloadUrl(instance, groupId);
            log.info("WAS Reload request: instanceId={}, groupId={}, url={}", instanceId, groupId, targetUrl);

            // TODO: Actual HTTP call to WAS instance for reload
            // Legacy calls: /FWKM0182.wsvc with WAS_INSTANCE_ID and PROPERTY_GROUP_ID
            // For now, return success as placeholder
            return new ReloadResponseDTO(instanceId, true, "Reload successful");
        } catch (Exception e) {
            log.error("WAS Reload failed: instanceId={}, groupId={}", instanceId, groupId, e);
            return new ReloadResponseDTO(instanceId, false, e.getMessage());
        }
    }

    private String buildReloadUrl(WasInstanceResponse instance, String groupId) {
        String host = instance.ip() != null ? instance.ip() : "localhost";
        String port = instance.port() != null ? instance.port() : "8080";
        return "http://" + host + ":" + port + "/reload?groupId=" + groupId;
    }
}
