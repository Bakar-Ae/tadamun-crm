package com.crm.backend.organization.membership;

import com.crm.backend.role.RoleName;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OrganizationMembershipRepository
        extends JpaRepository<OrganizationMembership, Long> {

    @Override
    @EntityGraph(attributePaths = {"organization", "user", "role"})
    Optional<OrganizationMembership> findById(Long id);

    @EntityGraph(attributePaths = {"organization", "user", "role"})
    Optional<OrganizationMembership> findByIdAndOrganizationId(
            Long id,
            Long organizationId
    );

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

    @EntityGraph(attributePaths = "user")
    @Query("""
            SELECT membership
            FROM OrganizationMembership membership
            WHERE membership.organization.id = :organizationId
            AND membership.status = :status
            AND membership.role.name IN :roleNames
            """)
    List<OrganizationMembership> findNotificationRecipients(
            @Param("organizationId") Long organizationId,
            @Param("status") OrganizationMembershipStatus status,
            @Param("roleNames") Set<RoleName> roleNames
    );

    long countByOrganizationIdAndStatus(
            Long organizationId,
            OrganizationMembershipStatus status
    );
}
