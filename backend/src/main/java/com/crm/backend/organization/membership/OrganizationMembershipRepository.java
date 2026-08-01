package com.crm.backend.organization.membership;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrganizationMembershipRepository
        extends JpaRepository<OrganizationMembership, Long> {

    @Override
    @EntityGraph(attributePaths = {"organization", "user", "role"})
    Optional<OrganizationMembership> findById(Long id);

    @EntityGraph(attributePaths = {"organization", "user", "role"})
    Optional<OrganizationMembership> findByOrganizationIdAndUserId(
            Long organizationId,
            Long userId
    );

    @EntityGraph(attributePaths = {"organization", "user", "role"})
    Optional<OrganizationMembership>
    findByOrganizationIdAndUserIdAndStatus(
            Long organizationId,
            Long userId,
            OrganizationMembershipStatus status
    );

    boolean existsByOrganizationIdAndUserId(
            Long organizationId,
            Long userId
    );

    boolean existsByOrganizationIdAndUserIdAndStatus(
            Long organizationId,
            Long userId,
            OrganizationMembershipStatus status
    );

    @EntityGraph(attributePaths = {"organization", "user", "role"})
    Page<OrganizationMembership> findByOrganizationId(
            Long organizationId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"organization", "user", "role"})
    List<OrganizationMembership>
    findByUserIdAndStatusOrderByOrganizationNameAsc(
            Long userId,
            OrganizationMembershipStatus status
    );
}
