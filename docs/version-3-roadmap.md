# Tadamun CRM Version 3 Roadmap

## Version 3 Objective

Transform Tadamun CRM into a secure multi-tenant SaaS platform while
protecting all existing Version 2 functionality and data.

## Phase 71 - Version 3 Planning

Goal:

Define Version 3 scope, architecture direction, risks, and delivery order.

Outputs:

- Version 3 scope
- Version 3 roadmap
- Initial architecture decisions
- Version 3 risk register

## Phase 72 - Multi-Tenant Architecture Design

Goal:

Select the tenant storage and request-isolation strategy.

Outputs:

- Tenant architecture decision record
- Tenant lifecycle definition
- Tenant resolution rules
- Data ownership rules
- Threat model

## Phase 73 - Organization Domain Foundation

Goal:

Introduce the organization or tenant domain without changing existing CRM behavior.

Outputs:

- Organization entity
- Organization repository and service
- Organization status model
- Flyway migration
- Organization tests

## Phase 74 - Tenant Context Foundation

Goal:

Resolve the active tenant safely for every authenticated request.

Outputs:

- Tenant context
- Tenant request resolver
- Tenant-aware authentication claims
- Missing-tenant rejection
- Context cleanup tests

## Phase 75 - Tenant Data Isolation

Goal:

Prevent records belonging to one tenant from being accessed by another tenant.

Outputs:

- Tenant ownership on CRM entities
- Tenant-scoped repository queries
- Service-layer ownership validation
- Cross-tenant access tests
- Audited platform access rules

## Phase 76 - Existing Data Migration

Goal:

Move existing Version 2 data into a default organization without data loss.

Outputs:

- Default organization
- Existing-user memberships
- Existing-record tenant ownership
- Safe Flyway backfill migration
- Migration validation and rollback plan

## Phase 77 - Organization Memberships

Goal:

Allow users to belong to organizations with controlled membership status.

Outputs:

- Organization membership entity
- Membership roles and statuses
- Membership API
- Membership lifecycle rules
- Membership tests

## Phase 78 - Organization Invitations

Goal:

Allow authorized administrators to invite users into an organization.

Outputs:

- Secure invitation tokens
- Invitation expiration and revocation
- Invitation email
- Accept-invitation flow
- Invitation audit logs

## Phase 79 - Tenant-Aware Roles and Permissions

Goal:

Apply roles and permissions inside each organization.

Outputs:

- Membership-based roles
- Tenant-scoped permissions
- Organization administrator rules
- Platform administrator separation
- Permission isolation tests

## Phase 80 - Workspace Selection Frontend

Goal:

Allow users to select and switch between organizations safely.

Outputs:

- Workspace selector
- Active-workspace state
- Tenant-aware API requests
- Protected workspace routes
- Workspace switching tests

## Phase 81 - Organization Administration

Goal:

Provide organization settings and membership administration.

Outputs:

- Organization settings page
- Member list
- Invite and deactivate actions
- Role assignment
- Organization audit history

## Phase 82 - Subscription Domain Foundation

Goal:

Model subscription plans independently from a payment provider.

Outputs:

- Subscription plan model
- Subscription status model
- Plan feature definitions
- Trial and grace-period rules
- Billing audit events

## Phase 83 - Billing Provider Integration

Goal:

Connect secure subscription payments through backend-controlled billing.

Outputs:

- Provider customer mapping
- Checkout session endpoint
- Billing portal endpoint
- Signed billing webhooks
- Subscription synchronization
- Billing integration tests

## Phase 84 - Usage Metering and Limits

Goal:

Measure tenant usage and enforce subscription limits.

Outputs:

- User and storage usage tracking
- Plan limit enforcement
- Usage summary API
- Limit warning notifications
- Upgrade-required responses

## Phase 85 - Public API Foundation

Goal:

Expose a secure, versioned API for approved integrations.

Outputs:

- Public API versioning
- API key management
- Hashed API key storage
- API scopes
- Rate limiting
- API documentation

## Phase 86 - Webhook Delivery System

Goal:

Notify external systems about important CRM events.

Outputs:

- Webhook subscriptions
- Signed webhook payloads
- Delivery retries
- Delivery history
- Failed-delivery handling
- Webhook security tests

## Phase 87 - Workflow Automation Foundation

Goal:

Allow CRM events to trigger controlled automated actions.

Outputs:

- Workflow definition model
- Trigger and action model
- Background execution
- Execution history
- Retry and failure rules
- Safety limits

## Phase 88 - Integration Foundation

Goal:

Prepare reusable architecture for external communication providers.

Outputs:

- Integration connection model
- Encrypted credentials
- Provider adapter interface
- WhatsApp integration foundation
- Email integration improvements
- Integration audit logs

## Phase 89 - SaaS Operations and Testing

Goal:

Prove tenant safety, reliability, and performance before release.

Outputs:

- Cross-tenant security test suite
- End-to-end organization tests
- Billing and webhook tests
- Load and performance tests
- Tenant-aware monitoring
- Scheduled backup verification
- Disaster-recovery exercise

## Phase 90 - Version 3 Release

Goal:

Validate, document, deploy, and close Version 3.

Outputs:

- Production migration rehearsal
- Final security review
- Deployment validation
- Version 3 documentation
- Release summary
- Git release tag `v3.0.0`

## Delivery Rules

- Tenant isolation must be completed before billing.
- Existing Version 2 behavior must remain functional.
- Every tenant-owned query must include tenant scope.
- Every cross-tenant security rule must have automated tests.
- Database migrations must support existing production data.
- Billing secrets and API keys must never reach the frontend.
- Each stable phase should be tested and committed separately.
- Version 4 features must not enter Version 3 accidentally.

## Version 4 Preview

Potential Version 4 features:

- AI-assisted lead scoring
- AI customer insights
- Predictive sales analytics
- CRM assistant and chatbot
- Native mobile applications
- Advanced no-code workflow builder
- Large integration marketplace