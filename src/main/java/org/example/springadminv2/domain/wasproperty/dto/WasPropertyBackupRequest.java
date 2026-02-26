package org.example.springadminv2.domain.wasproperty.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

public record WasPropertyBackupRequest(@NotEmpty List<@NotBlank String> instanceIds, String reason) {}
