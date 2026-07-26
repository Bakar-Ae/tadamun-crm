# Tadamun CRM Version 3 Architecture Decisions

## Status

Initial decisions approved for Version 3 planning.

These decisions may be refined during Phase 72, but changes must be
documented before implementation.

## Terminology

- Tenant: the technical isolation boundary.
- Organization: the business-facing name shown to users.
- Workspace: the frontend representation of an active organization.
- Platform administrator: an operator managing the entire SaaS platform.
- Organization member: a user who belongs to an organization.

## ADR-001 - Tenancy Storage Model

### Decision

Use a shared MySQL database and shared schema.

Tenant-owned tables will include a required `organization_id` foreign key.

### Reason

- Fits the existing MySQL and JPA architecture.
- Avoids operating a separate database for every customer.
- Supports efficient migrations and deployment.
- Is appropriate for the expected early SaaS scale.
- Allows the current Version 2 database to be migrated gradually.

### Rejected Alternatives

- Database per tenant: too operationally expensive for the current stage.
- Schema per tenant: difficult to manage with MySQL and Flyway at scale.

## ADR-002 - Organization as Tenant Root

### Decision

Create an `organizations` table as the root of tenant ownership.

Every tenant-owned record must ultimately belong to one organization.

### Initial Organization Fields

- `id`
- `name`
- `slug`
- `status`
- `created_at`
- `updated_at`

### Rules

- Organization slugs must be unique.
- Deactivated organizations retain their data.
- Deleting organizations permanently is not part of the normal user flow.
- Organization lifecycle changes must be audited.

## ADR-003 - Global User Identity

### Decision

Keep `users` as global login identities.

Connect users to organizations through an
`organization_memberships` table.

### Reason

A user may belong to more than one organization while keeping one login.

### Membership Responsibilities

- Organization
- User
- Organization role
- Membership status
- Invitation and joining dates
- Created and updated timestamps

## ADR-004 - Active Organization Resolution

### Decision

The frontend may request an active organization using an
`X-Organization-Id` header.

The backend must validate that:

1. The request is authenticated.
2. The organization exists and is active.
3. The authenticated user has an active membership.
4. The requested operation is permitted in that organization.

Only after validation may the backend create the request-scoped tenant context.

### Security Rule

A client-provided organization ID is a request, not proof of access.

The backend must never use it without membership validation.

## ADR-005 - Tenant Context

### Decision

Use a request-scoped tenant context that stores the validated organization ID.

The context must:

- Be created after authentication.
- Be available to tenant-aware services.
- Reject missing tenant context for tenant-owned operations.
- Be cleared when request processing finishes.
- Never leak between requests or background jobs.

## ADR-006 - Data Isolation Enforcement

### Decision

Use explicit organization-scoped repository methods and service validation.

Examples:

- Find customer by `id` and `organizationId`
- Search leads by `organizationId`
- List tasks by `organizationId`
- Load attachments by entity and `organizationId`

### Rule

Generic `findById(id)` must not be used for tenant-owned business operations.

Hibernate filters may provide additional protection later, but they must not
be the only isolation mechanism.

## ADR-007 - Tenant-Owned Data

The following modules will become tenant-owned:

- Customers
- Leads
- Contacts
- Tasks
- Notes
- Attachments
- Audit logs
- Notifications
- Reports and activity records
- Workflow definitions
- Webhook subscriptions
- API keys
- Billing and usage records

## ADR-008 - Platform-Level Data

The following may remain platform-level:

- Global user login identity
- Password reset tokens
- Global permission catalogue
- Platform configuration
- Supported subscription plan definitions

Tenant-specific assignments and settings must still belong to an organization.

## ADR-009 - Database Constraints

Tenant-owned unique constraints must include `organization_id` where suitable.

Example:

A customer email may be unique within one organization without being globally
unique across all organizations.

Indexes should begin with `organization_id` for frequently queried tenant data.

## ADR-010 - Existing Data Migration

### Decision

Create one default organization for all existing Version 2 data.

Migration order:

1. Create the organizations table.
2. Create the default Tadamun organization.
3. Create memberships for existing users.
4. Add nullable organization foreign keys.
5. Backfill existing records.
6. Validate that no tenant-owned record is missing ownership.
7. Make organization ownership non-null.
8. Add tenant-aware indexes and constraints.

Each migration must be tested against a restored production-style backup.

## ADR-011 - Attachment Isolation

### Decision

Attachment metadata must include organization ownership.

Stored files should use an organization-based path structure:

`organizations/{organizationId}/attachments/{storedFileName}`

Download and delete operations must validate both membership and ownership.

## ADR-012 - Background Processing

### Decision

Scheduled jobs, webhook deliveries, emails, and workflow executions must receive
an explicit organization ID.

Background processing must never depend on a leftover web-request tenant context.

## ADR-013 - Platform Administrator Access

### Decision

Platform administrators will use separate explicit permissions.

Platform access must:

- Never happen silently
- Require a stated support or administration purpose
- Produce an audit event
- Avoid normal organization-user endpoints where possible

## ADR-014 - Delivery Strategy

Multi-tenancy will be introduced gradually behind tested migration steps.

Existing Version 2 functionality must continue working while the default
organization represents the original CRM workspace.

No billing, public API, or workflow implementation begins until tenant
isolation tests pass.