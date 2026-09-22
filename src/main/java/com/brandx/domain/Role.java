package com.brandx.domain;

/**
 * What a signed-in user is allowed to do.
 *
 * <p>Persisted by {@code name} (see {@code @Enumerated(EnumType.STRING)} on
 * {@code AppUser.roles}), so these constants may be reordered freely but must not be
 * renamed without a data migration.
 *
 * <p>The constants deliberately do <em>not</em> carry the {@code ROLE_} prefix Spring
 * Security expects on a {@code GrantedAuthority}. The prefix is a framework detail and
 * is added in one place — {@link com.brandx.service.AppUserDetailsService} — so that
 * the database, this enum and the {@code hasRole(...)} expressions all read as plain
 * {@code ADMIN} / {@code STAFF}.
 */
public enum Role {

    /** Full access: everything STAFF can do, plus product/customer changes and all deletes. */
    ADMIN("Administrator"),

    /** Day-to-day access: read every screen, and create or edit orders. */
    STAFF("Staff");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    /** Human-readable form used by the Thymeleaf views. */
    public String getLabel() {
        return label;
    }
}
