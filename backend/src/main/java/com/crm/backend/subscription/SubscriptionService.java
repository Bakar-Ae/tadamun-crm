package com.crm.backend.subscription;

import com.crm.backend.audit.AuditLogService;
import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.organization.Organization;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.subscription.dto.OrganizationSubscriptionResponse;
import com.crm.backend.subscription.dto.SubscriptionPlanResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionLifecyclePolicy lifecyclePolicy;
    private final SubscriptionTimeProvider timeProvider;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public SubscriptionService(
            SubscriptionPlanRepository planRepository,
            OrganizationSubscriptionRepository subscriptionRepository,
            SubscriptionMapper subscriptionMapper,
            SubscriptionLifecyclePolicy lifecyclePolicy,
            SubscriptionTimeProvider timeProvider,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.subscriptionMapper = subscriptionMapper;
        this.lifecyclePolicy = lifecyclePolicy;
        this.timeProvider = timeProvider;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public OrganizationSubscriptionResponse getCurrentSubscription() {
        Long organizationId = TenantContextHolder.getRequired()
                .organizationId();

        OrganizationSubscription subscription =
                findSubscription(organizationId);
        SubscriptionStatus effectiveStatus = lifecyclePolicy
                .resolveEffectiveStatus(subscription, timeProvider.now());

        return subscriptionMapper.toResponse(
                subscription,
                effectiveStatus,
                lifecyclePolicy.allowsAccess(effectiveStatus)
        );
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanResponse> getAvailablePlans() {
        return planRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(subscriptionMapper::toPlanResponse)
                .toList();
    }

    @Transactional
    public OrganizationSubscriptionResponse startTrialForOrganization(
            Organization organization,
            Long actorUserId
    ) {
        if (organization == null || organization.getId() == null) {
            throw new IllegalArgumentException(
                    "Organization is required to start a trial"
            );
        }

        return subscriptionRepository
                .findByOrganizationId(organization.getId())
                .map(this::toCurrentResponse)
                .orElseGet(() -> createTrial(organization, actorUserId));
    }

    @Transactional
    public OrganizationSubscriptionResponse activateSubscription(
            Long organizationId,
            SubscriptionPlanCode planCode,
            LocalDateTime periodStartsAt,
            LocalDateTime periodEndsAt,
            Long actorUserId
    ) {
        if (periodStartsAt == null
                || periodEndsAt == null
                || !periodEndsAt.isAfter(periodStartsAt)) {
            throw new IllegalArgumentException(
                    "Subscription period end must be after its start"
            );
        }

        OrganizationSubscription subscription =
                findSubscription(organizationId);
        SubscriptionPlan plan = findActivePlan(planCode);
        SubscriptionStatus previousStatus = subscription.getStatus();
        SubscriptionPlanCode previousPlan = subscription.getPlan().getCode();

        lifecyclePolicy.requireAllowedTransition(
                previousStatus,
                SubscriptionStatus.ACTIVE
        );

        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setCurrentPeriodStartsAt(periodStartsAt);
        subscription.setCurrentPeriodEndsAt(periodEndsAt);
        subscription.setGracePeriodEndsAt(null);
        subscription.setCancelAtPeriodEnd(false);
        subscription.setCanceledAt(null);

        OrganizationSubscription saved =
                subscriptionRepository.saveAndFlush(subscription);

        if (previousPlan != planCode) {
            logEvent(
                    saved,
                    actorUserId,
                    SubscriptionAuditAction.SUBSCRIPTION_PLAN_CHANGED,
                    Map.of(
                            "previousPlan", previousPlan.name(),
                            "plan", planCode.name()
                    )
            );
        }

        logEvent(
                saved,
                actorUserId,
                SubscriptionAuditAction.SUBSCRIPTION_ACTIVATED,
                Map.of(
                        "previousStatus", previousStatus.name(),
                        "plan", planCode.name(),
                        "periodEndsAt", periodEndsAt.toString()
                )
        );

        return toCurrentResponse(saved);
    }

    @Transactional
    public OrganizationSubscriptionResponse startGracePeriod(
            Long organizationId,
            Long actorUserId
    ) {
        OrganizationSubscription subscription =
                findSubscription(organizationId);

        lifecyclePolicy.requireAllowedTransition(
                subscription.getStatus(),
                SubscriptionStatus.GRACE_PERIOD
        );

        LocalDateTime startedAt = timeProvider.now();
        LocalDateTime endsAt = lifecyclePolicy.calculateGracePeriodEnd(
                subscription.getPlan(),
                startedAt
        );

        subscription.setStatus(SubscriptionStatus.GRACE_PERIOD);
        subscription.setGracePeriodEndsAt(endsAt);

        OrganizationSubscription saved =
                subscriptionRepository.saveAndFlush(subscription);

        logEvent(
                saved,
                actorUserId,
                SubscriptionAuditAction.SUBSCRIPTION_GRACE_PERIOD_STARTED,
                Map.of("gracePeriodEndsAt", endsAt.toString())
        );

        return toCurrentResponse(saved);
    }

    @Transactional
    public OrganizationSubscriptionResponse scheduleCancellation(
            Long organizationId,
            Long actorUserId
    ) {
        OrganizationSubscription subscription =
                findSubscription(organizationId);
        LocalDateTime now = timeProvider.now();

        if (subscription.getStatus() != SubscriptionStatus.ACTIVE
                || subscription.getCurrentPeriodEndsAt() == null
                || !subscription.getCurrentPeriodEndsAt().isAfter(now)) {
            throw new IllegalArgumentException(
                    "Only an active subscription with a future period end can be canceled later"
            );
        }

        if (!subscription.isCancelAtPeriodEnd()) {
            subscription.setCancelAtPeriodEnd(true);
            subscriptionRepository.saveAndFlush(subscription);

            logEvent(
                    subscription,
                    actorUserId,
                    SubscriptionAuditAction.SUBSCRIPTION_CANCELLATION_SCHEDULED,
                    Map.of(
                            "effectiveAt",
                            subscription.getCurrentPeriodEndsAt().toString()
                    )
            );
        }

        return toCurrentResponse(subscription);
    }

    @Transactional
    public OrganizationSubscriptionResponse cancelImmediately(
            Long organizationId,
            Long actorUserId
    ) {
        OrganizationSubscription subscription =
                findSubscription(organizationId);

        if (subscription.getStatus() == SubscriptionStatus.CANCELED) {
            return toCurrentResponse(subscription);
        }

        lifecyclePolicy.requireAllowedTransition(
                subscription.getStatus(),
                SubscriptionStatus.CANCELED
        );

        LocalDateTime canceledAt = timeProvider.now();
        subscription.setStatus(SubscriptionStatus.CANCELED);
        subscription.setCanceledAt(canceledAt);
        subscription.setCancelAtPeriodEnd(false);

        OrganizationSubscription saved =
                subscriptionRepository.saveAndFlush(subscription);

        logEvent(
                saved,
                actorUserId,
                SubscriptionAuditAction.SUBSCRIPTION_CANCELED,
                Map.of("canceledAt", canceledAt.toString())
        );

        return toCurrentResponse(saved);
    }

    @Transactional
    public OrganizationSubscriptionResponse expireSubscription(
            Long organizationId,
            Long actorUserId
    ) {
        OrganizationSubscription subscription =
                findSubscription(organizationId);

        if (subscription.getStatus() == SubscriptionStatus.EXPIRED) {
            return toCurrentResponse(subscription);
        }

        lifecyclePolicy.requireAllowedTransition(
                subscription.getStatus(),
                SubscriptionStatus.EXPIRED
        );
        subscription.setStatus(SubscriptionStatus.EXPIRED);

        OrganizationSubscription saved =
                subscriptionRepository.saveAndFlush(subscription);

        logEvent(
                saved,
                actorUserId,
                SubscriptionAuditAction.SUBSCRIPTION_EXPIRED,
                Map.of("expiredAt", timeProvider.now().toString())
        );

        return toCurrentResponse(saved);
    }

    private OrganizationSubscriptionResponse createTrial(
            Organization organization,
            Long actorUserId
    ) {
        SubscriptionPlan plan = findActivePlan(
                SubscriptionPlanCode.STARTER
        );
        LocalDateTime startedAt = timeProvider.now();

        OrganizationSubscription subscription =
                new OrganizationSubscription();
        subscription.setOrganization(organization);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.TRIALING);
        subscription.setStartedAt(startedAt);
        subscription.setTrialEndsAt(
                lifecyclePolicy.calculateTrialEnd(plan, startedAt)
        );
        subscription.setCancelAtPeriodEnd(false);

        OrganizationSubscription saved =
                subscriptionRepository.saveAndFlush(subscription);

        logEvent(
                saved,
                actorUserId,
                SubscriptionAuditAction.SUBSCRIPTION_TRIAL_STARTED,
                Map.of(
                        "plan", plan.getCode().name(),
                        "trialEndsAt", saved.getTrialEndsAt().toString()
                )
        );

        return toCurrentResponse(saved);
    }

    private OrganizationSubscription findSubscription(Long organizationId) {
        if (organizationId == null) {
            throw new IllegalArgumentException("Organization is required");
        }

        return subscriptionRepository.findByOrganizationId(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization subscription not found"
                ));
    }

    private SubscriptionPlan findActivePlan(SubscriptionPlanCode planCode) {
        if (planCode == null) {
            throw new IllegalArgumentException(
                    "Subscription plan is required"
            );
        }

        return planRepository.findByCodeAndActiveTrue(planCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Subscription plan is unavailable"
                ));
    }

    private OrganizationSubscriptionResponse toCurrentResponse(
            OrganizationSubscription subscription
    ) {
        SubscriptionStatus effectiveStatus = lifecyclePolicy
                .resolveEffectiveStatus(subscription, timeProvider.now());

        return subscriptionMapper.toResponse(
                subscription,
                effectiveStatus,
                lifecyclePolicy.allowsAccess(effectiveStatus)
        );
    }

    private void logEvent(
            OrganizationSubscription subscription,
            Long actorUserId,
            SubscriptionAuditAction action,
            Map<String, Object> eventDetails
    ) {
        LinkedHashMap<String, Object> details = new LinkedHashMap<>();
        details.put("organizationId", subscription.getOrganization().getId());
        details.put("subscriptionId", subscription.getId());
        details.putAll(eventDetails);

        auditLogService.logForOrganization(
                subscription.getOrganization(),
                actorUserId,
                action.name(),
                "ORGANIZATION_SUBSCRIPTION",
                subscription.getId(),
                writeDetails(details)
        );
    }

    private String writeDetails(Map<String, Object> details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JacksonException exception) {
            return "{}";
        }
    }
}
