package com.brandx.service;

import com.brandx.domain.Product;
import com.brandx.repository.OrderItemRepository;
import com.brandx.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository products;
    private final OrderItemRepository orderItems;

    public ProductService(ProductRepository products, OrderItemRepository orderItems) {
        this.products = products;
        this.orderItems = orderItems;
    }

    public Page<Product> list(String search, Pageable pageable) {
        if (!StringUtils.hasText(search)) {
            return products.findAll(pageable);
        }
        return products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCase(search, search, pageable);
    }

    public Product get(Long id) {
        return products.findById(id)
                .orElseThrow(() -> new NotFoundException("No product exists with id " + id));
    }

    /** Selectable products for the order form's line dropdown. */
    public List<Product> activeProducts() {
        return products.findByActiveTrueOrderByNameAsc();
    }

    public long count() {
        return products.count();
    }

    @Transactional
    public Product create(Product product) {
        requireUniqueSku(product.getSku(), null);
        return products.save(product);
    }

    /**
     * Copies the editable fields onto the managed entity rather than saving the
     * detached instance from the request: {@code id} and {@code createdAt} must come
     * from the database, not from whatever the browser posted.
     */
    @Transactional
    public Product update(Long id, Product submitted) {
        Product existing = get(id);
        requireUniqueSku(submitted.getSku(), id);
        existing.setSku(submitted.getSku());
        existing.setName(submitted.getName());
        existing.setDescription(submitted.getDescription());
        existing.setPrice(submitted.getPrice());
        existing.setStockQuantity(submitted.getStockQuantity());
        existing.setActive(submitted.isActive());
        return products.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Product product = get(id);
        long lines = orderItems.countByProductId(id);
        if (lines > 0) {
            throw new EntityInUseException(
                    "%s cannot be deleted because it appears on %d order line(s). Mark it inactive instead."
                            .formatted(product.getName(), lines));
        }
        products.delete(product);
    }

    private void requireUniqueSku(String sku, Long excludeId) {
        if (!StringUtils.hasText(sku)) {
            return; // bean validation already reports the blank
        }
        boolean taken = excludeId == null
                ? products.existsBySkuIgnoreCase(sku)
                : products.existsBySkuIgnoreCaseAndIdNot(sku, excludeId);
        if (taken) {
            throw new DuplicateValueException("sku", "SKU '" + sku + "' is already used by another product");
        }
    }
}
