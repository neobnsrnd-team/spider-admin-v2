package org.example.springadminv2.global.security.converter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.example.springadminv2.global.security.config.MenuResourcePermissions;
import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AuthorityConverter {

    private final MenuResourcePermissions menuResourcePermissions;

    public Set<GrantedAuthority> convert(List<MenuPermission> permissions) {
        Set<GrantedAuthority> authorities = new HashSet<>();

        for (MenuPermission permission : permissions) {
            MenuAccessLevel level = MenuAccessLevel.fromCode(permission.authCode());
            String menuId = permission.menuId();

            authorities.add(new SimpleGrantedAuthority(menuId + ":" + MenuAccessLevel.READ.getCode()));
            if (level == MenuAccessLevel.WRITE) {
                authorities.add(new SimpleGrantedAuthority(menuId + ":" + MenuAccessLevel.WRITE.getCode()));
            }

            Set<String> derivedResources = menuResourcePermissions.getDerivedResourceAuthorities(menuId, level);
            for (String resource : derivedResources) {
                authorities.add(new SimpleGrantedAuthority(resource));
            }
        }

        return authorities;
    }
}
