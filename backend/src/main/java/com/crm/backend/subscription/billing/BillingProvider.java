package com.crm.backend.subscription.billing;

public interface BillingProvider {

    BillingProviderName name();

    BillingCustomerReference createCustomer(
            BillingCustomerRequest request
    );

    BillingSession createCheckoutSession(
            BillingCheckoutRequest request
    );

    BillingSession createPortalSession(
            BillingPortalRequest request
    );

    BillingWebhookNotification verifyWebhook(
            String payload,
            String signature
    );
}
