package org.example.springadminv2.domain.system.menu;

import java.util.List;

import org.example.springadminv2.domain.system.menu.dto.MenuCreateRequest;
import org.example.springadminv2.domain.system.menu.dto.MenuResponse;
import org.example.springadminv2.domain.system.menu.dto.MenuSortUpdateRequest;
import org.example.springadminv2.domain.system.menu.dto.MenuTreeNode;
import org.example.springadminv2.domain.system.menu.dto.MenuUpdateRequest;
import org.example.springadminv2.global.dto.ApiResponse;
import org.example.springadminv2.global.dto.ErrorDetail;
import org.example.springadminv2.global.security.CustomUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system/menu")
@RequiredArgsConstructor
public class MenuRestController {

    private final MenuService menuService;

    /**
     * 전체 메뉴 트리 조회.
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('MENU:R')")
    public ResponseEntity<ApiResponse<List<MenuTreeNode>>> getMenuTree() {
        List<MenuTreeNode> tree = menuService.getMenuTree();
        return ResponseEntity.ok(ApiResponse.success(tree));
    }

    /**
     * 메뉴 상세 조회.
     */
    @GetMapping("/{menuId}")
    @PreAuthorize("hasAuthority('MENU:R')")
    public ResponseEntity<ApiResponse<MenuResponse>> getMenuDetail(@PathVariable String menuId) {
        MenuResponse menu = menuService.getMenuDetail(menuId);
        if (menu == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(ErrorDetail.builder()
                            .code("MENU_NOT_FOUND")
                            .message("메뉴를 찾을 수 없습니다: " + menuId)
                            .build()));
        }
        return ResponseEntity.ok(ApiResponse.success(menu));
    }

    /**
     * 메뉴 생성.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('MENU:W')")
    public ResponseEntity<ApiResponse<Void>> createMenu(
            @Validated @RequestBody MenuCreateRequest request, @AuthenticationPrincipal CustomUserDetails user) {
        menuService.createMenu(request, user.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(null));
    }

    /**
     * 메뉴 수정.
     */
    @PutMapping("/{menuId}")
    @PreAuthorize("hasAuthority('MENU:W')")
    public ResponseEntity<ApiResponse<Void>> updateMenu(
            @PathVariable String menuId,
            @Validated @RequestBody MenuUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        menuService.updateMenu(menuId, request, user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 메뉴 삭제. 하위 메뉴가 있으면 400 오류 반환.
     */
    @DeleteMapping("/{menuId}")
    @PreAuthorize("hasAuthority('MENU:W')")
    public ResponseEntity<ApiResponse<Void>> deleteMenu(@PathVariable String menuId) {
        try {
            menuService.deleteMenu(menuId);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(ErrorDetail.builder()
                            .code("HAS_CHILDREN")
                            .message(e.getMessage())
                            .build()));
        }
    }

    /**
     * 메뉴 순서 변경.
     */
    @PutMapping("/{menuId}/sort")
    @PreAuthorize("hasAuthority('MENU:W')")
    public ResponseEntity<ApiResponse<Void>> updateSortOrder(
            @PathVariable String menuId,
            @Validated @RequestBody MenuSortUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {
        menuService.updateSortOrder(menuId, request.sortOrder(), request.priorMenuId(), user.getUserId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
