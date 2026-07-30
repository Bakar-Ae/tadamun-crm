# Phase 76 - Tenant Context Foundation

## Status

Implemented and validated.

## Purpose

Tenant context connects an authenticated global user to an active organization
membership for each tenant-owned API request.

The `X-Organization-Id` request header selects an organization but never grants
access by itself. The backend validates the organization and membership from the
database before creating the request context.

## Request Flow

1. `JwtAuthenticationFilter` validates global user identity.
2. `TenantResolutionFilter` checks whether the route is tenant-owned.
3. The filter reads and validates `X-Organization-Id` when supplied.
4. `TenantResolutionService` loads an active organization membership.
5. The service rejects suspended or archived organizations.
6. The filter installs membership role and permission authorities.
7. The request executes with an immutable `TenantContext`.
8. Tenant context is cleared in a `finally` block.

## Compatibility Rule

Until the frontend workspace selector is implemented in Phase 80, a user with
exactly one active membership is resolved automatically when the header is
missing.

Users with more than one active organization must supply
`X-Organization-Id`. This temporary compatibility behavior must be removed in
Phase 80.

## Context Contents

- Organization ID
- Membership ID
- User ID
- Membership role
- Membership data scope
- Team ID when available
- Membership permission set

The context never stores passwords, JWTs, refresh tokens, or other secrets.

## Route Categories

- Public and global account routes do not require tenant context.
- CRM data, dashboard, reporting, search, administration, notifications, and
  audit routes require tenant context.
- Platform routes remain separate and will receive explicit platform security
  later.

## Error Codes

- `INVALID_ORGANIZATION_ID` - malformed or non-positive header
- `TENANT_CONTEXT_REQUIRED` - multiple memberships without a selected tenant
- `ORGANIZATION_ACCESS_DENIED` - no active membership
- `ORGANIZATION_UNAVAILABLE` - suspended or archived organization

## Configuration

`TENANT_ENFORCEMENT_ENABLED=true` enables tenant request resolution.

The switch exists for controlled rollback. Production environments should keep
it enabled.

## Deferred to Phase 77

Phase 76 validates request membership but does not yet add organization
conditions to every CRM repository query. Repository and service data isolation,
cross-tenant `404` behavior, tenant-aware audit ownership, and final database
`NOT NULL` enforcement belong to Phase 77.

## Validation

- Tenant-focused tests: 14 passing
- Complete backend tests: 97 passing
- Flyway migrations V1 through V20 validated with MySQL 8.4 Testcontainers
