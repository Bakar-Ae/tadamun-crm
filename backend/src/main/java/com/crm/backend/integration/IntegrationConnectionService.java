package com.crm.backend.integration;

import com.crm.backend.common.ResourceNotFoundException;
import com.crm.backend.integration.dto.CreateIntegrationConnectionRequest;
import com.crm.backend.integration.dto.IntegrationConnectionResponse;
import com.crm.backend.integration.dto.UpdateIntegrationConnectionRequest;
import com.crm.backend.integration.provider.IntegrationProviderAdapter;
import com.crm.backend.integration.provider.IntegrationProviderContext;
import com.crm.backend.integration.provider.IntegrationProviderException;
import com.crm.backend.integration.provider.IntegrationProviderRegistry;
import com.crm.backend.integration.provider.IntegrationSecrets;
import com.crm.backend.integration.provider.IntegrationVerificationResult;
import com.crm.backend.security.tenant.CurrentOrganizationProvider;
import com.crm.backend.subscription.SubscriptionTimeProvider;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class IntegrationConnectionService {

    private static final int MAX_CONFIGURATION_BYTES = 16 * 1024;

    private final IntegrationConnectionRepository connectionRepository;
    private final IntegrationCredentialService credentialService;
    private final IntegrationConnectionMapper mapper;
    private final IntegrationProviderRegistry providerRegistry;
    private final IntegrationPublicIdGenerator idGenerator;
    private final IntegrationAuditService auditService;
    private final CurrentOrganizationProvider organizationProvider;
    private final UserRepository userRepository;
    private final SubscriptionTimeProvider timeProvider;
    private final ObjectMapper objectMapper;

    public IntegrationConnectionService(
            IntegrationConnectionRepository connectionRepository,
            IntegrationCredentialService credentialService,
            IntegrationConnectionMapper mapper,
            IntegrationProviderRegistry providerRegistry,
            IntegrationPublicIdGenerator idGenerator,
            IntegrationAuditService auditService,
            CurrentOrganizationProvider organizationProvider,
            UserRepository userRepository,
            SubscriptionTimeProvider timeProvider,
            ObjectMapper objectMapper
    ) {
        this.connectionRepository = connectionRepository;
        this.credentialService = credentialService;
        this.mapper = mapper;
        this.providerRegistry = providerRegistry;
        this.idGenerator = idGenerator;
        this.auditService = auditService;
        this.organizationProvider = organizationProvider;
        this.userRepository = userRepository;
        this.timeProvider = timeProvider;
        this.objectMapper = objectMapper;
    }

    public Page<IntegrationConnectionResponse> getConnections(
            Pageable pageable
    ) {
        return connectionRepository.findByOrganizationId(
                organizationProvider.getOrganizationId(),
                pageable
        ).map(mapper::toResponse);
    }

    public IntegrationConnectionResponse getConnection(Long id) {
        return mapper.toResponse(find(id));
    }

    @Transactional
    public IntegrationConnectionResponse createConnection(
            CreateIntegrationConnectionRequest request,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        String name = normalizeName(request.name());
        requireUniqueName(organizationId, request.provider(), name, null);
        IntegrationProviderAdapter adapter = providerRegistry.require(
                request.provider()
        );
        adapter.validateConfiguration(request.configuration());
        IntegrationSecrets secrets = new IntegrationSecrets(
                request.credentials()
        );
        adapter.validateCredentials(secrets);

        User actor = userRepository.getReferenceById(actorUserId);
        IntegrationConnection connection = new IntegrationConnection();
        connection.setPublicConnectionId(idGenerator.connectionId());
        connection.setOrganization(
                organizationProvider.getOrganizationReference()
        );
        connection.setProvider(request.provider());
        connection.setName(name);
        connection.setConfiguration(serializeConfiguration(
                request.configuration()
        ));
        connection.setCreatedByUser(actor);
        connection.setCredentialsUpdatedAt(timeProvider.now());
        IntegrationConnection saved = connectionRepository.saveAndFlush(
                connection
        );
        credentialService.store(saved, request.credentials());
        auditService.log(
                organizationId,
                actorUserId,
                IntegrationAuditAction.INTEGRATION_CREATED,
                saved.getId(),
                auditService.details(
                        "publicConnectionId", saved.getPublicConnectionId(),
                        "provider", saved.getProvider().name(),
                        "name", saved.getName(),
                        "credentialFields", secrets.names()
                )
        );
        return mapper.toResponse(saved);
    }

    @Transactional
    public IntegrationConnectionResponse updateConnection(
            Long id,
            UpdateIntegrationConnectionRequest request,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        IntegrationConnection connection = find(id);
        requireNotRevoked(connection);
        String name = normalizeName(request.name());
        requireUniqueName(
                organizationId,
                connection.getProvider(),
                name,
                connection.getId()
        );
        IntegrationProviderAdapter adapter = providerRegistry.require(
                connection.getProvider()
        );
        adapter.validateConfiguration(request.configuration());
        connection.setName(name);
        connection.setConfiguration(serializeConfiguration(
                request.configuration()
        ));
        connection.setUpdatedByUser(userRepository.getReferenceById(
                actorUserId
        ));
        connection.setStatus(IntegrationConnectionStatus.DRAFT);
        clearVerification(connection);

        if (request.credentials() != null) {
            IntegrationSecrets secrets = new IntegrationSecrets(
                    request.credentials()
            );
            adapter.validateCredentials(secrets);
            credentialService.store(connection, request.credentials());
            connection.setCredentialsUpdatedAt(timeProvider.now());
            auditService.log(
                    organizationId,
                    actorUserId,
                    IntegrationAuditAction.INTEGRATION_CREDENTIALS_UPDATED,
                    connection.getId(),
                    auditService.details(
                            "publicConnectionId",
                            connection.getPublicConnectionId(),
                            "credentialFields", secrets.names()
                    )
            );
        }
        IntegrationConnection saved = connectionRepository.saveAndFlush(
                connection
        );
        auditService.log(
                organizationId,
                actorUserId,
                IntegrationAuditAction.INTEGRATION_UPDATED,
                saved.getId(),
                auditService.details(
                        "publicConnectionId", saved.getPublicConnectionId(),
                        "provider", saved.getProvider().name(),
                        "name", saved.getName()
                )
        );
        return mapper.toResponse(saved);
    }

    @Transactional
    public IntegrationConnectionResponse verifyConnection(
            Long id,
            Long actorUserId
    ) {
        Long organizationId = organizationProvider.getOrganizationId();
        IntegrationConnection connection = find(id);
        requireNotRevoked(connection);
        IntegrationProviderAdapter adapter = providerRegistry.require(
                connection.getProvider()
        );
        IntegrationProviderContext context = new IntegrationProviderContext(
                organizationId,
                connection.getPublicConnectionId(),
                configuration(connection),
                credentialService.load(connection)
        );
        connection.setUpdatedByUser(userRepository.getReferenceById(
                actorUserId
        ));
        try {
            IntegrationVerificationResult result = adapter.verify(context);
            if (!result.successful()) {
                markVerificationFailure(connection, "VERIFICATION_FAILED",
                        result.message());
            } else {
                connection.setStatus(IntegrationConnectionStatus.ACTIVE);
                connection.setExternalAccountId(result.externalAccountId());
                connection.setLastVerifiedAt(timeProvider.now());
                connection.setLastErrorCategory(null);
                connection.setLastErrorMessage(null);
                auditService.log(
                        organizationId,
                        actorUserId,
                        IntegrationAuditAction.INTEGRATION_VERIFIED,
                        connection.getId(),
                        auditService.details(
                                "publicConnectionId",
                                connection.getPublicConnectionId(),
                                "provider", connection.getProvider().name(),
                                "externalAccountId",
                                result.externalAccountId()
                        )
                );
            }
        } catch (IntegrationProviderException exception) {
            markVerificationFailure(
                    connection,
                    exception.getCategory(),
                    exception.getMessage()
            );
        }
        IntegrationConnection saved = connectionRepository.saveAndFlush(
                connection
        );
        if (saved.getStatus() == IntegrationConnectionStatus.ERROR) {
            auditService.log(
                    organizationId,
                    actorUserId,
                    IntegrationAuditAction.INTEGRATION_VERIFICATION_FAILED,
                    saved.getId(),
                    auditService.details(
                            "publicConnectionId",
                            saved.getPublicConnectionId(),
                            "provider", saved.getProvider().name(),
                            "errorCategory", saved.getLastErrorCategory()
                    )
            );
        }
        return mapper.toResponse(saved);
    }

    @Transactional
    public IntegrationConnectionResponse disableConnection(
            Long id,
            Long actorUserId
    ) {
        IntegrationConnection connection = find(id);
        requireNotRevoked(connection);
        connection.setStatus(IntegrationConnectionStatus.DISABLED);
        connection.setUpdatedByUser(userRepository.getReferenceById(
                actorUserId
        ));
        IntegrationConnection saved = connectionRepository.saveAndFlush(
                connection
        );
        auditLifecycle(
                saved,
                actorUserId,
                IntegrationAuditAction.INTEGRATION_DISABLED
        );
        return mapper.toResponse(saved);
    }

    @Transactional
    public IntegrationConnectionResponse revokeConnection(
            Long id,
            Long actorUserId
    ) {
        IntegrationConnection connection = find(id);
        if (connection.getStatus() == IntegrationConnectionStatus.REVOKED) {
            return mapper.toResponse(connection);
        }
        User actor = userRepository.getReferenceById(actorUserId);
        connection.setStatus(IntegrationConnectionStatus.REVOKED);
        connection.setRevokedAt(timeProvider.now());
        connection.setRevokedByUser(actor);
        connection.setUpdatedByUser(actor);
        IntegrationConnection saved = connectionRepository.saveAndFlush(
                connection
        );
        auditLifecycle(
                saved,
                actorUserId,
                IntegrationAuditAction.INTEGRATION_REVOKED
        );
        return mapper.toResponse(saved);
    }

    public IntegrationConnection requireActive(Long id) {
        IntegrationConnection connection = find(id);
        if (connection.getStatus() != IntegrationConnectionStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Integration connection is not active"
            );
        }
        return connection;
    }

    private IntegrationConnection find(Long id) {
        return connectionRepository.findByIdAndOrganizationId(
                id,
                organizationProvider.getOrganizationId()
        ).orElseThrow(() -> new ResourceNotFoundException(
                "Integration connection not found"
        ));
    }

    private String serializeConfiguration(Map<String, Object> configuration) {
        try {
            String value = objectMapper.writeValueAsString(configuration);
            if (value.getBytes(StandardCharsets.UTF_8).length
                    > MAX_CONFIGURATION_BYTES) {
                throw new IllegalArgumentException(
                        "Integration configuration is too large"
                );
            }
            return value;
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Integration configuration is invalid",
                    exception
            );
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> configuration(
            IntegrationConnection connection
    ) {
        try {
            return objectMapper.readValue(
                    connection.getConfiguration(),
                    Map.class
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Stored integration configuration is invalid",
                    exception
            );
        }
    }

    private void requireUniqueName(
            Long organizationId,
            IntegrationProvider provider,
            String name,
            Long currentId
    ) {
        boolean exists = currentId == null
                ? connectionRepository
                .existsByOrganizationIdAndProviderAndNameIgnoreCase(
                        organizationId,
                        provider,
                        name
                )
                : connectionRepository
                .existsByOrganizationIdAndProviderAndNameIgnoreCaseAndIdNot(
                        organizationId,
                        provider,
                        name,
                        currentId
                );
        if (exists) {
            throw new IllegalArgumentException(
                    "An integration with this provider and name already exists"
            );
        }
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private void requireNotRevoked(IntegrationConnection connection) {
        if (connection.getStatus() == IntegrationConnectionStatus.REVOKED) {
            throw new IllegalArgumentException(
                    "A revoked integration cannot be changed"
            );
        }
    }

    private void clearVerification(IntegrationConnection connection) {
        connection.setExternalAccountId(null);
        connection.setLastVerifiedAt(null);
        connection.setLastErrorCategory(null);
        connection.setLastErrorMessage(null);
    }

    private void markVerificationFailure(
            IntegrationConnection connection,
            String category,
            String message
    ) {
        connection.setStatus(IntegrationConnectionStatus.ERROR);
        connection.setLastErrorCategory(category);
        connection.setLastErrorMessage(message);
    }

    private void auditLifecycle(
            IntegrationConnection connection,
            Long actorUserId,
            IntegrationAuditAction action
    ) {
        auditService.log(
                connection.getOrganization().getId(),
                actorUserId,
                action,
                connection.getId(),
                auditService.details(
                        "publicConnectionId",
                        connection.getPublicConnectionId(),
                        "provider", connection.getProvider().name(),
                        "status", connection.getStatus().name()
                )
        );
    }
}
