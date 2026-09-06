package com.crm.backend.subscription.billing;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.subscription.SubscriptionAuditAction;
import com.crm.backend.subscription.SubscriptionPlanCode;
import com.crm.backend.subscription.SubscriptionService;
import com.crm.backend.subscription.SubscriptionStatus;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class BillingWebhookService {

    private static final BillingProviderName PROVIDER =
            BillingProviderName.STRIPE;

    private final BillingProviderRegistry providerRegistry;
    private final BillingWebhookEventRepository eventRepository;
    private final BillingCustomerRepository customerRepository;
    private final BillingPlanPriceRepository priceRepository;
    private final OrganizationRepository organizationRepository;
    private final SubscriptionService subscriptionService;
    private final SubscriptionTimeProvider timeProvider;
    private final AuditLogService auditLogService;

    public BillingWebhookService(
            BillingProviderRegistry providerRegistry,
            BillingWebhookEventRepository eventRepository,
            BillingCustomerRepository customerRepository,
            BillingPlanPriceRepository priceRepository,
            OrganizationRepository organizationRepository,
            SubscriptionService subscriptionService,
            SubscriptionTimeProvider timeProvider,
            AuditLogService auditLogService
    ) {
        this.providerRegistry = providerRegistry;
        this.eventRepository = eventRepository;
        this.customerRepository = customerRepository;
        this.priceRepository = priceRepository;
        this.organizationRepository = organizationRepository;
        this.subscriptionService = subscriptionService;
        this.timeProvider = timeProvider;
        this.auditLogService = auditLogService;
    }

    public BillingWebhookProcessingResult processStripeWebhook(
            String payload,
            String signature
    ) {
        BillingWebhookNotification notification = providerRegistry
                .require(PROVIDER)
                .verifyWebhook(payload, signature);
        BillingWebhookEvent event = eventRepository
                .findByProviderAndProviderEventId(
                        PROVIDER,
                        notification.providerEventId()
                )
                .orElseGet(BillingWebhookEvent::new);

        if (event.getId() != null
                && (event.getProcessingStatus()
                        == BillingWebhookProcessingStatus.PROCESSED
                || event.getProcessingStatus()
                        == BillingWebhookProcessingStatus.IGNORED)) {
            return new BillingWebhookProcessingResult(
                    true,
                    true,
                    event.getProcessingStatus()
            );
        }

        prepareEvent(event, notification, payload);

        try {
            if (notification.type() == BillingWebhookType.UNSUPPORTED) {
                event.setProcessingStatus(
                        BillingWebhookProcessingStatus.IGNORED
                );
                event.setProcessedAt(timeProvider.now());
                eventRepository.saveAndFlush(event);
                return new BillingWebhookProcessingResult(
                        true,
                        false,
                        BillingWebhookProcessingStatus.IGNORED
                );
            }

            Long organizationId = resolveOrganizationId(notification);
            Organization organization = organizationRepository
                    .findById(organizationId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Billing event organization was not found"
                    ));
            event.setOrganization(organization);
            synchronizeCustomer(notification, organization);
            synchronizeSubscription(notification, organizationId);

            event.setProcessingStatus(
                    BillingWebhookProcessingStatus.PROCESSED
            );
            event.setProcessedAt(timeProvider.now());
            event.setLastError(null);
            eventRepository.saveAndFlush(event);

            auditLogService.logForOrganization(
                    organization,
                    null,
                    SubscriptionAuditAction.BILLING_WEBHOOK_PROCESSED.name(),
                    "BILLING_WEBHOOK_EVENT",
                    event.getId(),
                    "{\"eventType\":\""
                            + safeJson(notification.providerEventType())
                            + "\"}"
            );

            return new BillingWebhookProcessingResult(
                    true,
                    false,
                    BillingWebhookProcessingStatus.PROCESSED
            );
        } catch (RuntimeException exception) {
            event.setProcessingStatus(BillingWebhookProcessingStatus.FAILED);
            event.setLastError(exception.getClass().getSimpleName());
            eventRepository.saveAndFlush(event);
            return new BillingWebhookProcessingResult(
                    true,
                    false,
                    BillingWebhookProcessingStatus.FAILED
            );
        }
    }

    private void prepareEvent(
            BillingWebhookEvent event,
            BillingWebhookNotification notification,
            String payload
    ) {
        event.setProvider(PROVIDER);
        event.setProviderEventId(notification.providerEventId());
        event.setEventType(notification.providerEventType());
        event.setProcessingStatus(BillingWebhookProcessingStatus.RECEIVED);
        event.setPayloadSha256(sha256(payload));
        event.setAttempts(event.getAttempts() + 1);
        eventRepository.saveAndFlush(event);
    }

    private Long resolveOrganizationId(
            BillingWebhookNotification notification
    ) {
        if (notification.organizationId() != null) {
            return notification.organizationId();
        }
        if (notification.providerCustomerId() != null) {
            return customerRepository
                    .findByProviderAndProviderCustomerId(
                            PROVIDER,
                            notification.providerCustomerId()
                    )
                    .map(customer -> customer.getOrganization().getId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Billing event is not linked to an organization"
                    ));
        }
        throw new IllegalArgumentException(
                "Billing event is missing organization metadata"
        );
    }

    private void synchronizeCustomer(
            BillingWebhookNotification notification,
            Organization organization
    ) {
        String providerCustomerId = notification.providerCustomerId();
        if (providerCustomerId == null || providerCustomerId.isBlank()) {
            return;
        }

        BillingCustomer customer = customerRepository
                .findByOrganizationIdAndProvider(
                        organization.getId(),
                        PROVIDER
                )
                .orElseGet(BillingCustomer::new);

        if (customer.getProviderCustomerId() != null
                && !customer.getProviderCustomerId()
                        .equals(providerCustomerId)) {
            throw new IllegalArgumentException(
                    "Organization billing customer reference does not match"
            );
        }

        customer.setOrganization(organization);
        customer.setProvider(PROVIDER);
        customer.setProviderCustomerId(providerCustomerId);
        customerRepository.saveAndFlush(customer);
    }

    private void synchronizeSubscription(
            BillingWebhookNotification notification,
            Long organizationId
    ) {
        String providerSubscriptionId =
                notification.providerSubscriptionId();
        if (providerSubscriptionId == null
                || providerSubscriptionId.isBlank()) {
            throw new IllegalArgumentException(
                    "Billing event is missing subscription reference"
            );
        }

        if (notification.type() == BillingWebhookType.CHECKOUT_COMPLETED) {
            subscriptionService.linkProviderSubscription(
                    organizationId,
                    PROVIDER,
                    providerSubscriptionId
            );
            return;
        }

        SubscriptionPlanCode planCode = resolvePlanCode(notification);
        SubscriptionStatus status = subscriptionStatus(notification);
        subscriptionService.synchronizeProviderSubscription(
                organizationId,
                PROVIDER,
                providerSubscriptionId,
                planCode,
                status,
                notification.occurredAt(),
                notification.periodStartsAt(),
                notification.periodEndsAt(),
                notification.cancelAtPeriodEnd()
        );
    }

    private SubscriptionPlanCode resolvePlanCode(
            BillingWebhookNotification notification
    ) {
        if (notification.providerPriceId() != null) {
            return priceRepository
                    .findByProviderAndProviderPriceId(
                            PROVIDER,
                            notification.providerPriceId()
                    )
                    .map(price -> price.getPlan().getCode())
                    .orElseGet(notification::planCode);
        }
        return notification.planCode();
    }

    private SubscriptionStatus subscriptionStatus(
            BillingWebhookNotification notification
    ) {
        if (notification.type() == BillingWebhookType.SUBSCRIPTION_DELETED) {
            return SubscriptionStatus.CANCELED;
        }

        return switch (notification.subscriptionStatus()) {
            case ACTIVE -> SubscriptionStatus.ACTIVE;
            case TRIALING -> SubscriptionStatus.TRIALING;
            case PAST_DUE, INCOMPLETE, PAUSED ->
                    SubscriptionStatus.GRACE_PERIOD;
            case CANCELED -> SubscriptionStatus.CANCELED;
            case INCOMPLETE_EXPIRED, UNPAID -> SubscriptionStatus.EXPIRED;
            case UNKNOWN -> throw new IllegalArgumentException(
                    "Billing event contains an unsupported subscription status"
            );
        };
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    digest.digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    private String safeJson(String value) {
        return value == null
                ? ""
                : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
