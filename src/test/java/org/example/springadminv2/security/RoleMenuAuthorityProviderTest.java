package org.example.springadminv2.security;

import java.util.List;
import java.util.Set;

import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.example.springadminv2.global.security.provider.RoleMenuAuthorityProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class RoleMenuAuthorityProviderTest {

    @Mock
    AuthorityMapper authorityMapper;

    @InjectMocks
    RoleMenuAuthorityProvider provider;

    @Test
    void write_permission_grants_both_read_and_write() {
        given(authorityMapper.selectMenuPermissionsByRoleId("ROLE_ADMIN"))
                .willReturn(List.of(new MenuPermission("MENU01", "W")));

        Set<GrantedAuthority> authorities = provider.getAuthorities("user01", "ROLE_ADMIN");

        assertThat(authorities)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("MENU01:R", "MENU01:W");
    }

    @Test
    void read_permission_grants_only_read() {
        given(authorityMapper.selectMenuPermissionsByRoleId("ROLE_USER"))
                .willReturn(List.of(new MenuPermission("MENU02", "R")));

        Set<GrantedAuthority> authorities = provider.getAuthorities("user01", "ROLE_USER");

        assertThat(authorities).extracting(GrantedAuthority::getAuthority).containsExactly("MENU02:R");
    }

    @Test
    void empty_permissions_returns_empty_set() {
        given(authorityMapper.selectMenuPermissionsByRoleId("ROLE_NONE")).willReturn(List.of());

        Set<GrantedAuthority> authorities = provider.getAuthorities("user01", "ROLE_NONE");

        assertThat(authorities).isEmpty();
    }
}
