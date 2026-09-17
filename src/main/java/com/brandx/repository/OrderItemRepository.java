package com.brandx.repository;

import com.brandx.domain.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Order lines are created and removed through their owning {@link com.brandx.domain.Order}
 * (cascade + orphan removal), so this repository exists only for the referential
 * check that stops a product being deleted while orders still reference it.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    long countByProductId(Long productId);
}
