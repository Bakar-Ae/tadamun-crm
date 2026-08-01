package com.crm.backend.contact;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContactRepository extends JpaRepository<Contact, Long> {

    @EntityGraph(attributePaths = {
            "customer",
            "customer.ownerUser",
            "customer.ownerUser.team"
    })
    @Query("""
        SELECT c FROM Contact c
        LEFT JOIN c.customer customer
        LEFT JOIN customer.ownerUser owner
        LEFT JOIN owner.team ownerTeam
        WHERE c.organization.id = :organizationId
        AND (:customerId IS NULL OR customer.id = :customerId)
        AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(c.position) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:status IS NULL OR c.status = :status)
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
    Page<Contact> searchAccessibleContactsInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("customerId") Long customerId,
            @Param("keyword") String keyword,
            @Param("status") ContactStatus status,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "customer",
            "customer.ownerUser",
            "customer.ownerUser.team"
    })
    @Query("""
        SELECT c FROM Contact c
        LEFT JOIN c.customer customer
        LEFT JOIN customer.ownerUser owner
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
    Optional<Contact> findAccessibleByIdInOrganization(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );
}
