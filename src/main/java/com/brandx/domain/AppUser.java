package com.brandx.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * An account that can sign in.
 *
 * <p>Named {@code AppUser} rather than {@code User} because {@code user} is a reserved
 * word in PostgreSQL and several other engines, which would force a quoted table name
 * and undercut this app's database-portability constraint. The table is {@code app_users}
 * for the same reason.
 *
 * <p>Deliberately not part of the CRUD surface: there is no controller, no service and
 * no screen for editing accounts. At this stage users exist only so the role checks have
 * something to resolve against, and they are created by the dev seeder.
 */
@Entity
@Table(name = "app_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_app_users_username", columnNames = "username"))
public class AppUser {

    /** IDENTITY, matching the other entities — see the note on {@link Product}. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String username;

    /**
     * BCrypt hash, never a plaintext password — long enough for any
     * {@code DelegatingPasswordEncoder} prefix plus the hash itself.
     */
    @NotBlank
    @Size(max = 200)
    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @NotBlank
    @Size(max = 120)
    @Column(name = "display_name", nullable = false, length = 120)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * An {@code @ElementCollection} of the enum rather than a {@code Role} entity with a
     * join table: the role set is fixed in code, so a separate table of role rows would
     * add a join and a repository without adding anything a database row can express.
     *
     * <p>Left LAZY, as collections should be. Authentication happens in a servlet filter,
     * outside any transaction, so {@link com.brandx.repository.AppUserRepository} attaches
     * an {@code @EntityGraph} to the one query that needs this initialised — see the note
     * there. Marking it EAGER here would hide that requirement rather than satisfy it.
     */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "app_user_roles",
            joinColumns = @JoinColumn(name = "user_id",
                    foreignKey = @ForeignKey(name = "fk_app_user_roles_user")))
    @Column(name = "role", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = EnumSet.noneOf(Role.class);

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
