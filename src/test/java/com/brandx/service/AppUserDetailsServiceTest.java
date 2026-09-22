package com.brandx.service;

import com.brandx.domain.AppUser;
import com.brandx.domain.Role;
import com.brandx.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private AppUserRepository users;

    private AppUserDetailsService service;

    @BeforeEach
    void setUp() {
        service = new AppUserDetailsService(users);
    }

    @Test
    void loadUserByUsername_mapsRolesToPrefixedAuthorities() {
        when(users.findByUsernameIgnoreCase("admin"))
                .thenReturn(Optional.of(aUser("admin", EnumSet.of(Role.ADMIN))));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_mapsEveryRoleOnTheAccount() {
        when(users.findByUsernameIgnoreCase("both"))
                .thenReturn(Optional.of(aUser("both", EnumSet.of(Role.ADMIN, Role.STAFF))));

        UserDetails details = service.loadUserByUsername("both");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_STAFF");
    }

    @Test
    void loadUserByUsername_exposesStoredHashNotAPlaintextPassword() {
        when(users.findByUsernameIgnoreCase("staff"))
                .thenReturn(Optional.of(aUser("staff", EnumSet.of(Role.STAFF))));

        assertThat(service.loadUserByUsername("staff").getPassword()).isEqualTo("{bcrypt}$2a$10$hash");
    }

    @Test
    void loadUserByUsername_disabledAccountIsNotEnabled() {
        AppUser user = aUser("dormant", EnumSet.of(Role.STAFF));
        user.setEnabled(false);
        when(users.findByUsernameIgnoreCase("dormant")).thenReturn(Optional.of(user));

        assertThat(service.loadUserByUsername("dormant").isEnabled()).isFalse();
    }

    @Test
    void loadUserByUsername_accountWithNoRoles_hasNoAuthorities() {
        when(users.findByUsernameIgnoreCase("nobody"))
                .thenReturn(Optional.of(aUser("nobody", EnumSet.noneOf(Role.class))));

        assertThat(service.loadUserByUsername("nobody").getAuthorities()).isEmpty();
    }

    @Test
    void loadUserByUsername_unknown_throwsWithoutRevealingThatTheUserIsMissing() {
        when(users.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("Bad credentials");
    }

    private static AppUser aUser(String username, Set<Role> roles) {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash("{bcrypt}$2a$10$hash");
        user.setDisplayName("Test User");
        user.setEnabled(true);
        user.setRoles(roles);
        return user;
    }
}
