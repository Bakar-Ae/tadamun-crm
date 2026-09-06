package com.crm.backend.subscription.billing.stripe;

import com.crm.backend.subscription.billing.BillingCheckoutRequest;
import com.crm.backend.subscription.billing.BillingCustomerReference;
import com.crm.backend.subscription.billing.BillingCustomerRequest;
import com.crm.backend.subscription.billing.BillingPortalRequest;
import com.crm.backend.subscription.billing.BillingProvider;
import com.crm.backend.subscription.billing.BillingProviderException;
import com.crm.backend.subscription.billing.BillingProviderName;
import com.crm.backend.subscription.billing.BillingProviderSubscriptionStatus;
import com.crm.backend.subscription.billing.BillingSession;
import com.crm.backend.subscription.billing.BillingUnavailableException;
import com.crm.backend.subscription.billing.BillingWebhookNotification;
import com.crm.backend.subscription.billing.BillingWebhookType;
import com.crm.backend.subscription.billing.InvalidBillingWebhookException;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@Component
@ConditionalOnProperty(
        prefix = "app.billing.stripe",
        name = "enabled",
        havingValue = "true"
)
public class StripeBillingProvider implements BillingProvider {

    private static final String ORGANIZATION_ID = "organizationId";
    private static final String PLAN_CODE = "planCode";

    private final StripeClient stripeClient;
    private final StripeBillingProperties properties;

    public StripeBillingProvider(
            StripeClient stripeClient,
            StripeBillingProperties properties
    ) {
        this.stripeClient = stripeClient;
        this.properties = properties;
    }

    @Override
    public BillingProviderName name() {
        return BillingProviderName.STRIPE;
    }

    @Override
    public BillingCustomerReference createCustomer(
            BillingCustomerRequest request
    ) {
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setName(request.organizationName())
                .setEmail(request.customerEmail())
                .putMetadata(
                        ORGANIZATION_ID,
                        request.organizationId().toString()
                )
                .build();

        try {
            Customer customer = stripeClient.customers().create(
                    params,
                    requestOptions(request.idempotencyKey())
            );
            return new BillingCustomerReference(customer.getId());
        } catch (StripeException exception) {
            throw providerFailure(exception);
        }
    }

    @Override
    public BillingSession createCheckoutSession(
            BillingCheckoutRequest request
    ) {
        SessionCreateParams.SubscriptionData subscriptionData =
                SessionCreateParams.SubscriptionData.builder()
                        .putMetadata(
                                ORGANIZATION_ID,
                                request.organizationId().toString()
                        )
                        .putMetadata(PLAN_CODE, request.planCode().name())
                        .build();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setCustomer(request.providerCustomerId())
                .setClientReferenceId(request.organizationId().toString())
                .setSuccessUrl(request.successUrl().toString())
                .setCancelUrl(request.cancelUrl().toString())
                .addLineItem(
                        SessionCreateParams.LineItem.builder()
                                .setPrice(request.providerPriceId())
                                .setQuantity(1L)
                                .build()
                )
                .putMetadata(
                        ORGANIZATION_ID,
                        request.organizationId().toString()
                )
                .putMetadata(PLAN_CODE, request.planCode().name())
                .setSubscriptionData(subscriptionData)
                .build();

        try {
            Session session = stripeClient.checkout().sessions().create(
                    params,
                    requestOptions(request.idempotencyKey())
            );
            return session(session.getId(), session.getUrl());
        } catch (StripeException exception) {
            throw providerFailure(exception);
        }
    }

    @Override
    public BillingSession createPortalSession(BillingPortalRequest request) {
        com.stripe.param.billingportal.SessionCreateParams params =
                com.stripe.param.billingportal.SessionCreateParams.builder()
                        .setCustomer(request.providerCustomerId())
                        .setReturnUrl(request.returnUrl().toString())
                        .build();

        try {
            com.stripe.model.billingportal.Session session = stripeClient
                    .billingPortal()
                    .sessions()
                    .create(params);
            return session(session.getId(), session.getUrl());
        } catch (StripeException exception) {
            throw providerFailure(exception);
        }
    }

    @Override
    public BillingWebhookNotification verifyWebhook(
            String payload,
            String signature
    ) {
        if (properties.webhookSecret() == null
                || properties.webhookSecret().isBlank()) {
            throw new BillingUnavailableException(
                    "Billing webhook is not configured"
            );
        }
        if (payload == null || signature == null || signature.isBlank()) {
            throw new InvalidBillingWebhookException(
                    "Invalid billing webhook",
                    null
            );
        }

        try {
            Event event = stripeClient.constructEvent(
                    payload,
                    signature,
                    properties.webhookSecret()
            );
            return normalize(event);
        } catch (SignatureVerificationException exception) {
            throw new InvalidBillingWebhookException(
                    "Invalid billing webhook signature",
                    exception
            );
        }
    }

