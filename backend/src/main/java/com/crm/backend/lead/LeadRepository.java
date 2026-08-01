package com.crm.backend.lead;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    @EntityGraph(attributePaths = {
            "assignedToUser",
            "assignedToUser.team",
            "convertedCustomer"
    })
    @Query("""
        SELECT l FROM Lead l
        LEFT JOIN l.assignedToUser assignee
        LEFT JOIN assignee.team assigneeTeam
        WHERE l.organization.id = :organizationId
        AND (
            :allAccess = true
            OR assignee.id = :currentUserId
            OR (
                :teamAccess = true
                AND :currentTeamId IS NOT NULL
                AND assigneeTeam.id = :currentTeamId
            )
        )
        AND (
            :keyword IS NULL OR :keyword = ''
            OR LOWER(l.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(l.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(l.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(l.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        AND (:status IS NULL OR l.status = :status)
        """)
    Page<Lead> searchAccessibleLeadsInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("keyword") String keyword,
            @Param("status") LeadStatus status,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "assignedToUser",
            "assignedToUser.team",
            "convertedCustomer"
    })
    @Query("""
        SELECT l FROM Lead l
        LEFT JOIN l.assignedToUser assignee
        LEFT JOIN assignee.team assigneeTeam
        WHERE l.id = :id
        AND l.organization.id = :organizationId
        AND (
            :allAccess = true
            OR assignee.id = :currentUserId
            OR (
                :teamAccess = true
                AND :currentTeamId IS NOT NULL
                AND assigneeTeam.id = :currentTeamId
            )
        )
        """)
    Optional<Lead> findAccessibleByIdInOrganization(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );

    @Query("""
        SELECT COUNT(l) FROM Lead l
        LEFT JOIN l.assignedToUser assignee
        LEFT JOIN assignee.team assigneeTeam
        WHERE l.organization.id = :organizationId
        AND l.status = :status
        AND (
            :allAccess = true
            OR assignee.id = :currentUserId
            OR (
                :teamAccess = true
                AND :currentTeamId IS NOT NULL
                AND assigneeTeam.id = :currentTeamId
            )
        )
        """)
    long countAccessibleByStatusInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("status") LeadStatus status,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );
}
