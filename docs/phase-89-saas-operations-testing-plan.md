# Phase 89 SaaS Operations And Testing Plan

## Goal

Prove tenant safety and recovery behavior before performance, monitoring,
backup, and release validation.

## Steps 1-4 Test Matrix

| Risk | Required proof | Automated evidence |
| --- | --- | --- |
| Tenant data exposure | Foreign organization identifiers cannot read tenant-owned records | `SaasTenantBoundaryIntegrationTest` plus existing customer isolation tests |
| Subscription isolation | Each organization resolves only its own subscription | `SaasTenantBoundaryIntegrationTest` |
| API key isolation | Management lookup/list cannot cross organizations | `SaasTenantBoundaryIntegrationTest`, `PublicApiKeyRepositoryTest` |
| Webhook isolation | Subscription and delivery history require the owning organization | `SaasTenantBoundaryIntegrationTest`, webhook persistence/service tests |
| Workflow isolation | Definitions and executions require the owning organization | `SaasTenantBoundaryIntegrationTest`, workflow persistence tests |
| Integration isolation | Connections, credentials, and delivery history require the owning organization | `SaasTenantBoundaryIntegrationTest`, integration persistence tests |
| Organization lifecycle | Organization, trial, owner membership, invitation, workspace access, and deactivation work together | `SaasTenantBoundaryIntegrationTest` |
| Billing idempotency/recovery | Processed Stripe events are ignored and failed events can recover exactly once | `BillingWebhookServiceTest` |
| Webhook recovery | Duplicate fan-out is prevented and stale claims return to retry/dead state | `WebhookEventFanoutServiceTest`, `WebhookQueueClaimServiceTest`, `WebhookPersistenceRepositoryTest` |
| Workflow recovery | Duplicate dispatch is prevented and abandoned action claims re-enter bounded retry | `WorkflowPersistenceRepositoryTest`, `WorkflowActionResultServiceTest` |
| Integration recovery | Claims are owned, stale work is recovered, worker failures retry, and exhausted work becomes dead | `IntegrationDeliveryClaimServiceTest`, `IntegrationDeliveryServiceTest` |

## Security Invariants

- Tenant identity comes from trusted request/authentication context.
- Tenant-owned management queries include `organization_id`.
- A record ID or public ID from another organization behaves as not found.
- Background workers may claim global queues, but stored graphs use composite
  tenant foreign keys and user-facing reads remain tenant-scoped.
- Provider tokens, SMTP passwords, API secrets, webhook secrets, and payload
  details never appear in assertions, logs, API errors, or documentation.
- Retry paths are bounded, idempotent where required, and preserve attempt
  evidence without duplicating side effects.

## Execution Commands

```powershell
cd backend
.\mvnw.cmd "-Dtest=SaasTenantBoundaryIntegrationTest,BillingWebhookServiceTest,WebhookEventFanoutServiceTest,WebhookQueueClaimServiceTest,WorkflowPersistenceRepositoryTest,WorkflowActionResultServiceTest,IntegrationDeliveryClaimServiceTest,IntegrationDeliveryServiceTest" test
.\mvnw.cmd test
```

### Read-only load test

The local performance profile logs in once, discovers an active workspace,
warms each route, and sends only `GET` requests to authentication, dashboard,
customer, lead, task, report, and subscription-usage endpoints. Credentials are
read from the ignored `.env` file and are never written to the report.

```powershell
.\scripts\Invoke-CrmReadOnlyLoadTest.ps1
.\scripts\Invoke-CrmReadOnlyLoadTest.ps1 -VirtualUsers 10 -Duration 1m
```

The default acceptance thresholds are less than 1% failed requests, an overall
95th percentile below 750 ms, an overall 99th percentile below 1500 ms, and a
per-endpoint 95th percentile below 1000 ms. A compact machine-readable report
is written to the ignored `tmp/load-test/summary.json`. The runner also prints
the ten MySQL application-statement digests with the greatest historical
average latency; transaction-control noise is excluded.

