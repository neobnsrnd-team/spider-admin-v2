package org.example.springadminv2.domain.property.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record PropertyGroupCreateRequest(
        @NotBlank String groupId, @NotBlank String groupName, @Valid List<PropertyItemRequest> properties) {}
