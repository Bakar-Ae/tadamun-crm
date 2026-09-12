# Monitoring And Logging Plan

## Goal

Detect service degradation and failed asynchronous work while preserving
tenant isolation and keeping logs safe for operational use.

## Request Observability

Every backend response includes `X-Request-Id`. A caller-supplied value is
accepted only when it uses safe characters and is between 8 and 100
characters; otherwise the backend generates a UUID.

Backend log lines include these MDC fields:

- `requestId`: correlation identifier for one HTTP request.
- `organizationId`: trusted organization selected by authenticated tenant or
  public API resolution.
- `userId`: authenticated CRM user when one exists.

The request completion message records the HTTP method, matched route, status,
and duration. It never records query strings, request bodies, authorization
headers, API keys, provider payloads, or credentials.

## Metrics

Spring Boot Actuator exposes metrics to platform administrators only:

- `crm.http.server.requests`: duration and count by method, matched route,
  status, and whether a trusted tenant was present.
- `crm.operations`: completed, failed, recovered, duplicate, and ignored work
  for billing webhooks, outbound webhooks, workflows, and integration
  deliveries.
- Standard JVM, process, datasource, and HTTP metrics supplied by Actuator.

Organization and user identifiers are intentionally not metric labels. This
avoids tenant information leakage and unbounded metric cardinality. Runtime
metrics reset when the backend process restarts; a production metrics backend
should scrape and retain them externally.

## Initial Alert Targets

- Backend health is not `UP` for two consecutive checks.
- HTTP 5xx responses exceed 5% for five minutes.
- HTTP p95 latency exceeds 750 ms for ten minutes.
- Any `crm.operations` failed or recovered counter increases unexpectedly.
- Database, attachment storage, or host disk usage exceeds 80%.
- A scheduled backup is missing or its restore verification fails.

Tune these targets after observing normal production traffic. Alerts should
link to the request ID or affected subsystem, not include secrets or payloads.

## Access Policy

`/actuator/health` and `/actuator/info` remain public for deployment probes.
`/actuator/metrics` and every metric detail endpoint require
`PLATFORM_ADMIN`. Organization owners and ordinary CRM administrators cannot
read operational metrics.
