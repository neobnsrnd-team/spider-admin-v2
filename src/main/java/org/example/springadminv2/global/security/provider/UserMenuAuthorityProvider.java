package org.example.springadminv2.global.security.provider;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "security.access.authority-source", havingValue = "USER_MENU")
public class UserMenuAuthorityProvider implements AuthorityProvider {

    private final AuthorityMapper authorityMapper;

    @Override
    public Set<GrantedAuthority> getAuthorities(String userId, String roleId) {
        List<MenuPermission> permissions = authorityMapper.selectMenuPermissionsByUserId(userId);
        return toGrantedAuthorities(permissions);
    }

    private Set<GrantedAuthority> toGrantedAuthorities(List<MenuPermission> permissions) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        for (MenuPermission permission : permissions) {
            MenuAccessLevel level = MenuAccessLevel.fromCode(permission.authCode());
            String menuId = permission.menuId();
            authorities.add(new SimpleGrantedAuthority(menuId + ":" + MenuAccessLevel.READ.getCode()));
            if (level == MenuAccessLevel.WRITE) {
                authorities.add(new SimpleGrantedAuthority(menuId + ":" + MenuAccessLevel.WRITE.getCode()));
            }
        }
        return authorities;
    }
}
