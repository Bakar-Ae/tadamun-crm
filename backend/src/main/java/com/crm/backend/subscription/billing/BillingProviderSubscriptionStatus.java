package com.crm.backend.subscription.billing;

public enum BillingProviderSubscriptionStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELED,
    INCOMPLETE,
    INCOMPLETE_EXPIRED,
    UNPAID,
    PAUSED,
    UNKNOWN
}
