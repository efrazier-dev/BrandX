package com.brandx.service;

import com.brandx.domain.Customer;
import com.brandx.repository.CustomerRepository;
import com.brandx.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customers;

    @Mock
    private OrderRepository orders;

    private CustomerService service;

    @BeforeEach
    void setUp() {
        service = new CustomerService(customers, orders);
    }

    @Test
    void list_blankSearch_delegatesToFindAll() {
        Pageable pageable = Pageable.unpaged();
        Page<Customer> page = new PageImpl<>(List.of(aCustomer(1L, "Jane", "Doe", "jane@example.com")));
        when(customers.findAll(pageable)).thenReturn(page);

        Page<Customer> result = service.list(null, pageable);

        assertThat(result).isSameAs(page);
        verify(customers, never())
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                        any(), any(), any(), any());
    }

    @Test
    void list_withSearch_delegatesToTriTermSearch() {
        Pageable pageable = Pageable.unpaged();
        Page<Customer> page = new PageImpl<>(List.of(aCustomer(1L, "Jane", "Doe", "jane@example.com")));
        when(customers.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                "jane", "jane", "jane", pageable)).thenReturn(page);

        Page<Customer> result = service.list("jane", pageable);

        assertThat(result).isSameAs(page);
    }

    @Test
    void get_found_returnsCustomer() {
        Customer customer = aCustomer(1L, "Jane", "Doe", "jane@example.com");
        when(customers.findById(1L)).thenReturn(Optional.of(customer));

        assertThat(service.get(1L)).isSameAs(customer);
    }

    @Test
    void get_notFound_throwsNotFoundException() {
        when(customers.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void allForSelection_delegatesToFindAllByOrderByLastNameAscFirstNameAsc() {
        List<Customer> all = List.of(aCustomer(1L, "Jane", "Doe", "jane@example.com"));
        when(customers.findAllByOrderByLastNameAscFirstNameAsc()).thenReturn(all);

        assertThat(service.allForSelection()).isSameAs(all);
    }

    @Test
    void count_delegatesToRepositoryCount() {
        when(customers.count()).thenReturn(4L);

        assertThat(service.count()).isEqualTo(4L);
    }

    @Test
    void orderCount_delegatesToOrdersCountByCustomerId() {
        when(orders.countByCustomerId(1L)).thenReturn(2L);

        assertThat(service.orderCount(1L)).isEqualTo(2L);
    }

    @Test
    void create_success_savesFreshEntityCopyingFirstLastEmailPhoneAddress() {
        Customer submitted = aCustomer(999L, "Jane", "Doe", "jane@example.com");
        submitted.setPhone("555-1234");
        when(customers.existsByEmailIgnoreCase("jane@example.com")).thenReturn(false);
        when(customers.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer result = service.create(submitted);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customers).save(captor.capture());
        Customer saved = captor.getValue();
        assertThat(saved).isNotSameAs(submitted);
        assertThat(saved.getId()).isNull();
        assertThat(saved.getFirstName()).isEqualTo("Jane");
        assertThat(saved.getLastName()).isEqualTo("Doe");
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getPhone()).isEqualTo("555-1234");
        assertThat(saved.getAddress()).isSameAs(submitted.getAddress());
        assertThat(result).isSameAs(saved);
    }

    @Test
    void create_duplicateEmail_throwsDuplicateValueException_andNeverSaves() {
        Customer submitted = aCustomer(null, "Jane", "Doe", "jane@example.com");
        when(customers.existsByEmailIgnoreCase("jane@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(submitted))
                .isInstanceOf(DuplicateValueException.class)
                .satisfies(ex -> assertThat(((DuplicateValueException) ex).getField()).isEqualTo("email"));
        verify(customers, never()).save(any());
    }

    @Test
    void create_blankEmail_skipsUniquenessCheck() {
        Customer submitted = aCustomer(null, "Jane", "Doe", "  ");
        when(customers.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(submitted);

        verify(customers, never()).existsByEmailIgnoreCase(any());
    }

    @Test
    void update_success_mutatesManagedEntity() {
        Customer existing = aCustomer(1L, "Jane", "Doe", "jane@example.com");
        Customer submitted = aCustomer(null, "Janet", "Smith", "janet@example.com");
        submitted.setPhone("555-9999");
        when(customers.findById(1L)).thenReturn(Optional.of(existing));
        when(customers.existsByEmailIgnoreCaseAndIdNot("janet@example.com", 1L)).thenReturn(false);
        when(customers.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Customer result = service.update(1L, submitted);

        assertThat(result).isSameAs(existing);
        assertThat(existing.getId()).isEqualTo(1L);
        assertThat(existing.getFirstName()).isEqualTo("Janet");
        assertThat(existing.getLastName()).isEqualTo("Smith");
        assertThat(existing.getEmail()).isEqualTo("janet@example.com");
        assertThat(existing.getPhone()).isEqualTo("555-9999");
    }

    @Test
    void update_duplicateEmailExcludingSelf_throwsDuplicateValueException() {
        Customer existing = aCustomer(1L, "Jane", "Doe", "jane@example.com");
        Customer submitted = aCustomer(null, "Jane", "Doe", "taken@example.com");
        when(customers.findById(1L)).thenReturn(Optional.of(existing));
        when(customers.existsByEmailIgnoreCaseAndIdNot("taken@example.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, submitted)).isInstanceOf(DuplicateValueException.class);
        verify(customers, never()).save(any());
    }

    @Test
    void update_notFound_throwsNotFoundException() {
        when(customers.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(1L, aCustomer(null, "Jane", "Doe", "jane@example.com")))
                .isInstanceOf(NotFoundException.class);
        verify(customers, never()).existsByEmailIgnoreCaseAndIdNot(any(), any());
    }

    @Test
    void delete_success_deletesWhenNoOrders() {
        Customer customer = aCustomer(1L, "Jane", "Doe", "jane@example.com");
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(orders.countByCustomerId(1L)).thenReturn(0L);

        service.delete(1L);

        verify(customers).delete(customer);
    }

    @Test
    void delete_throwsEntityInUseException_whenReferencedByOrders() {
        Customer customer = aCustomer(1L, "Jane", "Doe", "jane@example.com");
        when(customers.findById(1L)).thenReturn(Optional.of(customer));
        when(orders.countByCustomerId(1L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(EntityInUseException.class);
        verify(customers, never()).delete(any());
    }

    @Test
    void delete_notFound_throwsNotFoundException_beforeCountCheck() {
        when(customers.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(NotFoundException.class);
        verify(orders, never()).countByCustomerId(any());
    }

    private static Customer aCustomer(Long id, String firstName, String lastName, String email) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setEmail(email);
        return customer;
    }
}
