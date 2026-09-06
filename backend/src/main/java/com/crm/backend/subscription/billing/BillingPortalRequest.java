package com.crm.backend.subscription.billing;

import java.net.URI;

public record BillingPortalRequest(
        Long organizationId,
        String providerCustomerId,
        URI returnUrl
) {
}