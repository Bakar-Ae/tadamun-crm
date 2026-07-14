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
            WHERE (:customerId IS NULL OR c.customer.id = :customerId)
            AND (:keyword IS NULL OR :keyword = ''
                OR LOWER(c.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(c.position) LIKE LOWER(CONCAT('%', :keyword, '%')))
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
    Page<Contact> searchAccessibleContacts(
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
    Optional<Contact> findAccessibleById(
            @Param("id") Long id,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );
}
