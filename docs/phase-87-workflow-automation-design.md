# Phase 87 - Workflow Automation Foundation Design

## Goal

Allow approved CRM events to start controlled, tenant-scoped background
workflows without arbitrary code execution, duplicate side effects, runaway
loops, or changes to existing CRM transaction behavior.

## Scope

Phase 87 supports one event trigger and up to ten ordered actions per workflow.
The initial action catalog is deliberately small:

- `CREATE_TASK`
- `SEND_IN_APP_NOTIFICATION`

The initial trigger catalog reuses the versioned, allowlisted CRM event names
from Phase 86. Conditions may inspect allowlisted event payload fields, but may
not execute scripts, SQL, templates with code, or outbound HTTP requests.

External communication belongs to the provider adapters planned for Phase 88.

## Workflow Lifecycle

Workflow statuses:

- `DRAFT`: editable and never triggered.
- `ACTIVE`: eligible to create executions.
- `PAUSED`: temporarily prevented from creating executions.
- `ARCHIVED`: immutable and retained with its history.

Only a paused or draft workflow may be structurally edited. Activation requires
one enabled trigger, between one and ten enabled actions, valid action
configuration, and an available `WORKFLOW_AUTOMATION` plan allowance.

Each structural edit increments `definition_version`; the separate `version`
column is reserved for optimistic locking. Executions store the definition
version, trigger input, action type, order, and action configuration snapshots
so an in-flight run is deterministic. Lifecycle-only changes do not create a
new definition revision.

## Trigger Model

Phase 87 supports `CRM_EVENT` triggers. A trigger contains:

- one reviewed event name from the Phase 86 catalog
- an optional declarative condition document
- an enabled flag

Initial condition operators are `EQUALS`, `NOT_EQUALS`, `IN`, and `EXISTS`.
Field paths must come from an event-specific allowlist. Conditions have a
maximum serialized size of 16 KiB and a maximum nesting depth of five.

The existing `webhook_events` rows provide the durable, versioned CRM event
envelope. Phase 87 dispatch will create workflow execution rows in the same
business transaction. Event creation will occur when either an active webhook
subscription or an active workflow trigger is interested in the event.

## Action Model

Actions execute in ascending `action_order` and have stable idempotency keys.

`CREATE_TASK` may configure an allowlisted title, priority, due-date offset,
assignee source, and customer/lead source from the trigger. It cannot assign a
user outside the current organization.

`SEND_IN_APP_NOTIFICATION` may configure a bounded title/message template and
recipient policy. Recipients must be active members of the same organization.

No action can select an organization ID, provide raw SQL, invoke arbitrary
classes, read secrets, or call an arbitrary URL.

## Execution Model

Creating a run and its action execution snapshots is transactional with the CRM
event. A scheduled worker later claims pending rows using MySQL
`FOR UPDATE SKIP LOCKED` and a unique claim token.

Execution statuses:

- `PENDING`, `PROCESSING`, `RETRY_SCHEDULED`
- `SUCCEEDED`, `PARTIALLY_SUCCEEDED`, `FAILED`, `DEAD`, `CANCELLED`

Action execution statuses:

- `PENDING`, `PROCESSING`, `RETRY_SCHEDULED`
- `SUCCEEDED`, `FAILED`, `DEAD`, `SKIPPED`

The worker executes actions sequentially. `STOP_ON_FAILURE` skips remaining
actions after a terminal failure. `CONTINUE_ON_FAILURE` runs independent
remaining actions and completes the workflow as `PARTIALLY_SUCCEEDED`.

Each action attempt is recorded separately with duration and sanitized error
metadata. Successful side effects store only a safe resource type, resource ID,
and bounded JSON summary.

## Retry and Recovery Rules

- A workflow permits one to five action attempts; the default is three.
- Retryable infrastructure errors use bounded backoff of 1 minute, 5 minutes,
  30 minutes, and 2 hours as applicable.
- Validation, authorization, missing-resource, and tenant-boundary errors are
  terminal.
- A stale processing claim is returned to the queue after two minutes.
- A worker may update a row only while it owns the current claim token.
- Exhausted actions become `DEAD`; the parent execution then follows its failure
  policy.

Actions must use `workflow_action_executions.idempotency_key` before creating a
side effect. Reclaiming or retrying a row never creates a second task or
notification for the same action execution.

## Loop and Abuse Controls

- Root executions have automation depth zero.
- Events emitted by an action carry the execution correlation and causation
  context; child executions increment the depth.
- Automation depth is capped at five in the database and defaults to three in
  application policy.
- Activation rejects direct loops such as a `task.created` trigger whose action
  creates another task.
- A workflow has a configurable executions-per-hour ceiling from 1 to 10,000,
  with a default of 100.
