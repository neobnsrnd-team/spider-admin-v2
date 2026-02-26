package org.example.springadminv2.domain.property.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public record PropertySaveRequest(@NotBlank String groupId, @Valid List<PropertyItemRequest> items) {}
