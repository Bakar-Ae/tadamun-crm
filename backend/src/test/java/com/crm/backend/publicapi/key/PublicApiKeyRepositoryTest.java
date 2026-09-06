package com.crm.backend.publicapi.key;

import com.crm.backend.organization.Organization;
import com.crm.backend.organization.OrganizationRepository;
import com.crm.backend.organization.OrganizationStatus;
import com.crm.backend.role.Role;
import com.crm.backend.role.RoleName;
import com.crm.backend.role.RoleRepository;
import com.crm.backend.support.MySqlTestContainerConfiguration;
import com.crm.backend.user.User;
import com.crm.backend.user.UserRepository;
import com.crm.backend.user.UserStatus;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(MySqlTestContainerConfiguration.class)
@Transactional
class PublicApiKeyRepositoryTest {

    @Autowired
    private PublicApiKeyRepository apiKeyRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistScopesAndEnforceOrganizationLookup() {
        Role role = roleRepository.findByName(RoleName.ADMIN).orElseThrow();
        User actor = new User();
        actor.setFullName("Phase 85 API Administrator");
        actor.setEmail("phase85.api.admin@crm.test");
        actor.setPasswordHash("integration-test-password-hash");
        actor.setRole(role);
        actor.setStatus(UserStatus.ACTIVE);
        actor = userRepository.save(actor);

        Organization organization = new Organization();
        organization.setName("Phase 85 Organization");
        organization.setSlug("phase-85-organization");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization.setTimeZone("Africa/Mogadishu");
        organization.setCreatedByUser(actor);
        organization = organizationRepository.save(organization);

        PublicApiKey apiKey = new PublicApiKey();
        apiKey.setOrganization(organization);
        apiKey.setName("Repository integration test");
        apiKey.setPublicId("abcdefghijklmnop");
        apiKey.setDisplayPrefix("tdm_live_abcdefghijklmnop.abcdef...");
        apiKey.setSecretHash("a".repeat(64));
        apiKey.setStatus(PublicApiKeyStatus.ACTIVE);
        apiKey.setRateLimitPerMinute(60);
        apiKey.setScopes(new LinkedHashSet<>(Set.of(
                PublicApiScope.CUSTOMERS_READ,
                PublicApiScope.LEADS_READ
        )));
        apiKey.setCreatedByUser(actor);
        apiKey = apiKeyRepository.saveAndFlush(apiKey);

        Long apiKeyId = apiKey.getId();
        Long organizationId = organization.getId();
        entityManager.clear();

        PublicApiKey loaded = apiKeyRepository
                .findByPublicId("abcdefghijklmnop")
                .orElseThrow();

        assertEquals(
                Set.of(
                        PublicApiScope.CUSTOMERS_READ,
                        PublicApiScope.LEADS_READ
                ),
                loaded.getScopes()
        );
        assertEquals(organizationId, loaded.getOrganization().getId());
        assertTrue(apiKeyRepository.findByIdAndOrganizationId(
                apiKeyId,
                organizationId + 1_000L
        ).isEmpty());

        @SuppressWarnings("unchecked")
        java.util.List<String> storedScopes = entityManager
                .createNativeQuery("""
                        SELECT scope_key
                        FROM public_api_key_scopes
                        WHERE api_key_id = :apiKeyId
                        ORDER BY scope_key
                        """, String.class)
                .setParameter("apiKeyId", apiKeyId)
                .getResultList();
        assertEquals(
                java.util.List.of("customers:read", "leads:read"),
                storedScopes
        );
    }
}
