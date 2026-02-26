package org.example.springadminv2.domain.system.menu;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.springadminv2.domain.system.menu.dto.MenuCreateRequest;
import org.example.springadminv2.domain.system.menu.dto.MenuResponse;
import org.example.springadminv2.domain.system.menu.dto.MenuUpdateRequest;

@Mapper
public interface MenuMapper {

    List<MenuResponse> selectAllMenus();

    MenuResponse selectMenuById(String menuId);

    void insertMenu(
            @Param("req") MenuCreateRequest request,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    void updateMenu(
            @Param("menuId") String menuId,
            @Param("req") MenuUpdateRequest request,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);

    void deleteMenu(String menuId);

    int countChildMenus(String menuId);

    void updateSortOrder(
            @Param("menuId") String menuId,
            @Param("sortOrder") int sortOrder,
            @Param("priorMenuId") String priorMenuId,
            @Param("lastUpdateUserId") String userId,
            @Param("lastUpdateDtime") String dtime);
}