    private BillingWebhookNotification normalize(Event event) {
        BillingWebhookType type = webhookType(event.getType());
        LocalDateTime occurredAt = utc(event.getCreated());

        if (type == BillingWebhookType.UNSUPPORTED) {
            return new BillingWebhookNotification(
                    event.getId(),
                    event.getType(),
                    type,
                    null,
                    null,
                    null,
                    null,
                    null,
                    BillingProviderSubscriptionStatus.UNKNOWN,
                    occurredAt,
                    null,
                    null,
                    false
            );
        }

        StripeObject object = event.getDataObjectDeserializer()
                .getObject()
                .orElseThrow(() -> new BillingProviderException(
                        "Billing provider event could not be read",
                        null
                ));

        if (type == BillingWebhookType.CHECKOUT_COMPLETED
                && object instanceof Session session) {
            return checkoutNotification(event, session, occurredAt);
        }

        if (object instanceof Subscription subscription) {
            return subscriptionNotification(
                    event,
                    type,
                    subscription,
                    occurredAt
            );
        }

        throw new BillingProviderException(
                "Billing provider returned an unexpected event object",
                null
        );
    }

    private BillingWebhookNotification checkoutNotification(
            Event event,
            Session session,
            LocalDateTime occurredAt
    ) {
        Map<String, String> metadata = session.getMetadata();
        Long organizationId = parseOrganizationId(
                firstNonBlank(
                        session.getClientReferenceId(),
                        value(metadata, ORGANIZATION_ID)
                )
        );

        return new BillingWebhookNotification(
                event.getId(),
                event.getType(),
                BillingWebhookType.CHECKOUT_COMPLETED,
                organizationId,
                session.getCustomer(),
                session.getSubscription(),
                null,
                parsePlanCode(value(metadata, PLAN_CODE)),
                BillingProviderSubscriptionStatus.UNKNOWN,
                occurredAt,
                null,
                null,
                false
        );
    }

    private BillingWebhookNotification subscriptionNotification(
            Event event,
            BillingWebhookType type,
            Subscription subscription,
            LocalDateTime occurredAt
    ) {
        SubscriptionItem item = firstSubscriptionItem(subscription);
        String priceId = item == null || item.getPrice() == null
                ? null
                : item.getPrice().getId();

        return new BillingWebhookNotification(
                event.getId(),
                event.getType(),
                type,
                parseOrganizationId(value(
                        subscription.getMetadata(),
                        ORGANIZATION_ID
                )),
                subscription.getCustomer(),
                subscription.getId(),
                priceId,
                parsePlanCode(value(
                        subscription.getMetadata(),
                        PLAN_CODE
                )),
                subscriptionStatus(subscription.getStatus()),
                occurredAt,
                item == null ? null : utc(item.getCurrentPeriodStart()),
                item == null ? null : utc(item.getCurrentPeriodEnd()),
                Boolean.TRUE.equals(subscription.getCancelAtPeriodEnd())
        );
    }

    private SubscriptionItem firstSubscriptionItem(
            Subscription subscription
    ) {
        if (subscription.getItems() == null
                || subscription.getItems().getData() == null
                || subscription.getItems().getData().isEmpty()) {
            return null;
        }
        return subscription.getItems().getData().getFirst();
    }

    private BillingWebhookType webhookType(String eventType) {
        return switch (eventType) {
            case "checkout.session.completed" ->
                    BillingWebhookType.CHECKOUT_COMPLETED;
            case "customer.subscription.created",
                 "customer.subscription.updated" ->
                    BillingWebhookType.SUBSCRIPTION_CHANGED;
            case "customer.subscription.deleted" ->
                    BillingWebhookType.SUBSCRIPTION_DELETED;
            default -> BillingWebhookType.UNSUPPORTED;
        };
    }

    private BillingProviderSubscriptionStatus subscriptionStatus(
            String status
    ) {
        if (status == null) {
            return BillingProviderSubscriptionStatus.UNKNOWN;
        }

        return switch (status) {
            case "trialing" -> BillingProviderSubscriptionStatus.TRIALING;
            case "active" -> BillingProviderSubscriptionStatus.ACTIVE;
            case "past_due" -> BillingProviderSubscriptionStatus.PAST_DUE;
            case "canceled" -> BillingProviderSubscriptionStatus.CANCELED;
            case "incomplete" -> BillingProviderSubscriptionStatus.INCOMPLETE;
            case "incomplete_expired" ->
                    BillingProviderSubscriptionStatus.INCOMPLETE_EXPIRED;
            case "unpaid" -> BillingProviderSubscriptionStatus.UNPAID;
            case "paused" -> BillingProviderSubscriptionStatus.PAUSED;
            default -> BillingProviderSubscriptionStatus.UNKNOWN;
        };
    }

    private Long parseOrganizationId(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BillingProviderException(
                    "Billing event has invalid organization metadata",
                    exception
            );
        }
    }

    private com.crm.backend.subscription.SubscriptionPlanCode parsePlanCode(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return com.crm.backend.subscription.SubscriptionPlanCode
                    .valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new BillingProviderException(
                    "Billing event has invalid plan metadata",
                    exception
            );
        }
    }

    private String value(Map<String, String> metadata, String key) {
        return metadata == null ? null : metadata.get(key);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private LocalDateTime utc(Long epochSeconds) {
        return epochSeconds == null
                ? null
                : LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(epochSeconds),
                        ZoneOffset.UTC
                );
    }

    private RequestOptions requestOptions(String idempotencyKey) {
        return RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();
    }

    private BillingSession session(String id, String url) {
        if (id == null || url == null || url.isBlank()) {
            throw new BillingProviderException(
                    "Billing provider returned an invalid session",
                    null
            );
        }
        return new BillingSession(id, URI.create(url));
    }

    private BillingProviderException providerFailure(
            StripeException exception
    ) {
        return new BillingProviderException(
                "Billing provider request failed",
                exception
        );
    }
}
