package com.crm.backend.email;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class EmailServiceTest {

    private JavaMailSender mailSender;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
    }

    @Test
    void disabledEmailShouldNotSendMessage() {
        EmailService emailService = new EmailService(
                mailSender,
                false,
                "no-reply@test.local",
                "support@test.local"
        );

        emailService.sendPasswordResetEmail(
                "user@example.com",
                "http://localhost/reset-password?token=secret"
        );

        verify(mailSender, never())
                .send(any(SimpleMailMessage.class));
    }

    @Test
    void enabledEmailShouldSendConfiguredMessage() {
        EmailService emailService = new EmailService(
                mailSender,
                true,
                "no-reply@test.local",
                "support@test.local"
        );

        String resetLink =
                "http://localhost/reset-password?token=test-token";

        emailService.sendPasswordResetEmail(
                "user@example.com",
                resetLink
        );

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);

        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage message = messageCaptor.getValue();

        assertEquals("no-reply@test.local", message.getFrom());
        assertEquals("support@test.local", message.getReplyTo());
        assertEquals("Reset your Tadamun password", message.getSubject());
        assertEquals("user@example.com", message.getTo()[0]);

        assertNotNull(message.getText());
        assertTrue(message.getText().contains(resetLink));
    }

    @Test
    void smtpFailureShouldReturnSafeApplicationError() {
        EmailService emailService = new EmailService(
                mailSender,
                true,
                "no-reply@test.local",
                "support@test.local"
        );

        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(any(SimpleMailMessage.class));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> emailService.sendPasswordResetEmail(
                        "user@example.com",
                        "http://localhost/reset-password?token=secret"
                )
        );

        assertEquals(
                "Could not send password reset email",
                exception.getMessage()
        );
    }
}