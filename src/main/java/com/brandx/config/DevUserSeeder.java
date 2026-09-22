package com.brandx.config;

import com.brandx.domain.AppUser;
import com.brandx.domain.Role;
import com.brandx.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

/**
 * Creates the two demo accounts so a fresh dev database is actually usable.
 *
 * <p>Separate from {@link DevDataSeeder} rather than folded into it: that one skips
 * everything when any product, customer or order already exists, and accounts need their
 * own emptiness check — otherwise pointing the app at a database that has records but no
 * users would leave nobody able to sign in.
 *
 * <p>Guarded by the same {@code brandx.seed-data} property, which only the dev profile
 * sets. The passwords below are throwaway values for local work; nothing creates an
 * account outside this profile, so the first task before any real deployment is adding a
 * way to provision one.
 */
@Component
@ConditionalOnProperty(name = "brandx.seed-data", havingValue = "true")
public class DevUserSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevUserSeeder.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public DevUserSeeder(AppUserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (users.count() > 0) {
            log.debug("Skipping user seed: accounts already exist");
            return;
        }

        users.save(user("adoyle", "admin", "Avery Doyle", EnumSet.of(Role.ADMIN)));
        users.save(user("sokonkwo", "staff", "Sam Okonkwo", EnumSet.of(Role.STAFF)));
        users.save(user("jdoe", "staff", "Jane Doe", EnumSet.of(Role.STAFF)));

        // Logged, not printed to stdout, and only ever reachable on the dev profile.
        log.warn("Seeded demo accounts for local use: admin/admin (ADMIN), staff/staff (STAFF)");
    }

    private AppUser user(String username, String rawPassword, String displayName, Set<Role> roles) {
        AppUser user = new AppUser();
        // Stored lowercase so the case-sensitive unique constraint and the
        // case-insensitive login lookup cannot disagree about what is a duplicate.
        user.setUsername(username.toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setDisplayName(displayName);
        user.setRoles(roles);
        user.setEnabled(true);
        return user;
    }
}
