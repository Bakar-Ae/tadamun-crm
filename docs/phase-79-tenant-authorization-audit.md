# Phase 79 - Tenant Authorization Audit

## Scope

This checkpoint covers Phase 79.1 through Phase 79.7:

- Existing authorization-flow audit
- Membership-based role resolution
- Tenant-scoped permission resolution
- Organization administrator rules
- Platform-administrator separation
- Cross-organization and authority-isolation tests
- Full regression and live migration verification

Phase 79 is complete.

## Findings

The Phase 76 tenant filter already replaced request authorities with the active
organization membership role. Three gaps remained:

1. `DataScopeService` still read the legacy role and team from `users`.
2. The global `PERMISSION_MANAGE` authority could enter tenant authorization.
3. Invitation endpoints used legacy `USER_*` permissions and did not protect
   ownership or administrator assignment.

Global users remain identities rather than tenant-owned records. The legacy
`/api/v1/users` management surface must be replaced by organization membership
administration in Phase 81; it must not become the platform-admin API.

## Implemented Rules

- The active membership role is authoritative for tenant role, permissions,
  and data scope.
- Global user role claims do not elevate a tenant request.
- `PERMISSION_MANAGE` is excluded from tenant authorities because standard role
  templates are platform-level catalogue data.
- `OWNER` is a distinct organization role.
- Existing organization creators are promoted to `OWNER` by migration V23.
- Owners may assign administrators and operational roles.
- Administrators may assign operational roles but cannot assign another admin.
- Ownership cannot be granted through a normal invitation; a dedicated audited
  ownership-transfer workflow is required later.
- Invitation endpoints use `MEMBERSHIP_VIEW`, `MEMBERSHIP_INVITE`, and
  `MEMBERSHIP_UPDATE` rather than legacy user-management permissions.
- Platform administration is granted only by an active row in
  `platform_administrators`; neither `OWNER` nor the legacy global `ADMIN` role
  implies platform access.
- Platform endpoints require the dedicated `PLATFORM_ADMIN` authority and live
  outside tenant route resolution.
- Tenant requests replace global authorities with the selected membership's
  authorities, so platform status never bypasses organization membership.
- V24 bootstraps one existing active legacy administrator as the initial
  platform administrator. Future grants and revocations must use an explicit,
  audited platform workflow.

## Permission Catalogue

V23 adds:

- `ORGANIZATION_VIEW`
- `ORGANIZATION_UPDATE`
- `MEMBERSHIP_VIEW`
- `MEMBERSHIP_INVITE`
- `MEMBERSHIP_UPDATE`
- `MEMBERSHIP_DEACTIVATE`
- `TEAM_MANAGE`

The catalogue remains global while standard roles are assigned per membership.
Custom organization roles remain out of Version 3 scope.

## Platform Boundary

`/api/v1/platform/**` is the reserved platform-control surface. It requires
`PLATFORM_ADMIN` both in the HTTP security rules and at controller method
security. Organization administration remains under tenant-aware endpoints and
uses `OWNER` or membership permissions instead.

## Verification

- Focused authorization suite: 48 tests passed.
- Full backend suite: 138 tests passed.
- Frontend suite: 10 tests passed; lint and production build passed.
- Testcontainers validated all migrations through V24 on MySQL 8.4.
- The local database migrated from V22 to V24 successfully after a fresh
  ignored backup was created.
- All four Docker services reported healthy.
- A live login confirmed both the explicit platform endpoint and a
  tenant-resolved customer request.
