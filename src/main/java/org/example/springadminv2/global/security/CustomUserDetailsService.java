package org.example.springadminv2.global.security;

import java.util.Set;

import org.example.springadminv2.global.security.dto.AuthenticatedUser;
import org.example.springadminv2.global.security.mapper.AuthorityMapper;
import org.example.springadminv2.global.security.provider.AuthorityProvider;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AuthorityProvider authorityProvider;
    private final AuthorityMapper authorityMapper;

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        AuthenticatedUser user = authorityMapper.selectUserById(userId);
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + userId);
        }

        Set<GrantedAuthority> authorities = authorityProvider.getAuthorities(user.userId(), user.roleId());

        return new CustomUserDetails(
                user.userId(),
                user.password(),
                user.roleId(),
                user.userStateCode(),
                user.loginFailCount(),
                authorities);
    }
}
