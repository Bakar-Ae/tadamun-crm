package com.crm.backend.subscription;

import com.crm.backend.subscription.billing.BillingProviderName;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface OrganizationSubscriptionRepository
        extends JpaRepository<OrganizationSubscription, Long> {

    @EntityGraph(attributePaths = {"plan", "plan.features"})
    Optional<OrganizationSubscription> findByOrganizationId(
            Long organizationId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"plan", "plan.features"})
    @Query("""
            SELECT subscription
            FROM OrganizationSubscription subscription
            WHERE subscription.organization.id = :organizationId
            """)
    Optional<OrganizationSubscription> findForUpdateByOrganizationId(
            @Param("organizationId") Long organizationId
    );

    boolean existsByOrganizationId(Long organizationId);

    @EntityGraph(attributePaths = {"plan", "plan.features"})
    Optional<OrganizationSubscription>
    findByBillingProviderAndProviderSubscriptionId(
            BillingProviderName billingProvider,
            String providerSubscriptionId
    );
}
