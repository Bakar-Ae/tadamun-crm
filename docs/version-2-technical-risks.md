# Tadamun CRM Version 2 Technical Risks

## Purpose

This document lists technical risks before Version 2 development starts.

The goal is to avoid adding features on top of weak foundations.

## Risk 1 - Permission Complexity

Version 2 will add granular permissions.

Risk:

- Too many permissions can become hard to manage.
- Frontend and backend permissions can become inconsistent.

Mitigation:

- Backend remains the source of truth.
- Frontend only hides/disables UI for convenience.
- Permissions should be seeded clearly.
- Permission names should follow consistent naming.

Example:

- `CUSTOMER_CREATE`
- `CUSTOMER_UPDATE`
- `CUSTOMER_ARCHIVE`
- `REPORT_VIEW`

## Risk 2 - Email Deliverability

Real email sending can fail because of SMTP configuration, spam rules, or provider limits.

Risk:

- Password reset emails may not arrive.
- Task notifications may be blocked.
- SMTP secrets may leak if mishandled.

Mitigation:

- Keep email disabled by default locally.
- Use environment variables for SMTP secrets.
- Log safe messages only.
- Never log email passwords.
- Test with a development mailbox before production.

## Risk 3 - File Upload Security

File attachments introduce security risks.

Risk:

- Large files can overload storage.
- Dangerous file types may be uploaded.
- Private customer files may leak.
- File paths can be abused.

Mitigation:

- Limit file size.
- Limit allowed file types.
- Store metadata in database.
- Do not expose raw server file paths.
- Require authentication and permission checks.
- Use generated storage names instead of original filenames.

## Risk 4 - Reporting Performance

Advanced reports can become slow with large datasets.

Risk:

- Dashboard/report queries may become expensive.
- Tables like notes, tasks, and audit logs can grow quickly.

Mitigation:

- Use pagination.
- Add database indexes.
- Avoid loading huge datasets into memory.
- Add date filters.
- Measure query performance before production.

## Risk 5 - Global Search Performance

Searching across multiple modules can become expensive.

Risk:

- Slow search responses
- Too many unrelated results
- Complex query logic

Mitigation:

- Start with simple indexed searches.
- Limit result size.
- Return grouped results by module.
- Add full-text search later only if needed.

## Risk 6 - Frontend Complexity

The frontend already has many pages and modals.

Risk:

- Duplicated table/filter/drawer patterns
- Harder maintenance
- More TypeScript warnings

Mitigation:

- Extract shared components only when duplication is painful.
- Keep pages readable.
- Avoid building generic abstractions too early.
- Run build/lint after each feature.

## Risk 7 - Testing Gaps

Version 2 features will touch security, files, reports, and notifications.

Risk:

- Bugs may appear in critical flows.
- Manual testing may miss permission issues.

Mitigation:

- Add backend integration tests for permissions.
- Add tests for password reset and email logic.
- Add tests for file upload constraints.
- Keep manual QA checklist updated.

## Risk 8 - Deployment Secrets

More integrations mean more secrets.

Risk:

- SMTP secrets
- Storage secrets
- API keys
- Production database credentials

Mitigation:

- Use `.env` locally.
- Never commit `.env`.
- Use deployment secret management in production.
- Keep `.env.example` with placeholders only.

## Version 2 Rule

Every Version 2 feature must answer:

1. What security risk does this add?
2. What database change does this require?
3. What test proves it works?
4. What should happen if it fails?