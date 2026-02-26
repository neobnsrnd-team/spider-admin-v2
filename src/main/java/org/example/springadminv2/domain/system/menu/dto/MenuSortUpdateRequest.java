package org.example.springadminv2.domain.system.menu.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MenuSortUpdateRequest(@NotNull Integer sortOrder, @NotBlank String priorMenuId) {}
