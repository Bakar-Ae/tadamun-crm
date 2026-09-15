# Tadamun CRM - Version 3 Release Summary

## Status

Version 3 implementation and local release validation are complete. Public
Railway deployment validation and the `v3.0.0` Git tag remain pending.

## Version 3 Goal

Version 3 transforms Tadamun CRM from a single-organization application into
a multi-tenant SaaS foundation with tenant isolation, subscriptions, external
APIs, automation, integrations, and production operations controls.

## Major Improvements

- Multi-organization domain and workspace switching
- Organization memberships, invitations, onboarding, and settings
- Tenant-aware roles, permissions, platform administration, audit logs, and
  reporting
- Enforced tenant ownership across existing CRM records and attachments
- Safe migration of Version 2 data into a default organization
- Stripe subscription checkout, customer portal, and signed webhook handling
- Subscription plans, lifecycle rules, usage metering, and feature limits
- Public API keys with scopes, rate limits, rotation, and revocation
- Signed outbound webhooks with endpoint validation, retries, replay, and
  delivery history
- Event-driven workflow definitions, conditions, actions, retry, and history
- Encrypted WhatsApp Cloud and SMTP integration credentials with durable
  delivery processing
- Tenant-aware observability, operational metrics, load testing, backup
  verification, and disaster-recovery exercises

## Release Validation

- Production-style migration rehearsal passed from Flyway V15 to V35.
- Seven core table counts were preserved during rehearsal.
- No unowned core tenant records remain.
- All 315 backend tests pass.
- All 10 frontend tests pass.
- Frontend lint and production build pass.
- The frontend dependency audit reports zero known vulnerabilities.
- Backend and frontend production Docker images build successfully.
- High-confidence tracked-secret scan is clean.
- Local Docker services are healthy, the backend reports Version 3.0.0 and
  health `UP`, and the frontend returns HTTP 200.
- Automated backup creation and isolated restoration pass.

## Security Position

- Tenant context is derived from trusted authentication and active membership.
- API key secrets are hashed and never stored in raw form.
- Webhook and integration secrets use authenticated encryption.
- Tenant attachment operations require organization and data-scope access.
- Platform administration and operational metrics are separately authorized.
- Cross-tenant behavior, authorization, billing, public API, webhook, workflow,
  and integration paths have automated regression coverage.

## Remaining Release Gate

The previously documented Railway domains return HTTP 404. On 2026-09-14,
the account dashboard showed an expired trial and all three services offline
in the confirmed CRM project, `sparkling-simplicity`. Hosting must be restored
with the account owner's approval for any paid plan, the current `main` branch
must be deployed, and public smoke checks must pass before creating `v3.0.0`.

CLI authorization succeeded on 2026-09-15. Railway then explicitly rejected a
frontend redeployment because the trial had expired. Both public domains and
the MySQL volume remain listed. Before deploying Version 3, validate the
retained MySQL 9.4 database, supply the missing API-key pepper and webhook
encryption configuration, and attach persistent storage for uploaded files.

See `docs/phase-90-version-3-release-checklist.md` for the exact evidence and
remaining deployment steps.

## Version 4 Direction

Version 4 planning covers PWA and mobile improvements, AI-assisted CRM
features, richer analytics, communication expansion, and a broader integration
ecosystem. Those features are intentionally excluded from Version 3.
