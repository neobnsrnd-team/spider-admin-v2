package org.example.springadminv2.domain.wasinstance.controller;

import java.util.List;

import org.example.springadminv2.domain.wasinstance.dto.WasInstanceResponse;
import org.example.springadminv2.domain.wasinstance.service.WasInstanceService;
import org.example.springadminv2.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/was-instances")
@RequiredArgsConstructor
public class WasInstanceController {

    private final WasInstanceService wasInstanceService;

    @GetMapping
    @PreAuthorize("hasAuthority('WAS_INSTANCE:R')")
    public ResponseEntity<ApiResponse<List<WasInstanceResponse>>> getAllInstances() {
        List<WasInstanceResponse> instances = wasInstanceService.getAllInstances();
        return ResponseEntity.ok(ApiResponse.success(instances));
    }
}
