package com.crm.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final boolean emailEnabled;
    private final String fromAddress;
    private final String replyTo;

    public EmailService(
            JavaMailSender mailSender,
            @Value("${app.email.enabled:false}") boolean emailEnabled,
            @Value("${app.email.from-address}") String fromAddress,
            @Value("${app.email.reply-to}") String replyTo
    ) {
        this.mailSender = mailSender;
        this.emailEnabled = emailEnabled;
        this.fromAddress = fromAddress;
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

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setReplyTo(replyTo);
        message.setTo(toEmail);
        message.setSubject("Reset your Tadamun password");
        message.setText("""
                Hello,

                We received a request to reset your Tadamun password.

                Reset your password using this link:
                %s

                If you did not request this change, you can ignore this email.
                """.formatted(resetLink));

        try {
            mailSender.send(message);
            log.info(
                    "Password reset email sent to {}",
                    maskEmail(toEmail)
            );
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