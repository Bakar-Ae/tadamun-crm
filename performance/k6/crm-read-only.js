import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = (__ENV.CRM_BASE_URL || 'http://backend:8081')
  .replace(/\/$/, '');
const VIRTUAL_USERS = Number.parseInt(__ENV.LOAD_TEST_VUS || '5', 10);
const DURATION = __ENV.LOAD_TEST_DURATION || '30s';
const PAUSE_SECONDS = Number.parseFloat(
  __ENV.LOAD_TEST_PAUSE_SECONDS || '0.1',
);

const endpoints = [
  { name: 'auth_me', path: '/api/v1/auth/me' },
  { name: 'dashboard', path: '/api/v1/dashboard/summary' },
  { name: 'customers', path: '/api/v1/customers?page=0&size=20' },
  { name: 'leads', path: '/api/v1/leads?page=0&size=20' },
  { name: 'tasks', path: '/api/v1/tasks?page=0&size=20' },
  { name: 'report_summary', path: '/api/v1/reports/summary' },
  { name: 'subscription_usage', path: '/api/v1/subscription/usage' },
];

const endpointDurations = Object.fromEntries(
  endpoints.map(({ name }) => [
    name,
    new Trend(`crm_${name}_duration`, true),
  ]),
);
const readFailures = new Rate('crm_read_failures');

const endpointThresholds = Object.fromEntries(
  endpoints.map(({ name }) => [
    `http_req_duration{endpoint:${name}}`,
    ['p(95)<1000', 'p(99)<2000'],
  ]),
);

export const options = {
  scenarios: {
    read_only_crm: {
      executor: 'constant-vus',
      vus: Number.isFinite(VIRTUAL_USERS) ? VIRTUAL_USERS : 5,
      duration: DURATION,
      gracefulStop: '5s',
    },
  },
  thresholds: {
    checks: ['rate>0.99'],
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<750', 'p(99)<1500'],
    crm_read_failures: ['rate<0.01'],
    ...endpointThresholds,
  },
  discardResponseBodies: false,
  summaryTrendStats: ['avg', 'med', 'p(90)', 'p(95)', 'p(99)', 'max'],
};

function requestHeaders(token, organizationId) {
  return {
    Accept: 'application/json',
    Authorization: `Bearer ${token}`,
    'X-Organization-Id': String(organizationId),
  };
}

function requireSuccessfulResponse(response, operation) {
  if (response.status !== 200) {
    fail(`${operation} failed with HTTP ${response.status}`);
  }
}

export function setup() {
  const email = __ENV.CRM_ADMIN_EMAIL;
  const password = __ENV.CRM_ADMIN_PASSWORD;
  if (!email || !password) {
    fail(
      'Set LOAD_TEST_ADMIN_EMAIL and LOAD_TEST_ADMIN_PASSWORD in .env',
    );
  }

  const loginResponse = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ email, password }),
    {
      headers: { 'Content-Type': 'application/json' },
      tags: { endpoint: 'setup_login' },
      timeout: '10s',
    },
  );
  requireSuccessfulResponse(loginResponse, 'Load-test login');
  const accessToken = loginResponse.json('accessToken');
  if (!accessToken) {
    fail('Load-test login returned no access token');
  }

  const workspaceResponse = http.get(`${BASE_URL}/api/v1/workspaces`, {
    headers: {
      Accept: 'application/json',
      Authorization: `Bearer ${accessToken}`,
    },
    tags: { endpoint: 'setup_workspaces' },
    timeout: '10s',
  });
  requireSuccessfulResponse(workspaceResponse, 'Workspace discovery');
  const workspaces = workspaceResponse.json();
  if (!Array.isArray(workspaces) || workspaces.length === 0) {
    fail('The load-test account has no active workspace');
  }

  const requestedOrganizationId = __ENV.LOAD_TEST_ORGANIZATION_ID;
  const workspace = requestedOrganizationId
    ? workspaces.find(
      (candidate) => String(candidate.organizationId)
        === String(requestedOrganizationId),
    )
    : workspaces[0];
  if (!workspace) {
    fail('LOAD_TEST_ORGANIZATION_ID is not an active workspace');
  }

  const headers = requestHeaders(accessToken, workspace.organizationId);
  for (const endpoint of endpoints) {
    const response = http.get(`${BASE_URL}${endpoint.path}`, {
      headers,
      tags: { endpoint: `warmup_${endpoint.name}` },
      timeout: '10s',
    });
    requireSuccessfulResponse(response, `Warm-up ${endpoint.name}`);
  }

  return {
    accessToken,
    organizationId: workspace.organizationId,
  };
}

