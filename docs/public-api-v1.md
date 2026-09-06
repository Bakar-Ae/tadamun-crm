# Tadamun CRM Public API v1

## Base URL

Local development:

`http://localhost:8081/api/public/v1`

Public API v1 is read-only. Browser CRM endpoints remain under `/api/v1`.

## Authentication

Send the complete API key as a bearer credential:

```http
Authorization: Bearer tdm_live_<public-id>.<secret>
```

The complete key is returned only when it is created or rotated. Store it in a
secret manager. Tadamun stores only an HMAC-SHA-256 digest and cannot recover a
lost key.

## Scopes

- `customers:read`: list and retrieve customers.
- `leads:read`: list and retrieve leads.

An API key can contain either or both scopes. A missing scope returns `403`.

## Customer Endpoints

### List customers

`GET /customers`

Optional parameters:

- `keyword`
- `status`
- `customerType`
- `page` (default `0`)
- `size` (default `20`, maximum `100`)
- `sort` (for example `createdAt,desc`)

Allowed sort fields are `id`, `name`, `companyName`, `status`, `createdAt`, and
`updatedAt`.

### Retrieve a customer

`GET /customers/{id}`

## Lead Endpoints

### List leads

`GET /leads`

Optional parameters:

- `keyword`
- `status`
- `page` (default `0`)
- `size` (default `20`, maximum `100`)
- `sort` (for example `createdAt,desc`)

Allowed sort fields are `id`, `fullName`, `companyName`, `status`,
`estimatedValue`, `createdAt`, and `updatedAt`.

### Retrieve a lead

`GET /leads/{id}`

## Example

```bash
curl "http://localhost:8081/api/public/v1/customers?page=0&size=20" \
  -H "Authorization: Bearer $TADAMUN_API_KEY"
```

The organization is always derived from the API key. Client-supplied
organization identifiers cannot override it.

## Key Management

Authenticated organization owners and administrators with
`SUBSCRIPTION_MANAGE` use the browser API:

- `GET /api/v1/public-api-keys`
- `POST /api/v1/public-api-keys`
- `POST /api/v1/public-api-keys/{id}/rotate`
- `POST /api/v1/public-api-keys/{id}/revoke`

Creation request:

```json
{
  "name": "Reporting integration",
  "scopes": ["customers:read", "leads:read"],
  "rateLimitPerMinute": 60,
  "expiresAt": null
}
```

Creating and rotating keys requires a subscription with the `PUBLIC_API`
feature. Listing and revoking existing keys remain available after a downgrade.

Rotation creates the replacement before permanently revoking the previous key.

## Errors

- `400 Bad Request`: invalid filters, sort, pagination, or management input.
- `401 Unauthorized`: missing, malformed, expired, revoked, or invalid API key.
- `402 Payment Required`: the subscription does not include public API access.
- `403 Forbidden`: the API key lacks the required scope.
- `404 Not Found`: the record does not exist in the authenticated organization.
- `429 Too Many Requests`: per-key rate limit exceeded. Honor `Retry-After`.
- `500 Internal Server Error`: sanitized unexpected server error.

Authentication and authorization responses never reveal whether a public key ID
exists. Raw keys are excluded from application audit records.

## Rate-Limit Deployment Note

Phase 85 uses an in-memory, per-instance fixed-window limiter. Run one backend
instance to preserve an exact global limit. Before horizontally scaling the
backend, replace the limiter state with a shared store such as Redis.
