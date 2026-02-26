package org.example.springadminv2.security;

import java.util.List;
import java.util.Set;

import org.example.springadminv2.global.security.converter.AuthorityConverter;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.example.springadminv2.global.security.provider.RoleMenuAuthorityProvider;
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
class RoleMenuAuthorityProviderTest {

    @Mock
    AuthorityMapper authorityMapper;

    @Mock
    AuthorityConverter authorityConverter;

    @InjectMocks
    RoleMenuAuthorityProvider provider;

    @Test
    @DisplayName("역할 기반 권한 조회 시 AuthorityConverter로 변환한 결과를 반환한다")
    void get_authorities_delegates_to_converter() {
        // given
        List<MenuPermission> permissions = List.of(new MenuPermission("MENU01", "W"));
        Set<GrantedAuthority> expected =
                Set.of(new SimpleGrantedAuthority("MENU01:R"), new SimpleGrantedAuthority("MENU01:W"));

        given(authorityMapper.selectMenuPermissionsByRoleId("ROLE_ADMIN")).willReturn(permissions);
        given(authorityConverter.convert(permissions)).willReturn(expected);

        // when
        Set<GrantedAuthority> result = provider.getAuthorities("user01", "ROLE_ADMIN");

        // then
        assertThat(result).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("MENU01:R", "MENU01:W");
    }

    @Test
    @DisplayName("빈 권한 목록 시 빈 Set을 반환한다")
    void get_authorities_empty_permissions_returns_empty() {
        // given
        List<MenuPermission> permissions = List.of();
        given(authorityMapper.selectMenuPermissionsByRoleId("ROLE_NONE")).willReturn(permissions);
        given(authorityConverter.convert(permissions)).willReturn(Set.of());

        // when
        Set<GrantedAuthority> result = provider.getAuthorities("user01", "ROLE_NONE");

        // then
        assertThat(result).isEmpty();
    }
}
