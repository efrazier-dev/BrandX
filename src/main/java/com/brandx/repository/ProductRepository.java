package com.brandx.repository;

import com.brandx.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCase(String sku);

    /** Uniqueness check for updates: "is this SKU taken by anyone other than me?" */
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    Page<Product> findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(
            String name, String sku, Pageable pageable);

    /** Candidates for the order-line product dropdown. */
    List<Product> findByActiveTrueOrderByNameAsc();
}
