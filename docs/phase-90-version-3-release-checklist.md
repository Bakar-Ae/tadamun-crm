# Phase 90 - Version 3 Release Checklist

## Status

Release candidate prepared. Local validation and the final security review
passed. A separate free Render/Aiven demo is now live and its basic public smoke
checks passed on 2026-09-15. The old Railway deployment remains unresolved.
Production release acceptance and the `v3.0.0` tag remain pending: a constrained
demo with uploads, outbound email, and Stripe disabled is not the full release.

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
- Local backup/restore validation: resolved in Phase 89; hosted Aiven recovery
  validation remains pending

## Automated Validation

Status: PASS

The full local release-candidate baseline below predates the Render adaptations.
Those adaptations were separately checked with 9 focused attachment/error-handler
tests, startup-script checks, a Docker build, and a resource-limited startup/login
test. See `render-free-demo-guide.md`; the full suite was not rerun during the
guided public deployment.

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

Status: BASIC FREE-DEMO CHECKS PASS; PRODUCTION ACCEPTANCE PENDING

### Render and Aiven Demo (2026-09-15)

- Frontend: <https://tadamun-crm-1.onrender.com/login>
- Backend: <https://tadamun-crm.onrender.com/actuator/health>
- Both Render services deployed `8f406bb` from `main` and reported Live.
- The separate Aiven MySQL 8.4 database has 44 tables and 35 successful Flyway
  migrations, latest version V35. No local CRM backup or Railway data was imported.
- Public backend health is `UP`; actuator reports version `3.0.0`.
- Public bootstrap login, authenticated identity, and logout passed before the
  operator changed the temporary password. No passwords or tokens were printed.
- Frontend root and direct `/login` requests return HTTP 200 with the same app
  shell after the `/*` to `/index.html` rewrite.
- Backend CORS permits the exact frontend origin for login and authenticated
  customer requests. An unapproved origin was rejected with HTTP 403; an
  unauthenticated customer-list request was denied with HTTP 401.
- The operator confirmed customer creation and rename persisted after refresh,
  customer archival worked, and sign-out/sign-in returned to the dashboard using
  the new password. Archival retains history; permanent deletion was not tested.
- Uploads, outbound email, and Stripe are intentionally disabled. Background
  workers only run while the free backend is awake.
- The existing Windows backup task still backs up local MySQL, not Aiven.
- Full hosted tenant-isolation, billing, integration, load, and recovery checks
  remain outside this basic demo smoke test.

See `render-free-demo-guide.md` for configuration and detailed evidence.

### Previous Railway Deployment

Status: BLOCKED; retained resources have not been migrated to the demo.

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

Authenticated CLI inspection on 2026-09-15:

- CLI authorization completed for the confirmed CRM workspace and project.
- All three services have no active deployments.
- Both existing public domains remain configured; regeneration is not
  currently indicated.
- The MySQL volume is `READY`, approximately 156 MB used, and is not marked
  for deletion. Its database contents still require validation.
- MySQL is configured with `mysql:9.4`; local validation used MySQL 8.4.
  Validate the retained database and migration compatibility before upgrading
  the backend. Do not downgrade the existing database volume in place.
- The backend has no attached volume despite using `/data/attachments`.
- Backend variables `PUBLIC_API_KEY_PEPPER` and
  `WEBHOOK_SECRET_ENCRYPTION_KEYS` are absent. Version 3 requires these values;
  integration encryption defaults to the webhook key when not configured
  separately. Existing credential values were not printed or changed.
- Service root directories are `/backend` and `/frontend`; the frontend API
  URL already points to the existing backend domain.
- The backend domain targets port 8080. Verify `PORT` and the service's
  listening port agree when restoring the deployment.
- The account has no active paid subscription. An attempted frontend
  redeployment was rejected with: `Your trial has expired. Please select a
  plan to continue using Railway.` No deployment started.

Required only if restoring the retained Railway deployment:

1. Resolve the expired hosting trial with the account owner's approval for any
   paid plan.
2. Restore MySQL, validate and back up its retained data, and verify migration
   compatibility with its configured MySQL version.
3. Configure the missing required backend secrets securely without replacing
   existing keys or exposing their values.
4. Add persistent attachment storage and verify the Docker build configuration,
   health check, and port mapping for both application services.
5. Deploy the current `main` commit using the existing public domains.
6. Verify frontend HTTP 200 and backend health `UP` over HTTPS.
7. Run authenticated tenant and billing smoke checks.
8. Review production logs for migration, startup, and request errors.

### Remaining Production Gate

1. Confirm the production hosting and operating requirements. Do not enable any
   paid plan without the account owner's approval; restoring Railway is not a
   prerequisite for continuing the separate Render demo.
2. Establish and verify backups and restoration for the chosen hosted database.
   Local Docker backup evidence does not cover the Aiven service.
3. Use durable attachment storage before enabling uploads; configure and verify
   outbound email and billing before claiming these workflows are available.
4. Restrict application database privileges, review secret handling and logs,
   and verify the required hosted tenant-isolation and integration workflows.
5. Validate cold-start latency, load, worker availability, monitoring, and
   recovery against the intended production requirements.
6. Review the completed production evidence before authorizing the release tag.

## Tag Gate

The `v3.0.0` tag remains intentionally absent. A passing free-demo smoke test
does not satisfy production acceptance. Create and push the annotated tag only
after the remaining production gate above has passed and release is authorized:

```powershell
git tag -a v3.0.0 -m "Tadamun CRM Version 3"
git push origin v3.0.0
```
