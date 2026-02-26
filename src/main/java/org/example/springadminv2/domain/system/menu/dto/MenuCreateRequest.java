package org.example.springadminv2.domain.system.menu.dto;

import jakarta.validation.constraints.NotBlank;

public record MenuCreateRequest(
        @NotBlank String menuId,
        @NotBlank String menuName,
        String priorMenuId,
        String menuUrl,
        String menuImage,
        int sortOrder,
        String displayYn,
        String useYn) {

    public MenuCreateRequest {
        if (displayYn == null) displayYn = "Y";
        if (useYn == null) useYn = "Y";
    }
}
