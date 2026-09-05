# Phase 82 Subscription Domain Design

## Purpose

Phase 82 establishes subscription plans, features, tenant subscriptions, and lifecycle rules without coupling Tadamun CRM to Stripe or another payment provider. Provider customer IDs, price IDs, checkout sessions, portal sessions, and webhooks belong to Phase 83.

## Ownership and Security

- Each organization has exactly one subscription.
- Subscription reads use the organization from the authenticated tenant context.
- `SUBSCRIPTION_VIEW` allows subscription and plan visibility.
- `SUBSCRIPTION_MANAGE` is reserved for owners and administrators and will protect billing mutations introduced in Phase 83.
- Subscription changes produce organization-scoped audit events.
- Payment credentials and provider identifiers never appear in frontend models.

## Plans

| Code | Intended use | Trial | Grace period |
| --- | --- | ---: | ---: |
| `STARTER` | Small teams using core CRM tools | 14 days | 7 days |
| `PROFESSIONAL` | Growing sales teams | 14 days | 7 days |
| `BUSINESS` | Larger teams needing integrations and automation | 14 days | 14 days |
| `ENTERPRISE` | Complex organizations requiring expanded controls | 30 days | 30 days |

Plans and feature definitions are database records so limits can be inspected and changed without altering organization subscriptions. Phase 83 will map plan codes to provider prices through a separate provider mapping.

## Features and Limits

- `MEMBERS`: maximum active organization memberships.
- `STORAGE_BYTES`: maximum attachment storage owned by the organization.
- `ADVANCED_REPORTING`: access to advanced report capabilities.
- `PUBLIC_API`: access to the Version 3 public API.
- `WEBHOOKS`: access to outbound webhook subscriptions.
- `WORKFLOW_AUTOMATION`: access to workflows; a numeric value limits active workflows.
- `PRIORITY_SUPPORT`: access to the priority support offering.

An enabled feature with a null numeric limit is unlimited. A disabled feature is unavailable even when its numeric limit is null. Enforcement is implemented in Phase 84 after usage metering is available.

## Lifecycle

Statuses are `TRIALING`, `ACTIVE`, `PAST_DUE`, `GRACE_PERIOD`, `CANCELED`, and `EXPIRED`.

- New organizations receive an idempotently provisioned Starter trial.
- Existing organizations receive a Starter trial during migration V25.
- A trial is expired when its end time is reached.
- Active subscriptions may enter past-due or grace-period states after provider payment failures.
- Grace periods expire at their configured end time.
- Cancellation can be immediate or scheduled for the current period end.
- Canceled and expired subscriptions may become active again after a successful future checkout.
- Trialing, active, past-due, and grace-period subscriptions retain access. Canceled and expired subscriptions do not.

GET requests calculate the effective status but do not silently modify billing state. Phase 83 provider synchronization or a controlled reconciliation job will persist provider-driven transitions.

## API

- `GET /api/v1/subscription`: returns the current organization's subscription, effective status, access decision, plan, features, dates, and optimistic-lock version.
- `GET /api/v1/subscription/plans`: returns active plans and feature definitions in display order.

Phase 82 intentionally exposes no billing mutation endpoint.

## Audit Events

- `SUBSCRIPTION_TRIAL_STARTED`
- `SUBSCRIPTION_ACTIVATED`
- `SUBSCRIPTION_PLAN_CHANGED`
- `SUBSCRIPTION_GRACE_PERIOD_STARTED`
- `SUBSCRIPTION_CANCELLATION_SCHEDULED`
- `SUBSCRIPTION_CANCELED`
- `SUBSCRIPTION_EXPIRED`

## Phase 83 Boundary

The billing provider integration may call the subscription service only after verifying checkout ownership or webhook signatures. Provider event identifiers must support idempotency, and provider secrets must remain backend-only.
