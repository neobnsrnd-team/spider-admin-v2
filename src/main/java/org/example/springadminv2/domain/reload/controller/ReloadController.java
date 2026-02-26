package org.example.springadminv2.domain.reload.controller;

import org.example.springadminv2.domain.reload.dto.request.ReloadRequestDTO;
import org.example.springadminv2.domain.reload.dto.response.ReloadResponseDTO;
import org.example.springadminv2.domain.reload.service.ReloadService;
import org.example.springadminv2.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reload")
@RequiredArgsConstructor
public class ReloadController {

    private final ReloadService reloadService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROPERTY:W')")
    public ResponseEntity<ApiResponse<ReloadResponseDTO>> reload(@Validated @RequestBody ReloadRequestDTO request) {
        ReloadResponseDTO result = reloadService.reloadInstance(request.groupId(), request.instanceId());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
