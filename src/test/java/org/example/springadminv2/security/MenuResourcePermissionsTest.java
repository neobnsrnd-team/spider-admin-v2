package org.example.springadminv2.security;

import java.util.List;
import java.util.Set;

import org.example.springadminv2.global.security.config.MenuResourcePermissions;
import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class MenuResourcePermissionsTest {

    private final MenuResourcePermissions permissions = createPermissions();

    private static MenuResourcePermissions createPermissions() {
        MenuResourcePermissions p = new MenuResourcePermissions();
        ReflectionTestUtils.invokeMethod(p, "init");
        return p;
    }

    @Test
    @DisplayName("READ 레벨 조회 시 READ 리소스만 반환한다")
    void get_derived_read_returns_only_read_resources() {
        // given
        String menuId = "v3_role_manage";
        MenuAccessLevel level = MenuAccessLevel.READ;

        // when
        Set<String> result = permissions.getDerivedResourceAuthorities(menuId, level);

        // then
        assertThat(result).containsExactlyInAnyOrder("RES_ROLE:R", "RES_MENU:R");
    }

    @Test
    @DisplayName("WRITE 레벨 조회 시 READ + WRITE 리소스를 모두 반환한다")
    void get_derived_write_returns_read_and_write_resources() {
        // given
        String menuId = "v3_role_manage";
        MenuAccessLevel level = MenuAccessLevel.WRITE;

        // when
        Set<String> result = permissions.getDerivedResourceAuthorities(menuId, level);

        // then
        assertThat(result).containsExactlyInAnyOrder("RES_ROLE:R", "RES_MENU:R", "RES_ROLE:W");
    }

    @Test
    @DisplayName("존재하지 않는 메뉴 ID 조회 시 빈 Set을 반환한다")
    void get_derived_unknown_menu_returns_empty() {
        // given
        String menuId = "nonexistent_menu";
        MenuAccessLevel level = MenuAccessLevel.WRITE;

        // when
        Set<String> result = permissions.getDerivedResourceAuthorities(menuId, level);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("permitAll/isAuthenticated 특수 값은 무시한다")
    void special_values_are_ignored() {
        // given – login, dashboard 등은 특수 값으로 정의됨
        String loginMenu = "login";
        String dashboardMenu = "dashboard";

        // when
        Set<String> loginResult = permissions.getDerivedResourceAuthorities(loginMenu, MenuAccessLevel.READ);
        Set<String> dashboardResult = permissions.getDerivedResourceAuthorities(dashboardMenu, MenuAccessLevel.READ);

        // then
        assertThat(loginResult).isEmpty();
        assertThat(dashboardResult).isEmpty();
    }

    @Test
    @DisplayName("YAML 리소스 접미사가 :R/:W 형식으로 변환된다")
    void yaml_suffix_converted_to_authority_format() {
        // given
        String menuId = "v3_menu_manage";

        // when
        Set<String> readResult = permissions.getDerivedResourceAuthorities(menuId, MenuAccessLevel.READ);
        Set<String> writeResult = permissions.getDerivedResourceAuthorities(menuId, MenuAccessLevel.WRITE);

        // then
        assertThat(readResult).containsExactly("RES_MENU:R");
        assertThat(writeResult).containsExactlyInAnyOrder("RES_MENU:R", "RES_MENU:W");
    }

    @Test
    @DisplayName("WRITE 미정의 메뉴의 WRITE 조회 시 READ 리소스만 반환한다")
    void write_undefined_menu_returns_only_read_on_write_level() {
        // given – v3_db_log는 _WRITE가 정의되지 않음
        String menuId = "v3_db_log";

        // when
        Set<String> result = permissions.getDerivedResourceAuthorities(menuId, MenuAccessLevel.WRITE);

        // then
        assertThat(result).containsExactlyInAnyOrder("RES_MESSAGEINSTANCE:R", "RES_ORG:R");
    }

    @Test
    @DisplayName("접미사 없는 리소스명은 그대로 반환된다")
    void resource_without_suffix_returned_as_is() {
        // given
        String rawValue = "PLAIN_RESOURCE";

        // when
        List<String> result = ReflectionTestUtils.invokeMethod(permissions, "parseResources", rawValue);

        // then
        assertThat(result).containsExactly("PLAIN_RESOURCE");
    }
}
