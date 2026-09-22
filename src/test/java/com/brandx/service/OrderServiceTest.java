package com.brandx.service;

import com.brandx.domain.Customer;
import com.brandx.domain.Order;
import com.brandx.domain.OrderItem;
import com.brandx.domain.OrderStatus;
import com.brandx.domain.Product;
import com.brandx.repository.CustomerRepository;
import com.brandx.repository.OrderRepository;
import com.brandx.repository.ProductRepository;
import com.brandx.web.form.OrderForm;
import com.brandx.web.form.OrderItemForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orders;

    @Mock
    private CustomerRepository customers;

    @Mock
    private ProductRepository products;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orders, customers, products);
    }

    @Test
    void list_blankSearch_delegatesToFindAll() {
        Pageable pageable = Pageable.unpaged();
        Page<Order> page = new PageImpl<>(List.of());
        when(orders.findAll(pageable)).thenReturn(page);

        assertThat(service.list(null, pageable)).isSameAs(page);
        verify(orders, never()).search(any(), any());
    }

    @Test
    void list_withSearch_delegatesToSearch() {
        Pageable pageable = Pageable.unpaged();
        Page<Order> page = new PageImpl<>(List.of());
        when(orders.search("BX-1", pageable)).thenReturn(page);

        assertThat(service.list("BX-1", pageable)).isSameAs(page);
    }

    @Test
    void getDetail_found_delegatesToFindDetailById() {
        Order order = anOrder(1L, "BX-1");
        when(orders.findDetailById(1L)).thenReturn(Optional.of(order));

        assertThat(service.getDetail(1L)).isSameAs(order);
    }

    @Test
    void getDetail_notFound_throwsNotFoundException() {
        when(orders.findDetailById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void count_delegatesToRepositoryCount() {
        when(orders.count()).thenReturn(9L);

        assertThat(service.count()).isEqualTo(9L);
    }

    @Test
    void totalsFor_emptyCollection_returnsEmptyMapWithoutQuerying() {
        Map<Long, BigDecimal> totals = service.totalsFor(List.of());

        assertThat(totals).isEmpty();
        verify(orders, never()).findTotalsByOrderIds(anyCollection());
    }

    @Test
    void totalsFor_nonEmpty_buildsMapFromProjection() {
        OrderRepository.OrderTotal row1 = totalRow(1L, new BigDecimal("30.00"));
        OrderRepository.OrderTotal row2 = totalRow(2L, new BigDecimal("15.50"));
        when(orders.findTotalsByOrderIds(List.of(1L, 2L))).thenReturn(List.of(row1, row2));

        Map<Long, BigDecimal> totals = service.totalsFor(List.of(1L, 2L));

        assertThat(totals).containsEntry(1L, new BigDecimal("30.00")).containsEntry(2L, new BigDecimal("15.50"));
    }

    @Test
    void create_success_generatesOrderNumberAndAppliesForm() {
        Customer customer = aCustomer(1L);
        Product product = aProduct(1L, new BigDecimal("50.00"));
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(orders.existsByOrderNumberIgnoreCase(any())).thenReturn(false);
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderForm form = anOrderForm(null, 1L, "notes", line(1L, 2));
        Order result = service.create(form);

        // The exact random digits and instant aren't asserted: OrderService doesn't
        // take an injectable Clock/Random, so only format/non-null are checkable here.
        assertThat(result.getOrderNumber()).matches("BX-\\d{8}-\\d{4}");
        assertThat(result.getOrderedAt()).isNotNull();
        assertThat(result.getCustomer()).isSameAs(customer);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(result.getNotes()).isEqualTo("notes");
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getUnitPrice()).isEqualByComparingTo("50.00");
    }

    @Test
    void create_customerNotFound_throwsNotFoundException_andNeverSaves() {
        when(orders.existsByOrderNumberIgnoreCase(any())).thenReturn(false);
        when(customers.findById(1L)).thenReturn(Optional.empty());

        OrderForm form = anOrderForm(null, 1L, null, line(1L, 1));

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(NotFoundException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void create_productNotFound_throwsNotFoundException_andNeverSaves() {
        Customer customer = aCustomer(1L);
        when(orders.existsByOrderNumberIgnoreCase(any())).thenReturn(false);
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(1L)).thenReturn(Optional.empty());

        OrderForm form = anOrderForm(null, 1L, null, line(1L, 1));

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(NotFoundException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void create_orderNumberRetriesOnCollision_thenSucceeds() {
        Customer customer = aCustomer(1L);
        Product product = aProduct(1L, new BigDecimal("50.00"));
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(1L)).thenReturn(Optional.of(product));
        when(orders.existsByOrderNumberIgnoreCase(any())).thenReturn(true, true, false);
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderForm form = anOrderForm(null, 1L, null, line(1L, 1));
        Order result = service.create(form);

        assertThat(result.getOrderNumber()).matches("BX-\\d{8}-\\d{4}");
        verify(orders, times(3)).existsByOrderNumberIgnoreCase(any());
    }

    @Test
    void create_orderNumberExhaustsAllAttempts_throwsIllegalStateException() {
        when(orders.existsByOrderNumberIgnoreCase(any())).thenReturn(true);

        OrderForm form = anOrderForm(null, 1L, null, line(1L, 1));

        assertThatThrownBy(() -> service.create(form)).isInstanceOf(IllegalStateException.class);
        verify(orders, times(10)).existsByOrderNumberIgnoreCase(any());
        verify(customers, never()).findById(any());
        verify(orders, never()).save(any());
    }

    @Test
    void update_priceSnapshot_reusedProductKeepsBookedPrice_newProductUsesCurrentPrice() {
        Product productA = aProduct(1L, new BigDecimal("99.00")); // current price changed since booking
        Product productB = aProduct(2L, new BigDecimal("50.00")); // never previously booked
        Customer customer = aCustomer(1L);

        Order existingOrder = anOrder(10L, "BX-1");
        existingOrder.setCustomer(customer);
        existingOrder.addItem(new OrderItem(productA, 1, new BigDecimal("10.00")));

        when(orders.findDetailById(10L)).thenReturn(Optional.of(existingOrder));
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(1L)).thenReturn(Optional.of(productA));
        when(products.findById(2L)).thenReturn(Optional.of(productB));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderForm form = anOrderForm(10L, 1L, null, line(1L, 2), line(2L, 1));
        Order result = service.update(10L, form);

        assertThat(result.getItems()).hasSize(2);
        OrderItem itemA = result.getItems().stream().filter(i -> i.getProduct() == productA).findFirst().orElseThrow();
        OrderItem itemB = result.getItems().stream().filter(i -> i.getProduct() == productB).findFirst().orElseThrow();
        assertThat(itemA.getUnitPrice()).isEqualByComparingTo("10.00");
        assertThat(itemB.getUnitPrice()).isEqualByComparingTo("50.00");
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        when(orders.findDetailById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(10L, anOrderForm(10L, 1L, null, line(1L, 1))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_customerNotFound_throwsNotFoundException_andNeverSaves() {
        Order existingOrder = anOrder(10L, "BX-1");
        existingOrder.setCustomer(aCustomer(1L));
        when(orders.findDetailById(10L)).thenReturn(Optional.of(existingOrder));
        when(customers.findById(2L)).thenReturn(Optional.empty());

        OrderForm form = anOrderForm(10L, 2L, null, line(1L, 1));

        assertThatThrownBy(() -> service.update(10L, form)).isInstanceOf(NotFoundException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void update_productNotFound_throwsNotFoundException_andNeverSaves() {
        Customer customer = aCustomer(1L);
        Order existingOrder = anOrder(10L, "BX-1");
        existingOrder.setCustomer(customer);
        when(orders.findDetailById(10L)).thenReturn(Optional.of(existingOrder));
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(products.findById(1L)).thenReturn(Optional.empty());

        OrderForm form = anOrderForm(10L, 1L, null, line(1L, 1));

        assertThatThrownBy(() -> service.update(10L, form)).isInstanceOf(NotFoundException.class);
        verify(orders, never()).save(any());
    }

    @Test
    void update_skipsNullOrProductlessLine() {
        Customer customer = aCustomer(1L);
        Order existingOrder = anOrder(10L, "BX-1");
        existingOrder.setCustomer(customer);
        when(orders.findDetailById(10L)).thenReturn(Optional.of(existingOrder));
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(orders.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OrderItemForm blankLine = new OrderItemForm();
        blankLine.setProductId(null);
        OrderForm form = anOrderForm(10L, 1L, null);
        form.getItems().add(blankLine);

        Order result = service.update(10L, form);

        assertThat(result.getItems()).isEmpty();
        verify(products, never()).findById(any());
    }

    @Test
    void delete_success_findsAndDeletesNoGuard() {
        // Unlike Product/Customer, order deletion has no EntityInUseException guard --
        // nothing else references an order, so cascade/orphanRemoval is sufficient.
        Order order = anOrder(10L, "BX-1");
        when(orders.findById(10L)).thenReturn(Optional.of(order));

        service.delete(10L);

        verify(orders).delete(order);
    }

    @Test
    void delete_notFound_throwsNotFoundException_andNeverDeletes() {
        when(orders.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(10L)).isInstanceOf(NotFoundException.class);
        verify(orders, never()).delete(any());
    }

    private static Order anOrder(Long id, String orderNumber) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        return order;
    }

    private static Customer aCustomer(Long id) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFirstName("Jane");
        customer.setLastName("Doe");
        customer.setEmail("jane@example.com");
        return customer;
    }

    private static Product aProduct(Long id, BigDecimal price) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-" + id);
        product.setName("Product " + id);
        product.setPrice(price);
        product.setStockQuantity(100);
        product.setActive(true);
        return product;
    }

    private static OrderItemForm line(Long productId, int quantity) {
        OrderItemForm line = new OrderItemForm();
        line.setProductId(productId);
        line.setQuantity(quantity);
        return line;
    }

    private static OrderForm anOrderForm(Long id, Long customerId, String notes, OrderItemForm... lines) {
        OrderForm form = new OrderForm();
        form.setId(id);
        form.setCustomerId(customerId);
        form.setStatus(OrderStatus.NEW);
        form.setNotes(notes);
        for (OrderItemForm line : lines) {
            form.getItems().add(line);
        }
        return form;
    }

    private static OrderRepository.OrderTotal totalRow(Long orderId, BigDecimal total) {
        OrderRepository.OrderTotal row = org.mockito.Mockito.mock(OrderRepository.OrderTotal.class);
        when(row.getOrderId()).thenReturn(orderId);
        when(row.getTotal()).thenReturn(total);
        return row;
    }
}
