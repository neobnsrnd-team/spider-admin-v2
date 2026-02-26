package org.example.springadminv2.domain.system.menu.dto;

import java.util.List;

/**
 * 트리 구조 응답용 DTO.
 * MenuResponse(flat DB row)를 기반으로 children 리스트를 추가한다.
 */
public record MenuTreeNode(
        String menuId,
        String priorMenuId,
        int sortOrder,
        String menuName,
        String menuUrl,
        String menuImage,
        String displayYn,
        String useYn,
        String lastUpdateDtime,
        String lastUpdateUserId,
        List<MenuTreeNode> children) {

    public static MenuTreeNode of(MenuResponse menu, List<MenuTreeNode> children) {
        return new MenuTreeNode(
                menu.menuId(),
                menu.priorMenuId(),
                menu.sortOrder(),
                menu.menuName(),
                menu.menuUrl(),
                menu.menuImage(),
                menu.displayYn(),
                menu.useYn(),
                menu.lastUpdateDtime(),
                menu.lastUpdateUserId(),
                children);
    }
}
