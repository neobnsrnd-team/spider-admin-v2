package org.example.springadminv2.domain.menu.dto;

import java.util.List;

/**
 * 사용자 권한 기반 메뉴 트리 응답용 DTO.
 * UserMenuRow(flat DB row)를 기반으로 children 리스트를 추가한다.
 */
public record UserMenuTreeNode(
        String menuId,
        String priorMenuId,
        int sortOrder,
        String menuName,
        String menuUrl,
        String menuImage,
        String authCode,
        List<UserMenuTreeNode> children) {

    public static UserMenuTreeNode of(UserMenuRow row, List<UserMenuTreeNode> children) {
        return new UserMenuTreeNode(
                row.menuId(),
                row.priorMenuId(),
                row.sortOrder(),
                row.menuName(),
                row.menuUrl(),
                row.menuImage(),
                row.authCode(),
                children);
    }
}
