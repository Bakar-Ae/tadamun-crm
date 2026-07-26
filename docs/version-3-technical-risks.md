# Tadamun CRM Version 3 Technical Risk Register

## Status

Initial Version 3 risks identified.

This document must be reviewed whenever the tenancy architecture or scope changes.

## Risk Levels

- Critical: could expose data, corrupt production, or stop the release.
- High: could significantly affect security, reliability, or customers.
- Medium: could reduce performance, maintainability, or usability.
- Low: limited impact with a simple recovery path.

## RISK-001 - Cross-Tenant Data Exposure

Level: Critical

### Description

A user from one organization may access another organization’s customer,
lead, task, note, report, audit, or attachment data.

### Prevention

- Require validated tenant context.
- Use organization-scoped repository queries.
- Validate entity ownership in services.
- Test every tenant-owned endpoint with two organizations.
- Deny requests with missing tenant context.

### Required Evidence

Automated cross-tenant tests return `403` or `404` and never expose data.

## RISK-002 - Tenant Header Spoofing

Level: Critical

### Description

A client may send another organization’s ID using the organization header.

### Prevention

- Authenticate the user first.
- Validate active membership server-side.
- Never trust the organization header by itself.
- Audit repeated unauthorized tenant-selection attempts.

### Required Evidence

A user cannot activate an organization without an active membership.

## RISK-003 - Incomplete Existing-Data Migration

Level: Critical

### Description

Existing Version 2 records may be lost, orphaned, or assigned to the wrong
organization during migration.

### Prevention

- Create a default organization.
- Add ownership columns as nullable first.
- Backfill data in controlled migrations.
- Count records before and after migration.
- Make ownership non-null only after validation.
- Test with a restored database backup.

### Required Evidence

Record counts match and no tenant-owned record has a null organization.

## RISK-004 - Repository Isolation Bypass

Level: Critical

### Description

A developer may use `findById()` or an unscoped query for tenant-owned data.

### Prevention

- Introduce tenant-scoped repository methods.
- Restrict generic repository access to approved internal operations.
- Review all queries during each module migration.
- Add integration tests for repository isolation.

### Required Evidence

Tenant-owned service methods do not load records by ID without organization scope.

## RISK-005 - Background Job Context Leakage

Level: High

### Description

Scheduled jobs, emails, workflows, or webhook deliveries may run without the
correct organization context.

### Prevention

- Pass organization ID explicitly in every job.
- Create and clear job context for each execution.
- Never reuse a web-request context.
- Include organization ID in job and delivery records.

### Required Evidence

Jobs process only records belonging to their recorded organization.

## RISK-006 - File and Attachment Leakage

Level: Critical

### Description

An attachment URL or stored filename may allow cross-tenant download or deletion.

### Prevention

- Store organization ownership in attachment metadata.
- Use organization-based storage paths.
- Authorize every download and delete request.
- Never expose physical storage paths.
- Use generated storage names.

### Required Evidence

Users cannot download attachments owned by another organization.

## RISK-007 - Search, Reports, and Export Leakage

Level: Critical

### Description

Global search, dashboards, reports, or exports may aggregate records across tenants.

### Prevention

- Apply tenant scope before filtering, grouping, or pagination.
- Add organization constraints to analytics queries.
- Test exported files with multiple organizations.
- Review native SQL and aggregate queries separately.

### Required Evidence

Search results, report totals, and exports contain only active-tenant data.

## RISK-008 - Role and Permission Confusion

Level: High

### Description

Global roles, organization roles, and platform roles may be mixed together.

### Prevention

- Keep platform administration separate.
- Assign organization roles through memberships.
- Resolve permissions within the active organization.
- Revalidate sensitive permissions server-side.
- Audit role and membership changes.

### Required Evidence

A manager in one organization has no manager rights in another organization.

## RISK-009 - Unique Constraint Conflicts

Level: High

### Description

Fields currently unique globally may need to be unique only within an organization.

### Prevention

- Review all unique constraints.
- Replace global business uniqueness with composite organization constraints.
- Preserve truly global identity constraints, such as login email, where required.
- Test duplicate business data in separate organizations.

### Required Evidence

Different organizations can safely use the same customer or business identifiers.

## RISK-010 - Query Performance Degradation

Level: High

### Description

Adding tenant conditions to every query may reduce performance as data grows.

### Prevention

- Add indexes beginning with `organization_id`.
- Review query plans for common searches and reports.
- Keep pagination mandatory for large collections.
- Perform load testing with realistic tenant volumes.
- Monitor slow queries.

### Required Evidence

Core APIs meet agreed response-time targets under representative load.

## RISK-011 - Cache Contamination

Level: Critical

### Description

Cached data may be returned to the wrong organization if cache keys omit tenant identity.

### Prevention

- Include organization ID in every tenant-owned cache key.
- Avoid caching sensitive records until the cache strategy is reviewed.
- Clear tenant caches independently.
- Add cross-tenant cache tests before enabling caching.

### Required Evidence

Identical record IDs in different organizations never share cached values.

## RISK-012 - Billing State Inconsistency

Level: High

### Description

Payment-provider state and local subscription state may disagree.

### Prevention

- Treat signed provider webhooks as authoritative.
- Make webhook processing idempotent.
- Store provider event IDs.
- Reconcile subscription state periodically.
- Use grace periods instead of immediate destructive action.

### Required Evidence

Repeated or out-of-order billing events do not corrupt subscription state.

## RISK-013 - API Key and Webhook Secret Exposure

Level: Critical

### Description

Public API keys or webhook secrets may be logged, committed, or stored as plain text.

### Prevention

- Show secrets only once.
- Store API keys as secure hashes.
- Encrypt secrets that must be recovered.
- Redact secrets from logs and API responses.
- Support rotation and revocation.

### Required Evidence

Database and logs do not contain recoverable API keys.

## RISK-014 - Audit Log Gaps

Level: High

### Description

Tenant changes or platform-administrator access may not be traceable.

### Prevention

- Store organization ownership on audit records.
- Audit membership, role, billing, API key, webhook, and platform-access changes.
- Protect audit logs from normal modification.
- Include actor, organization, action, entity, and time.

### Required Evidence

Sensitive Version 3 actions produce complete tenant-aware audit events.

## RISK-015 - Rollback Failure

Level: High

### Description

A failed production migration may leave the database partially upgraded.

### Prevention

- Back up the database before migrations.
- Rehearse migrations against restored production-style data.
- Use additive migrations before destructive cleanup.
- Document rollback and recovery procedures.
- Avoid mixing many domain migrations into one file.

### Required Evidence

The migration rehearsal and restore procedure complete successfully.

## RISK-016 - Version 3 Scope Overload

Level: High

### Description

Building tenancy, billing, APIs, workflows, integrations, mobile, and AI together
may reduce quality and leave core security unfinished.

### Prevention

- Follow the approved Phase 71–90 order.
- Keep AI and native mobile work in Version 4.
- Do not start billing before isolation passes.
- Finish and validate one stable phase at a time.

### Required Evidence

Every implemented feature belongs to an approved Version 3 phase.

## Release Blocking Rules

Version 3 must not be released while any of the following remain unresolved:

- Known cross-tenant data exposure
- Failed tenant isolation tests
- Unowned tenant records
- Unverified production migration
- Plain-text API keys or webhook secrets
- Unprotected tenant attachment access
- Missing database backup and restore validation

## Review Schedule

Review this register:

- Before Phase 72 implementation
- Before database ownership migrations
- Before billing integration
- Before public API release
- Before production deployment
- During the final Phase 90 security review