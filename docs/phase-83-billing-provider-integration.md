# Phase 83 - Billing Provider Integration

## Scope

Phase 83 connects the provider-neutral subscription domain to Stripe while
keeping billing credentials and provider decisions in the backend.

Implemented capabilities:

- One Stripe customer mapping per organization
- Backend-created subscription Checkout sessions
- Backend-created Stripe Billing Portal sessions
- Signed Stripe webhook verification using the raw request body
- Idempotent webhook storage and processing
- Subscription plan, status, period, cancellation, and grace-state synchronization
- Billing audit events scoped to the organization
- Automated service, security-endpoint, application-context, and migration tests

## API

Authenticated tenant endpoints require `X-Organization-Id` and the listed
permission:

| Method | Path | Permission | Purpose |
| --- | --- | --- | --- |
| `GET` | `/api/v1/subscription` | `SUBSCRIPTION_VIEW` | Current subscription |
| `GET` | `/api/v1/subscription/plans` | `SUBSCRIPTION_VIEW` | Available plans |
| `POST` | `/api/v1/subscription/checkout` | `SUBSCRIPTION_MANAGE` | Create Checkout session |
| `POST` | `/api/v1/subscription/portal` | `SUBSCRIPTION_MANAGE` | Open Billing Portal |
| `POST` | `/api/v1/billing/webhooks/stripe` | Public, Stripe-signed | Process provider events |

Checkout accepts `planCode` and `billingInterval`. Send a unique
`Idempotency-Key` header for each intentional Checkout attempt. The response
contains the provider session ID and the trusted Stripe redirect URL.

## Configuration

Stripe is disabled by default. Put real values only in the ignored local
`.env` file or the deployment platform's secret store:

```text
STRIPE_ENABLED=true
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...
STRIPE_PRICE_STARTER=price_...
STRIPE_PRICE_PROFESSIONAL=price_...
STRIPE_PRICE_BUSINESS=price_...
STRIPE_PRICE_ENTERPRISE=price_...
```

The four configured price IDs are synchronized as active monthly USD prices
when the backend starts. The database supports monthly and yearly prices;
yearly checkout remains unavailable until yearly provider-price mappings are
configured in a later pricing-catalog enhancement.

When `STRIPE_ENABLED=true`, both the secret key and webhook secret are required
at startup. Never place either value in frontend environment variables, source
control, logs, screenshots, or API responses.

## Stripe Test-Mode Setup

1. In Stripe test mode, create recurring monthly prices for Starter,
   Professional, Business, and Enterprise.
   Assign each product the business SaaS tax code `txcd_10103001` when
   Managed Payments is enabled.
2. Put their `price_...` identifiers and the test secret key in the ignored
   `.env` file.
3. Install and authenticate the Stripe CLI.
4. Forward test events locally:

   ```powershell
   stripe listen --forward-to localhost:8081/api/v1/billing/webhooks/stripe
   ```

5. Put the CLI-provided `whsec_...` value in `STRIPE_WEBHOOK_SECRET`.
6. Rebuild the backend:

   ```powershell
   docker compose up -d --build backend
   ```

7. Test Checkout from an organization-owner session and complete payment with
   a Stripe test card. Confirm the webhook event becomes `PROCESSED` and the
   organization subscription becomes `ACTIVE`.

Required event types are:

- `checkout.session.completed`
- `customer.subscription.created`
- `customer.subscription.updated`
- `customer.subscription.deleted`

Unsupported signed events are acknowledged and stored as `IGNORED`. Invalid
signatures return `400`. Processing failures return `500` so Stripe retries.
Processed event IDs are idempotent, and older provider updates do not overwrite
newer subscription state.

## Production Checklist

- Store Stripe values in Railway environment secrets, never GitHub or Vercel
  frontend variables.
- Use the production frontend HTTPS URL for `APP_FRONTEND_BASE_URL`.
- Register the production backend webhook URL in Stripe.
- Enable only the required event types.
- Complete a test-mode rehearsal before switching to live keys and prices.
- Monitor failed webhook records and Stripe delivery attempts.
- Rotate any key immediately if it appears in source control or shared output.
- Preserve billing records during organization suspension or payment failure.

## Verification

Focused verification:

```powershell
cd backend
.\mvnw.cmd "-Dtest=SubscriptionBillingServiceTest,BillingWebhookServiceTest,SubscriptionServiceTest,SecurityEndpointTest,BackendApplicationTests" test
```

Full backend verification:

```powershell
cd backend
.\mvnw.cmd test
```

No live Stripe request is performed by the automated suite. Provider calls are
mocked at the adapter boundary, while signed-route behavior, Flyway migration
V26, tenant ownership, lifecycle synchronization, and idempotency are tested
locally.

## Sandbox Acceptance - 2026-09-06

Phase 83 was accepted end to end against a dedicated Stripe sandbox:

- All four monthly prices synchronized as active USD mappings.
- CRM-created Checkout completed with a Stripe test card.
- Signed Checkout and subscription webhooks were processed idempotently.
- The organization subscription synchronized to `STARTER` / `ACTIVE` with
  the provider billing period.
- A failed event was replayed successfully without overwriting newer state.
- The CRM-created Billing Portal displayed the active subscription, test
  payment method, paid invoice, and next billing date.

The rehearsal exposed and fixed a detached-plan lookup in
`BillingPlanPriceRepository`; provider-price lookups now fetch the related plan
using `@EntityGraph`. It also confirmed that Stripe Managed Payments rejects
products without an eligible tax code.
