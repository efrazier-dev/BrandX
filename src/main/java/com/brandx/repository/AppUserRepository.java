package com.brandx.repository;

import com.brandx.domain.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    /**
     * Login lookup.
     *
     * <p>The {@code @EntityGraph} is required, not an optimisation. Spring Security
     * authenticates inside a servlet filter, so by the time the authorities are read —
     * during a {@code @PreAuthorize} check, or by {@code sec:authorize} while Thymeleaf
     * renders — both the transaction and (with {@code open-in-view: false}) the Hibernate
     * session are long gone. Fetching {@code roles} lazily here would throw
     * LazyInitializationException on the first authorization check rather than quietly
     * issuing a second query.
     *
     * <p>Case-insensitive so that "Admin" and "admin" are the same account; the unique
     * constraint on the column is case-sensitive, so the seeder stores usernames
     * lowercase and nothing creates two accounts differing only in case.
     */
    @EntityGraph(attributePaths = "roles")
    Optional<AppUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}
