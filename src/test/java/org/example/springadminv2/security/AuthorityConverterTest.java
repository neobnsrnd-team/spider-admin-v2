package org.example.springadminv2.security;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.example.springadminv2.global.security.config.MenuResourcePermissions;
import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.example.springadminv2.global.security.converter.AuthorityConverter;
import org.example.springadminv2.global.security.dto.MenuPermission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthorityConverterTest {

    @Mock
    MenuResourcePermissions menuResourcePermissions;

    @InjectMocks
    AuthorityConverter converter;

    @Test
    @DisplayName("READ 권한 변환 시 파생 리소스만 반환한다")
    void convert_read_returns_derived_resources_only() {
        // given
        List<MenuPermission> permissions = List.of(new MenuPermission("v3_menu_manage", "R"));
        given(menuResourcePermissions.getDerivedResourceAuthorities("v3_menu_manage", MenuAccessLevel.READ))
                .willReturn(Set.of("MENU:R"));

        // when
        Set<GrantedAuthority> result = converter.convert(permissions);

        // then
        assertThat(result).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("MENU:R");
    }

    @Test
    @DisplayName("WRITE 권한 변환 시 READ + WRITE 파생 리소스를 반환한다")
    void convert_write_returns_read_and_write_derived_resources() {
        // given
        List<MenuPermission> permissions = List.of(new MenuPermission("v3_role_manage", "W"));
        given(menuResourcePermissions.getDerivedResourceAuthorities("v3_role_manage", MenuAccessLevel.WRITE))
                .willReturn(Set.of("ROLE:R", "ROLE:W", "MENU:R"));

        // when
        Set<GrantedAuthority> result = converter.convert(permissions);

        // then
        assertThat(result)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE:R", "ROLE:W", "MENU:R");
    }

    @Test
    @DisplayName("빈 권한 목록 변환 시 빈 Set을 반환한다")
    void convert_empty_returns_empty_set() {
        // given
        List<MenuPermission> permissions = Collections.emptyList();

        // when
        Set<GrantedAuthority> result = converter.convert(permissions);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("YAML에 정의되지 않은 메뉴는 빈 Set을 반환한다")
    void convert_unknown_menu_returns_empty() {
        // given
        List<MenuPermission> permissions = List.of(new MenuPermission("UNKNOWN_MENU", "W"));
        given(menuResourcePermissions.getDerivedResourceAuthorities("UNKNOWN_MENU", MenuAccessLevel.WRITE))
                .willReturn(Collections.emptySet());

        // when
        Set<GrantedAuthority> result = converter.convert(permissions);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("여러 메뉴 권한을 한번에 변환한다")
    void convert_multiple_permissions() {
        // given
        List<MenuPermission> permissions =
                List.of(new MenuPermission("v3_menu_manage", "R"), new MenuPermission("v3_role_manage", "W"));
        given(menuResourcePermissions.getDerivedResourceAuthorities("v3_menu_manage", MenuAccessLevel.READ))
                .willReturn(Set.of("MENU:R"));
        given(menuResourcePermissions.getDerivedResourceAuthorities("v3_role_manage", MenuAccessLevel.WRITE))
                .willReturn(Set.of("ROLE:R", "ROLE:W", "MENU:R"));

        // when
        Set<GrantedAuthority> result = converter.convert(permissions);

        // then
        assertThat(result)
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("MENU:R", "ROLE:R", "ROLE:W");
    }
}
