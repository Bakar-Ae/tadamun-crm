package com.crm.backend.organization.invitation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrganizationInvitationTokenServiceTest {

    private final OrganizationInvitationTokenService tokenService =
            new OrganizationInvitationTokenService();

    @Test
    void generateShouldReturnUniqueRawTokensAndHashes() {
        GeneratedInvitationToken first = tokenService.generate();
        GeneratedInvitationToken second = tokenService.generate();

        assertNotEquals(first.rawToken(), first.tokenHash());
        assertNotEquals(first.rawToken(), second.rawToken());
        assertNotEquals(first.tokenHash(), second.tokenHash());
    }

    @Test
    void hashShouldBeDeterministic() {
        String rawToken = tokenService.generate().rawToken();

        assertEquals(
                tokenService.hash(rawToken),
                tokenService.hash(rawToken)
        );
    }

    @Test
    void hashShouldRejectBlankToken() {
        assertThrows(
                IllegalArgumentException.class,
                () -> tokenService.hash(" ")
        );
    }
}
