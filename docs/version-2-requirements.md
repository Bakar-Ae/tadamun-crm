# Tadamun CRM Version 2 Requirements

## Goal

Expand Version 1 with advanced enterprise productivity, reporting,
communication, and administration features.

## Included Features

1. Advanced roles and permissions
2. Permission management UI
3. Production email infrastructure
4. Email templates
5. Notification preferences
6. Customer and lead file attachments
7. Calendar and task scheduling
8. Advanced reports
9. Excel and PDF exports
10. Team performance dashboard
11. Unified activity timeline
12. Global search
13. Expanded automated testing
14. CI/CD pipeline
15. Deployment hardening

## Primary Users

- Administrators
- Managers
- Sales representatives
- Support and operations staff

## Important Business Rules

- Administrators manage permissions.
- Managers access information for their teams.
- Users only perform explicitly permitted actions.
- Attachments require type and size validation.
- Reports respect user permissions.
- Sensitive actions create audit logs.
- Email and notification preferences are respected.

## Quality Requirements

- Responsive frontend
- Pagination for large datasets
- Secure file handling
- Clear loading and error states
- Automated backend and frontend tests
- No secrets committed to Git
- Production-ready Docker configuration

## Out of Scope

- Multi-tenancy
- SaaS billing
- Mobile applications
- AI features
- Public API
- Advanced workflow automation
- Third-party CRM integrations

## Completion Criteria

Version 2 is complete when all included features are implemented,
tested, documented, secured, and successfully deployed.