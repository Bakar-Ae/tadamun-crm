package com.crm.backend.integration;

import com.crm.backend.organization.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "integration_credentials")
public class IntegrationCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false, updatable = false)
    private Organization organization;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "connection_id", nullable = false, unique = true,
            updatable = false)
    private IntegrationConnection connection;

    @Column(nullable = false, columnDefinition = "BLOB")
    private byte[] ciphertext;

    @Column(nullable = false, length = 12)
    private byte[] nonce;

    @Column(name = "authentication_tag", nullable = false, length = 16)
    private byte[] authenticationTag;

    @Column(name = "key_version", nullable = false, length = 50)
    private String keyVersion;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion = 1;

    @Column(name = "created_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false,
            insertable = false, updatable = false)
    private LocalDateTime updatedAt;
}