### Local baseline - 2026-09-11

- Profile: 5 virtual users for 30 seconds against the Docker backend.
- Result: 1,029 requests at 33.70 requests/second with 0% failures.
- Overall response time: 76.26 ms at p95.
- Slowest endpoint: report summary at 93.58 ms p95.
- Slowest eligible MySQL digest: 1.819 ms average; no query exceeded the
  endpoint or overall performance thresholds.

## Completion Status

- Step 1 - security and operations test matrix: implemented.
- Step 2 - cross-tenant isolation regression suite: implemented.
- Step 3 - end-to-end organization lifecycle suite: implemented.
- Step 4 - billing/webhook/workflow/integration recovery coverage: implemented.
- Step 5 - read-only load and performance testing: implemented.
- Step 6 - tenant-aware monitoring and protected metrics: implemented.
- Step 7 - scheduled backup verification and isolated restore proof: implemented.
- Step 8 - disaster-recovery rehearsal and final regression evidence:
  implemented.
- Phase 89 status: complete.

### Tenant-aware monitoring evidence

- Every backend response carries a validated or generated `X-Request-Id`.
- Authenticated request logs include trusted organization and user context.
- HTTP metrics use bounded route/status tags and only tenant
  `present`/`absent`; organization identifiers never become metric labels.
- Billing, webhook, workflow, and integration workers publish outcome counters
  without allowing metric failures to interrupt business processing.
- Health remains public for probes, while metrics require `PLATFORM_ADMIN`.
- `RequestObservabilityFilterTest`, `SaasOperationsMetricsTest`, and security
  endpoint tests cover correlation, tenant-safe labels, and access control.
- Final backend regression: 315 tests, 0 failures, 0 errors, 0 skipped.

### Backup verification evidence

- A transaction-consistent MySQL dump is created without stopping the CRM.
- Every run validates dump structure, restores into a disposable MySQL 8.4
  container, verifies critical tables and Flyway history, and records JSON
  evidence.
- The daily Windows task runs at 20:00, starts after a missed trigger, and is
  permitted to finish while the laptop is on battery.
- The scheduled proof run completed on 2026-09-12 with task result `0`: 44
  tables restored, Flyway version 35, and all five critical tables present.
- Timestamped backups and evidence are retained for 30 days; secrets and backup
  payloads remain excluded from Git.

### Final release gate - 2026-09-12

- Repository baseline: clean `main` at `81ed4f0` before this evidence update.
- Backend: `./mvnw.cmd test` passed 315 tests with 0 failures, 0 errors, and
  0 skipped; all 35 Flyway migrations applied in the integration suites.
- Frontend: 2 Vitest files passed 10 tests; ESLint and the Vite production
  build both passed.
- Runtime: the complete Compose stack rebuilt successfully, Compose
  configuration validated, and MySQL, backend, frontend, and Mailpit were all
  healthy.
- Persistence: MySQL uses a volume at `/var/lib/mysql`; backend attachments use
  a volume at `/data/attachments`.
- Database: the live schema reported Flyway version 35 and 0 failed migrations.
- Public smoke: backend health, frontend root, and frontend dashboard route
  returned HTTP 200; the health body reported `UP`.
- Security smoke: unauthenticated customer and metrics requests returned HTTP
  401; a supplied request ID was echoed; authenticated admin login, tenant
  customer access, and protected metrics returned successfully.
- Read-only runtime exercise: 83 requests completed with 0% failures, 7.81
  requests/second, 61.52 ms overall p95, and all performance thresholds passed.
- Operations: recent backend and frontend logs contained no error entries.
- Secret hygiene: `.env`, generated backups, and verification logs were not
  tracked; only `backups/.gitkeep` was tracked, and the high-confidence secret
  scan found no committed key material.
- Disaster recovery: the scheduled verifier restored a real backup into an
  isolated MySQL 8.4 container, verified 44 tables and Flyway version 35, and
  removed the temporary container without modifying the live database.

Release-gate result: **PASS**. Phase 90 may begin.
