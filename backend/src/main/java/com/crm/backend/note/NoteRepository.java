package com.crm.backend.note;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @EntityGraph(attributePaths = {"createdByUser"})
    Page<Note> findByOrganizationIdAndCustomerId(
            Long organizationId,
            Long customerId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"createdByUser"})
    Page<Note> findByOrganizationIdAndLeadId(
            Long organizationId,
            Long leadId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "customer",
            "customer.ownerUser",
            "customer.ownerUser.team",
            "lead",
            "lead.assignedToUser",
            "lead.assignedToUser.team",
            "createdByUser"
    })
    @Query("""
            SELECT n FROM Note n
            LEFT JOIN n.customer customer
            LEFT JOIN customer.ownerUser customerOwner
            LEFT JOIN customerOwner.team customerOwnerTeam
            LEFT JOIN n.lead lead
            LEFT JOIN lead.assignedToUser leadOwner
            LEFT JOIN leadOwner.team leadOwnerTeam
            WHERE n.organization.id = :organizationId
            AND (
                :keyword IS NULL
                OR :keyword = ''
                OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(customer.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(lead.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(lead.companyName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            AND (
                :allAccess = true
                OR customerOwner.id = :currentUserId
                OR leadOwner.id = :currentUserId
                OR (
                    :teamAccess = true
                    AND :currentTeamId IS NOT NULL
                    AND (
                        customerOwnerTeam.id = :currentTeamId
                        OR leadOwnerTeam.id = :currentTeamId
                    )
                )
            )
            """)
    Page<Note> searchAccessibleNotesInOrganization(
            @Param("organizationId") Long organizationId,
            @Param("keyword") String keyword,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "customer",
            "customer.ownerUser",
            "customer.ownerUser.team",
            "lead",
            "lead.assignedToUser",
            "lead.assignedToUser.team",
            "createdByUser"
    })
    @Query("""
            SELECT n FROM Note n
            LEFT JOIN n.customer customer
            LEFT JOIN customer.ownerUser customerOwner
            LEFT JOIN customerOwner.team customerOwnerTeam
            LEFT JOIN n.lead lead
            LEFT JOIN lead.assignedToUser leadOwner
            LEFT JOIN leadOwner.team leadOwnerTeam
            WHERE n.id = :id
            AND n.organization.id = :organizationId
            AND (
                :allAccess = true
                OR customerOwner.id = :currentUserId
                OR leadOwner.id = :currentUserId
                OR (
                    :teamAccess = true
                    AND :currentTeamId IS NOT NULL
                    AND (
                        customerOwnerTeam.id = :currentTeamId
                        OR leadOwnerTeam.id = :currentTeamId
                    )
                )
            )
            """)
    Optional<Note> findAccessibleByIdInOrganization(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );
}
