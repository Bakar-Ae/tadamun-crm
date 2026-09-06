package com.crm.backend.subscription.billing;

import com.crm.backend.subscription.SubscriptionPlanCode;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingPlanPriceRepository
        extends JpaRepository<BillingPlanPrice, Long> {

    @EntityGraph(attributePaths = "plan")
    Optional<BillingPlanPrice>
    findByPlan_CodeAndProviderAndBillingIntervalAndActiveTrue(
            SubscriptionPlanCode planCode,
            BillingProviderName provider,
            BillingInterval billingInterval
    );

    @EntityGraph(attributePaths = "plan")
    Optional<BillingPlanPrice> findByProviderAndProviderPriceId(
            BillingProviderName provider,
            String providerPriceId
    );

    Optional<BillingPlanPrice>
    findByPlan_IdAndProviderAndBillingInterval(
            Long planId,
            BillingProviderName provider,
            BillingInterval billingInterval
    );
}
