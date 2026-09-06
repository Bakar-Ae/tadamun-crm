package com.crm.backend.subscription.billing.stripe;

import com.crm.backend.subscription.SubscriptionPlan;
import com.crm.backend.subscription.SubscriptionPlanCode;
import com.crm.backend.subscription.SubscriptionPlanRepository;
import com.crm.backend.subscription.billing.BillingInterval;
import com.crm.backend.subscription.billing.BillingPlanPrice;
import com.crm.backend.subscription.billing.BillingPlanPriceRepository;
import com.crm.backend.subscription.billing.BillingProviderName;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        prefix = "app.billing.stripe",
        name = "enabled",
        havingValue = "true"
)
public class StripePriceCatalogSynchronizer {

    private final StripeBillingProperties properties;
    private final SubscriptionPlanRepository planRepository;
    private final BillingPlanPriceRepository priceRepository;

    public StripePriceCatalogSynchronizer(
            StripeBillingProperties properties,
            SubscriptionPlanRepository planRepository,
            BillingPlanPriceRepository priceRepository
    ) {
        this.properties = properties;
        this.planRepository = planRepository;
        this.priceRepository = priceRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void synchronizeConfiguredPrices() {
        for (SubscriptionPlanCode planCode : SubscriptionPlanCode.values()) {
            synchronize(planCode, properties.priceIdFor(planCode));
        }
    }

    private void synchronize(
            SubscriptionPlanCode planCode,
            String configuredPriceId
    ) {
        if (configuredPriceId == null || configuredPriceId.isBlank()) {
            return;
        }

        SubscriptionPlan plan = planRepository
                .findByCodeAndActiveTrue(planCode)
                .orElseThrow(() -> new IllegalStateException(
                        "Active subscription plan not found: " + planCode
                ));

        BillingPlanPrice price = priceRepository
                .findByPlan_IdAndProviderAndBillingInterval(
                        plan.getId(),
                        BillingProviderName.STRIPE,
                        BillingInterval.MONTHLY
                )
                .orElseGet(BillingPlanPrice::new);

        price.setPlan(plan);
        price.setProvider(BillingProviderName.STRIPE);
        price.setProviderPriceId(configuredPriceId.trim());
        price.setBillingInterval(BillingInterval.MONTHLY);
        price.setCurrency("USD");
        price.setActive(true);
        priceRepository.save(price);
    }
}
