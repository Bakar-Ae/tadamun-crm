# Phase 88 Integration Foundation

## Goal

Provide one tenant-safe boundary for external communication providers without
coupling CRM services to WhatsApp, SMTP, or future vendor SDKs.

## Architecture

- `integration_connections` stores tenant ownership, provider, lifecycle,
  non-secret configuration, health, and actor metadata.
- `integration_credentials` stores only AES-256-GCM encrypted credential JSON.
  It is a separate entity and is never traversed from connection responses.
- `IntegrationProviderAdapter` is the provider boundary. Provider code validates
  configuration, verifies credentials, and delivers messages without changing
  connection storage.
- `IntegrationProviderRegistry` rejects duplicate adapters and provides explicit
  failure when a provider is unavailable.
- `integration_deliveries` is the durable outbox. API requests enqueue work;
  only the background worker calls providers.
- `integration_delivery_attempts` preserves success and failure history without
  storing request headers or credentials.

## Security Model

- Every connection and credential row carries `organization_id`; composite
  foreign keys prevent cross-tenant credential attachment.
- Repositories used by application services must include organization ID.
- Credentials use a versioned key ring and a unique 96-bit nonce per write.
- AES-GCM additional authenticated data binds ciphertext to the key version,
  organization, public connection ID, and provider. Moving encrypted bytes to
  another tenant or connection makes decryption fail.
- API responses, audit details, logs, exceptions, and `toString()` output must
  never contain credential values. Audit data may contain credential field
  names and key version only.
- Revocation is terminal. A revoked connection cannot be reactivated; replacing
  it requires a new connection and credentials.
- Delivery records and idempotency keys are unique inside an organization.
  Every query exposed to users includes the current organization ID.

## Lifecycle

`DRAFT -> ACTIVE <-> DISABLED`, verification or delivery failures may move an
active connection to `ERROR`, and an owner/admin can move any non-revoked
connection to `REVOKED`. Activation requires credentials and successful provider
verification.

## Delivery Contract

- Delivery is at least once. A provider may accept a request immediately before
  a process or database failure, so callers should supply an idempotency key and
  tolerate a duplicate at the external provider.
- The worker claims rows with `FOR UPDATE SKIP LOCKED`; abandoned claims are
  recovered after the configured stale-claim interval.
- Retryable failures use bounded backoff and end in `DEAD` after the configured
  attempt limit. Validation and permanent provider failures end in
  `TERMINAL_FAILURE`.
- Administrators may retry a failed delivery or cancel work that has not begun.
- Destinations and message content are operational data, not credentials. They
  remain tenant-scoped and should follow the same retention policy as CRM notes.

## Provider Foundations

- WhatsApp Cloud supports credential verification and text delivery through a
  configurable HTTPS Graph API base URL and version. The bearer token is never
  included in transport diagnostics.
- SMTP supports connection verification, plain-text email, optional
  authentication, STARTTLS, sender name, and reply-to configuration.
- Additional providers implement the same adapter contract and register through
  `IntegrationProviderRegistry`; CRM domain services do not branch on vendor
  SDKs.

## Completed Milestones

1. Shared schema, encrypted credential vault, and provider adapter contract.
2. Tenant-safe connection management API, validation, audit logs, and UI.
3. WhatsApp Cloud API adapter and connection verification.
4. SMTP adapter and organization-specific email delivery.
5. Durable outbound delivery, retries, observability, and end-to-end tests.

## Definition Of Done

- Credentials are encrypted at rest and context-bound.
- No API or audit path can reveal a stored secret.
- Provider code is selected through the registry, not conditionals in CRM code.
- Tenant isolation, key rotation, tamper rejection, and adapter registration are
  covered by automated tests.
- Connection management, verification, test delivery, retry, cancellation, and
  delivery history are available under `/api/v1/integrations` and in the
  Integrations workspace page.
