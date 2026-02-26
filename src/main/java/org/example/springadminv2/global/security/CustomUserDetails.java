package org.example.springadminv2.global.security;

import java.util.Collection;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;

@Getter
public class CustomUserDetails implements UserDetails {

    private static final int MAX_LOGIN_FAIL_COUNT = 5;

    private final String userId;
    private final String password;
    private final String userStateCode;
    private final int loginFailCount;
    private final Set<GrantedAuthority> authorities;

    public CustomUserDetails(
            String userId,
            String password,
            String userStateCode,
            int loginFailCount,
            Set<GrantedAuthority> authorities) {
        this.userId = userId;
        this.password = password;
        this.userStateCode = userStateCode;
        this.loginFailCount = loginFailCount;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userId;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return loginFailCount < MAX_LOGIN_FAIL_COUNT;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "1".equals(userStateCode);
    }
}
