package org.example.springadminv2.security;

import java.util.Map;
import java.util.Set;

import org.example.springadminv2.global.security.config.MenuResourcePermissions;
import org.example.springadminv2.global.security.constant.MenuAccessLevel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MenuResourcePermissionsTest {

    private final MenuResourcePermissions permissions = createPermissions();

    private static MenuResourcePermissions createPermissions() {
        MenuResourcePermissions p = new MenuResourcePermissions();
        p.setPermissions(Map.of(
                "v3_role_manage", Map.of("R", "ROLE:R, MENU:R", "W", "ROLE:W, MENU:R"),
                "v3_menu_manage", Map.of("R", "MENU:R", "W", "MENU:W"),
                "v3_db_log", Map.of("R", "MESSAGEINSTANCE:R, ORG:R")));
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
        assertThat(result).containsExactlyInAnyOrder("ROLE:R", "MENU:R");
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
        assertThat(result).containsExactlyInAnyOrder("ROLE:R", "MENU:R", "ROLE:W");
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
    @DisplayName("permitAll/isAuthenticated 메뉴는 YAML에서 제외되어 빈 Set을 반환한다")
    void special_values_are_excluded_from_yaml() {
        // given – login, dashboard 등은 YAML에 없음
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
    @DisplayName("YAML 리소스가 RES_ prefix 없이 :R/:W 형식이다")
    void yaml_values_are_final_authority_format() {
        // given
        String menuId = "v3_menu_manage";

        // when
        Set<String> readResult = permissions.getDerivedResourceAuthorities(menuId, MenuAccessLevel.READ);
        Set<String> writeResult = permissions.getDerivedResourceAuthorities(menuId, MenuAccessLevel.WRITE);

        // then
        assertThat(readResult).containsExactly("MENU:R");
        assertThat(writeResult).containsExactlyInAnyOrder("MENU:R", "MENU:W");
    }

    @Test
    @DisplayName("WRITE 미정의 메뉴의 WRITE 조회 시 READ 리소스만 반환한다")
    void write_undefined_menu_returns_only_read_on_write_level() {
        // given – v3_db_log는 W가 정의되지 않음
        String menuId = "v3_db_log";

        // when
        Set<String> result = permissions.getDerivedResourceAuthorities(menuId, MenuAccessLevel.WRITE);

        // then
        assertThat(result).containsExactlyInAnyOrder("MESSAGEINSTANCE:R", "ORG:R");
    }
}
