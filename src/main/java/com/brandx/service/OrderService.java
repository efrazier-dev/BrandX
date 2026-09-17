package com.brandx.service;

import com.brandx.domain.Customer;
import com.brandx.domain.Order;
import com.brandx.domain.OrderItem;
import com.brandx.domain.Product;
import com.brandx.repository.CustomerRepository;
import com.brandx.repository.OrderRepository;
import com.brandx.repository.ProductRepository;
import com.brandx.web.form.OrderForm;
import com.brandx.web.form.OrderItemForm;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final int ORDER_NUMBER_ATTEMPTS = 10;

    private final OrderRepository orders;
    private final CustomerRepository customers;
    private final ProductRepository products;

    public OrderService(OrderRepository orders, CustomerRepository customers, ProductRepository products) {
        this.orders = orders;
        this.customers = customers;
        this.products = products;
    }

    public Page<Order> list(String search, Pageable pageable) {
        if (!StringUtils.hasText(search)) {
            return orders.findAll(pageable);
        }
        return orders.search(search, pageable);
    }

    /**
     * Loads the customer and the lines (with their products) eagerly.
     *
     * <p>Required, not an optimisation: {@code spring.jpa.open-in-view} is disabled,
     * so the Hibernate session is closed by the time Thymeleaf renders. Anything the
     * template touches must already be initialised or it throws LazyInitializationException.
     */
    public Order getDetail(Long id) {
        return orders.findDetailById(id)
                .orElseThrow(() -> new NotFoundException("No order exists with id " + id));
    }

    public long count() {
        return orders.count();
    }

    /**
     * Order id -> line total, for every order on the page currently being rendered.
     * One aggregate query for the whole page rather than one per row.
     */
    public Map<Long, BigDecimal> totalsFor(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) {
            // `in ()` is a syntax error on several databases, so never issue it.
            return Map.of();
        }
        Map<Long, BigDecimal> totals = new HashMap<>();
        for (OrderRepository.OrderTotal row : orders.findTotalsByOrderIds(orderIds)) {
            totals.put(row.getOrderId(), row.getTotal());
        }
        return totals;
    }

    @Transactional
    public Order create(OrderForm form) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setOrderedAt(LocalDateTime.now());
        applyForm(order, form);
        return orders.save(order);
    }

    @Transactional
    public Order update(Long id, OrderForm form) {
        Order order = getDetail(id);
        applyForm(order, form);
        return orders.save(order);
    }

    @Transactional
    public void delete(Long id) {
        Order order = orders.findById(id)
                .orElseThrow(() -> new NotFoundException("No order exists with id " + id));
        // Lines go with it: the association cascades and uses orphanRemoval.
        orders.delete(order);
    }

    private void applyForm(Order order, OrderForm form) {
        Customer customer = customers.findById(form.getCustomerId())
                .orElseThrow(() -> new NotFoundException("No customer exists with id " + form.getCustomerId()));
        order.setCustomer(customer);
        order.setStatus(form.getStatus());
        order.setNotes(form.getNotes());

        // Preserve the price each product was already booked at. Without this, editing
        // an order would re-read product.price and silently restate order history at
        // today's prices, defeating the point of the unitPrice snapshot.
        Map<Long, BigDecimal> bookedPrices = new HashMap<>();
        for (OrderItem existing : order.getItems()) {
            if (existing.getProduct() != null && existing.getUnitPrice() != null) {
                bookedPrices.putIfAbsent(existing.getProduct().getId(), existing.getUnitPrice());
            }
        }

        order.clearItems();
        for (OrderItemForm line : form.getItems()) {
            if (line == null || line.getProductId() == null) {
                continue; // blank row; bean validation reports it before we get here
            }
            Product product = products.findById(line.getProductId())
                    .orElseThrow(() -> new NotFoundException("No product exists with id " + line.getProductId()));
            BigDecimal unitPrice = bookedPrices.getOrDefault(product.getId(), product.getPrice());
            order.addItem(new OrderItem(product, line.getQuantity(), unitPrice));
        }
    }

    /**
     * Allocates a human-friendly order number, retrying on the (unlikely) collision.
     * The unique constraint on {@code order_number} remains the real guarantee.
     */
    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        for (int attempt = 0; attempt < ORDER_NUMBER_ATTEMPTS; attempt++) {
            String candidate = "BX-%s-%04d".formatted(datePart, ThreadLocalRandom.current().nextInt(10_000));
            if (!orders.existsByOrderNumberIgnoreCase(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException(
                "Could not allocate a unique order number after " + ORDER_NUMBER_ATTEMPTS + " attempts");
    }
}
