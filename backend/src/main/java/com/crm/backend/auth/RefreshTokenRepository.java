package com.crm.backend.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        UPDATE RefreshToken token
        SET token.revoked = true,
            token.revokedAt = :revokedAt
        WHERE token.user.id = :userId
        AND token.revoked = false
        """)
    void revokeAllActiveTokensForUser(
            @Param("userId") Long userId,
            @Param("revokedAt") LocalDateTime revokedAt
    );
}