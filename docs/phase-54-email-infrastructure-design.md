# Phase 54 Email Infrastructure Design

## Purpose

Provide secure transactional email delivery for Tadamun CRM.

## Environments

### Local Development

Use Mailpit in Docker.

- SMTP host: mailpit
- SMTP port: 1025
- Inbox UI: http://localhost:8025
- Authentication: disabled
- TLS: disabled

### Production

Use a verified SMTP provider.

- Authentication required
- TLS required
- Credentials stored in environment variables
- Sender domain must have SPF, DKIM, and DMARC

## Required Configuration

- MAIL_HOST
- MAIL_PORT
- MAIL_USERNAME
- MAIL_PASSWORD
- MAIL_FROM_ADDRESS
- MAIL_FROM_NAME
- MAIL_REPLY_TO
- MAIL_SMTP_AUTH
- MAIL_SMTP_STARTTLS
- APP_EMAIL_ENABLED

## Security Rules

- Never log password-reset links or tokens.
- Never commit SMTP credentials.
- Never reveal whether an email address exists.
- Never use personal Gmail credentials in production.
- Use a dedicated transactional sender address.

## Initial Sender Identity

Name: Tadamun
Address: no-reply@company-domain.example
Reply-to: support@company-domain.example

These placeholders must be replaced after a company domain is approved.

## Delivery Strategy

Version 2 begins with synchronous SMTP delivery and strict timeouts.

A durable email outbox and queue can be introduced later if delivery volume
or reliability requirements increase.