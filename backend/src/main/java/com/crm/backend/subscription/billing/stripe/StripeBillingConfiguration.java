package com.crm.backend.subscription.billing.stripe;

import com.stripe.StripeClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class StripeBillingConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "app.billing.stripe",
            name = "enabled",
            havingValue = "true"
    )
    StripeClient stripeClient(StripeBillingProperties properties) {
        if (properties.secretKey() == null
                || properties.secretKey().isBlank()) {
            throw new IllegalStateException(
                    "STRIPE_SECRET_KEY is required when Stripe billing is enabled"
            );
        }
        if (properties.webhookSecret() == null
                || properties.webhookSecret().isBlank()) {
            throw new IllegalStateException(
                    "STRIPE_WEBHOOK_SECRET is required when Stripe billing is enabled"
            );
        }

        return new StripeClient(properties.secretKey());
    }
}
