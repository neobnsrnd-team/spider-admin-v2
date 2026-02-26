package org.example.springadminv2.domain.menu.service;

import java.util.List;

import org.example.springadminv2.domain.menu.dto.MenuCreateRequest;
import org.example.springadminv2.domain.menu.dto.MenuResponse;
import org.example.springadminv2.domain.menu.dto.MenuTreeNode;
import org.example.springadminv2.domain.menu.dto.MenuUpdateRequest;
import org.example.springadminv2.domain.menu.dto.UserMenuRow;
import org.example.springadminv2.domain.menu.mapper.MenuMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    @Mock
    MenuMapper menuMapper;

    @InjectMocks
    MenuService menuService;

    // ── 테스트 데이터 헬퍼 ──────────────────────────────────

    private MenuResponse menu(String menuId, String priorMenuId, int sortOrder, String menuName) {
        return new MenuResponse(
                menuId, priorMenuId, sortOrder, menuName, "/url/" + menuId, null, "Y", "Y", "20260226120000", "admin");
    }

    // ── getMenuTree ────────────────────────────────────────

    @Nested
    @DisplayName("getMenuTree")
    class GetMenuTree {

        @Test
        @DisplayName("flat 리스트를 트리 구조로 변환한다 (루트 + 자식)")
        void builds_tree_from_flat_list() {
            // given
            List<MenuResponse> flatMenus = List.of(
                    menu("SYS", "ROOT", 1, "시스템관리"),
                    menu("MENU", "SYS", 1, "메뉴관리"),
                    menu("ROLE", "SYS", 2, "역할관리"),
                    menu("MON", "ROOT", 2, "모니터링"));
            given(menuMapper.selectAllMenus()).willReturn(flatMenus);

            // when
            List<MenuTreeNode> tree = menuService.getMenuTree();

            // then
            assertThat(tree).hasSize(2);

            MenuTreeNode sysNode = tree.get(0);
            assertThat(sysNode.menuId()).isEqualTo("SYS");
            assertThat(sysNode.children()).hasSize(2);
            assertThat(sysNode.children().get(0).menuId()).isEqualTo("MENU");
            assertThat(sysNode.children().get(1).menuId()).isEqualTo("ROLE");

            MenuTreeNode monNode = tree.get(1);
            assertThat(monNode.menuId()).isEqualTo("MON");
            assertThat(monNode.children()).isNull();
        }

        @Test
        @DisplayName("priorMenuId가 null인 메뉴도 루트로 처리한다")
        void null_prior_menu_id_treated_as_root() {
            // given
            List<MenuResponse> flatMenus = List.of(menu("DASH", null, 1, "대시보드"));
            given(menuMapper.selectAllMenus()).willReturn(flatMenus);

            // when
            List<MenuTreeNode> tree = menuService.getMenuTree();

            // then
            assertThat(tree).hasSize(1);
            assertThat(tree.get(0).menuId()).isEqualTo("DASH");
            assertThat(tree.get(0).children()).isNull();
        }

        @Test
        @DisplayName("빈 리스트이면 빈 트리를 반환한다")
        void empty_list_returns_empty_tree() {
            // given
            given(menuMapper.selectAllMenus()).willReturn(List.of());

            // when
            List<MenuTreeNode> tree = menuService.getMenuTree();

            // then
            assertThat(tree).isEmpty();
        }
    }

    // ── getMenuDetail ──────────────────────────────────────

    @Nested
    @DisplayName("getMenuDetail")
    class GetMenuDetail {

        @Test
        @DisplayName("menuId로 단건 메뉴를 조회하여 MenuResponse를 반환한다")
        void returns_menu_response() {
            // given
            MenuResponse expected = menu("MENU", "SYS", 1, "메뉴관리");
            given(menuMapper.selectMenuById("MENU")).willReturn(expected);

            // when
            MenuResponse result = menuService.getMenuDetail("MENU");

            // then
            assertThat(result).isEqualTo(expected);
            assertThat(result.menuId()).isEqualTo("MENU");
            assertThat(result.menuName()).isEqualTo("메뉴관리");
        }
    }

    // ── createMenu ─────────────────────────────────────────

    @Nested
    @DisplayName("createMenu")
    class CreateMenu {

        @Test
        @DisplayName("메뉴 생성 시 mapper.insertMenu를 호출한다")
        void calls_mapper_insert() {
            // given
            MenuCreateRequest request = new MenuCreateRequest("NEW_MENU", "신규메뉴", "SYS", "/new", null, 3, "Y", "Y");

            // when
            menuService.createMenu(request, "admin");

            // then
            then(menuMapper).should().insertMenu(eq(request), eq("admin"), anyString());
        }
    }

    // ── updateMenu ─────────────────────────────────────────

    @Nested
    @DisplayName("updateMenu")
    class UpdateMenu {

        @Test
        @DisplayName("메뉴 수정 시 mapper.updateMenu를 호출한다")
        void calls_mapper_update() {
            // given
            MenuUpdateRequest request = new MenuUpdateRequest("수정메뉴", "SYS", "/updated", null, 2, "Y", "Y");

            // when
            menuService.updateMenu("MENU", request, "admin");

            // then
            then(menuMapper).should().updateMenu(eq("MENU"), eq(request), eq("admin"), anyString());
        }
    }

    // ── deleteMenu ─────────────────────────────────────────

    @Nested
    @DisplayName("deleteMenu")
    class DeleteMenu {

        @Test
        @DisplayName("하위 메뉴가 없으면 삭제에 성공한다")
        void succeeds_when_no_children() {
            // given
            given(menuMapper.countChildMenus("MENU")).willReturn(0);

            // when
            menuService.deleteMenu("MENU");

            // then
            then(menuMapper).should().deleteMenu("MENU");
        }

        @Test
        @DisplayName("하위 메뉴가 존재하면 IllegalStateException을 던진다")
        void throws_when_has_children() {
            // given
            given(menuMapper.countChildMenus("SYS")).willReturn(2);

            // when & then
            assertThatThrownBy(() -> menuService.deleteMenu("SYS"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("하위 메뉴가 존재하여 삭제할 수 없습니다")
                    .hasMessageContaining("children=2");

            then(menuMapper).should().countChildMenus("SYS");
            then(menuMapper).shouldHaveNoMoreInteractions();
        }
    }

    // ── updateSortOrder ────────────────────────────────────

    @Nested
    @DisplayName("updateSortOrder")
    class UpdateSortOrder {

        @Test
        @DisplayName("정렬 순서 변경 시 mapper.updateSortOrder를 호출한다")
        void calls_mapper_update_sort_order() {
            // when
            menuService.updateSortOrder("MENU", 3, "SYS", "admin");

            // then
            then(menuMapper).should().updateSortOrder(eq("MENU"), eq(3), eq("SYS"), eq("admin"), anyString());
        }
    }

    // ── getUserMenuTree ────────────────────────────────────

    @Nested
    @DisplayName("getUserMenuTree")
    class GetUserMenuTree {

        @Test
        @DisplayName("Mapper가 반환한 사용자 메뉴 트리를 그대로 전달한다")
        void delegates_to_mapper() {
            // given
            List<UserMenuRow> expected = List.of(
                    new UserMenuRow("SYS", "ROOT", 1, "시스템관리", null, "settings", null),
                    new UserMenuRow("MENU", "SYS", 1, "메뉴관리", "/system/menu", "list", "RW"));
            given(menuMapper.selectUserMenuTree("testUser")).willReturn(expected);

            // when
            List<UserMenuRow> result = menuService.getUserMenuTree("testUser");

            // then
            assertThat(result).isEqualTo(expected);
            assertThat(result).hasSize(2);
            then(menuMapper).should().selectUserMenuTree("testUser");
        }

        @Test
        @DisplayName("권한이 없으면 빈 리스트를 반환한다")
        void returns_empty_when_no_permissions() {
            // given
            given(menuMapper.selectUserMenuTree("noPermUser")).willReturn(List.of());

            // when
            List<UserMenuRow> result = menuService.getUserMenuTree("noPermUser");

            // then
            assertThat(result).isEmpty();
        }
    }
}
