package com.brandx.service;

import com.brandx.domain.AppUser;
import com.brandx.domain.Role;
import com.brandx.repository.AppUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Bridges the {@code app_users} table to Spring Security.
 *
 * <p>This is the only place that knows about the {@code ROLE_} prefix: {@link Role}
 * stores plain {@code ADMIN}/{@code STAFF}, and {@code hasRole('ADMIN')} in the services
 * and templates re-adds the prefix on the other side.
 */
@Service
@Transactional(readOnly = true)
public class AppUserDetailsService implements UserDetailsService {

    private final AppUserRepository users;

    public AppUserDetailsService(AppUserRepository users) {
        this.users = users;
    }

    /**
     * Returns an immutable snapshot rather than the JPA entity itself. The returned
     * object outlives this transaction — it is stored in the HTTP session — so handing
     * back a detached {@code AppUser} would invite a lazy load long after the session
     * that could satisfy it has closed.
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        AppUser user = users.findByUsernameIgnoreCase(username)
                // Same message whichever way the login failed: distinguishing "no such
                // user" from "wrong password" turns the login form into a way to test
                // whether an account exists.
                .orElseThrow(() -> new UsernameNotFoundException("Bad credentials"));

        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .authorities(authoritiesOf(user))
                .disabled(!user.isEnabled())
                .build();
    }

    private static List<GrantedAuthority> authoritiesOf(AppUser user) {
        return user.getRoles().stream()
                .map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
    }
}