export default function runReadOnlyScenario(context) {
  const endpoint = endpoints[__ITER % endpoints.length];
  const response = http.get(`${BASE_URL}${endpoint.path}`, {
    headers: requestHeaders(
      context.accessToken,
      context.organizationId,
    ),
    tags: { endpoint: endpoint.name },
    timeout: '10s',
  });
  const successful = check(response, {
    [`${endpoint.name} returns HTTP 200`]: (result) =>
      result.status === 200,
  });
  endpointDurations[endpoint.name].add(response.timings.duration);
  readFailures.add(!successful);
  sleep(Number.isFinite(PAUSE_SECONDS) ? PAUSE_SECONDS : 0.1);
}

function metricValue(data, metricName, statistic) {
  return data.metrics[metricName]?.values?.[statistic] ?? null;
}

function formatNumber(value, digits = 2) {
  return value === null || value === undefined
    ? 'n/a'
    : Number(value).toFixed(digits);
}

export function handleSummary(data) {
  const endpointResults = Object.fromEntries(
    endpoints.map(({ name }) => [name, {
      averageMs: metricValue(data, `crm_${name}_duration`, 'avg'),
      p95Ms: metricValue(data, `crm_${name}_duration`, 'p(95)'),
      p99Ms: metricValue(data, `crm_${name}_duration`, 'p(99)'),
      maximumMs: metricValue(data, `crm_${name}_duration`, 'max'),
    }]),
  );
  const failedThresholds = Object.entries(data.metrics)
    .flatMap(([metricName, metric]) =>
      Object.entries(metric.thresholds || {})
        .filter(([, threshold]) => !threshold.ok)
        .map(([thresholdName]) => `${metricName}: ${thresholdName}`));
  const report = {
    generatedAt: new Date().toISOString(),
    profile: {
      virtualUsers: VIRTUAL_USERS,
      duration: DURATION,
      pauseSeconds: PAUSE_SECONDS,
      readOnly: true,
    },
    totals: {
      requests: metricValue(data, 'http_reqs', 'count'),
      requestsPerSecond: metricValue(data, 'http_reqs', 'rate'),
      failedRequestRate: metricValue(data, 'http_req_failed', 'rate'),
      averageMs: metricValue(data, 'http_req_duration', 'avg'),
      p95Ms: metricValue(data, 'http_req_duration', 'p(95)'),
      p99Ms: metricValue(data, 'http_req_duration', 'p(99)'),
    },
    endpointResults,
    thresholdsPassed: failedThresholds.length === 0,
    failedThresholds,
  };

  const endpointLines = Object.entries(endpointResults)
    .map(([name, result]) =>
      `  ${name.padEnd(20)} p95=${formatNumber(result.p95Ms)} ms`)
    .join('\n');
  const summary = [
    '',
    'CRM read-only load-test summary',
    `  requests: ${formatNumber(report.totals.requests, 0)}`,
    `  throughput: ${formatNumber(report.totals.requestsPerSecond)} req/s`,
    `  failure rate: ${formatNumber(
      (report.totals.failedRequestRate || 0) * 100,
    )}%`,
    `  overall p95: ${formatNumber(report.totals.p95Ms)} ms`,
    endpointLines,
    `  thresholds: ${report.thresholdsPassed ? 'PASS' : 'FAIL'}`,
    '  JSON: tmp/load-test/summary.json',
    '',
  ].join('\n');

  return {
    stdout: summary,
    '/results/summary.json': JSON.stringify(report, null, 2),
  };
}
