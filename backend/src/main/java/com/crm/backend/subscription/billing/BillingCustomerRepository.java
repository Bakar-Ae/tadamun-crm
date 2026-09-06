package com.crm.backend.subscription.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.Optional;

public interface BillingCustomerRepository
        extends JpaRepository<BillingCustomer, Long> {

    @EntityGraph(attributePaths = "organization")
    Optional<BillingCustomer> findByOrganizationIdAndProvider(
            Long organizationId,
            BillingProviderName provider
    );

    @EntityGraph(attributePaths = "organization")
    Optional<BillingCustomer> findByProviderAndProviderCustomerId(
            BillingProviderName provider,
            String providerCustomerId
    );
}
