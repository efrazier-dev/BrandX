package com.brandx.web.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** One editable order line. Bound as {@code items[n].*} by the order form. */
public class OrderItemForm {

    private Long id;

    @NotNull(message = "Select a product")
    private Long productId;

    @NotNull(message = "Enter a quantity")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
