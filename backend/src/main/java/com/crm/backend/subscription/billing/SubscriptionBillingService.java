package com.crm.backend.subscription.billing;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.subscription.OrganizationSubscription;
import com.crm.backend.subscription.OrganizationSubscriptionRepository;
import com.crm.backend.subscription.SubscriptionAuditAction;
import com.crm.backend.subscription.billing.dto.BillingSessionResponse;
import com.crm.backend.subscription.billing.dto.CreateBillingCheckoutRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.util.UUID;

@Service
public class SubscriptionBillingService {

    private static final BillingProviderName PROVIDER =
            BillingProviderName.STRIPE;

    private final BillingProviderRegistry providerRegistry;
    private final BillingCustomerRepository customerRepository;
    private final BillingPlanPriceRepository priceRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditLogService auditLogService;
    private final String frontendBaseUrl;

    public SubscriptionBillingService(
            BillingProviderRegistry providerRegistry,
            BillingCustomerRepository customerRepository,
            BillingPlanPriceRepository priceRepository,
            OrganizationSubscriptionRepository subscriptionRepository,
            OrganizationRepository organizationRepository,
            AuditLogService auditLogService,
            @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.providerRegistry = providerRegistry;
        this.customerRepository = customerRepository;
        this.priceRepository = priceRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.organizationRepository = organizationRepository;
        this.auditLogService = auditLogService;
        this.frontendBaseUrl = normalizeBaseUrl(frontendBaseUrl);
    }

    public BillingSessionResponse createCheckout(
            CreateBillingCheckoutRequest request,
            String customerEmail,
            Long actorUserId,
            String requestedIdempotencyKey
    ) {
        Long organizationId = TenantContextHolder.getRequired()
                .organizationId();
        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found"
                ));
        OrganizationSubscription subscription = subscriptionRepository
                .findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization subscription not found"
                ));

        if (subscription.getProviderSubscriptionId() != null
                && !subscription.getProviderSubscriptionId().isBlank()) {
            throw new IllegalArgumentException(
                    "Manage the existing subscription through the billing portal"
            );
        }

        BillingProvider provider = providerRegistry.require(PROVIDER);
        BillingPlanPrice price = priceRepository
                .findByPlan_CodeAndProviderAndBillingIntervalAndActiveTrue(
                        request.planCode(),
                        PROVIDER,
                        request.billingInterval()
                )
                .orElseThrow(() -> new BillingUnavailableException(
                        "The selected billing plan is unavailable"
                ));
        BillingCustomer customer = getOrCreateCustomer(
                provider,
                organization,
                customerEmail,
                actorUserId
        );
        String idempotencyKey = checkoutIdempotencyKey(
                organizationId,
                requestedIdempotencyKey
        );

        BillingSession session = provider.createCheckoutSession(
                new BillingCheckoutRequest(
                        organizationId,
                        organization.getName(),
                        customerEmail,
                        request.planCode(),
                        customer.getProviderCustomerId(),
                        price.getProviderPriceId(),
                        appUri("/organization?tab=billing&checkout=success"),
                        appUri("/organization?tab=billing&checkout=canceled"),
                        idempotencyKey
                )
        );

        auditLogService.logForOrganization(
                organization,
                actorUserId,
                SubscriptionAuditAction.BILLING_CHECKOUT_CREATED.name(),
                "ORGANIZATION_SUBSCRIPTION",
                subscription.getId(),
                "{\"plan\":\"" + request.planCode().name()
                        + "\",\"interval\":\""
                        + request.billingInterval().name() + "\"}"
        );

        return response(session);
    }

    public BillingSessionResponse createPortal(Long actorUserId) {
        Long organizationId = TenantContextHolder.getRequired()
                .organizationId();
        Organization organization = organizationRepository
                .findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found"
                ));
        BillingCustomer customer = customerRepository
                .findByOrganizationIdAndProvider(organizationId, PROVIDER)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Billing account not found"
                ));
        BillingProvider provider = providerRegistry.require(PROVIDER);
        BillingSession session = provider.createPortalSession(
                new BillingPortalRequest(
                        organizationId,
                        customer.getProviderCustomerId(),
                        appUri("/organization?tab=billing")
                )
        );

        auditLogService.logForOrganization(
                organization,
                actorUserId,
                SubscriptionAuditAction.BILLING_PORTAL_OPENED.name(),
                "ORGANIZATION",
                organizationId,
                "{}"
        );

        return response(session);
    }

    private BillingCustomer getOrCreateCustomer(
            BillingProvider provider,
            Organization organization,
            String customerEmail,
            Long actorUserId
    ) {
        return customerRepository
                .findByOrganizationIdAndProvider(
                        organization.getId(),
                        PROVIDER
                )
                .orElseGet(() -> createCustomer(
                        provider,
                        organization,
                        customerEmail,
                        actorUserId
                ));
    }

    private BillingCustomer createCustomer(
            BillingProvider provider,
            Organization organization,
            String customerEmail,
            Long actorUserId
    ) {
        BillingCustomerReference reference = provider.createCustomer(
                new BillingCustomerRequest(
                        organization.getId(),
                        organization.getName(),
                        customerEmail,
                        "billing-customer-" + organization.getId()
                )
        );

        BillingCustomer customer = new BillingCustomer();
        customer.setOrganization(organization);
        customer.setProvider(PROVIDER);
        customer.setProviderCustomerId(reference.providerCustomerId());

        try {
            BillingCustomer saved = customerRepository.saveAndFlush(customer);
            auditLogService.logForOrganization(
                    organization,
                    actorUserId,
                    SubscriptionAuditAction.BILLING_CUSTOMER_CREATED.name(),
                    "BILLING_CUSTOMER",
                    saved.getId(),
                    "{\"provider\":\"STRIPE\"}"
            );
            return saved;
        } catch (DataIntegrityViolationException exception) {
            return customerRepository
                    .findByOrganizationIdAndProvider(
                            organization.getId(),
                            PROVIDER
                    )
                    .orElseThrow(() -> exception);
        }
    }

    private BillingSessionResponse response(BillingSession session) {
        return new BillingSessionResponse(
                session.providerSessionId(),
                session.redirectUrl().toString()
        );
    }

    private String checkoutIdempotencyKey(
            Long organizationId,
            String requestedKey
    ) {
        String key = requestedKey == null || requestedKey.isBlank()
                ? UUID.randomUUID().toString()
                : requestedKey.trim();

        if (key.length() > 200
                || !key.matches("[A-Za-z0-9._:-]+")) {
            throw new IllegalArgumentException(
                    "Idempotency-Key contains an invalid value"
            );
        }

        return "checkout-" + organizationId + "-" + key;
    }

    private URI appUri(String path) {
        return URI.create(frontendBaseUrl + path);
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "APP_FRONTEND_BASE_URL is required"
            );
        }

        String normalized = value.trim().replaceAll("/+$", "");
        URI uri = URI.create(normalized);
        if (!"http".equalsIgnoreCase(uri.getScheme())
                && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new IllegalStateException(
                    "APP_FRONTEND_BASE_URL must use HTTP or HTTPS"
            );
        }
        return normalized;
    }
}
