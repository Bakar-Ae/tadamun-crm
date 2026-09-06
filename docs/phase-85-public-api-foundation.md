# Phase 85 - Public API Foundation

## Delivered

- Versioned `/api/public/v1` boundary.
- Tenant-bound API keys with one-time secret display.
- HMAC-SHA-256 secret hashing with a server-side pepper.
- Key listing, creation, rotation, and permanent revocation.
- `customers:read` and `leads:read` scope enforcement.
- `PUBLIC_API` subscription feature enforcement.
- Read-only customer and lead list/detail endpoints.
- Per-key fixed-window rate limiting with `Retry-After`.
- Secret-free security and lifecycle audit events.
- Sanitized `401`, `402`, `403`, `429`, and `500` behavior.
- Public API consumer documentation.

## Security Invariants

- Raw API keys are never persisted.
- The complete key appears only in creation and rotation responses.
- JWT authentication skips the public API boundary.
- Public API credentials are rejected outside the public API boundary.
- Organization identity comes only from the authenticated API key.
- Revoked keys cannot be restored.
- Invalid credentials do not reveal whether their public ID exists.

## Verification

Run focused tests:

```powershell
cd backend
.\mvnw.cmd "-Dtest=PublicApiKeyTokenServiceTest,PublicApiKeyServiceTest,PublicApiRateLimiterTest,PublicApiAuthenticationServiceTest,PublicApiAuthenticationFilterTest,PublicApiScopeGuardTest,PublicApiReadServiceTest,SubscriptionFeatureAccessServiceTest" test
```

Run the full backend suite:

```powershell
cd backend
.\mvnw.cmd test
```

Validate Docker configuration:

```powershell
docker compose config --quiet
```

## Operational Notes

- `PUBLIC_API_KEY_PEPPER` is mandatory and must remain secret.
- Rotating the server pepper invalidates every existing API key.
- The current rate limiter is suitable for a single backend instance.
- A shared rate-limit store is required before horizontal scaling.
