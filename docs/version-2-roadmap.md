# CRM Version 2 Roadmap

## Goal

Improve CRM security, reliability, and business usefulness after Version 1.

## Priority 1: Security Improvements

### Refresh Tokens

Add short-lived access tokens and longer-lived refresh tokens.

Benefit:

- Better security
- Better user experience
- Users do not need to login too often

### Login Rate Limiting

Limit repeated failed login attempts.

Benefit:

- Reduces brute-force attacks
- Protects admin accounts

### Password Reset

Allow users to reset forgotten passwords.

Benefit:

- Required for real business use
- Reduces admin support work

## Priority 2: Reliability Improvements

### Automated Backups

Schedule daily database backups.

Benefit:

- Reduces manual work
- Protects CRM data

### Test Database Isolation

Use a separate test database or Testcontainers.

Benefit:

- Tests become safer
- Tests do not depend on local CRM data

## Priority 3: Business Features

### Email Notifications

Notify users about assigned tasks or important updates.

Benefit:

- Better workflow
- More useful CRM

### Export Reports

Export customers, leads, tasks, and reports to CSV or Excel.

Benefit:

- Business users can analyze data outside the CRM

### Advanced Filters

Improve filtering for customers, leads, tasks, and audit logs.

Benefit:

- Better usability with large data

## Deferred To Version 3

- Multi-tenancy
- SaaS billing
- Public API
- Mobile app
- Workflow automation
- Advanced analytics
- AI features

## Recommended Build Order

1. Test database isolation
2. Login rate limiting
3. Refresh tokens
4. Password reset
5. Email notifications
6. Export reports
7. Automated backups
8. Advanced filterss