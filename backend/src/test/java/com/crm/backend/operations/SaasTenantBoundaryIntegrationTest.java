package com.crm.backend.operations;

import com.crm.backend.email.EmailService;
import com.crm.backend.integration.IntegrationConnection;
import com.crm.backend.integration.IntegrationConnectionRepository;
import com.crm.backend.integration.IntegrationProvider;
import com.crm.backend.integration.delivery.IntegrationDelivery;
import com.crm.backend.integration.delivery.IntegrationDeliveryRepository;
import com.crm.backend.integration.delivery.IntegrationDeliveryStatus;
import com.crm.backend.integration.provider.IntegrationDeliveryType;
import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationService;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.organization.dto.CreateOrganizationRequest;
import com.crm.backend.organization.dto.OrganizationResponse;
import com.crm.backend.organization.invitation.OrganizationInvitationRepository;
import com.crm.backend.organization.invitation.OrganizationInvitationService;
import com.crm.backend.organization.invitation.OrganizationInvitationStatus;
import com.crm.backend.organization.invitation.dto.AcceptOrganizationInvitationRequest;
import com.crm.backend.organization.invitation.dto.CreateOrganizationInvitationRequest;
import com.crm.backend.organization.invitation.dto.OrganizationInvitationAcceptanceResponse;
import com.crm.backend.organization.invitation.dto.OrganizationInvitationResponse;
import com.crm.backend.organization.membership.OrganizationMembership;
import com.crm.backend.organization.membership.OrganizationMembershipRepository;
import com.crm.backend.organization.membership.OrganizationMembershipService;
import com.crm.backend.organization.membership.OrganizationMembershipStatus;
import com.crm.backend.organization.membership.dto.CreateOrganizationMembershipRequest;
import com.crm.backend.organization.membership.dto.DeactivateOrganizationMembershipRequest;
import com.crm.backend.organization.membership.dto.OrganizationMembershipResponse;
import com.crm.backend.organization.workspace.WorkspaceService;
import com.crm.backend.publicapi.key.PublicApiKey;
import com.crm.backend.publicapi.key.PublicApiKeyRepository;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.security.tenant.TenantContext;
import com.crm.backend.security.tenant.TenantContextHolder;
import com.crm.backend.security.tenant.TenantPermissionPolicy;
import com.crm.backend.subscription.OrganizationSubscription;
import com.crm.backend.subscription.OrganizationSubscriptionRepository;
import com.crm.backend.subscription.SubscriptionPlan;
import com.crm.backend.subscription.SubscriptionPlanCode;
import com.crm.backend.subscription.SubscriptionPlanRepository;
import com.crm.backend.subscription.SubscriptionStatus;
import com.crm.backend.subscription.billing.BillingCustomer;
import com.crm.backend.subscription.billing.BillingCustomerRepository;
import com.crm.backend.subscription.billing.BillingProviderName;
import com.crm.backend.support.MySqlTestContainerConfiguration;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import com.crm.backend.webhook.EncryptedWebhookSecret;
import com.crm.backend.webhook.GeneratedWebhookSecret;
import com.crm.backend.webhook.WebhookDelivery;
import com.crm.backend.webhook.WebhookDeliveryRepository;
import com.crm.backend.webhook.WebhookEvent;
import com.crm.backend.webhook.WebhookEventRepository;
import com.crm.backend.webhook.WebhookEventType;
import com.crm.backend.webhook.WebhookSecretEncryptionService;
import com.crm.backend.webhook.WebhookSubscription;
import com.crm.backend.webhook.WebhookSubscriptionRepository;
import com.crm.backend.workflow.WorkflowDefinition;
import com.crm.backend.workflow.WorkflowDefinitionRepository;
import com.crm.backend.workflow.WorkflowExecution;
import com.crm.backend.workflow.WorkflowExecutionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class SaasTenantBoundaryIntegrationTest {

    private static final LocalDateTime NOW = LocalDateTime.of(
            2026, 9, 11, 10, 0
    );

    @MockitoBean
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationSubscriptionRepository subscriptionRepository;

    @Autowired
    private SubscriptionPlanRepository planRepository;

    @Autowired
    private BillingCustomerRepository billingCustomerRepository;

    @Autowired
    private PublicApiKeyRepository apiKeyRepository;

    @Autowired
    private WebhookSecretEncryptionService webhookEncryptionService;

    @Autowired
    private WebhookSubscriptionRepository webhookSubscriptionRepository;

    @Autowired
    private WebhookEventRepository webhookEventRepository;

    @Autowired
    private WebhookDeliveryRepository webhookDeliveryRepository;

    @Autowired
    private WorkflowDefinitionRepository workflowRepository;

    @Autowired
    private WorkflowExecutionRepository workflowExecutionRepository;

    @Autowired
    private IntegrationConnectionRepository connectionRepository;

    @Autowired
    private IntegrationDeliveryRepository integrationDeliveryRepository;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private OrganizationMembershipService membershipService;

    @Autowired
    private OrganizationMembershipRepository membershipRepository;

    @Autowired
    private OrganizationInvitationService invitationService;

    @Autowired
    private OrganizationInvitationRepository invitationRepository;

    @Autowired
    private WorkspaceService workspaceService;

    @Autowired
    private TenantPermissionPolicy tenantPermissionPolicy;

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void highRiskSaasRepositoriesShouldNotCrossTenantBoundary() {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow();
        User actor = createUser(
                "Phase 89 Boundary Owner",
                "phase89.boundary.owner@crm.test",
                adminRole
        );
        Organization organizationA = createOrganization(
                "Phase 89 Boundary A",
                "phase-89-boundary-a",
                actor
        );
        Organization organizationB = createOrganization(
                "Phase 89 Boundary B",
                "phase-89-boundary-b",
                actor
        );

        createSubscription(organizationA);
        createSubscription(organizationB);
        BillingCustomer billingA = createBillingCustomer(
                organizationA,
                "cus_phase89_a"
        );
        createBillingCustomer(organizationB, "cus_phase89_b");
        PublicApiKey apiKeyA = createApiKey(
                organizationA,
                actor,
                "phase89_api_key_a"
        );
        PublicApiKey apiKeyB = createApiKey(
                organizationB,
                actor,
                "phase89_api_key_b"
        );
        WebhookGraph webhookA = createWebhookGraph(
                organizationA,
                actor,
                "a"
        );
        WebhookGraph webhookB = createWebhookGraph(
                organizationB,
                actor,
                "b"
        );
        WorkflowGraph workflowA = createWorkflowGraph(
                organizationA,
                actor,
                webhookA.event(),
                "a"
        );
        WorkflowGraph workflowB = createWorkflowGraph(
                organizationB,
                actor,
                webhookB.event(),
                "b"
        );
        IntegrationGraph integrationA = createIntegrationGraph(
                organizationA,
                actor,
                "a"
        );
        IntegrationGraph integrationB = createIntegrationGraph(
                organizationB,
                actor,
                "b"
        );

        assertEquals(
                organizationA.getId(),
                subscriptionRepository.findByOrganizationId(
                        organizationA.getId()
                ).orElseThrow().getOrganization().getId()
        );
        assertEquals(
                billingA.getId(),
                billingCustomerRepository
                        .findByOrganizationIdAndProvider(
                                organizationA.getId(),
                                BillingProviderName.STRIPE
                        ).orElseThrow().getId()
        );
        assertEquals(
                List.of(apiKeyA.getId()),
                apiKeyRepository.findByOrganizationId(
                        organizationA.getId(),
                        PageRequest.of(0, 10)
                ).map(PublicApiKey::getId).getContent()
        );
        assertTrue(apiKeyRepository.findByIdAndOrganizationId(
                apiKeyA.getId(),
                organizationB.getId()
        ).isEmpty());
        assertFalse(apiKeyRepository.findByIdAndOrganizationId(
                apiKeyB.getId(),
                organizationB.getId()
        ).isEmpty());

        assertEquals(
                List.of(webhookA.subscription().getId()),
                webhookSubscriptionRepository.findByOrganizationId(
                        organizationA.getId(),
                        PageRequest.of(0, 10)
                ).map(WebhookSubscription::getId).getContent()
        );
        assertTrue(webhookSubscriptionRepository
                .findByIdAndOrganizationId(
                        webhookA.subscription().getId(),
                        organizationB.getId()
                ).isEmpty());
        assertTrue(webhookDeliveryRepository
                .findByPublicDeliveryIdAndOrganizationIdAndSubscriptionId(
                        webhookA.delivery().getPublicDeliveryId(),
                        organizationB.getId(),
                        webhookA.subscription().getId()
                ).isEmpty());

        assertEquals(
                List.of(workflowA.workflow().getId()),
                workflowRepository.findByOrganizationId(
                        organizationA.getId(),
                        PageRequest.of(0, 10)
                ).map(WorkflowDefinition::getId).getContent()
        );
        assertTrue(workflowRepository.findByIdAndOrganizationId(
                workflowA.workflow().getId(),
                organizationB.getId()
        ).isEmpty());
        assertTrue(workflowExecutionRepository
                .findByOrganizationIdAndPublicExecutionId(
                        organizationB.getId(),
                        workflowA.execution().getPublicExecutionId()
                ).isEmpty());
        assertFalse(workflowExecutionRepository
                .findByOrganizationIdAndPublicExecutionId(
                        organizationB.getId(),
                        workflowB.execution().getPublicExecutionId()
                ).isEmpty());

        assertEquals(
                List.of(integrationA.connection().getId()),
                connectionRepository.findByOrganizationId(
                        organizationA.getId(),
                        PageRequest.of(0, 10)
                ).map(IntegrationConnection::getId).getContent()
        );
        assertTrue(connectionRepository
                .findByPublicConnectionIdAndOrganizationId(
                        integrationA.connection().getPublicConnectionId(),
                        organizationB.getId()
                ).isEmpty());
        assertTrue(integrationDeliveryRepository
                .findByPublicDeliveryIdAndOrganizationId(
                        integrationA.delivery().getPublicDeliveryId(),
                        organizationB.getId()
                ).isEmpty());
        assertEquals(
                0,
                integrationDeliveryRepository
                        .findByOrganizationIdAndConnectionIdOrderByCreatedAtDesc(
                                organizationB.getId(),
                                integrationA.connection().getId(),
                                PageRequest.of(0, 10)
                        ).getTotalElements()
        );
        assertFalse(integrationDeliveryRepository
                .findByPublicDeliveryIdAndOrganizationId(
                        integrationB.delivery().getPublicDeliveryId(),
                        organizationB.getId()
                ).isEmpty());
    }

    @Test
    void organizationLifecycleShouldCreateInviteActivateAndDeactivateWorkspace() {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseThrow();
        Role ownerRole = roleRepository.findByName(RoleName.OWNER)
                .orElseThrow();
        Role salesRole = roleRepository.findByName(RoleName.SALES_REP)
                .orElseThrow();
        User owner = createUser(
                "Phase 89 Lifecycle Owner",
                "phase89.lifecycle.owner@crm.test",
                adminRole
        );
        User invitee = createUser(
                "Phase 89 Lifecycle Member",
                "phase89.lifecycle.member@crm.test",
                salesRole
        );

        OrganizationResponse organization =
                organizationService.createOrganization(
                        new CreateOrganizationRequest(
                                "Phase 89 Lifecycle Organization",
                                "phase-89-lifecycle",
                                "Africa/Mogadishu"
                        ),
                        owner.getId()
                );
        OrganizationMembershipResponse ownerMembership =
                membershipService.createMembership(
                        organization.id(),
                        new CreateOrganizationMembershipRequest(
                                owner.getId(),
                                RoleName.OWNER
                        ),
                        owner.getId()
                );
        TenantContextHolder.set(new TenantContext(
                organization.id(),
                ownerMembership.id(),
                owner.getId(),
                RoleName.OWNER,
                ownerRole.getDataScope(),
                null,
                tenantPermissionPolicy.resolvePermissions(ownerRole)
        ));

        OrganizationInvitationResponse invitation =
                invitationService.createInvitation(
                        new CreateOrganizationInvitationRequest(
                                invitee.getEmail(),
                                RoleName.SALES_REP
                        )
                );
        ArgumentCaptor<String> linkCaptor =
                ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOrganizationInvitationEmail(
                eq(invitee.getEmail()),
                eq(organization.name()),
                eq(owner.getFullName()),
                anyString(),
                linkCaptor.capture(),
                any(LocalDateTime.class)
        );

        OrganizationInvitationAcceptanceResponse accepted =
                invitationService.acceptInvitation(
                        new AcceptOrganizationInvitationRequest(
                                extractToken(linkCaptor.getValue()),
                                null,
                                null,
                                null
                        )
                );
        OrganizationMembership member = membershipRepository
                .findByOrganizationIdAndUserId(
                        organization.id(),
                        invitee.getId()
                ).orElseThrow();

        assertEquals(organization.id(), accepted.organizationId());
        assertEquals(RoleName.SALES_REP, accepted.role());
        assertEquals(
                OrganizationInvitationStatus.ACCEPTED,
                invitationRepository.findByIdAndOrganizationId(
                        invitation.id(),
                        organization.id()
                ).orElseThrow().getStatus()
        );
        assertEquals(
                SubscriptionStatus.TRIALING,
                subscriptionRepository.findByOrganizationId(
                        organization.id()
                ).orElseThrow().getStatus()
        );
        assertTrue(workspaceService.getActiveWorkspaces(invitee.getId())
                .stream()
                .anyMatch(workspace -> workspace.organizationId()
                        .equals(organization.id())));

        membershipService.deactivateMembership(
                member.getId(),
                new DeactivateOrganizationMembershipRequest(
                        member.getVersion()
                )
        );

        assertEquals(
                OrganizationMembershipStatus.INACTIVE,
                membershipRepository.findByIdAndOrganizationId(
                        member.getId(),
                        organization.id()
                ).orElseThrow().getStatus()
        );
        assertTrue(workspaceService.getActiveWorkspaces(invitee.getId())
                .isEmpty());
        assertFalse(workspaceService.getActiveWorkspaces(owner.getId())
                .isEmpty());
    }

    private User createUser(String fullName, String email, Role role) {
        User user = new User();
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPasswordHash("phase-89-integration-test-password-hash");
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        return userRepository.saveAndFlush(user);
    }

    private Organization createOrganization(
            String name,
            String slug,
            User creator
    ) {
        Organization organization = new Organization();
        organization.setName(name);
        organization.setSlug(slug);
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTimeZone("Africa/Mogadishu");
        organization.setCreatedByUser(creator);
        return organizationRepository.saveAndFlush(organization);
    }

    private OrganizationSubscription createSubscription(
            Organization organization
    ) {
        SubscriptionPlan plan = planRepository.findByCodeAndActiveTrue(
                SubscriptionPlanCode.STARTER
        ).orElseThrow();
        OrganizationSubscription subscription =
                new OrganizationSubscription();
        subscription.setOrganization(organization);
        subscription.setPlan(plan);
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setStartedAt(NOW);
        return subscriptionRepository.saveAndFlush(subscription);
    }

    private BillingCustomer createBillingCustomer(
            Organization organization,
            String providerId
    ) {
        BillingCustomer customer = new BillingCustomer();
        customer.setOrganization(organization);
        customer.setProvider(BillingProviderName.STRIPE);
        customer.setProviderCustomerId(providerId);
        return billingCustomerRepository.saveAndFlush(customer);
    }

    private PublicApiKey createApiKey(
            Organization organization,
            User actor,
            String publicId
    ) {
        PublicApiKey apiKey = new PublicApiKey();
        apiKey.setOrganization(organization);
        apiKey.setName("Phase 89 API key " + publicId);
        apiKey.setPublicId(publicId);
        apiKey.setDisplayPrefix("tdm_test_" + publicId + "...");
        apiKey.setSecretHash("a".repeat(64));
        apiKey.setCreatedByUser(actor);
        return apiKeyRepository.saveAndFlush(apiKey);
    }

    private WebhookGraph createWebhookGraph(
            Organization organization,
            User actor,
            String suffix
    ) {
        GeneratedWebhookSecret generated = webhookEncryptionService.generate();
        EncryptedWebhookSecret encrypted = generated.encryptedSecret();
        WebhookSubscription subscription = new WebhookSubscription();
        subscription.setOrganization(organization);
        subscription.setName("Phase 89 webhook " + suffix);
        subscription.setEndpointUrl(
                "https://example.test/phase89/" + suffix
        );
        subscription.setSecretDisplaySuffix(generated.displaySuffix());
        subscription.setCurrentSecretCiphertext(encrypted.ciphertext());
        subscription.setCurrentSecretNonce(encrypted.nonce());
        subscription.setCurrentSecretTag(encrypted.authenticationTag());
        subscription.setCurrentSecretKeyVersion(encrypted.keyVersion());
        subscription.setCreatedByUser(actor);
        subscription = webhookSubscriptionRepository.saveAndFlush(
                subscription
        );

        WebhookEvent event = new WebhookEvent();
        event.setPublicEventId("evt_phase89_boundary_" + suffix);
        event.setOrganization(organization);
        event.setEventType(WebhookEventType.CUSTOMER_CREATED);
        event.setAggregateType("CUSTOMER");
        event.setAggregateId(100L);
        event.setPayload("{\"data\":{\"customer\":{\"id\":100}}}");
        event.setNextPublicationAttemptAt(NOW);
        event.setOccurredAt(NOW);
        event = webhookEventRepository.saveAndFlush(event);

        WebhookDelivery delivery = new WebhookDelivery();
        delivery.setPublicDeliveryId("dlv_phase89_boundary_" + suffix);
        delivery.setOrganization(organization);
        delivery.setEvent(event);
        delivery.setSubscription(subscription);
        delivery.setNextAttemptAt(NOW);
        delivery = webhookDeliveryRepository.saveAndFlush(delivery);
        return new WebhookGraph(subscription, event, delivery);
    }

    private WorkflowGraph createWorkflowGraph(
            Organization organization,
            User actor,
            WebhookEvent event,
            String suffix
    ) {
        WorkflowDefinition workflow = new WorkflowDefinition();
        workflow.setPublicWorkflowId("wf_phase89_boundary_" + suffix);
        workflow.setOrganization(organization);
        workflow.setName("Phase 89 workflow " + suffix);
        workflow.setCreatedByUser(actor);
        workflow = workflowRepository.saveAndFlush(workflow);

        WorkflowExecution execution = new WorkflowExecution();
        execution.setPublicExecutionId("wfx_phase89_boundary_" + suffix);
        execution.setOrganization(organization);
        execution.setWorkflow(workflow);
        execution.setSourceEvent(event);
        execution.setWorkflowVersion(workflow.getDefinitionVersion());
        execution.setTriggerEventType(WebhookEventType.CUSTOMER_CREATED);
        execution.setSourceEventPublicId(event.getPublicEventId());
        execution.setCorrelationId("cor_phase89_boundary_" + suffix);
        execution.setNextAttemptAt(NOW);
        execution.setInputPayload(event.getPayload());
        execution = workflowExecutionRepository.saveAndFlush(execution);
        return new WorkflowGraph(workflow, execution);
    }

    private IntegrationGraph createIntegrationGraph(
            Organization organization,
            User actor,
            String suffix
    ) {
        IntegrationConnection connection = new IntegrationConnection();
        connection.setPublicConnectionId("int_phase89_boundary_" + suffix);
        connection.setOrganization(organization);
        connection.setProvider(IntegrationProvider.SMTP);
        connection.setName("Phase 89 SMTP " + suffix);
        connection.setConfiguration("{}");
        connection.setCreatedByUser(actor);
        connection = connectionRepository.saveAndFlush(connection);

        IntegrationDelivery delivery = new IntegrationDelivery();
        delivery.setPublicDeliveryId("idl_phase89_boundary_" + suffix);
        delivery.setOrganization(organization);
        delivery.setConnection(connection);
        delivery.setProvider(IntegrationProvider.SMTP);
        delivery.setDeliveryType(IntegrationDeliveryType.EMAIL);
        delivery.setDestination("phase89-" + suffix + "@example.test");
        delivery.setSubject("Phase 89 boundary test");
        delivery.setMessageBody("Tenant boundary test");
        delivery.setIdempotencyKey("phase89-boundary-" + suffix);
        delivery.setStatus(IntegrationDeliveryStatus.PENDING);
        delivery.setMaximumAttempts(6);
        delivery.setNextAttemptAt(NOW);
        delivery.setCreatedByUser(actor);
        delivery = integrationDeliveryRepository.saveAndFlush(delivery);
        return new IntegrationGraph(connection, delivery);
    }

    private String extractToken(String link) {
        String rawQuery = URI.create(link).getRawQuery();
        for (String parameter : rawQuery.split("&")) {
            if (parameter.startsWith("token=")) {
                return URLDecoder.decode(
                        parameter.substring("token=".length()),
                        StandardCharsets.UTF_8
                );
            }
        }
        throw new IllegalArgumentException("Invitation token is missing");
    }

    private record WebhookGraph(
            WebhookSubscription subscription,
            WebhookEvent event,
            WebhookDelivery delivery
    ) {
    }

    private record WorkflowGraph(
            WorkflowDefinition workflow,
            WorkflowExecution execution
    ) {
    }

    private record IntegrationGraph(
            IntegrationConnection connection,
            IntegrationDelivery delivery
    ) {
    }
}
