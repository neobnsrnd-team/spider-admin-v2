package org.example.springadminv2.global.security.provider;

import java.util.List;
import java.util.Set;

import org.example.springadminv2.global.security.converter.AuthorityConverter;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.access.authority-source", havingValue = "ROLE_MENU")
public class RoleMenuAuthorityProvider implements AuthorityProvider {

    private final AuthorityMapper authorityMapper;
    private final AuthorityConverter authorityConverter;

    @Override
    public Set<GrantedAuthority> getAuthorities(String userId, String roleId) {
        List<MenuPermission> permissions = authorityMapper.selectMenuPermissionsByRoleId(roleId);
        return authorityConverter.convert(permissions);
    }
}
