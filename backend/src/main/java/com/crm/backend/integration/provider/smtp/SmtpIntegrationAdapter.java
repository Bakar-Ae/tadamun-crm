package com.crm.backend.integration.provider.smtp;

import com.crm.backend.integration.IntegrationProvider;
import com.crm.backend.integration.provider.IntegrationCapability;
import com.crm.backend.integration.provider.IntegrationDeliveryType;
import com.crm.backend.integration.provider.IntegrationOutboundMessage;
import com.crm.backend.integration.provider.IntegrationProviderAdapter;
import com.crm.backend.integration.provider.IntegrationProviderContext;
import com.crm.backend.integration.provider.IntegrationProviderDeliveryResult;
import com.crm.backend.integration.provider.IntegrationProviderException;
import com.crm.backend.integration.provider.IntegrationSecrets;
import com.crm.backend.integration.provider.IntegrationVerificationResult;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;

@Component
public class SmtpIntegrationAdapter implements IntegrationProviderAdapter {

    @Override
    public IntegrationProvider provider() {
        return IntegrationProvider.SMTP;
    }

    @Override
    public Set<IntegrationCapability> capabilities() {
        return Set.of(
                IntegrationCapability.SEND_EMAIL,
                IntegrationCapability.TEST_CONNECTION
        );
    }

    @Override
    public void validateConfiguration(Map<String, Object> configuration) {
        String host = requiredText(configuration, "host", 255);
        if (host.contains(":") || host.contains("/") || host.contains("@")) {
            throw new IllegalArgumentException("SMTP host is invalid");
        }
        int port = integer(configuration, "port");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("SMTP port is invalid");
        }
        validateEmail(requiredText(configuration, "fromAddress", 320));
        optionalText(configuration, "fromName", 100);
        String replyTo = optionalText(configuration, "replyTo", 320);
        if (replyTo != null) {
            validateEmail(replyTo);
        }
        bool(configuration, "smtpAuth", true);
        bool(configuration, "startTls", true);
    }

    @Override
    public void validateCredentials(IntegrationSecrets secrets) {
        secrets.optional("username").ifPresent(value -> {
            if (value.length() > 320) {
                throw new IllegalArgumentException("SMTP username is too long");
            }
        });
        secrets.optional("password").ifPresent(value -> {
            if (value.length() > 4096) {
                throw new IllegalArgumentException("SMTP password is too long");
            }
        });
    }

    @Override
    public IntegrationVerificationResult verify(
            IntegrationProviderContext context
    ) {
        validateConfiguration(context.configuration());
        validateCredentials(context.secrets());
        try {
            sender(context).testConnection();
            return IntegrationVerificationResult.success(
                    requiredText(
                            context.configuration(),
                            "fromAddress",
                            320
                    )
            );
        } catch (Exception exception) {
            throw new IntegrationProviderException(
                    "SMTP_CONNECTION_FAILED",
                    "SMTP connection verification failed",
                    true,
                    exception
            );
        }
    }

    @Override
    public IntegrationProviderDeliveryResult deliver(
            IntegrationProviderContext context,
            IntegrationOutboundMessage message
    ) {
        if (message.type() != IntegrationDeliveryType.EMAIL) {
            throw new IllegalArgumentException(
                    "SMTP connections only support email delivery"
            );
        }
        validateConfiguration(context.configuration());
        validateCredentials(context.secrets());
        validateEmail(message.destination());
        if (message.subject() == null
                || message.subject().isBlank()
                || message.subject().length() > 200) {
            throw new IllegalArgumentException(
                    "Email subject must contain 1 to 200 characters"
            );
        }
        if (message.body().isBlank() || message.body().length() > 50_000) {
            throw new IllegalArgumentException(
                    "Email body must contain 1 to 50000 characters"
            );
        }

        try {
            JavaMailSenderImpl sender = sender(context);
            sender.send(mimeMessage -> {
                MimeMessageHelper helper = new MimeMessageHelper(
                        mimeMessage,
                        false,
                        StandardCharsets.UTF_8.name()
                );
                String fromAddress = requiredText(
                        context.configuration(),
                        "fromAddress",
                        320
                );
                String fromName = optionalText(
                        context.configuration(),
                        "fromName",
                        100
                );
                if (fromName == null) {
                    helper.setFrom(fromAddress);
                } else {
                    helper.setFrom(fromAddress, fromName);
                }
                String replyTo = optionalText(
                        context.configuration(),
                        "replyTo",
                        320
                );
                if (replyTo != null) {
                    helper.setReplyTo(replyTo);
                }
                helper.setTo(message.destination());
                helper.setSubject(message.subject());
                helper.setText(message.body(), false);
            });
            return new IntegrationProviderDeliveryResult(
                    "smtp_" + UUID.randomUUID().toString().replace("-", "")
            );
        } catch (MailException exception) {
            throw new IntegrationProviderException(
                    "SMTP_SEND_FAILED",
                    "SMTP email delivery failed",
                    true,
                    exception
            );
        }
    }

    private JavaMailSenderImpl sender(IntegrationProviderContext context) {
        Map<String, Object> configuration = context.configuration();
        boolean smtpAuth = bool(configuration, "smtpAuth", true);
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(requiredText(configuration, "host", 255));
        sender.setPort(integer(configuration, "port"));
        if (smtpAuth) {
            sender.setUsername(context.secrets().require("username"));
            sender.setPassword(context.secrets().require("password"));
        }
        Properties properties = sender.getJavaMailProperties();
        properties.setProperty("mail.smtp.auth", Boolean.toString(smtpAuth));
        properties.setProperty(
                "mail.smtp.starttls.enable",
                Boolean.toString(bool(configuration, "startTls", true))
        );
        properties.setProperty("mail.smtp.connectiontimeout", "5000");
        properties.setProperty("mail.smtp.timeout", "10000");
        properties.setProperty("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private String requiredText(
            Map<String, Object> configuration,
            String field,
            int maxLength
    ) {
        String value = optionalText(configuration, field, maxLength);
        if (value == null) {
            throw new IllegalArgumentException(
                    "Integration configuration requires " + field
            );
        }
        return value;
    }

    private String optionalText(
            Map<String, Object> configuration,
            String field,
            int maxLength
    ) {
        Object raw = configuration.get(field);
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof String text)) {
            throw new IllegalArgumentException(field + " must be text");
        }
        String value = text.trim();
        if (value.isEmpty()) {
            return null;
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " is too long");
        }
        return value;
    }

    private int integer(Map<String, Object> configuration, String field) {
        Object raw = configuration.get(field);
        if (!(raw instanceof Number number)) {
            throw new IllegalArgumentException(field + " must be a number");
        }
        return number.intValue();
    }

    private boolean bool(
            Map<String, Object> configuration,
            String field,
            boolean defaultValue
    ) {
        Object raw = configuration.get(field);
        if (raw == null) {
            return defaultValue;
        }
        if (!(raw instanceof Boolean value)) {
            throw new IllegalArgumentException(field + " must be true or false");
        }
        return value;
    }

    private void validateEmail(String value) {
        try {
            InternetAddress address = new InternetAddress(value, true);
            address.validate();
        } catch (AddressException exception) {
            throw new IllegalArgumentException("Email address is invalid");
        }
    }
}
