package com.crm.backend.attachment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AttachmentRepository
        extends JpaRepository<Attachment, Long> {

    @EntityGraph(attributePaths = {"customer", "uploadedByUser"})
    Page<Attachment>
    findByOrganizationIdAndCustomerIdAndStatusOrderByCreatedAtDesc(
            Long organizationId,
            Long customerId,
            AttachmentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"lead", "uploadedByUser"})
    Page<Attachment>
    findByOrganizationIdAndLeadIdAndStatusOrderByCreatedAtDesc(
            Long organizationId,
            Long leadId,
            AttachmentStatus status,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "customer",
            "customer.ownerUser",
            "customer.ownerUser.team",
            "lead",
            "lead.assignedToUser",
            "lead.assignedToUser.team",
            "uploadedByUser"
    })
    @Query("""
            SELECT a FROM Attachment a
            LEFT JOIN a.customer customer
            LEFT JOIN customer.ownerUser customerOwner
            LEFT JOIN customerOwner.team customerOwnerTeam
            LEFT JOIN a.lead lead
            LEFT JOIN lead.assignedToUser leadOwner
            LEFT JOIN leadOwner.team leadOwnerTeam
            WHERE a.id = :id
            AND a.organization.id = :organizationId
            AND a.status = :status
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
    Optional<Attachment> findAccessibleByIdAndStatusInOrganization(
            @Param("id") Long id,
            @Param("organizationId") Long organizationId,
            @Param("status") AttachmentStatus status,
            @Param("allAccess") boolean allAccess,
            @Param("teamAccess") boolean teamAccess,
            @Param("currentUserId") Long currentUserId,
            @Param("currentTeamId") Long currentTeamId
    );
}
