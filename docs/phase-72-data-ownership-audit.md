# Phase 72 - Current Data Ownership Audit

## Status

Current Version 2 database ownership audited.

No database or application changes were made during this step.

## Sources Inspected

- Flyway migrations V1 through V15
- Current JPA entity structure
- Existing user, role, team, and permission relationships
- Existing CRM foreign-key relationships

## Current Table Count

Current application tables audited: 16

The Flyway schema-history table is managed by Flyway and is not counted as an
application table.

## Ownership Categories

### Platform-Level

Shared across the entire SaaS platform and not owned by one organization.

### Tenant-Owned

Belongs directly to one organization and requires `organization_id`.

### Membership-Scoped

Belongs to a user within one organization and should reference organization
membership.

### Hybrid

May represent either platform-level or organization-level information and
requires an explicit scope.

## Current Table Classification

| Current table | Target classification | Required Version 3 change |
| --- | --- | --- |
| `users` | Platform-level identity | Keep global identity fields. Move role and team assignments into organization membership. |
| `refresh_tokens` | Platform-level identity | Keep linked to global user. Do not add organization ownership. |
| `password_reset_tokens` | Platform-level identity | Keep linked to global user. Do not add organization ownership. |
| `permissions` | Platform-level catalogue | Keep global permission names and definitions. |
| `roles` | Platform-level role catalogue | Keep reusable role definitions initially. Assign roles through organization memberships. |
| `role_permissions` | Platform-level role catalogue | Keep permission mappings with role definitions unless custom organization roles are introduced later. |
| `teams` | Tenant-owned | Add required `organization_id`. Team names become unique within an organization. |
| `customers` | Tenant-owned | Add required `organization_id` and tenant-first indexes. |
| `contacts` | Tenant-owned | Add required `organization_id` and validate that the customer belongs to the same organization. |
| `leads` | Tenant-owned | Add required `organization_id`; assigned users must be active organization members. |
| `tasks` | Tenant-owned | Add required `organization_id`; linked customer, lead, and assignee must belong to the same organization. |
| `notes` | Tenant-owned | Add required `organization_id`; linked customer or lead and author membership must match the organization. |
| `attachments` | Tenant-owned | Add required `organization_id`; storage paths and parent records must match the organization. |
| `notifications` | Tenant-owned | Add required `organization_id`; recipient must have membership in that organization. |
| `notification_preferences` | Membership-scoped | Replace user-only ownership with organization membership ownership. |
| `audit_logs` | Hybrid | Add organization scope for tenant events and explicit platform scope for platform events. |

## Existing User Columns Requiring Migration

The current `users` table contains:

- `role_id`
- `team_id`

These fields only support one role and one team for each user.

A Version 3 user may belong to several organizations, so these assignments
must move into organization membership.

Target direction:

- Keep login identity in `users`
- Move organization role to `organization_memberships`
- Move organization team assignment to membership or a tenant-aware team-membership relationship
- Remove the old user-level role and team columns only after migration is proven safe

## New Foundation Tables

### `organizations`

Root tenant record.

Expected responsibilities:

- Organization identity
- Unique slug
- Lifecycle status
- Organization settings
- Creation and update timestamps

### `organization_memberships`

Connects global users to organizations.

Expected responsibilities:

- Organization
- User
- Organization role
- Membership status
- Team assignment where appropriate
- Joining and lifecycle timestamps

### `organization_invitations`

Stores pending invitations.

Expected responsibilities:

- Organization
- Invited email
- Intended role
- Secure token hash
- Expiration
- Invitation status
- Inviting user

## Direct Ownership Rule

Every tenant-owned business table will receive its own `organization_id`,
even when its parent record already contains organization ownership.

Reasons:

- Safer repository queries
- Faster tenant filtering
- Clear ownership
- Better indexing
- Easier cross-tenant tests
- Reduced risk from incorrect joins

## Relationship Consistency Rules

- A contact and its customer must share the same organization.
- A converted lead and customer must share the same organization.
- A task and its linked customer or lead must share the same organization.
- A task assignee must be an active member of the task organization.
- A note and its customer or lead must share the same organization.
- A note author must be a member of the note organization.
- An attachment and its parent must share the same organization.
- A notification recipient must belong to the notification organization.
- A team manager must belong to the team organization.

## Uniqueness Changes

The following uniqueness rules must become tenant-aware where applicable:

- Team name: unique by `(organization_id, name)`
- Customer business identifiers: unique within an organization if uniqueness is required
- Organization slug: globally unique
- Membership: unique by `(organization_id, user_id)`
- Notification preferences: one record per organization membership

Global login email may remain unique in `users`.

## Index Direction

Frequently queried tenant tables should use indexes beginning with
`organization_id`.

Examples:

- `(organization_id, status)`
- `(organization_id, created_at)`
- `(organization_id, email)`
- `(organization_id, assigned_to_user_id)`
- `(organization_id, customer_id)`
- `(organization_id, lead_id)`

Exact indexes will be selected using actual query patterns.

## Audit Log Scope

Tenant audit events require an organization ID.

Platform events may have no organization, but must use an explicit platform
scope. A null organization by itself must not silently imply platform access.

Examples of platform events:

- Organization creation
- Organization suspension
- Platform administrator support access
- Subscription-plan catalogue changes

## Migration Safety Direction

Ownership migration must be additive:

1. Create organizations.
2. Create a default organization.
3. Create memberships for existing users.
4. Add nullable ownership columns.
5. Backfill existing records.
6. Validate record counts and ownership.
7. Add indexes and constraints.
8. Make required ownership non-null.
9. Remove obsolete user role and team columns only in a later migration.

## Current Ownership Gaps

The existing Version 2 schema has no organization ownership.

Current global assumptions include:

- One global set of CRM records
- One role per user
- One team per user
- Globally shared role-permission assignments
- User-only notification preferences
- Audit records without tenant scope
- Attachment paths without organization grouping

These are expected Version 3 migration targets, not Version 2 defects.

## Open Design Questions For Phase 72.2

- Whether organization roles remain fixed templates or become customizable later
- Whether membership contains one team or uses a separate team-membership table
- How platform administrator identity is represented
- Exact audit-log platform-scope constraint
- Whether composite foreign keys should enforce organization consistency
- Exact organization lifecycle statuses