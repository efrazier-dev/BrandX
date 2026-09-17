package com.brandx.service;

import com.brandx.domain.Customer;
import com.brandx.repository.CustomerRepository;
import com.brandx.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customers;
    private final OrderRepository orders;

    public CustomerService(CustomerRepository customers, OrderRepository orders) {
        this.customers = customers;
        this.orders = orders;
    }

    public Page<Customer> list(String search, Pageable pageable) {
        if (!StringUtils.hasText(search)) {
            return customers.findAll(pageable);
        }
        return customers.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                search, search, search, pageable);
    }

    public Customer get(Long id) {
        return customers.findById(id)
                .orElseThrow(() -> new NotFoundException("No customer exists with id " + id));
    }

    /** Candidates for the order form's customer dropdown. */
    public List<Customer> allForSelection() {
        return customers.findAllByOrderByLastNameAscFirstNameAsc();
    }

    public long count() {
        return customers.count();
    }

    /** How many orders reference this customer; shown on the detail screen. */
    public long orderCount(Long customerId) {
        return orders.countByCustomerId(customerId);
    }

    @Transactional
    public Customer create(Customer customer) {
        requireUniqueEmail(customer.getEmail(), null);
        return customers.save(customer);
    }

    @Transactional
    public Customer update(Long id, Customer submitted) {
        Customer existing = get(id);
        requireUniqueEmail(submitted.getEmail(), id);
        existing.setFirstName(submitted.getFirstName());
        existing.setLastName(submitted.getLastName());
        existing.setEmail(submitted.getEmail());
        existing.setPhone(submitted.getPhone());
        existing.setAddress(submitted.getAddress());
        return customers.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = get(id);
        long orderCount = orders.countByCustomerId(id);
        if (orderCount > 0) {
            throw new EntityInUseException(
                    "%s cannot be deleted because they have %d order(s). Delete or reassign those orders first."
                            .formatted(customer.getFullName(), orderCount));
        }
        customers.delete(customer);
    }

    private void requireUniqueEmail(String email, Long excludeId) {
        if (!StringUtils.hasText(email)) {
            return; // bean validation already reports the blank
        }
        boolean taken = excludeId == null
                ? customers.existsByEmailIgnoreCase(email)
                : customers.existsByEmailIgnoreCaseAndIdNot(email, excludeId);
        if (taken) {
            throw new DuplicateValueException("email", "Email '" + email + "' is already used by another customer");
        }
    }
}
