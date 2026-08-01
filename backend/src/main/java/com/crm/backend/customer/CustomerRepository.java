package com.crm.backend.customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByOrganizationIdAndEmail(
            Long organizationId,
            String email
    );

    boolean existsByOrganizationIdAndEmailAndIdNot(
            Long organizationId,
            String email,
            Long id
    );
    @EntityGraph(attributePaths = {"ownerUser", "ownerUser.team"})
    @Query("""
        SELECT c FROM Customer c
        LEFT JOIN c.ownerUser owner
        LEFT JOIN owner.team ownerTeam
        WHERE c.organization.id = :organizationId
        AND (
            :allAccess = true
            OR owner.id = :currentUserId
            OR (
                :teamAccess = true
                AND :currentTeamId IS NOT NULL
                AND ownerTeam.id = :currentTeamId
            )
        )
        AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:status IS NULL OR c.status = :status)
        AND (:customerType IS NULL OR c.customerType = :customerType)
        """)
    Page<Customer> searchAccessibleCustomersInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("keyword") String keyword,
            @Param("status") CustomerStatus status,
            @Param("customerType") CustomerType customerType,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId,
            Pageable pageable
    );
    @EntityGraph(attributePaths = {"ownerUser", "ownerUser.team"})
    @Query("""
        SELECT c FROM Customer c
        LEFT JOIN c.ownerUser owner
        LEFT JOIN owner.team ownerTeam
        WHERE c.id = :id
        AND c.organization.id = :organizationId
        AND (
            :allAccess = true
            OR owner.id = :currentUserId
            OR (
                :teamAccess = true
                AND :currentTeamId IS NOT NULL
                AND ownerTeam.id = :currentTeamId
            )
        )
        """)
    Optional<Customer> findAccessibleByIdInOrganization(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );

    @Query("""
            SELECT COUNT(c) FROM Customer c
            LEFT JOIN c.ownerUser owner
            LEFT JOIN owner.team ownerTeam
            WHERE c.organization.id = :organizationId
            AND c.status = :status
            AND (
                :allAccess = true
                OR owner.id = :currentUserId
                OR (
                    :teamAccess = true
                    AND :currentTeamId IS NOT NULL
                    AND ownerTeam.id = :currentTeamId
                )
            )
            """)
    long countAccessibleByStatusInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("status") CustomerStatus status,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );
}
