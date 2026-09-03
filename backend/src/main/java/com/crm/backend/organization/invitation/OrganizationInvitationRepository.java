package com.crm.backend.organization.invitation;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface OrganizationInvitationRepository
        extends JpaRepository<OrganizationInvitation, Long> {

    @EntityGraph(attributePaths = {"organization", "role", "invitedByUser"})
    Optional<OrganizationInvitation> findByIdAndOrganizationId(
            Long id,
            Long organizationId
    );

    @EntityGraph(attributePaths = {"organization", "role", "invitedByUser"})
    Page<OrganizationInvitation> findByOrganizationId(
            Long organizationId,
            Pageable pageable
    );

    List<OrganizationInvitation>
    findByOrganizationIdAndEmailIgnoreCaseAndStatus(
            Long organizationId,
            String email,
            OrganizationInvitationStatus status
    );

    @EntityGraph(attributePaths = {"organization", "role", "invitedByUser"})
    Optional<OrganizationInvitation> findFirstByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"organization", "role", "invitedByUser"})
    Optional<OrganizationInvitation> findByTokenHash(String tokenHash);
}
