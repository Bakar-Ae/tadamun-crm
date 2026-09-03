package com.crm.backend.email;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class OrganizationInvitationEmailTemplate {

    private static final DateTimeFormatter EXPIRY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, uuuu 'at' HH:mm");

    public String subject() {
        return "You're invited to join a Tadamun workspace";
    }

    public String plainText(
            String organizationName,
            String inviterName,
            String roleName,
            String invitationLink,
            LocalDateTime expiresAt
    ) {
        return """
                Join %s on Tadamun

                %s invited you to join %s as %s.

                Accept the invitation:
                %s

                This invitation expires on %s and can only be used once.
                If you were not expecting this invitation, you can ignore this email.
                """.formatted(
                plainValue(organizationName),
                plainValue(inviterName),
                plainValue(organizationName),
                plainValue(roleName),
                invitationLink,
                expiresAt.format(EXPIRY_FORMAT)
        );
    }

    public String html(
            String organizationName,
            String inviterName,
            String roleName,
            String invitationLink,
            LocalDateTime expiresAt
    ) {
        String safeOrganizationName = HtmlUtils.htmlEscape(organizationName);
        String safeInviterName = HtmlUtils.htmlEscape(inviterName);
        String safeRoleName = HtmlUtils.htmlEscape(roleName);
        String safeInvitationLink = HtmlUtils.htmlEscape(invitationLink);
        String safeExpiresAt = HtmlUtils.htmlEscape(
                expiresAt.format(EXPIRY_FORMAT)
        );

        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width">
                    <title>Join your Tadamun workspace</title>
                </head>
                <body style="margin:0;background:#f4f6fb;font-family:Arial,sans-serif;color:#172033;">
                    <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                           style="background:#f4f6fb;padding:32px 16px;">
                        <tr>
                            <td align="center">
                                <table role="presentation" width="100%%" cellspacing="0" cellpadding="0"
                                       style="max-width:560px;background:#ffffff;border:1px solid #e4e8f1;">
                                    <tr>
                                        <td style="padding:28px 32px;border-bottom:1px solid #e4e8f1;">
                                            <strong style="font-size:20px;color:#133c55;">Tadamun</strong>
                                            <div style="font-size:12px;color:#697386;margin-top:4px;">
                                                Business Solutions
                                            </div>
                                        </td>
                                    </tr>
                                    <tr>
                                        <td style="padding:32px;">
                                            <h1 style="margin:0 0 16px;font-size:24px;">
                                                Join %s
                                            </h1>
                                            <p style="line-height:1.6;color:#4d5870;">
                                                %s invited you to join this Tadamun workspace as
                                                <strong>%s</strong>.
                                            </p>
                                            <p style="margin:28px 0;">
                                                <a href="%s"
                                                   style="display:inline-block;background:#3154d8;color:#ffffff;
                                                          padding:13px 20px;text-decoration:none;font-weight:700;">
                                                    Accept invitation
                                                </a>
                                            </p>
                                            <p style="line-height:1.6;color:#697386;font-size:14px;">
                                                This invitation expires on %s and can only be used once.
                                                If you were not expecting it, you can ignore this email.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(
                safeOrganizationName,
                safeInviterName,
                safeRoleName,
                safeInvitationLink,
                safeExpiresAt
        );
    }

    private String plainValue(String value) {
        return value.replace('\r', ' ').replace('\n', ' ');
    }
}
