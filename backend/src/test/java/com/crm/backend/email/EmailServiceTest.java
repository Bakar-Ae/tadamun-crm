package com.crm.backend.email;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailServiceTest {

    private JavaMailSender mailSender;
    private PasswordResetEmailTemplate template;
    private OrganizationInvitationEmailTemplate invitationTemplate;

    @BeforeEach
    void setUp() {
        mailSender = mock(JavaMailSender.class);
        template = mock(PasswordResetEmailTemplate.class);
        invitationTemplate = mock(OrganizationInvitationEmailTemplate.class);
    }

    @Test
    void disabledEmailShouldNotSendMessage() {
        EmailService emailService = createEmailService(false);

        emailService.sendPasswordResetEmail(
                "user@example.com",
                "https://example.com/reset?token=secret"
        );

        verify(mailSender, never())
                .send(any(MimeMessagePreparator.class));
    }

    @Test
    void disabledEmailShouldNotSendInvitation() {
        EmailService emailService = createEmailService(false);
        Logger logger = (Logger) LoggerFactory.getLogger(EmailService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            emailService.sendOrganizationInvitationEmail(
                    "user@example.com",
                    "Tadamun",
                    "System Administrator",
                    "Sales Rep",
                    "https://example.com/accept-invitation?token=secret",
                    java.time.LocalDateTime.now().plusHours(72)
            );
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        verify(mailSender, never())
                .send(any(MimeMessagePreparator.class));
        assertTrue(appender.list.stream().anyMatch(event ->
                event.getFormattedMessage().contains("u***@example.com")
        ));
        assertFalse(appender.list.stream().anyMatch(event ->
                event.getFormattedMessage().contains("secret")
        ));
    }

    @Test
    void enabledEmailShouldSendTextAndHtml() throws Exception {
        EmailService emailService = createEmailService(true);
        String resetLink = "https://example.com/reset?token=test-token";

        when(template.subject()).thenReturn("Reset your Tadamun password");
        when(template.plainText(resetLink)).thenReturn("Plain reset content");
        when(template.html(resetLink))
                .thenReturn("<html><body>HTML reset content</body></html>");

        ArgumentCaptor<MimeMessagePreparator> captor =
                ArgumentCaptor.forClass(MimeMessagePreparator.class);

        emailService.sendPasswordResetEmail("user@example.com", resetLink);

        verify(mailSender).send(captor.capture());

        MimeMessage message = new MimeMessage(
                Session.getInstance(new Properties())
        );

        captor.getValue().prepare(message);
        message.saveChanges();

        InternetAddress from = (InternetAddress) message.getFrom()[0];

        assertEquals("no-reply@test.local", from.getAddress());
        assertEquals("Tadamun Test", from.getPersonal());
        assertEquals("support@test.local", message.getReplyTo()[0].toString());
        assertEquals("Reset your Tadamun password", message.getSubject());
        assertEquals(
                "user@example.com",
                ((InternetAddress) message.getAllRecipients()[0]).getAddress()
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        message.writeTo(output);

        String rawMessage = output.toString(StandardCharsets.UTF_8);

        assertTrue(rawMessage.contains("text/plain"));
        assertTrue(rawMessage.contains("text/html"));
        assertTrue(rawMessage.contains("Plain reset content"));
        assertTrue(rawMessage.contains("HTML reset content"));
    }

    @Test
    void smtpFailureShouldReturnSafeApplicationError() {
        EmailService emailService = createEmailService(true);

        doThrow(new MailSendException("SMTP unavailable"))
                .when(mailSender)
                .send(any(MimeMessagePreparator.class));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> emailService.sendPasswordResetEmail(
                        "user@example.com",
                        "https://example.com/reset?token=secret"
                )
        );

        assertEquals(
                "Could not send password reset email",
                exception.getMessage()
        );
    }

    private EmailService createEmailService(boolean enabled) {
        return new EmailService(
                mailSender,
                template,
                invitationTemplate,
                enabled,
                "no-reply@test.local",
                "Tadamun Test",
                "support@test.local"
        );
    }
}
