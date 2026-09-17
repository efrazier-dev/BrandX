package com.brandx.web.form;

import com.brandx.domain.Order;
import com.brandx.domain.OrderItem;
import com.brandx.domain.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * Backing object for the order create/edit screen.
 *
 * <p>Orders bind through a form rather than straight onto the entity: the screen
 * submits customer and product <em>ids</em> plus a variable number of line rows,
 * which does not map onto the entity graph, and line prices must be decided by the
 * service (snapshot semantics) rather than accepted from the request.
 */
public class OrderForm {

    private Long id;

    /** Server-assigned; shown read-only when editing. */
    private String orderNumber;

    @NotNull(message = "Select a customer")
    private Long customerId;

    @NotNull(message = "Select a status")
    private OrderStatus status = OrderStatus.NEW;

    @Size(max = 2000)
    private String notes;

    // @Valid goes on the type argument, not the List: on the container it is deprecated
    // (Hibernate Validator HV000271) and does not express that it is each line that gets
    // cascaded into.
    @Size(min = 1, message = "An order needs at least one line item")
    private List<@Valid OrderItemForm> items = new ArrayList<>();

    public static OrderForm fromOrder(Order order) {
        OrderForm form = new OrderForm();
        form.setId(order.getId());
        form.setOrderNumber(order.getOrderNumber());
        form.setCustomerId(order.getCustomer() != null ? order.getCustomer().getId() : null);
        form.setStatus(order.getStatus());
        form.setNotes(order.getNotes());
        for (OrderItem item : order.getItems()) {
            OrderItemForm line = new OrderItemForm();
            line.setId(item.getId());
            line.setProductId(item.getProduct() != null ? item.getProduct().getId() : null);
            line.setQuantity(item.getQuantity());
            form.getItems().add(line);
        }
        return form;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /**
     * Returned mutable and never replaced, so Spring's data binder can auto-grow it
     * while binding sparse {@code items[n]} indexes from the submitted form.
     */
    public List<OrderItemForm> getItems() {
        return items;
    }

    public void setItems(List<OrderItemForm> items) {
        this.items = items;
    }
}
