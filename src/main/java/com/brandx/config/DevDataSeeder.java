package com.brandx.config;

import com.brandx.domain.Address;
import com.brandx.domain.Customer;
import com.brandx.domain.Order;
import com.brandx.domain.OrderItem;
import com.brandx.domain.OrderStatus;
import com.brandx.domain.Product;
import com.brandx.repository.CustomerRepository;
import com.brandx.repository.OrderRepository;
import com.brandx.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Inserts a small amount of demo data so the screens are not empty on first run.
 *
 * <p>Written as a runner against the repositories rather than as {@code data.sql}
 * on purpose: SQL seed scripts need
 * {@code spring.jpa.defer-datasource-initialization=true} to co-operate with
 * Hibernate's schema generation, and they bake in dialect-specific syntax — both of
 * which would undercut the goal of staying database-agnostic.
 *
 * <p>Enabled by {@code brandx.seed-data}, which only the dev profile sets to true.
 */
@Component
@ConditionalOnProperty(name = "brandx.seed-data", havingValue = "true")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final ProductRepository products;
    private final CustomerRepository customers;
    private final OrderRepository orders;

    public DevDataSeeder(ProductRepository products, CustomerRepository customers, OrderRepository orders) {
        this.products = products;
        this.customers = customers;
        this.orders = orders;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (products.count() > 0 || customers.count() > 0 || orders.count() > 0) {
            log.debug("Skipping seed data: database is not empty");
            return;
        }

        Product widget = products.save(product("BX-WIDGET-01", "Standard Widget",
                "The everyday widget. Steel body, 10mm thread.", "19.99", 250));
        Product widgetPro = products.save(product("BX-WIDGET-02", "Widget Pro",
                "Hardened widget rated for continuous duty.", "34.50", 80));
        Product gizmo = products.save(product("BX-GIZMO-01", "Gizmo Assembly",
                "Pre-assembled gizmo with mounting bracket.", "129.00", 42));
        Product sprocket = products.save(product("BX-SPROCKET-01", "Sprocket, 12T",
                "Twelve-tooth sprocket, zinc plated.", "7.25", 1000));

        Customer acme = customers.save(customer("Dana", "Whitfield", "dana.whitfield@acme-supply.example",
                "+1-312-555-0142", "400 W Erie St", "Suite 210", "Chicago", "IL", "60654"));
        Customer northwind = customers.save(customer("Priya", "Raghunathan", "p.raghunathan@northwind.example",
                "+1-206-555-0188", "1120 Pike St", null, "Seattle", "WA", "98101"));
        Customer harbor = customers.save(customer("Marcus", "Oyelaran", "marcus@harborworks.example",
                "+1-617-555-0110", "88 Seaport Blvd", "Floor 3", "Boston", "MA", "02210"));

        orders.save(order("BX-SEED-0001", acme, OrderStatus.DELIVERED, 21,
                "Standing monthly resupply.",
                List.of(new OrderItem(widget, 40, widget.getPrice()),
                        new OrderItem(sprocket, 120, sprocket.getPrice()))));

        orders.save(order("BX-SEED-0002", northwind, OrderStatus.SHIPPED, 6,
                "Rush order - expedited freight requested.",
                List.of(new OrderItem(gizmo, 3, gizmo.getPrice()),
                        new OrderItem(widgetPro, 10, widgetPro.getPrice()))));

        orders.save(order("BX-SEED-0003", harbor, OrderStatus.NEW, 1,
                null,
                List.of(new OrderItem(widgetPro, 25, widgetPro.getPrice()))));

        log.info("Seeded {} products, {} customers and {} orders",
                products.count(), customers.count(), orders.count());
    }

    private static Product product(String sku, String name, String description, String price, int stock) {
        Product p = new Product();
        p.setSku(sku);
        p.setName(name);
        p.setDescription(description);
        p.setPrice(new BigDecimal(price));
        p.setStockQuantity(stock);
        p.setActive(true);
        return p;
    }

    private static Customer customer(String first, String last, String email, String phone,
                                     String line1, String line2, String city, String state, String postalCode) {
        Customer c = new Customer();
        c.setFirstName(first);
        c.setLastName(last);
        c.setEmail(email);
        c.setPhone(phone);
        Address address = new Address();
        address.setLine1(line1);
        address.setLine2(line2);
        address.setCity(city);
        address.setState(state);
        address.setPostalCode(postalCode);
        address.setCountry("USA");
        c.setAddress(address);
        return c;
    }

    private static Order order(String number, Customer customer, OrderStatus status,
                               int daysAgo, String notes, List<OrderItem> items) {
        Order o = new Order();
        o.setOrderNumber(number);
        o.setCustomer(customer);
        o.setStatus(status);
        o.setOrderedAt(LocalDateTime.now().minusDays(daysAgo));
        o.setNotes(notes);
        items.forEach(o::addItem);
        return o;
    }
}
