# Phase 84 - Usage Metering and Limits

## Outcome

Phase 84 measures organization member and attachment-storage usage, exposes the
current values through the subscription API, and prevents writes that would
exceed the active plan. Limits remain provider-neutral and come from the
subscription plan feature records introduced in Phase 82.

## Usage API

`GET /api/v1/subscription/usage` requires `SUBSCRIPTION_VIEW` and returns:

- organization ID and plan code
- `MEMBERS` usage based on active organization memberships
- `STORAGE_BYTES` usage based on active attachment metadata
- limit, remaining capacity, percentage used, warning state, and reached state

An enabled feature with a null limit is unlimited. A missing or disabled
metered feature has zero available capacity.

## Enforcement

Capacity is checked before:

- direct organization membership creation
- organization invitation acceptance
- customer attachment uploads
- lead attachment uploads

An operation that would exceed a finite limit returns HTTP `402` with code
`SUBSCRIPTION_LIMIT_REACHED`, the feature, current usage, limit, and
`upgradeRequired: true`.

## Warning Notifications

When an allowed operation would bring projected usage to at least 80 percent,
active organization owners and administrators receive a mandatory in-app
`SUBSCRIPTION_USAGE_WARNING` notification.

Warnings use a database uniqueness key containing the organization, recipient,
feature, and billing-period start. This makes warning insertion atomic and
prevents repeated actions from creating duplicate warnings in one period.

Migration `V27__add_notification_deduplication.sql` adds the nullable
deduplication key without changing existing notifications.

## Main Files

- `backend/src/main/java/com/crm/backend/subscription/usage/SubscriptionUsageService.java`
- `backend/src/main/java/com/crm/backend/subscription/usage/SubscriptionLimitExceededException.java`
- `backend/src/main/java/com/crm/backend/subscription/SubscriptionController.java`
- `backend/src/main/java/com/crm/backend/organization/membership/OrganizationMembershipService.java`
- `backend/src/main/java/com/crm/backend/organization/invitation/OrganizationInvitationService.java`
- `backend/src/main/java/com/crm/backend/attachment/AttachmentService.java`
- `backend/src/main/java/com/crm/backend/notification/NotificationService.java`
- `backend/src/main/resources/db/migration/V27__add_notification_deduplication.sql`

## Verification

Run the complete backend suite with bounded memory:

```powershell
cd backend
$env:MAVEN_OPTS='-Xmx768m -XX:MaxMetaspaceSize=256m'
.\mvnw.cmd -q -DforkCount=0 test
```

Rebuild the local stack and verify health:

```powershell
docker compose up -d --build
docker compose ps
```

After authentication, request current usage:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8081/api/v1/subscription/usage" `
  -Headers $headers
```

## Acceptance Result

- Flyway applied and validated all migrations through `V27` on MySQL 8.4.
- The local backend, frontend, MySQL, and Mailpit containers are healthy.
- The complete backend suite passed with 172 tests and no failures.
- Focused tests cover usage calculations, finite and unlimited limits,
  projected 80-percent warnings, notification insertion, and the `402` API
  error contract.
