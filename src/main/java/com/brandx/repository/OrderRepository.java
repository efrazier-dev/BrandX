package com.brandx.repository;

import com.brandx.domain.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Overridden purely to attach the customer graph. {@code spring.jpa.open-in-view}
     * is off, so the list template cannot lazily initialise {@code order.customer} —
     * and without the graph this would be a textbook N+1 (one query per row).
     *
     * <p>Fetching a to-one association alongside a {@code Pageable} is safe; it is
     * fetching a <em>collection</em> that would force Hibernate to paginate in memory.
     */
    @Override
    @EntityGraph(attributePaths = "customer")
    Page<Order> findAll(Pageable pageable);

    @Query(value = """
            select o from Order o
            join fetch o.customer c
            where lower(o.orderNumber) like lower(concat('%', :term, '%'))
               or lower(c.firstName) like lower(concat('%', :term, '%'))
               or lower(c.lastName) like lower(concat('%', :term, '%'))
               or lower(c.email) like lower(concat('%', :term, '%'))
            """,
            countQuery = """
            select count(o) from Order o
            join o.customer c
            where lower(o.orderNumber) like lower(concat('%', :term, '%'))
               or lower(c.firstName) like lower(concat('%', :term, '%'))
               or lower(c.lastName) like lower(concat('%', :term, '%'))
               or lower(c.email) like lower(concat('%', :term, '%'))
            """)
    Page<Order> search(@Param("term") String term, Pageable pageable);

    /**
     * Loads an order with everything the detail and edit views touch, in one query.
     * No {@code distinct} needed: Hibernate 6+ de-duplicates root entities from a
     * collection join fetch automatically, and adding SQL DISTINCT would only cost.
     */
    @Query("""
            select o from Order o
            join fetch o.customer
            left join fetch o.items i
            left join fetch i.product
            where o.id = :id
            """)
    Optional<Order> findDetailById(@Param("id") Long id);

    /** Guards customer deletion: a customer with orders must not be removed. */
    long countByCustomerId(Long customerId);

    boolean existsByOrderNumberIgnoreCase(String orderNumber);

    /**
     * Line-item totals for a batch of orders, aggregated in the database.
     *
     * <p>This exists so the order list can show a total without walking
     * {@code order.items}: that association is not fetched by the list query (a
     * collection fetch plus {@code Pageable} forces Hibernate to paginate in memory),
     * and with {@code open-in-view} off, touching it in the template would throw
     * LazyInitializationException. One grouped query for the whole page instead.
     */
    @Query("""
            select i.order.id as orderId, sum(i.unitPrice * i.quantity) as total
            from OrderItem i
            where i.order.id in :orderIds
            group by i.order.id
            """)
    List<OrderTotal> findTotalsByOrderIds(@Param("orderIds") Collection<Long> orderIds);

    /** Projection for {@link #findTotalsByOrderIds}. */
    interface OrderTotal {
        Long getOrderId();

        BigDecimal getTotal();
    }
}
