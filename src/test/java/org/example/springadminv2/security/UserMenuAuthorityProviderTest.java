package org.example.springadminv2.security;

import java.util.List;
import java.util.Set;

import org.example.springadminv2.global.security.converter.AuthorityConverter;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.example.springadminv2.global.security.provider.UserMenuAuthorityProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class UserMenuAuthorityProviderTest {

    @Mock
    AuthorityMapper authorityMapper;

    @Mock
    AuthorityConverter authorityConverter;

    @InjectMocks
    UserMenuAuthorityProvider provider;

    @Test
    @DisplayName("사용자 기반 권한 조회 시 selectMenuPermissionsByUserId로 위임한다")
    void get_authorities_delegates_to_user_permissions() {
        // given
        List<MenuPermission> permissions = List.of(new MenuPermission("v3_menu_manage", "W"));
        Set<GrantedAuthority> expected =
                Set.of(new SimpleGrantedAuthority("MENU:R"), new SimpleGrantedAuthority("MENU:W"));

        given(authorityMapper.selectMenuPermissionsByUserId("admin")).willReturn(permissions);
        given(authorityConverter.convert(permissions)).willReturn(expected);

        // when
        Set<GrantedAuthority> result = provider.getAuthorities("admin", "ADMIN");

        // then
        assertThat(result).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("MENU:R", "MENU:W");
    }

    @Test
    @DisplayName("빈 권한 목록 시 빈 Set을 반환한다")
    void get_authorities_empty_permissions_returns_empty() {
        // given
        List<MenuPermission> permissions = List.of();
        given(authorityMapper.selectMenuPermissionsByUserId("unknown")).willReturn(permissions);
        given(authorityConverter.convert(permissions)).willReturn(Set.of());

        // when
        Set<GrantedAuthority> result = provider.getAuthorities("unknown", "USER");

        // then
        assertThat(result).isEmpty();
    }
}
