# Tadamun CRM Version 3 Scope

## Status

Planning approved. Implementation has not started.

## Version 3 Goal

Transform Tadamun CRM from a single-organization system into a secure,
scalable multi-tenant SaaS platform.

## Primary Users

- Platform administrators
- Organization owners
- Organization administrators
- Managers
- Sales representatives
- Support and operations staff

## Included Features

1. Multi-tenant organization and workspace model
2. Strong tenant data isolation
3. Organization memberships and invitations
4. Tenant-aware roles and permissions
5. Tenant onboarding and organization settings
6. Safe migration of existing Version 2 data
7. Subscription and billing foundation
8. Subscription plans, limits, and usage tracking
9. Public API and secure API keys
10. Outbound webhook delivery
11. Workflow automation foundation
12. Third-party integration foundation
13. Platform administration
14. Tenant-aware audit logs and reporting
15. Improved observability and operational monitoring
16. Multi-tenant security, performance, and regression testing

## Critical Business Rules

- Every business record must belong to one tenant.
- Users may belong to one or more organizations.
- A user must select an active organization when required.
- Tenant identity must come from trusted authentication context.
- Client-provided tenant IDs must never be trusted by themselves.
- Cross-tenant data access must be prevented and tested.
- Platform administrators must use explicit, audited access.
- Billing failure must not immediately delete customer data.
- API keys and webhook secrets must never be stored as plain text.
- Existing Version 2 data must migrate without being lost.

## Delivery Order

1. Tenant architecture and security
2. Organization membership and onboarding
3. Existing data migration
4. Tenant-aware administration
5. Billing and usage controls
6. Public API and webhooks
7. Workflow and integration foundation
8. Testing, deployment, and release hardening

## Out of Scope

The following are postponed to Version 4 or later:

- AI lead scoring
- Predictive analytics
- AI assistants and chatbots
- Native Android and iOS applications
- Full accounting or ERP functionality
- Large third-party integration marketplace
- Fully configurable no-code automation builder

## Completion Criteria

Version 3 is complete when:

- Multiple organizations can safely use the same deployment.
- Automated tests prove tenant data isolation.
- Organization membership and invitations work.
- Roles and permissions operate within each tenant.
- Existing Version 2 data is migrated safely.
- Subscription plans and usage limits work.
- Public API keys and webhook delivery are secured.
- Workflow automation has a stable foundation.
- Monitoring, backups, documentation, and deployment checks pass.
- Version 3 is deployed, validated, documented, and tagged.