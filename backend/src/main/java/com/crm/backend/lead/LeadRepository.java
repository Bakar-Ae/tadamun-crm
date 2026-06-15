package com.crm.backend.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    default long countByStatus(LeadStatus status) {
        return 0;
    }

    @Query("""
            SELECT l FROM Lead l
            WHERE (:keyword IS NULL OR :keyword = ''
                OR LOWER(l.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(l.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(l.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(l.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:status IS NULL OR l.status = :status)
            """)
    Page<Lead> searchLeads(
            @Param("keyword") String keyword,
            @Param("status") LeadStatus status,
            Pageable pageable
    );
}