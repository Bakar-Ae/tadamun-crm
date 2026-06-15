package com.crm.backend.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    @Query("""
            SELECT c FROM Contact c
            WHERE (:customerId IS NULL OR c.customer.id = :customerId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.position) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:status IS NULL OR c.status = :status)
            """)
    Page<Contact> searchContacts(
            @Param("customerId") Long customerId,
            @Param("keyword") String keyword,
            @Param("status") ContactStatus status,
            Pageable pageable
    );
}