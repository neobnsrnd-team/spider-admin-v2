package org.example.springadminv2.security;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.example.springadminv2.global.security.config.MenuResourcePermissions;
import org.example.springadminv2.global.security.converter.AuthorityConverter;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.example.springadminv2.global.security.provider.UserMenuAuthorityProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthorityProviderTest {

    @Mock
    private AuthorityMapper authorityMapper;

    private UserMenuAuthorityProvider provider;

    @BeforeEach
    void setUp() {
        // menu-resource-permissions.yml 에 해당하는 설정을 직접 구성
        MenuResourcePermissions permissions = new MenuResourcePermissions();
        permissions.setPermissions(java.util.Map.of(
                "v3_menu_manage", java.util.Map.of("R", "MENU:R", "W", "MENU:W"),
                "v3_role_manage", java.util.Map.of("R", "ROLE:R, MENU:R", "W", "ROLE:W, MENU:R")));

        AuthorityConverter converter = new AuthorityConverter(permissions);
        provider = new UserMenuAuthorityProvider(authorityMapper, converter);
    }

    @Test
    @DisplayName("USER_MENU 기반: userId로 리소스 권한 로딩")
    void load_authorities_by_user_id() {
        // given
        given(authorityMapper.selectMenuPermissionsByUserId("admin"))
                .willReturn(
                        List.of(new MenuPermission("v3_menu_manage", "W"), new MenuPermission("v3_role_manage", "W")));

        // when
        Set<GrantedAuthority> authorities = provider.getAuthorities("admin", "ADMIN");

        // then
        Set<String> names =
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder("MENU:R", "MENU:W", "ROLE:R", "ROLE:W");
    }

    @Test
    @DisplayName("WRITE → READ + WRITE 리소스 자동 확장")
    void write_includes_read() {
        // given
        given(authorityMapper.selectMenuPermissionsByUserId("admin"))
                .willReturn(List.of(new MenuPermission("v3_menu_manage", "W")));

        // when
        Set<GrantedAuthority> authorities = provider.getAuthorities("admin", "ADMIN");

        // then
        Set<String> names =
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        assertThat(names).containsExactlyInAnyOrder("MENU:R", "MENU:W");
    }

    @Test
    @DisplayName("READ 전용 사용자: READ 리소스만 포함, WRITE 미포함")
    void read_only_user() {
        // given
        given(authorityMapper.selectMenuPermissionsByUserId("user01"))
                .willReturn(List.of(new MenuPermission("v3_menu_manage", "R")));

        // when
        Set<GrantedAuthority> authorities = provider.getAuthorities("user01", "USER");

        // then
        Set<String> names =
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        assertThat(names).containsExactly("MENU:R");
        assertThat(names).doesNotContain("MENU:W");
    }

    @Test
    @DisplayName("권한 없는 사용자 → 빈 Set")
    void no_permissions_returns_empty_set() {
        // given
        given(authorityMapper.selectMenuPermissionsByUserId("disabled")).willReturn(List.of());

        // when
        Set<GrantedAuthority> authorities = provider.getAuthorities("disabled", "USER");

        // then
        assertThat(authorities).isEmpty();
    }
}
