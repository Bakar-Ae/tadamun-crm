package com.crm.backend.email;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetEmailTemplateTest {

    private final PasswordResetEmailTemplate template =
            new PasswordResetEmailTemplate();

    @Test
    void shouldCreateBrandedTextAndHtmlContent() {
        String resetLink =
                "https://crm.example.com/reset-password?token=test-token";

        String plainText = template.plainText(resetLink);
        String html = template.html(resetLink);

        assertTrue(plainText.contains(resetLink));
        assertTrue(html.contains(resetLink));
        assertTrue(html.contains("Tadamun"));
        assertTrue(html.contains("Reset password"));
    }

    @Test
    void shouldEscapeUnsafeHtmlInsideResetLink() {
        String unsafeLink =
                "https://crm.example.com/reset?token=<script>alert(1)</script>";

        String html = template.html(unsafeLink);

        assertFalse(html.contains("<script>"));
        assertTrue(html.contains("&lt;script&gt;"));
    }
}