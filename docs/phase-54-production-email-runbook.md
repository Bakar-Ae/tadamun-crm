# Tadamun Production Email Runbook

## Recommended Provider

Amazon Simple Email Service (SES)

The application uses standard SMTP and is not locked to AWS.

## Required Company Information

- Approved company domain
- DNS administration access
- Sender address
- Reply-to support address
- AWS account and approved region

## Domain Security

Before production delivery:

1. Verify the sending domain.
2. Enable DKIM.
3. Configure SPF.
4. Configure DMARC.
5. Confirm the From address uses the verified domain.

## Amazon SES Setup

1. Create a domain identity in Amazon SES.
2. Publish the provided DNS records.
3. Wait for domain and DKIM verification.
4. Create region-specific SES SMTP credentials.
5. Request production access to leave the SES sandbox.
6. Store credentials only in the production environment.

## Production Environment Example

MAIL_HOST=email-smtp.REGION.amazonaws.com
MAIL_PORT=587
MAIL_USERNAME=replace_with_ses_smtp_username
MAIL_PASSWORD=replace_with_ses_smtp_password
MAIL_FROM_ADDRESS=no-reply@approved-company-domain.com
MAIL_FROM_NAME=Tadamun
MAIL_REPLY_TO=support@approved-company-domain.com
MAIL_SMTP_AUTH=true
MAIL_SMTP_STARTTLS=true
MAIL_CONNECTION_TIMEOUT_MS=5000
MAIL_TIMEOUT_MS=5000
MAIL_WRITE_TIMEOUT_MS=5000
APP_EMAIL_ENABLED=true
MANAGEMENT_HEALTH_MAIL_ENABLED=true

## Security Rules

- Never commit the production environment file.
- SES SMTP credentials are different from normal AWS access keys.
- Credentials are specific to an AWS region.
- Never log email tokens or password-reset links.
- Rotate credentials after suspected exposure.
- Restrict production SMTP credentials to email sending.

## Deployment Validation

1. Confirm the backend health endpoint is UP.
2. Send a reset email to an approved test account.
3. Verify From, Reply-To, subject, and reset link.
4. Confirm SPF, DKIM, and DMARC pass.
5. Confirm backend logs contain no reset tokens.
6. Confirm failed delivery creates an internal error log.

## Local Development

Continue using Mailpit:

- Inbox: http://localhost:8025
- SMTP host: mailpit
- SMTP port: 1025
- Authentication: false
- STARTTLS: false

Mailpit must never be publicly deployed as the production email provider.