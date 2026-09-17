package com.brandx.domain;

/**
 * Lifecycle of an {@link Order}.
 *
 * <p>Persisted by {@code name} (see {@code @Enumerated(EnumType.STRING)} on
 * {@code Order.status}), so these constants may be reordered freely but must not
 * be renamed without a data migration.
 */
public enum OrderStatus {

    NEW("New"),
    PAID("Paid"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered"),
    CANCELLED("Cancelled");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    /** Human-readable form used by the Thymeleaf views. */
    public String getLabel() {
        return label;
    }
}
