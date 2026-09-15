# Render and Aiven Free Demo

This is a constrained demo, not the Version 3 production release. Keep the
existing local Docker environment and backups unchanged. Never import the local
Docker `.env` into Render or publish real customer data as demo content.

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

The initial CORS/frontend origin is `http://localhost:5173` for local smoke
testing, not a guessed public hostname. After creating the frontend, update
`CORS_ALLOWED_ORIGINS` and `APP_FRONTEND_BASE_URL` to its actual HTTPS origin.
The bootstrap login is `admin@crm.com`; its generated password stays in the
private environment file. Complete the mandatory password change at first login.

## Limits and Pending Checks

- The free instance has 512 MB RAM and 0.1 CPU. The generated JVM, connection-pool,
  and HTTP-thread settings are conservative starting values, not a load guarantee.
  The local restart/login smoke test passed under those limits, but startup was
  slow. Verify actual Render startup time and normal workflows after deployment.
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
- Frontend setup, real cloud health/login checks, and public URL verification
  remain pending. This configuration does not complete the production release gate.

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
  `/api/v1/auth/me`, and logout passed. The mandatory first-login password change
  remains required; no user-chosen password was set by the smoke test.
- The final memory sample was 354.6 MiB out of 512 MiB, with no OOM termination.
  The temporary container was removed. Existing local CRM containers were not
  stopped or reconfigured. Public Render deployment remains pending.

References: [Render free limits](https://render.com/docs/free),
[environment import](https://render.com/docs/configure-environment-variables),
[monorepo paths](https://render.com/docs/monorepo-support),
[Connector/J security](https://dev.mysql.com/doc/connector-j/en/connector-j-connp-props-security.html).
