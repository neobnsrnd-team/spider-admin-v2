package org.example.springadminv2.security;

import java.util.Set;

import org.example.springadminv2.global.security.CustomUserDetails;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    @DisplayName("정상 사용자는 활성화 상태이고 잠금되지 않는다")
    void normal_user_is_enabled_and_not_locked() {
        // given
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("MENU:R"));

        // when
        CustomUserDetails user = new CustomUserDetails("user01", "pwd", "1", 0, authorities);

        // then
        assertThat(user.getUsername()).isEqualTo("user01");
        assertThat(user.getPassword()).isEqualTo("pwd");
        assertThat(user.getUserId()).isEqualTo("user01");
        assertThat(user.isEnabled()).isTrue();
        assertThat(user.isAccountNonLocked()).isTrue();
        assertThat(user.isAccountNonExpired()).isTrue();
        assertThat(user.isCredentialsNonExpired()).isTrue();
        assertThat(user.getAuthorities()).hasSize(1);
    }

    @Test
    @DisplayName("비활성 상태 코드의 사용자는 isEnabled가 false")
    void disabled_user_state_code() {
        // given & when
        CustomUserDetails user = new CustomUserDetails("user02", "pwd", "0", 0, Set.of());

        // then
        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("로그인 실패 5회 이상이면 계정 잠금")
    void locked_after_max_login_failures() {
        // given & when
        CustomUserDetails user = new CustomUserDetails("user03", "pwd", "1", 5, Set.of());

        // then
        assertThat(user.isAccountNonLocked()).isFalse();
    }

    @Test
    @DisplayName("로그인 실패 횟수가 최대치 미만이면 잠금되지 않는다")
    void not_locked_below_max_login_failures() {
        // given & when
        CustomUserDetails user = new CustomUserDetails("user04", "pwd", "1", 4, Set.of());

        // then
        assertThat(user.isAccountNonLocked()).isTrue();
    }
}
