package org.example.springadminv2.domain.wasproperty.controller;

import java.util.List;

import org.example.springadminv2.domain.wasproperty.dto.WasPropertyBackupRequest;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyHistoryResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyHistoryVersionResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertySaveRequest;
import org.example.springadminv2.domain.wasproperty.service.WasPropertyService;
import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.security.CustomUserDetails;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/was-properties")
@RequiredArgsConstructor
public class WasPropertyController {

    private final WasPropertyService wasPropertyService;

    @GetMapping("/groups/{groupId}/properties/{propertyId}")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasPropertyResponse>>> getWasProperties(
            @PathVariable String groupId, @PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.success(wasPropertyService.getWasProperties(groupId, propertyId)));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> saveWasProperties(
            @Validated @RequestBody List<WasPropertySaveRequest> requests) {
        wasPropertyService.saveWasProperties(requests);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/groups/{groupId}/backup")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> backupWasProperties(
            @PathVariable String groupId,
            @Validated @RequestBody WasPropertyBackupRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        wasPropertyService.backupWasProperties(groupId, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/groups/{groupId}/instances/{instanceId}/history/versions")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasPropertyHistoryVersionResponse>>> getWasHistoryVersions(
            @PathVariable String groupId, @PathVariable String instanceId) {
        return ResponseEntity.ok(ApiResponse.success(wasPropertyService.getWasHistoryVersions(groupId, instanceId)));
    }

    @GetMapping("/groups/{groupId}/instances/{instanceId}/history/{version}")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasPropertyHistoryResponse>>> getWasHistoryByVersion(
            @PathVariable String groupId, @PathVariable String instanceId, @PathVariable int version) {
        return ResponseEntity.ok(
                ApiResponse.success(wasPropertyService.getWasHistoryByVersion(groupId, instanceId, version)));
    }

    @GetMapping("/groups/{groupId}/instances/{instanceId}/current")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasPropertyResponse>>> getCurrentWasProperties(
            @PathVariable String groupId, @PathVariable String instanceId) {
        return ResponseEntity.ok(ApiResponse.success(wasPropertyService.getCurrentWasProperties(groupId, instanceId)));
    }

    @PostMapping("/groups/{groupId}/instances/{instanceId}/restore/{version}")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> restoreWasProperties(
            @PathVariable String groupId, @PathVariable String instanceId, @PathVariable int version) {
        wasPropertyService.restoreWasProperties(groupId, instanceId, version);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