- The plan feature limit caps active workflows per organization: Professional
  10, Business 100, Enterprise unlimited, Starter disabled.
- Worker batch size, action timeout, payload size, condition complexity, and
  output/error sizes are bounded.

## Tenant and Authorization Rules

- Every definition, trigger, action, execution, and result belongs to one
  organization.
- Composite foreign keys prevent cross-organization workflow graphs.
- The organization always comes from authenticated tenant context.
- `WORKFLOW_VIEW` permits reading definitions and execution history.
- `WORKFLOW_MANAGE` permits create, edit, activate, pause, archive, retry, and
  cancel operations.
- `OWNER`, `ADMIN`, and `MANAGER` receive view permission. Only `OWNER` and
  `ADMIN` receive manage permission.
- Every management operation also requires the `WORKFLOW_AUTOMATION` feature.
- Cross-tenant lookups return the same not-found response as missing records.

## Audit and Data Safety

Audit events will cover workflow create, update, activate, pause, archive,
manual retry, cancel, and exhausted execution. Audit details may include public
IDs, action types, statuses, and sanitized error categories.

Audit details, application logs, and API errors must not contain unrestricted
event payloads, notification bodies, credentials, stack traces, or action
configuration documents. Execution input and result summaries are visible only
through organization-scoped history APIs.

## Persistence Responsibilities

`workflow_definitions` owns lifecycle, failure policy, version, execution
timeout, attempt limit, and hourly safety limit.

`workflow_triggers` owns the event name and optional declarative condition.

`workflow_actions` owns ordered, versioned action definitions and validated
configuration. Replaced revisions receive `retired_at` and remain available to
historical executions.

`workflow_executions` is the durable run queue and immutable trigger snapshot.
Its unique workflow/source-event key prevents duplicate runs.

`workflow_action_executions` is the ordered action queue and immutable action
snapshot. Its globally unique idempotency key protects side effects.

`workflow_action_attempts` retains append-only attempt history.

Definitions and actions are archived instead of deleted after activation so
execution history remains referentially intact.

## Phase 87.1 Definition of Done

- Lifecycle, trigger, action, run, and attempt models are fixed.
- Initial trigger and action catalogs are intentionally allowlisted.
- Transaction, retry, idempotency, stale-claim, and failure policies are fixed.
- Tenant, permission, plan, loop, and abuse boundaries are explicit.
- V30 persistence constraints and indexes can be implemented without guessing.

## Management API

The authenticated tenant API is rooted at `/api/v1/workflows`:

- `GET /api/v1/workflows` and `GET /api/v1/workflows/{id}` require
  `WORKFLOW_VIEW`.
- `POST /api/v1/workflows` and `PUT /api/v1/workflows/{id}` require
  `WORKFLOW_MANAGE` and save a draft or paused structural revision.
- `POST /api/v1/workflows/{id}/activate` validates the complete definition,
  serializes activation on the organization subscription, and enforces the
  active-workflow plan limit.
- `POST /api/v1/workflows/{id}/pause` stops new executions from being created.
- `POST /api/v1/workflows/{id}/archive` permanently closes a draft or paused
  workflow. Active workflows must be paused first.

Action order comes from array order and cannot be supplied independently.
Each action has a stable public ID for its definition revision. Supported
configuration fields are intentionally closed:

- `CREATE_TASK`: `titleTemplate`, `priority`, `dueDateOffsetDays`,
  `assigneePolicy`, and `relationPolicy`.
- `SEND_IN_APP_NOTIFICATION`: `titleTemplate`, `messageTemplate`, and
  `recipientPolicy`.

Unknown fields, arbitrary IDs, unavailable event fields, malformed template
tokens, oversized JSON, excessive condition nesting, and direct task-creation
loops are rejected.

## Phase 87.2 Definition of Done

- V30 creates the durable workflow definition, queue, snapshot, and history
  tables with same-organization composite foreign keys.
- V31 separates structural revisions from optimistic locking and retains old
  action revisions.
- Workflow permissions and existing plan limits are verified by migration
  tests on MySQL 8.4.

## Phase 87.3 Definition of Done

- All six workflow tables have matching JPA entities and repositories.
- Definition, trigger, action, execution, action-execution, and attempt enums
  match database constraints.
- Repository tests persist and read the complete tenant-bound workflow graph.

## Phase 87.4 Definition of Done

- Tenant-scoped create, update, read, activate, pause, and archive APIs exist.
- Draft and paused edits create immutable action definition revisions.
- Feature access, active-workflow plan capacity, permission checks, lifecycle
  validation, audit events, and safe JSON configuration validation are active.
- Event dispatch and background action execution intentionally remain for the
  next Phase 87 steps.
