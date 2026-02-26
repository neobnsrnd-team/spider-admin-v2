package org.example.springadminv2.security;

import java.util.Set;

import org.example.springadminv2.global.security.CustomUserDetails;
import org.example.springadminv2.global.security.CustomUserDetailsService;
import org.example.springadminv2.global.security.dto.AuthenticatedUser;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.example.springadminv2.global.security.provider.AuthorityProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    AuthorityMapper authorityMapper;

    @Mock
    AuthorityProvider authorityProvider;

    @InjectMocks
    CustomUserDetailsService service;

    @Test
    void loadUserByUsername_returns_custom_user_details() {
        AuthenticatedUser authUser = new AuthenticatedUser("admin", "encPwd", "ROLE_ADMIN", "1", 0);
        Set<GrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("MENU01:R"));

        given(authorityMapper.selectUserById("admin")).willReturn(authUser);
        given(authorityProvider.getAuthorities("admin", "ROLE_ADMIN")).willReturn(authorities);

        CustomUserDetails result = (CustomUserDetails) service.loadUserByUsername("admin");

        assertThat(result.getUserId()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("encPwd");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities()).hasSize(1);
    }

    @Test
    void loadUserByUsername_throws_when_user_not_found() {
        given(authorityMapper.selectUserById("unknown")).willReturn(null);

        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("unknown");
    }
}
