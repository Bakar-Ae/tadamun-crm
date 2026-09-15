# Render and Aiven Free Demo

This is a constrained demo, not the Version 3 production release. Keep the
existing local Docker environment and backups unchanged. Never import the local
Docker `.env` into Render or publish real customer data as demo content.

## Live Demo

Status on 2026-09-15: deployment and basic live smoke checks passed.

- Frontend: <https://tadamun-crm-1.onrender.com/login>
- Backend health: <https://tadamun-crm.onrender.com/actuator/health>
- Both services deployed commit `8f406bb` from `main`.
- Database: the separate Aiven MySQL 8.4 demo service, initialized through V35.

## Backend

- Repository: `Bakar-Ae/tadamun-crm`, branch `main`.
- Service name: `tadamun-crm` (a different available name is also fine).
- Language: Docker; region: Frankfurt; compute: Free ($0/month).
- Root directory: `backend`; Dockerfile: `./Dockerfile`; build context: `.`.
- Health check: `/actuator/health`.
- Leave the Docker command unset. Render supplies `PORT`; Spring reads it.
- Do not deploy until the entrypoint/upload changes are tested, committed, and
  pushed and the private environment has been imported.

Generate the private environment once, using the downloaded Aiven CA certificate:

```powershell
powershell -NoProfile -File .\scripts\New-CrmRenderEnvironment.ps1 -CertificatePath "$HOME\Downloads\ca.pem"
```

The generator reads `.env.aiven.local`, creates independent random JWT, API,
encryption, and bootstrap credentials, and writes `.env.render.local`. Both files
must remain Git-ignored. Re-running refuses to overwrite keys. Keep a secure
backup of the generated secrets; replacing encryption keys can make stored
credentials unreadable. Import the generated file with Render's **Add from .env**.
Do not send screenshots of populated secret fields.

The Docker entrypoint converts `AIVEN_CA_CERT_BASE64` into a temporary Java trust
store and adds Connector/J `sslMode=VERIFY_IDENTITY`. Only MySQL uses this custom
CA; the trust roots for outbound HTTPS services are unchanged. Do not add competing
SSL options to `SPRING_DATASOURCE_URL`. Without the Aiven certificate variable,
the local Docker startup behavior remains unchanged.

The initial CORS/frontend origin was `http://localhost:5173` for local smoke
testing. The hosted backend now sets both `CORS_ALLOWED_ORIGINS` and
`APP_FRONTEND_BASE_URL` to `https://tadamun-crm-1.onrender.com`. The private
`.env.render.local` file has been synchronized with this origin; the existing
local Docker `.env` remains unchanged. Save and deploy backend environment
changes so the running service reads the new values.

The bootstrap login is `admin@crm.com`; its generated password stays in the
private environment file. Complete the mandatory password change at first login.
After changing it, use the new password for normal sign-in, not the old bootstrap
value. The operator confirmed signing out and signing back in after setup.

## Frontend

- Service name: `tadamun-crm-1`; type: Render Static Site.
- Repository: `Bakar-Ae/tadamun-crm`; branch: `main`; root directory: `frontend`.
- Build command: `npm ci && npm run build`; publish directory: `dist`, relative
  to the root directory. Do not enter `frontend/dist` in that field.
- Public build variable:
  `VITE_API_BASE_URL=https://tadamun-crm.onrender.com/api/v1`.
- Redirects/Rewrites: Source `/*`, Destination `/index.html`, Action `Rewrite`.
  This serves the React app for direct visits and page refreshes on client routes.
- Never import the backend environment file into the frontend service. Client
  bundles must not contain database passwords, bootstrap passwords, or keys.

## Limits and Pending Checks

- The free instance has 512 MB RAM and 0.1 CPU. The generated JVM, connection-pool,
  and HTTP-thread settings are conservative starting values, not a load guarantee.
  The local restart/login smoke test passed under those limits, but startup was
  slow. The first Render deployment reported 5m34s including build and deployment;
  this is not a cold-start benchmark. Basic hosted workflows passed, but idle
  wake-up latency and sustained performance are not guaranteed.
- `ATTACHMENT_UPLOADS_ENABLED=false` rejects uploads before storage writes. Reads
  and deletes remain available. Do not re-enable uploads without durable storage.
  The default is `true` for existing local installations.
