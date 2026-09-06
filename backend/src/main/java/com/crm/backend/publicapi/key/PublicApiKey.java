package com.crm.backend.publicapi.key;

import com.crm.backend.organization.Organization;
import com.crm.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "public_api_keys")
public class PublicApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "public_id", nullable = false, unique = true,
            length = 32, updatable = false)
    private String publicId;

    @Column(name = "display_prefix", nullable = false, length = 60,
            updatable = false)
    private String displayPrefix;

    @Column(name = "secret_hash", nullable = false, length = 64,
            updatable = false)
    private String secretHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PublicApiKeyStatus status = PublicApiKeyStatus.ACTIVE;

    @Column(name = "rate_limit_per_minute", nullable = false)
    private Integer rateLimitPerMinute = 60;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "public_api_key_scopes",
            joinColumns = @JoinColumn(name = "api_key_id")
    )
    @Column(name = "scope_key", nullable = false, length = 50)
    @Convert(converter = PublicApiScopeConverter.class)
    private Set<PublicApiScope> scopes = new LinkedHashSet<>();

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_user_id", nullable = false, updatable = false)
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id")
    private User revokedByUser;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rotated_from_key_id")
    private PublicApiKey rotatedFromKey;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
