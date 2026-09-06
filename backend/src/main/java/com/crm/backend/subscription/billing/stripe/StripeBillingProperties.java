package com.crm.backend.subscription.billing.stripe;

import com.crm.backend.subscription.SubscriptionPlanCode;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.billing.stripe")
public record StripeBillingProperties(
        boolean enabled,
        String secretKey,
        String webhookSecret,
        String starterPriceId,
        String professionalPriceId,
        String businessPriceId,
        String enterprisePriceId
) {

    public String priceIdFor(SubscriptionPlanCode planCode) {
        if (planCode == null) {
            return null;
        }

        return switch (planCode) {
            case STARTER -> starterPriceId;
            case PROFESSIONAL -> professionalPriceId;
            case BUSINESS -> businessPriceId;
            case ENTERPRISE -> enterprisePriceId;
        };
    }
}