- Email and Stripe are disabled. Do not imply external email or billing works.
- Free web services sleep after inactivity. Workflow/integration/webhook workers
  only run while the service is awake; scheduled delivery is not continuous.
- Use a separate Aiven database for the demo. Local backups/customer records are
  not automatically copied to it. The initial connection currently uses the
  service administrator; restrict application database privileges before any
  production use.
- The scheduled Windows backup task protects the local Docker MySQL database,
  not this Aiven database. A verified backup/restore process for hosted data is
  still required before relying on the demo for real records.
- Hosted multi-tenant, billing, email, integration, load, and disaster-recovery
  release checks are not established by the basic smoke test. This deployment
  does not complete the production release gate or authorize a `v3.0.0` tag.

## Local Verification (2026-09-15)

- The focused attachment/storage/error-handler suite passed: 9 tests, no failures
  or errors. Uploads disabled in configuration are rejected without storing files;
  existing reads and deletes remain available.
- The Docker image built successfully with the new entrypoint. Startup-script
  checks covered a valid CA, rejection of an invalid CA, and startup without the
  optional Aiven certificate.
- Both private environment files are Git-ignored and untracked. The generator
  refuses to overwrite an existing environment file and its encryption keys.
- A temporary local container connected to Aiven using certificate and hostname
  verification and applied all 35 Flyway migrations to the initially empty demo
  database. A separate read-only query confirmed 35 successful migrations, latest
  version 35, and 44 tables. No local CRM data or backup was imported.
- First-time initialization needed longer than the original 12-minute test
  window. Its local CPU cap was temporarily raised from 0.1 to 1 CPU to finish
  setup while retaining the 512 MiB memory limit. This is not evidence that an
  empty-database deployment will meet Render's startup deadline on free compute.
- A separate restart at 0.1 CPU and 512 MiB RAM, with no swap, passed against the
  initialized database. `/actuator/health` returned `UP` after 716 seconds
  (11 minutes 56 seconds). This is a slow local cold start, not a prediction of
  Render's timing or a guarantee of acceptable interactive performance.
- `/actuator/info` reported version `3.0.0`. Bootstrap login, authenticated
  `/api/v1/auth/me`, and logout passed. At this point the mandatory first-login
  password change was still required; the automated test did not choose a new
  password for the operator.
- The final memory sample was 354.6 MiB out of 512 MiB, with no OOM termination.
  The temporary container was removed. Existing local CRM containers were not
  stopped or reconfigured. Public deployment was verified separately below.

## Public Verification (2026-09-15)

Automated HTTP checks:

- Backend health returned HTTP 200 with `UP`; application version was `3.0.0`.
- One earlier health request timed out after 90 seconds; a retry returned `UP`.
  The cause was not isolated. The passing smoke checks do not establish a
  response-time or availability guarantee.
- Before the operator changed the bootstrap password, public login,
  authenticated `/api/v1/auth/me`, and logout passed. The test refresh session
  was revoked, and no passwords or tokens were printed.
- Frontend `/` and `/login` returned HTTP 200 with the same app shell, verifying
  the SPA rewrite.
- Login preflight allowed the exact frontend origin with the content-type
  header. Customer preflight also allowed the authorization header.
- An unapproved origin was rejected with HTTP 403 and no allow-origin header.
- An unauthenticated customer-list request was denied with HTTP 401.

Operator-confirmed browser checks:

- Created `Deployment Test`; the customer remained after refreshing.
- Renamed it to `Deployment Test Updated`; the new name remained after refreshing.
- Archived the test customer successfully. The UI preserves archived history;
  this was not a permanent deletion test.
- Signed out and signed back in using the new password, returning to the dashboard.

During this test, the local Windows backup task ran at 20:00 and recorded a
successful isolated restore at 20:00:31. It was not a backup of the Aiven demo.

References: [Render free limits](https://render.com/docs/free),
[environment import](https://render.com/docs/configure-environment-variables),
[monorepo paths](https://render.com/docs/monorepo-support),
[SPA rewrites](https://render.com/docs/redirects-rewrites),
[Connector/J security](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html).
