package org.example.springadminv2.global.security.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.example.springadminv2.global.security.dto.AuthenticatedUser;
import org.example.springadminv2.global.security.dto.MenuPermission;

@Mapper
public interface AuthorityMapper {

    List<MenuPermission> selectMenuPermissionsByUserId(String userId);

    List<MenuPermission> selectMenuPermissionsByRoleId(String roleId);

    AuthenticatedUser selectUserById(String userId);

    List<Map<String, Object>> selectAllMenus();
}
