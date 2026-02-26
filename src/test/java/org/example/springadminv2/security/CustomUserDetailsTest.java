package org.example.springadminv2.security;

import java.util.Set;

import org.example.springadminv2.global.security.CustomUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

class CustomUserDetailsTest {

    @Test
    void normal_user_is_enabled_and_not_locked() {
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("MENU01:R"));

        CustomUserDetails user = new CustomUserDetails("user01", "pwd", "1", 0, authorities);

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
    void disabled_user_state_code() {
        CustomUserDetails user = new CustomUserDetails("user02", "pwd", "0", 0, Set.of());

        assertThat(user.isEnabled()).isFalse();
    }

    @Test
    void locked_after_max_login_failures() {
        CustomUserDetails user = new CustomUserDetails("user03", "pwd", "1", 5, Set.of());

        assertThat(user.isAccountNonLocked()).isFalse();
    }

    @Test
    void not_locked_below_max_login_failures() {
        CustomUserDetails user = new CustomUserDetails("user04", "pwd", "1", 4, Set.of());

        assertThat(user.isAccountNonLocked()).isTrue();
    }
}
