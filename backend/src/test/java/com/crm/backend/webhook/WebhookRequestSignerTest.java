package com.crm.backend.webhook;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebhookRequestSignerTest {

    private final WebhookRequestSigner signer = new WebhookRequestSigner();

    @Test
    void shouldSignTimestampDotExactRawBodyWithHmacSha256() {
        byte[] body = "{\"id\":\"evt_test\"}".getBytes(
                StandardCharsets.UTF_8
        );

        String signature = signer.sign(
                "whsec_test_secret",
                1_700_000_000L,
                body
        );

        assertEquals(
                "13941114bb88ac44a76abcfddea5b92aa6182a4b63d8be3aae908a616083bd7e",
                signature
        );
        assertTrue(signer.matches(signature, signature));
        assertFalse(signer.matches(signature, signature + "0"));
    }

    @Test
    void shouldIncludeCurrentAndPreviousSecretDuringRotation() {
        byte[] body = "{}".getBytes(StandardCharsets.UTF_8);

        String header = signer.signatureHeader(
                List.of("current", "previous"),
                1_700_000_000L,
                body
        );

        assertTrue(header.startsWith("t=1700000000,v1="));
        assertEquals(2, header.split("v1=", -1).length - 1);
    }
}
