package com.crm.backend.subscription.billing;

import java.net.URI;

public record BillingSession(
        String providerSessionId,
        URI redirectUrl
) {
}