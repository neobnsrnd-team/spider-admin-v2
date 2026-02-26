package org.example.springadminv2.domain.menu.dto;

public record UserMenuRow(
        String menuId,
        String priorMenuId,
        int sortOrder,
        String menuName,
        String menuUrl,
        String menuImage,
        String authCode) {}
