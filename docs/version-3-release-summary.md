# Tadamun CRM - Version 3 Release Summary

## Status

Version 3 implementation and local release validation are complete. A free
Render/Aiven demo is live and passed basic public smoke checks on 2026-09-15.
The full production release gate and the `v3.0.0` Git tag remain pending.

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

Local release-candidate baseline, before the Render-specific adaptations:

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

Render preparation and public demo checks on 2026-09-15:

- The Render adaptations passed 9 focused attachment/error-handler tests,
  startup-script checks, and a Docker build. The full suite was not rerun during
  the guided deployment.
- Both Render services deployed commit `8f406bb` and reported Live.
- The separate Aiven database was initialized through V35: 35 successful
  migrations and 44 tables, without importing local or retained Railway data.
- Public backend health, version, bootstrap authentication, identity, and logout
  checks passed. Frontend root and login routes return HTTP 200.
- Exact-origin CORS preflights pass; an unapproved origin is rejected and
  unauthenticated customer access is denied.
- The operator confirmed customer create/edit persistence after refresh,
  archival, and sign-out/sign-in with the new password.

Frontend: <https://tadamun-crm-1.onrender.com/login>

Backend health: <https://tadamun-crm.onrender.com/actuator/health>

## Security Position

- Tenant context is derived from trusted authentication and active membership.
- API key secrets are hashed and never stored in raw form.
- Webhook and integration secrets use authenticated encryption.
- Tenant attachment operations require organization and data-scope access.
- Platform administration and operational metrics are separately authorized.
- Cross-tenant behavior, authorization, billing, public API, webhook, workflow,
  and integration paths have automated regression coverage.

## Remaining Release Gate

The free demo does not establish production readiness. Uploads, outbound email,
and Stripe are disabled, free-service sleep interrupts background workers, and
hosted load, tenant-isolation, integration, and recovery acceptance is pending.
The existing scheduled backup protects local MySQL, not the Aiven database.
Verified hosted backups and restoration, appropriate database privileges,
durable storage, operational monitoring, and the required enabled workflows
must be established before approving the production release and `v3.0.0` tag.

The previous Railway deployment remains separate and unresolved. Its trial
expired, all three services were offline, and its documented domains returned
HTTP 404 during the earlier inspection. The retained MySQL 9.4 volume was not
imported or downgraded. Any later restoration must validate its data, missing
secret configuration, and storage requirements. Continuing the Render demo does
not require purchasing a Railway plan.

See `docs/render-free-demo-guide.md` for the live configuration and
`docs/phase-90-version-3-release-checklist.md` for detailed evidence and the
remaining production gate. No paid hosting changes or release tag were made.

## Version 4 Direction

Version 4 planning covers PWA and mobile improvements, AI-assisted CRM
features, richer analytics, communication expansion, and a broader integration
ecosystem. Those features are intentionally excluded from Version 3.
