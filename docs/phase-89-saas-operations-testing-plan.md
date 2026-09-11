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

## Completion Status

- Step 1 - security and operations test matrix: implemented.
- Step 2 - cross-tenant isolation regression suite: implemented.
- Step 3 - end-to-end organization lifecycle suite: implemented.
- Step 4 - billing/webhook/workflow/integration recovery coverage: implemented.
- Steps 5-8 remain: load testing, tenant-aware monitoring, backup verification,
  disaster recovery, and final Phase 89 regression evidence.
