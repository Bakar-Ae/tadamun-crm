# Phase 77 - Tenant Data Isolation

## Goal

Ensure organization-owned CRM data can only be created, queried, updated,
reported, searched, and audited inside the active organization.

## Completed Scope

- Added organization ownership to teams, customers, contacts, leads, tasks,
  notes, attachments, notifications, and audit logs.
- Replaced unscoped repository reads with organization-aware methods.
- Scoped dashboard totals, reports, global search, activity timelines, and
  audit-log queries to the current organization.
- Validated that assigned owners and assignees are active members of the
  current organization.
- Return the same `404 Not Found` response for missing and cross-organization
  records to avoid leaking whether another tenant's record exists.
- Added organization-aware unit, repository, migration, and endpoint tests.

## Audit And Notification Rules

- Tenant business events use audit scope `ORGANIZATION` and require an
  organization ID.
- Platform authentication/security events use audit scope `PLATFORM` and may
  have no organization ID.
- Tenant notifications carry an organization ID.
- Global account/security notifications may have no organization ID.

## Database Migration

Migration:

`backend/src/main/resources/db/migration/V21__finalize_tenant_data_isolation.sql`

V21 repairs transitional ownership values and makes `organization_id`
mandatory for:

- `teams`
- `customers`
- `contacts`
- `leads`
- `tasks`
- `notes`
- `attachments`

`notifications.organization_id` and `audit_logs.organization_id` remain
nullable for the platform-level cases documented above.

## Verification

Backend tests:

```powershell
cd backend
mvn clean test
```

Result: 104 tests, 0 failures, 0 errors.

Production package:

```powershell
mvn clean package -DskipTests
```

Result: build successful.

Docker:

```powershell
cd ..
docker compose up -d --build backend
Invoke-RestMethod http://localhost:8081/actuator/health
```

Result: backend healthy; database, liveness, and readiness are `UP`.

Live Flyway result: V21 `finalize tenant data isolation`, success `1`.

## Definition Of Done

- No core CRM repository path reads tenant records without an organization
  constraint.
- Aggregate screens and exports use the active organization.
- Cross-organization IDs do not reveal record existence.
- The fresh migration chain and the existing Docker database both reach V21.
- The full backend regression suite and production package build pass.
