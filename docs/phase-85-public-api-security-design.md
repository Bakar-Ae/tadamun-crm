# Phase 85 - Public API Security Design

## Goal

Provide tenant-isolated, versioned API access for approved external systems without exposing CRM user passwords or JWT refresh tokens.

## API Boundary

- Public endpoints use `/api/public/v1`.
- Existing browser endpoints remain under `/api/v1`.
- Phase 85 initially exposes read-only customer and lead endpoints.
- Every request belongs to exactly one organization.

## API Key Format

Keys use this structure:

`tdm_live_<public-id>.<secret>`

- The public ID identifies the database record.
- The secret contains at least 256 bits of cryptographic randomness.
- The complete key is displayed only once.
- Raw secrets are never stored, logged, emailed, or returned again.
- Secrets are verified using HMAC-SHA-256 and a server-side pepper.

## Authentication

Clients send:

`Authorization: Bearer <api-key>`

The authentication filter must:

1. Accept API keys only on `/api/public/v1/**`.
2. Parse and validate the key format.
3. Reject missing, invalid, expired, or revoked keys with `401`.
4. Load the key's organization and scopes.
5. Establish a tenant context before controller execution.
6. Never expose whether a public ID exists.

## Scopes

Initial scopes:

- `customers:read`
- `leads:read`

Missing scopes return `403 Forbidden`. Scopes must be checked in the service or security layer, not trusted from request parameters.

## Subscription Access

Public API access requires the `PUBLIC_API` subscription feature. Disabled access returns the existing upgrade-required `402` response.

## Key Lifecycle

Each key records:

- organization
- display name
- public ID and safe prefix
- secret hash
- scopes
- status
- optional expiration
- creation actor and time
- last-used time
- revocation actor and time

Rotation creates a new key before revoking the old key. Revoked keys can never be restored.

## Rate Limiting

Rate limits apply per API key. Exceeded limits return `429 Too Many Requests` with a `Retry-After` header.

## Tenant Isolation

The organization comes only from the authenticated API key. Organization IDs supplied through query parameters or request bodies must never override the authenticated tenant.

## Auditing

Create, rotate, revoke, authentication failure, scope denial, and rate-limit events are audited without recording raw API keys.

## Error Contract

- `401`: missing, invalid, expired, or revoked API key
- `402`: subscription does not allow public API access
- `403`: valid key lacks the required scope
- `429`: rate limit exceeded
- `500`: sanitized unexpected error

## Testing Requirements

Tests must cover key hashing, display-once behavior, revocation, expiration, scope enforcement, tenant isolation, subscription gating, rate limiting, and secret-safe logs.