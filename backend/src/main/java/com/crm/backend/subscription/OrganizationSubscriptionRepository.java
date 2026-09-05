package com.crm.backend.subscription;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationSubscriptionRepository
        extends JpaRepository<OrganizationSubscription, Long> {

    @EntityGraph(attributePaths = {"plan", "plan.features"})
    Optional<OrganizationSubscription> findByOrganizationId(
            Long organizationId
    );

    boolean existsByOrganizationId(Long organizationId);
}
