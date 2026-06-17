package com.crm.backend.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("CRM Password Reset");
        message.setText("""
                Hello,

                We received a request to reset your CRM password.

                Reset your password using this link:
                %s

                If you did not request this, you can ignore this email.
                """.formatted(resetLink));

        try {
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException exception) {
            log.error("Failed to send password reset email to {}", toEmail, exception);
            throw new IllegalStateException("Could not send password reset email");
        }
    }
}