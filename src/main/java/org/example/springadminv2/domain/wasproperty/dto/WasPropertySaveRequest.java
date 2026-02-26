package org.example.springadminv2.domain.wasproperty.dto;

import jakarta.validation.constraints.NotBlank;

public record WasPropertySaveRequest(
        @NotBlank String instanceId,
        @NotBlank String groupId,
        @NotBlank String propertyId,
        String value,
        String desc) {}
