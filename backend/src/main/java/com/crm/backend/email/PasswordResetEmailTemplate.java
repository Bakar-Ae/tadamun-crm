package com.crm.backend.email;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class PasswordResetEmailTemplate {

    public String subject() {
        return "Reset your Tadamun password";
    }

    public String plainText(String resetLink) {
        return """
                Reset your Tadamun password

                We received a request to reset your password.

                Use this secure link:
                %s

                This link expires soon and can only be used once.

                If you did not request this change, ignore this email.
                """.formatted(resetLink);
    }

    public String html(String resetLink) {
        String safeResetLink = HtmlUtils.htmlEscape(resetLink);

        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width">
                    <title>Reset your password</title>
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
                                                Reset your password
                                            </h1>
                                            <p style="line-height:1.6;color:#4d5870;">
                                                We received a request to reset your Tadamun password.
                                            </p>
                                            <p style="margin:28px 0;">
                                                <a href="%s"
                                                   style="display:inline-block;background:#3154d8;color:#ffffff;
                                                          padding:13px 20px;text-decoration:none;font-weight:700;">
                                                    Reset password
                                                </a>
                                            </p>
                                            <p style="line-height:1.6;color:#697386;font-size:14px;">
                                                This link expires soon and can only be used once.
                                                If you did not request this change, ignore this email.
                                            </p>
                                        </td>
                                    </tr>
                                </table>
                            </td>
                        </tr>
                    </table>
                </body>
                </html>
                """.formatted(safeResetLink);
    }
}