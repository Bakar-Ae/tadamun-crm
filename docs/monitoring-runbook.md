# CRM Monitoring Runbook

## 1. Check Containers

```powershell
docker compose ps
docker compose logs backend --tail 120
```

All four local services should be running; MySQL, backend, frontend, and
Mailpit should report healthy after startup settles.

## 2. Check Backend Health

```powershell
$health = Invoke-RestMethod "http://localhost:8081/actuator/health"
$health.status
```

Expected result: `UP`. If it is not, inspect backend and MySQL logs before
restarting anything.

## 3. Inspect Metrics

Use a token belonging to a platform administrator:

```powershell
$headers = @{ Authorization = "Bearer $token" }
Invoke-RestMethod `
  -Uri "http://localhost:8081/actuator/metrics" `
  -Headers $headers

Invoke-RestMethod `
  -Uri "http://localhost:8081/actuator/metrics/crm.http.server.requests" `
  -Headers $headers

Invoke-RestMethod `
  -Uri "http://localhost:8081/actuator/metrics/crm.operations" `
  -Headers $headers
```

An organization admin should receive `403`; an unauthenticated request should
receive `401`. Do not weaken this restriction to simplify dashboard access.

## 4. Trace A Failed Request

Copy `X-Request-Id` from the API response, then search recent backend logs:

```powershell
docker compose logs backend --since 30m 2>&1 |
  Select-String "requestId=PASTE_REQUEST_ID_HERE"
```

Use the matched route, status, duration, organization ID, and user ID to narrow
the incident. Do not paste tokens, API keys, webhook signatures, or full
provider payloads into tickets or chat.

## 5. Investigate Background Failures

```powershell
docker compose logs backend --since 30m 2>&1 |
  Select-String "webhook|workflow|integration|billing|Recovered stale"
```

Check `crm.operations` for the affected `subsystem` and `outcome`. A recovered
counter increase means an abandoned claim was returned to bounded retry; it is
an operational warning, not proof of data loss.

## 6. Escalation Checklist

1. Record the UTC/local incident time, route, status, and request ID.
2. Confirm container and health endpoint state.
3. Check whether the failure affects one tenant or all tenants.
4. Preserve logs and metric snapshots before a restart.
5. Follow the backup/restore runbook before any destructive database action.
