package com.brandx.web;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Locale;

/**
 * Helpers for keeping list-screen state in the URL.
 *
 * <p>Without this the sort order silently resets whenever the user pages, because
 * the pagination links would not carry the current {@code sort} parameter forward.
 */
public final class PageSupport {

    private PageSupport() {
    }

    /**
     * Renders the active sort as the {@code sort=property,direction} string Spring
     * Data parses back.
     *
     * <p>Only the first order is emitted: a single {@code sort} request parameter
     * cannot express a multi-property sort, and every list screen here sorts by one
     * column at a time.
     *
     * @return the sort parameter, or {@code null} when unsorted
     */
    public static String sortParam(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return null;
        }
        Sort.Order order = sort.iterator().next();
        return order.getProperty() + "," + order.getDirection().name().toLowerCase(Locale.ROOT);
    }
}
