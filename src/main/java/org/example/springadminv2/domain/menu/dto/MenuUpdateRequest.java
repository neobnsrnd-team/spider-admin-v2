package org.example.springadminv2.domain.menu.dto;

import jakarta.validation.constraints.NotBlank;

public record MenuUpdateRequest(
        @NotBlank String menuName,
        String priorMenuId,
        String menuUrl,
        String menuImage,
        int sortOrder,
        String displayYn,
        String useYn) {}
