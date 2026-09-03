package com.crm.backend.email;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationInvitationEmailTemplateTest {

    private final OrganizationInvitationEmailTemplate template =
            new OrganizationInvitationEmailTemplate();

    @Test
    void plainTextShouldContainInvitationDetails() {
        String text = template.plainText(
                "Tadamun Sales",
                "Bakar Mohammed",
                "Sales Rep",
                "https://example.com/accept?token=secure-token",
                LocalDateTime.of(2026, 9, 5, 14, 30)
        );

        assertTrue(text.contains("Join Tadamun Sales on Tadamun"));
        assertTrue(text.contains("Bakar Mohammed"));
        assertTrue(text.contains("Sales Rep"));
        assertTrue(text.contains("token=secure-token"));
        assertTrue(text.contains("Sep 5, 2026 at 14:30"));
    }

    @Test
    void htmlShouldEscapeDynamicValues() {
        String html = template.html(
                "Tadamun <Sales>",
                "Bakar <script>alert(1)</script>",
                "Sales & Support",
                "https://example.com/accept?token=abc&source=email",
                LocalDateTime.of(2026, 9, 5, 14, 30)
        );

        assertTrue(html.contains("Tadamun &lt;Sales&gt;"));
        assertTrue(html.contains("Sales &amp; Support"));
        assertTrue(html.contains("token=abc&amp;source=email"));
        assertFalse(html.contains("<script>alert(1)</script>"));
    }
}
