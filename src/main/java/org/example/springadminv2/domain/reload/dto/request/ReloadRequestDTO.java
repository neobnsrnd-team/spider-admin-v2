package org.example.springadminv2.domain.reload.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ReloadRequestDTO(@NotBlank String groupId, @NotBlank String instanceId) {}
