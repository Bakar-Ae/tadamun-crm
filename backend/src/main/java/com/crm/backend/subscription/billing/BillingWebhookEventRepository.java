package com.crm.backend.subscription.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BillingWebhookEventRepository
        extends JpaRepository<BillingWebhookEvent, Long> {

    boolean existsByProviderAndProviderEventId(
            BillingProviderName provider,
            String providerEventId
    );

    Optional<BillingWebhookEvent> findByProviderAndProviderEventId(
            BillingProviderName provider,
            String providerEventId
    );
}
