# Phase 87: Workflow Automation Implementation

## Delivered

- Organization-scoped workflow definitions, triggers, ordered actions, execution records, and attempt history.
- CRM event dispatch from the existing durable `webhook_events` envelope.
- Conditions with `all`/`any`, `EQUALS`, `NOT_EQUALS`, `IN`, and `EXISTS`.
- `CREATE_TASK` and `SEND_IN_APP_NOTIFICATION` actions.
- Transactional action processing with idempotent notification delivery.
- Bounded retry delays of 1, 5, 15, and 60 minutes.
- Stale claim recovery and configurable worker polling.
- Execution history plus permission-protected retry and cancel endpoints.
- A Workflows page for draft creation, activation, pausing, archiving, and run inspection.
- Tenant-context routing for every workflow API endpoint.
- A plan-aware upgrade state when workflow automation is unavailable.

## API

- `GET /api/v1/workflows`
- `POST /api/v1/workflows`
- `PUT /api/v1/workflows/{id}`
- `POST /api/v1/workflows/{id}/activate`
- `POST /api/v1/workflows/{id}/pause`
- `POST /api/v1/workflows/{id}/archive`
- `GET /api/v1/workflows/executions`
- `GET /api/v1/workflows/{id}/executions`
- `GET /api/v1/workflows/executions/{publicExecutionId}`
- `POST /api/v1/workflows/executions/{publicExecutionId}/retry`
- `POST /api/v1/workflows/executions/{publicExecutionId}/cancel`

`WORKFLOW_VIEW` protects definitions and history. `WORKFLOW_MANAGE` protects mutations and recovery commands.

## Operations

Worker settings are exposed through `WORKFLOW_WORKER_ENABLED`, `WORKFLOW_WORKER_BATCH_SIZE`, `WORKFLOW_WORKER_POLL_INTERVAL_MS`, `WORKFLOW_WORKER_INITIAL_DELAY_MS`, and `WORKFLOW_STALE_CLAIM_SECONDS`.

Migrations `V30` and `V31` create and version the workflow schema. `V32` keeps action attempt history valid across manual retry cycles. `V33` removes obsolete five-attempt counter caps so retries remain monotonic across multiple actions and manual retry cycles.

## Verification

From `backend`:

```powershell
.\mvnw.cmd test
```

From `frontend`:

```powershell
npm test -- --run
npm run lint
npm run build
```

From the repository root:

```powershell
docker compose up -d --build
docker compose ps
docker exec crm_mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -e "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 3;"'
```

Open `http://localhost:5173/workflows`, create a draft, activate it, then create a matching CRM record. The execution should move from queued or processing to succeeded and the configured task or notification should appear once.
