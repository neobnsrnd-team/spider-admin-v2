package org.example.springadminv2.security;

import java.util.Set;
import java.util.stream.Collectors;

import org.example.springadminv2.global.security.provider.AuthorityProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AuthorityProviderTest {

    @Autowired
    private AuthorityProvider authorityProvider;

    @Test
    @DisplayName("USER_MENU 기반: userId로 리소스 권한 로딩")
    void loadAuthoritiesByUserId() {
        Set<GrantedAuthority> authorities = authorityProvider.getAuthorities("admin", "ADMIN");

        Set<String> authorityStrings =
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        assertThat(authorityStrings).contains("MENU:R", "MENU:W", "ROLE:R", "ROLE:W");
    }

    @Test
    @DisplayName("WRITE → READ + WRITE 리소스 자동 확장")
    void writeIncludesRead() {
        Set<GrantedAuthority> authorities = authorityProvider.getAuthorities("admin", "ADMIN");

        Set<String> authorityStrings =
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        assertThat(authorityStrings).contains("MENU:R", "MENU:W");
    }

    @Test
    @DisplayName("READ 전용 사용자: READ 리소스만 포함, WRITE 미포함")
    void readOnlyUser() {
        Set<GrantedAuthority> authorities = authorityProvider.getAuthorities("user01", "USER");

        Set<String> authorityStrings =
                authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());

        assertThat(authorityStrings).contains("MENU:R");
        assertThat(authorityStrings).doesNotContain("MENU:W");
    }

    @Test
    @DisplayName("권한 없는 사용자 → 빈 Set")
    void noPermissions_emptySet() {
        Set<GrantedAuthority> authorities = authorityProvider.getAuthorities("disabled", "USER");

        assertThat(authorities).isEmpty();
    }
}
