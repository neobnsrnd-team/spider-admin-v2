package org.example.springadminv2.domain.system.menu.dto;

public record MenuResponse(
        String menuId,
        String priorMenuId,
        int sortOrder,
        String menuName,
        String menuUrl,
        String menuImage,
        String displayYn,
        String useYn,
        String lastUpdateDtime,
        String lastUpdateUserId) {}
