package com.crm.backend.webhook;

import com.crm.backend.organization.Organization;
import com.crm.backend.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "organization_id",
            nullable = false,
            updatable = false
    )
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "endpoint_url", nullable = false, length = 2048)
    private String endpointUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebhookSubscriptionStatus status =
            WebhookSubscriptionStatus.ACTIVE;

    @Column(name = "secret_display_suffix", length = 8)
    private String secretDisplaySuffix;

    @Lob
    @Column(name = "current_secret_ciphertext", columnDefinition = "BLOB")
    private byte[] currentSecretCiphertext;

    @Column(name = "current_secret_nonce", length = 12)
    private byte[] currentSecretNonce;

    @Column(name = "current_secret_tag", length = 16)
    private byte[] currentSecretTag;

    @Column(name = "current_secret_key_version", length = 50)
    private String currentSecretKeyVersion;

    @Lob
    @Column(name = "previous_secret_ciphertext", columnDefinition = "BLOB")
    private byte[] previousSecretCiphertext;

    @Column(name = "previous_secret_nonce", length = 12)
    private byte[] previousSecretNonce;

    @Column(name = "previous_secret_tag", length = 16)
    private byte[] previousSecretTag;

    @Column(name = "previous_secret_key_version", length = 50)
    private String previousSecretKeyVersion;

    @Column(name = "previous_secret_expires_at")
    private LocalDateTime previousSecretExpiresAt;

    @Column(name = "consecutive_failures", nullable = false)
    private int consecutiveFailures;

    @Column(name = "last_success_at")
    private LocalDateTime lastSuccessAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false,
            updatable = false
    )
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by_user_id")
    private User updatedByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revoked_by_user_id")
    private User revokedByUser;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(
            name = "created_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "updated_at",
            nullable = false,
            insertable = false,
            updatable = false
    )
    private LocalDateTime updatedAt;
}