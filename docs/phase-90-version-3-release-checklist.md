# Phase 90 - Version 3 Release Checklist

## Status

Release candidate prepared. Local validation and the final security review
passed. Public deployment validation is blocked: Railway shows an expired trial
and all three project services offline; the documented frontend and backend
domains return HTTP 404. Do not create or push
the `v3.0.0` tag until both public services are restored and revalidated.

## Release Identity

- Release version: `3.0.0`
- Branch: `main`
- Expected Git tag: `v3.0.0`
- Flyway target: V35
- Backend Maven version: `3.0.0`
- Frontend package version: `3.0.0`
- Actuator application version: `3.0.0`

## Migration Rehearsal

Status: PASS

The current backend image was started against a disposable MySQL database
restored from `backups/crm_db_phase_69.sql`. The script automatically stages
historical UTF-16 PowerShell dumps as temporary UTF-8 SQL without changing the
source backup.

Results from 2026-09-13:

- Source Flyway version: V15
- Target Flyway version: V35
- Applied migrations: 20
- Failed migrations: 0
- Preserved core table counts: 7 of 7
- Unowned core tenant records: 0
- Default organizations: 1
- Organization memberships: 3
- Disposable containers and network removed after validation

Run the rehearsal again with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass `
  -File .\scripts\Invoke-CrmMigrationRehearsal.ps1
```

Machine-readable evidence is written under `logs/migration-rehearsal/`, which
is ignored by Git.

## Final Security Review

Status: PASS

- Tenant identity is resolved from authenticated membership context.
- Tenant-owned core records require organization ownership.
- The live local database has zero unowned teams, customers, contacts, leads,
  tasks, notes, or attachments.
- Tenant boundary and customer repository isolation tests pass.
- Organization membership, invitation, role, and permission tests pass.
- Public API keys store an HMAC-SHA-256 hash, public ID, and display prefix;
  raw API keys are not persisted.
- Public API authentication, scope, rate-limit, and key repository tests pass.
- Webhook secrets use versioned AES-GCM encrypted fields.
- Webhook endpoint validation, signing, encryption, retry, authorization, and
  persistence tests pass.
- Integration credentials use versioned AES-GCM encryption and are excluded
  from API responses.
- Attachment listing, download, and deletion queries require organization and
  data-scope checks.
- Stripe webhook input is verified by the billing webhook service.
- Platform and actuator metrics routes require platform-admin authority.
- `.env` is ignored and not tracked.
- Backup SQL files are ignored and not tracked.
- High-confidence tracked-secret scan found zero matches.
- Verified backup restoration is automated and scheduled locally.

Release-blocker review:

- Known cross-tenant exposure: none found
- Failed tenant-isolation tests: none
- Unowned tenant records: none
- Unverified production-style migration: resolved by rehearsal
- Plain-text API or webhook secrets: none found
- Unprotected tenant attachment access: none found
- Missing backup/restore validation: resolved in Phase 89

## Automated Validation

Status: PASS

- Backend: 315 tests, 0 failures, 0 errors, 0 skipped
- Frontend: 2 test files, 10 tests passed
- Frontend ESLint: pass
- Frontend TypeScript and Vite production build: pass
- Frontend dependency audit: 0 known vulnerabilities
- Backend production Docker image: build pass with `backend-3.0.0.jar`
- Frontend production Docker image: build pass with a clean `npm ci`
- Local Docker services: frontend, backend, MySQL, and Mailpit healthy
- Local backend health: `UP`
- Local actuator application version: `3.0.0`
- Local frontend response: HTTP 200
- Local Flyway state: V35, 0 failed migrations

Expected warnings that did not fail validation:

- Flyway warns that MySQL 8.4 is newer than its latest verified MySQL version.
- Mockito warns about future JDK dynamic-agent behavior.
- One Spring Data test path warns about direct `PageImpl` serialization.

## Public Deployment Validation

Status: BLOCKED

Checked on 2026-09-13:

- `https://tadamun-crm-web.up.railway.app/` returned HTTP 404.
- `https://tadamun-crm-production.up.railway.app/actuator/health` returned HTTP
  404 with `Application not found`.

Dashboard confirmed from user-provided screenshots on 2026-09-14:

- Workspace: `bakar-ae's Projects`
- Project: `sparkling-simplicity`
- Project ID: `e011fbf7-05a2-4bf6-82cf-d43c15ae70dc`
- Environment: `production`
- Services: `MySQL`, `tadamun-crm`, and `pleasant-learning`, all offline
- MySQL has a listed `mysql-volume`; its data has not yet been inspected.
- Railway displays `Trial expired` and requests an upgrade to continue.
- CLI authorization remains pending; browser sign-in alone does not authorize
  the CLI.

Required before release:

1. Complete CLI authorization for the confirmed CRM project and inspect its
   deployment settings and retained storage.
2. Resolve the expired hosting trial with the account owner's approval for any
   paid plan, then restore the services and check their existing public domains.
3. Confirm the backend production environment has all required secret values.
4. Confirm MySQL and `/data/attachments` use persistent Railway volumes.
5. Deploy the current `main` commit.
6. Verify frontend HTTP 200 and backend health `UP` over HTTPS.
7. Run authenticated tenant and billing smoke checks.
8. Review production logs for migration, startup, and request errors.

## Tag Gate

The `v3.0.0` tag remains intentionally absent. Create and push the annotated
tag only after the public deployment section changes from BLOCKED to PASS:

```powershell
git tag -a v3.0.0 -m "Tadamun CRM Version 3"
git push origin v3.0.0
```
