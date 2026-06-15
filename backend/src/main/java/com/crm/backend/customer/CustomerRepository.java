package com.crm.backend.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {


    default long countByStatus(CustomerStatus status) {
        return 0;
    }

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("""
            SELECT c FROM Customer c
            WHERE (:keyword IS NULL OR :keyword = ''
                OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:status IS NULL OR c.status = :status)
            AND (:customerType IS NULL OR c.customerType = :customerType)
            """)
    Page<Customer> searchCustomers(
            @Param("keyword") String keyword,
            @Param("status") CustomerStatus status,
            @Param("customerType") CustomerType customerType,
            Pageable pageable
    );
}