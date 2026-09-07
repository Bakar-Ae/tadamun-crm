package com.crm.backend.webhook;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.StringJoiner;

@Service
public class WebhookRequestSigner {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    public String signatureHeader(
            List<String> signingSecrets,
            long timestamp,
            byte[] body
    ) {
        if (signingSecrets == null || signingSecrets.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one webhook signing secret is required"
            );
        }

        StringJoiner header = new StringJoiner(",");
        header.add("t=" + timestamp);
        for (String signingSecret : signingSecrets) {
            header.add("v1=" + sign(signingSecret, timestamp, body));
        }
        return header.toString();
    }

    public String sign(
            String signingSecret,
            long timestamp,
            byte[] body
    ) {
        if (signingSecret == null || signingSecret.isBlank()) {
            throw new IllegalArgumentException(
                    "Webhook signing secret is required"
            );
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException(
                    "Webhook signature timestamp must be positive"
            );
        }

        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    signingSecret.getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM
            ));
            mac.update(Long.toString(timestamp).getBytes(
                    StandardCharsets.US_ASCII
            ));
            mac.update((byte) '.');
            return HexFormat.of().formatHex(mac.doFinal(body));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "HMAC-SHA-256 is unavailable",
                    exception
            );
        }
    }

    public boolean matches(String expectedHex, String actualHex) {
        if (expectedHex == null || actualHex == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedHex.getBytes(StandardCharsets.US_ASCII),
                actualHex.getBytes(StandardCharsets.US_ASCII)
        );
    }
}
