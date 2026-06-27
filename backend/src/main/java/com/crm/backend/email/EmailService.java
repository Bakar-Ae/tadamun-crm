package com.crm.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final PasswordResetEmailTemplate passwordResetTemplate;
    private final String fromName;
    private final boolean emailEnabled;
    private final String fromAddress;
    private final String replyTo;

    public EmailService(
            JavaMailSender mailSender,
            PasswordResetEmailTemplate passwordResetTemplate,
            @Value("${app.email.enabled:false}") boolean emailEnabled,
            @Value("${app.email.from-address}") String fromAddress,
            @Value("${app.email.from-name:Tadamun}") String fromName,
            @Value("${app.email.reply-to}") String replyTo
    ) {
        this.mailSender = mailSender;
        this.passwordResetTemplate = passwordResetTemplate;
        this.emailEnabled = emailEnabled;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.replyTo = replyTo;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        if (!emailEnabled) {
            log.info(
                    "Email delivery is disabled; password reset email was not sent to {}",
                    maskEmail(toEmail)
            );
            return;
        }

        try {
            mailSender.send(mimeMessage -> {
                MimeMessageHelper helper = new MimeMessageHelper(
                        mimeMessage,
                        true,
                        StandardCharsets.UTF_8.name()
                );

                helper.setFrom(fromAddress, fromName);
                helper.setReplyTo(replyTo);
                helper.setTo(toEmail);
                helper.setSubject(passwordResetTemplate.subject());
                helper.setText(
                        passwordResetTemplate.plainText(resetLink),
                        passwordResetTemplate.html(resetLink)
                );
            });

            log.info("Password reset email sent to {}", maskEmail(toEmail));
        } catch (MailException exception) {
            log.error(
                    "Password reset email delivery failed for {}",
                    maskEmail(toEmail),
                    exception
            );

            throw new IllegalStateException(
                    "Could not send password reset email"
            );
        }
    }

    private String maskEmail(String email) {
        int separatorIndex = email.indexOf('@');

        if (separatorIndex <= 0) {
            return "***";
        }

        return email.charAt(0) + "***" + email.substring(separatorIndex);
    }
    
}