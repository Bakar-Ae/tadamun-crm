package com.crm.backend.integration;

import com.crm.backend.integration.provider.IntegrationSecrets;
import com.crm.backend.integration.security.EncryptedIntegrationCredentials;
import com.crm.backend.integration.security.IntegrationCredentialContext;
import com.crm.backend.integration.security.IntegrationCredentialEncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.regex.Pattern;

@Service
public class IntegrationCredentialService {

    private static final int MAX_CREDENTIAL_FIELDS = 20;
    private static final int MAX_CREDENTIAL_VALUE_LENGTH = 4096;
    private static final Pattern FIELD_NAME = Pattern.compile(
            "[A-Za-z][A-Za-z0-9_]{0,63}"
    );
    private static final TypeReference<Map<String, String>> SECRET_MAP =
            new TypeReference<>() {
            };

    private final IntegrationCredentialRepository credentialRepository;
    private final IntegrationCredentialEncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    public IntegrationCredentialService(
            IntegrationCredentialRepository credentialRepository,
            IntegrationCredentialEncryptionService encryptionService,
            ObjectMapper objectMapper
    ) {
        this.credentialRepository = credentialRepository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void store(
            IntegrationConnection connection,
            Map<String, String> credentialValues
    ) {
        Map<String, String> credentials = validate(credentialValues);
        String serialized;
        try {
            serialized = objectMapper.writeValueAsString(credentials);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException(
                    "Integration credentials are invalid",
                    exception
            );
        }

        EncryptedIntegrationCredentials encrypted = encryptionService.encrypt(
                context(connection),
                serialized
        );
        IntegrationCredential credential = credentialRepository
                .findByConnectionIdAndOrganizationId(
                        connection.getId(),
                        connection.getOrganization().getId()
                )
                .orElseGet(IntegrationCredential::new);
        if (credential.getId() == null) {
            credential.setConnection(connection);
            credential.setOrganization(connection.getOrganization());
        }
        credential.setCiphertext(encrypted.ciphertext());
        credential.setNonce(encrypted.nonce());
        credential.setAuthenticationTag(encrypted.authenticationTag());
        credential.setKeyVersion(encrypted.keyVersion());
        credentialRepository.save(credential);
    }

    @Transactional(readOnly = true)
    public IntegrationSecrets load(IntegrationConnection connection) {
        IntegrationCredential credential = credentialRepository
                .findByConnectionIdAndOrganizationId(
                        connection.getId(),
                        connection.getOrganization().getId()
                )
                .orElseThrow(() -> new IllegalStateException(
                        "Integration credentials are not configured"
                ));
        EncryptedIntegrationCredentials encrypted =
                new EncryptedIntegrationCredentials(
                        credential.getCiphertext(),
                        credential.getNonce(),
                        credential.getAuthenticationTag(),
                        credential.getKeyVersion()
                );
        String serialized = encryptionService.decrypt(
                context(connection),
                encrypted
        );
        try {
            return new IntegrationSecrets(
                    objectMapper.readValue(serialized, SECRET_MAP)
            );
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                    "Stored integration credentials are invalid",
                    exception
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean isConfigured(IntegrationConnection connection) {
        return credentialRepository.existsByConnectionIdAndOrganizationId(
                connection.getId(),
                connection.getOrganization().getId()
        );
    }

    private Map<String, String> validate(Map<String, String> values) {
        if (values == null) {
            throw new IllegalArgumentException(
                    "Integration credentials are required"
            );
        }
        if (values.size() > MAX_CREDENTIAL_FIELDS) {
            throw new IllegalArgumentException(
                    "Too many integration credential fields"
            );
        }
        values.forEach((name, value) -> {
            if (name == null || !FIELD_NAME.matcher(name).matches()) {
                throw new IllegalArgumentException(
                        "Integration credential field name is invalid"
                );
            }
            if (value == null || value.length() > MAX_CREDENTIAL_VALUE_LENGTH) {
                throw new IllegalArgumentException(
                        "Integration credential value is invalid"
                );
            }
        });
        return Map.copyOf(values);
    }

    private IntegrationCredentialContext context(
            IntegrationConnection connection
    ) {
        return new IntegrationCredentialContext(
                connection.getOrganization().getId(),
                connection.getPublicConnectionId(),
                connection.getProvider()
        );
    }
}
