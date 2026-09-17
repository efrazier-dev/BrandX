package com.brandx.repository;

import com.brandx.domain.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

    Optional<Customer> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Uniqueness check for updates: "is this email taken by anyone other than me?" */
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    Page<Customer> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String firstName, String lastName, String email, Pageable pageable);

    /** Candidates for the order form's customer dropdown. */
    List<Customer> findAllByOrderByLastNameAscFirstNameAsc();
}
