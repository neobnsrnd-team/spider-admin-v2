package org.example.springadminv2.domain.property.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.example.springadminv2.domain.property.dto.PropertyBackupRequest;
import org.example.springadminv2.domain.property.dto.PropertyGroupCreateRequest;
import org.example.springadminv2.domain.property.dto.PropertyGroupResponse;
import org.example.springadminv2.domain.property.dto.PropertyGroupSearchRequest;
import org.example.springadminv2.domain.property.dto.PropertyHistoryResponse;
import org.example.springadminv2.domain.property.dto.PropertyHistoryVersionResponse;
import org.example.springadminv2.domain.property.dto.PropertyResponse;
import org.example.springadminv2.domain.property.dto.PropertySaveRequest;
import org.example.springadminv2.domain.property.service.PropertyService;
import org.example.springadminv2.domain.wasinstance.dto.WasInstanceResponse;
import org.example.springadminv2.domain.wasinstance.service.WasInstanceService;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyBackupRequest;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyHistoryResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyHistoryVersionResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertyResponse;
import org.example.springadminv2.domain.wasproperty.dto.WasPropertySaveRequest;
import org.example.springadminv2.domain.wasproperty.service.WasPropertyService;
import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.dto.PageResponse;
import org.example.springadminv2.global.security.CustomUserDetails;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/properties")
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService propertyService;
    private final WasPropertyService wasPropertyService;
    private final WasInstanceService wasInstanceService;

    // ── 그룹 ──

    @GetMapping("/groups/page")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<PageResponse<PropertyGroupResponse>>> getPropertyGroups(
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortDirection) {
        PropertyGroupSearchRequest req =
                new PropertyGroupSearchRequest(searchType, keyword, page, size, sortBy, sortDirection);
        return ResponseEntity.ok(ApiResponse.success(propertyService.getPropertyGroups(req)));
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<PropertyGroupResponse>>> getAllPropertyGroups() {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getAllPropertyGroups()));
    }

    @GetMapping("/groups/{groupId}/exists")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<Boolean>> existsPropertyGroup(@PathVariable String groupId) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.existsPropertyGroup(groupId)));
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> createPropertyGroup(
            @Validated @RequestBody PropertyGroupCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        propertyService.createPropertyGroup(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    @DeleteMapping("/groups/{groupId}")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> deletePropertyGroup(@PathVariable String groupId) {
        propertyService.deletePropertyGroup(groupId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── 속성 ──

    @GetMapping("/groups/{groupId}/properties")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> getProperties(
            @PathVariable String groupId,
            @RequestParam(required = false) String searchType,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(
                ApiResponse.success(propertyService.getPropertiesByGroup(groupId, searchType, keyword)));
    }

    @PostMapping("/save")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> saveProperties(
            @Validated @RequestBody PropertySaveRequest request, @AuthenticationPrincipal CustomUserDetails user) {
        propertyService.saveProperties(request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── WAS 속성 ──

    @GetMapping("/groups/{groupId}/properties/{propertyId}/was")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasPropertyResponse>>> getWasProperties(
            @PathVariable String groupId, @PathVariable String propertyId) {
        return ResponseEntity.ok(ApiResponse.success(wasPropertyService.getWasProperties(groupId, propertyId)));
    }

    @PostMapping("/was/save")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> saveWasProperties(
            @Validated @RequestBody List<WasPropertySaveRequest> requests) {
        wasPropertyService.saveWasProperties(requests);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/was/instances")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasInstanceResponse>>> getWasInstances() {
        return ResponseEntity.ok(ApiResponse.success(wasInstanceService.getAllInstances()));
    }

    // ── 히스토리 (속성) ──

    @PostMapping("/groups/{groupId}/backup")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> backupPropertyGroup(
            @PathVariable String groupId,
            @RequestBody PropertyBackupRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        propertyService.backupPropertyGroup(groupId, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/groups/{groupId}/history/versions")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<PropertyHistoryVersionResponse>>> getHistoryVersions(
            @PathVariable String groupId) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getHistoryVersions(groupId)));
    }

    @GetMapping("/groups/{groupId}/history/{version}")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<PropertyHistoryResponse>>> getHistoryByVersion(
            @PathVariable String groupId, @PathVariable int version) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.getHistoryByVersion(groupId, version)));
    }

    @PostMapping("/groups/{groupId}/restore/{version}")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> restorePropertyGroup(
            @PathVariable String groupId, @PathVariable int version, @AuthenticationPrincipal CustomUserDetails user) {
        propertyService.restorePropertyGroup(groupId, version, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── 히스토리 (WAS) ──

    @PostMapping("/groups/{groupId}/was/backup")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> backupWasProperties(
            @PathVariable String groupId,
            @Validated @RequestBody WasPropertyBackupRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        wasPropertyService.backupWasProperties(groupId, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/groups/{groupId}/was/{instanceId}/history/versions")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasPropertyHistoryVersionResponse>>> getWasHistoryVersions(
            @PathVariable String groupId, @PathVariable String instanceId) {
        return ResponseEntity.ok(ApiResponse.success(wasPropertyService.getWasHistoryVersions(groupId, instanceId)));
    }

    @GetMapping("/groups/{groupId}/was/{instanceId}/history/{version}")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasPropertyHistoryResponse>>> getWasHistoryByVersion(
            @PathVariable String groupId, @PathVariable String instanceId, @PathVariable int version) {
        return ResponseEntity.ok(
                ApiResponse.success(wasPropertyService.getWasHistoryByVersion(groupId, instanceId, version)));
    }

    @GetMapping("/groups/{groupId}/was/{instanceId}/current")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<ApiResponse<List<WasPropertyResponse>>> getCurrentWasProperties(
            @PathVariable String groupId, @PathVariable String instanceId) {
        return ResponseEntity.ok(ApiResponse.success(wasPropertyService.getCurrentWasProperties(groupId, instanceId)));
    }

    @PostMapping("/groups/{groupId}/was/{instanceId}/restore/{version}")
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<Void>> restoreWasProperties(
            @PathVariable String groupId, @PathVariable String instanceId, @PathVariable int version) {
        wasPropertyService.restoreWasProperties(groupId, instanceId, version);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ── 엑셀/파일 내보내기 ──

    @PostMapping("/excel")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<byte[]> exportExcel(@RequestParam(required = false) String groupId) throws IOException {
        byte[] workbook = propertyService.exportExcel(groupId);
        String fileName = propertyService.generateExcelFileName();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(workbook, headers, HttpStatus.OK);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('PROPERTY:R')")
    public ResponseEntity<byte[]> exportFile(
            @RequestParam(required = false) String groupId, @RequestParam(defaultValue = "properties") String format) {
        String content = propertyService.exportFile(groupId, format);
        String extension = "yaml".equalsIgnoreCase(format) ? ".yaml" : ".properties";
        String fileName = "property-export" + extension;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build());
        headers.setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));

        return new ResponseEntity<>(content.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.OK);
    }
}
