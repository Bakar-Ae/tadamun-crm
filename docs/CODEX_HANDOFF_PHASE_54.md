# Codex Handoff - Phase 54 Complete

Checkpoint date: 2026-06-27  
Project root: `C:\Users\hp\Documents\Codex\2026-06-01\you-are-a-principal-software-architect\crm-system`

## 1. Project Overview

- Project: **Tadamun CRM (Tadamun Business Solutions)**
- Goal: A production-oriented CRM for sales and customer management, including users, permissions, customers, leads, contacts, tasks, notes, reporting, notifications, and audit history.
- Architecture: Modular Spring Boot monolith with a React single-page frontend and MySQL database.
- Backend: Java 21, Spring Boot 4.0.6, Spring MVC, Spring Security, JWT, JPA/Hibernate, Flyway, Validation, Actuator, Spring Mail, Maven.
- Frontend: React 19, TypeScript 6, Vite 8, Tailwind CSS 4, Axios, React Router, Framer Motion, Recharts, Lucide, React Hook Form, Zod, TanStack Table, cmdk, react-hot-toast.
- Database: MySQL 8.4, database `crm_db`; test database `crm_test_db`.
- Docker services: MySQL, backend, frontend/Nginx, and Mailpit.
- Main URLs/ports:
  - Frontend: `http://localhost:5173`
  - Backend API: `http://localhost:8081/api/v1`
  - Actuator: `http://localhost:8081/actuator`
  - MySQL host port: `3307`
  - Mailpit UI: `http://localhost:8025`
  - Mailpit SMTP: `localhost:1025`

## 2. Current Status

- Version 1 phases 1-50 are complete and tagged `v1.0.0`.
- Version 2 has completed Phases 51-54.
- Phase 51: Version 2 requirements reset.
- Phase 52: Granular permission model and Flyway migration V11.
- Phase 53: Role-permission management API and frontend UI.
- Phase 54: Secure email infrastructure.

Phase 54 delivered:

- Local SMTP testing through Mailpit.
- Spring Mail environment configuration and timeouts.
- Password-reset email delivery without logging reset tokens.
- Masked email-address logging.
- Configurable sender and reply-to addresses.
- Tests for disabled, successful, and failed email delivery.
- Email infrastructure design and production runbook.
- Local Mailpit successfully received a password-reset message.

Git checkpoint:

- Branch: `main`
- Latest commit: `3f36488 Add secure email infrastructure`
- Remote: `https://github.com/Bakar-Ae/tadamun-crm.git`
- Phase 54 was pushed.
- Working tree was clean at handoff time.
- Check again before editing: `git status --short`

Latest validation:

- Backend package/tests passed. The suite should contain 27 tests after Phase 54.
- Frontend build and lint passed during the latest frontend validation.
- Backend, frontend, MySQL, and Mailpit containers were healthy.

## 3. Working Features

### Authentication and security

- Login and logout.
- BCrypt password hashing.
- JWT access tokens and database-backed refresh tokens.
- Refresh-token rotation/revocation flow.
- Current-user endpoint.
- Change password and forced password change.
- Forgot-password and one-use password reset tokens.
- Login attempt rate limiting.
- Role-based access and granular permissions.
- Permission-aware frontend routes and navigation.
- Admin role permissions are protected from unsafe mutation.
- CORS configuration, Nginx security headers, and public health/info endpoints.

### CRM modules

- Users: create, list, search, update, role change, deactivate.
- Customers: create, list, search, filter, update, archive; frontend restore through update.
- Leads: create, list, search, filter, update, archive, convert to customer.
- Contacts: create, list, search, filter, update, archive; frontend restore through update.
- Tasks: create, assign, list, search, filter, update, status changes, completion rules.
- Notes: customer and lead notes; create, list, and update.
- Dashboard: summary metrics and polished responsive UI.
- Reports: basic summary reporting.
- Notifications: list, unread count, and mark as read.
- Audit logs: important actions, permission changes, readable formatting, filters, and pagination.
- Search, filters, and pagination across the main CRM tables.
- Detail drawers and activity timelines.
- Quick-create actions for supported modules.
- Light and dark themes, responsive sidebar/layout, loading/error/empty states.

### Operations

- Flyway-managed schema.
- Docker health checks.
- Actuator health and info.
- Local email testing with Mailpit.
- Manual backup/restore documentation and ignored SQL backup files.
- Security, deployment, monitoring, and production runbooks.
- Git repository and GitHub remote.

## 4. Broken or Unclear Items

No known active compile or runtime blocker exists at this checkpoint.

Remaining or unclear work:

- Production email is not connected. Mailpit is development-only. Production needs a provider, verified company domain, SPF, DKIM, DMARC, credentials, and provider production access.
- Phase 55 HTML email templates are not started; current password-reset email is plain text.
- Ownership/data scoping (`own`, `team`, `all`) is not fully implemented. Some access is permission-based but data can still be globally visible to an authorized role.
- Dashboard authorization and data scope need alignment with granular permissions.
- Task permission expressions are complex and need dedicated regression tests.
- Lead restore was not explicitly verified. Lead archive and conversion work.
- Some confirmation interactions may still use browser `window.confirm`.
- CI/CD and production deployment are not implemented.
- HTTPS, automated backups, production monitoring/alerting, and performance testing remain.
- File attachments, calendar, advanced reports, exports, team dashboards, and global search remain.

Known non-blocking warnings:

- Git may warn that LF will be replaced by CRLF on Windows.
- Mockito may warn about dynamic agent loading.
- `JwtAuthenticationFilter` may use a deprecated API.
- Spring may warn about direct `PageImpl` JSON serialization stability.
- Flyway may warn that MySQL 8.4 is newer than its latest verified MySQL version.
- The dashboard frontend chunk is relatively large, but production build passes.

Security cautions:

- Never commit `.env`, database dumps, real SMTP credentials, JWT secrets, or admin passwords.
- Do not expose Mailpit publicly.
- Do not run `docker compose down -v` unless intentionally deleting the database volume.

## 5. Important Files and Folders

### Backend

- Main source: `backend/src/main/java/com/crm/backend`
- Main application: `backend/src/main/java/com/crm/backend/BackendApplication.java`
- Maven: `backend/pom.xml`
- Runtime config: `backend/src/main/resources/application.properties`
- Test config: `backend/src/test/resources/application.properties`
- Flyway migrations: `backend/src/main/resources/db/migration`
- Tests: `backend/src/test/java/com/crm/backend`
- Docker image: `backend/Dockerfile`

Backend module folders:

- `backend/src/main/java/com/crm/backend/auth`
- `backend/src/main/java/com/crm/backend/security`
- `backend/src/main/java/com/crm/backend/user`
- `backend/src/main/java/com/crm/backend/role`
- `backend/src/main/java/com/crm/backend/permission`
- `backend/src/main/java/com/crm/backend/customer`
- `backend/src/main/java/com/crm/backend/lead`
- `backend/src/main/java/com/crm/backend/contact`
- `backend/src/main/java/com/crm/backend/task`
- `backend/src/main/java/com/crm/backend/note`
- `backend/src/main/java/com/crm/backend/dashboard`
- `backend/src/main/java/com/crm/backend/report`
- `backend/src/main/java/com/crm/backend/notification`
- `backend/src/main/java/com/crm/backend/audit`
- `backend/src/main/java/com/crm/backend/email`
- `backend/src/main/java/com/crm/backend/common`

Important auth/security files:

- `backend/src/main/java/com/crm/backend/auth/AuthController.java`
- `backend/src/main/java/com/crm/backend/auth/AuthService.java`
- `backend/src/main/java/com/crm/backend/auth/RefreshTokenService.java`
- `backend/src/main/java/com/crm/backend/auth/PasswordResetService.java`
- `backend/src/main/java/com/crm/backend/auth/LoginAttemptService.java`
- `backend/src/main/java/com/crm/backend/security/SecurityConfig.java`
- `backend/src/main/java/com/crm/backend/security/JwtService.java`
- `backend/src/main/java/com/crm/backend/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/crm/backend/security/CustomUserDetails.java`
- `backend/src/main/java/com/crm/backend/security/CustomUserDetailsService.java`
- `backend/src/main/java/com/crm/backend/email/EmailService.java`
- `backend/src/test/java/com/crm/backend/email/EmailServiceTest.java`

Permissions:

- `backend/src/main/java/com/crm/backend/permission/Permission.java`
- `backend/src/main/java/com/crm/backend/permission/PermissionName.java`
- `backend/src/main/java/com/crm/backend/permission/PermissionRepository.java`
- `backend/src/main/java/com/crm/backend/permission/PermissionService.java`
- `backend/src/main/java/com/crm/backend/permission/PermissionController.java`
- `backend/src/main/java/com/crm/backend/role/RolePermissionService.java`
- `backend/src/main/java/com/crm/backend/role/RolePermissionController.java`

Controllers, services, repositories, entities, mappers, and DTOs are grouped inside each module. DTOs are normally under each module's `dto` folder.

### Frontend

- Main source: `frontend/src`
- Router/root: `frontend/src/App.tsx`
- API client: `frontend/src/services/api.ts`
- API services: `frontend/src/services`
- Pages: `frontend/src/pages`
- App layout: `frontend/src/layouts/AppLayout.tsx`
- Shared UI: `frontend/src/components/ui`
- Main styles/themes: `frontend/src/index.css` and `frontend/src/App.css`
- Utilities: `frontend/src/lib`
- Package config: `frontend/package.json`
- Docker image: `frontend/Dockerfile`
- Nginx config: `frontend/nginx.conf`

Important pages:

- `frontend/src/pages/LoginPage.tsx`
- `frontend/src/pages/DashboardPage.tsx`
- `frontend/src/pages/UsersPage.tsx`
- `frontend/src/pages/CustomersPage.tsx`
- `frontend/src/pages/LeadsPage.tsx`
- `frontend/src/pages/ContactsPage.tsx`
- `frontend/src/pages/TasksPage.tsx`
- `frontend/src/pages/NotesPage.tsx`
- `frontend/src/pages/ReportsPage.tsx`
- `frontend/src/pages/AuditLogsPage.tsx`
- `frontend/src/pages/NotificationsPage.tsx`
- `frontend/src/pages/RolePermissionsPage.tsx`
- `frontend/src/pages/ChangePasswordPage.tsx`
- `frontend/src/pages/ForgotPasswordPage.tsx`
- `frontend/src/pages/ResetPasswordPage.tsx`

Important shell/components:

- `frontend/src/components/QuickCreateMenu.tsx`
- `frontend/src/components/CommandMenu.tsx`
- `frontend/src/components/NotificationPanel.tsx`
- `frontend/src/components/SettingsPanel.tsx`
- `frontend/src/components/UserMenu.tsx`
- `frontend/src/components/PreLoginIntro.tsx`
- `frontend/src/lib/permissions.ts`
- `frontend/src/lib/formatters.ts`
- `frontend/src/lib/errors.ts`
- `frontend/src/lib/quickCreate.ts`
- `frontend/src/lib/dashboardPreferences.ts`

### Root/config/docs

- Compose: `docker-compose.yml`
- Private local environment: `.env` (ignored; never commit)
- Environment template: `.env.example`
- Git ignore: `.gitignore`
- Main readme: `README.md`
- Phase 54 design: `docs/phase-54-email-infrastructure-design.md`
- Production email runbook: `docs/phase-54-production-email-runbook.md`
- Version 2 requirements: `docs/version-2-requirements.md`
- This checkpoint: `docs/CODEX_HANDOFF_PHASE_54.md`

## 6. Database / Docker Setup

Database:

- MySQL image: `mysql:8.4`
- Database: `crm_db`
- Test database: `crm_test_db`
- App database user is configured by `MYSQL_USER`/`SPRING_DATASOURCE_USERNAME` in private `.env`.
- Passwords are configured by private `.env`; do not copy them into chat, code, or documentation.

Docker services:

- `mysql` / container `crm_mysql`: host `3307`, container `3306`.
- `backend` / container `crm_backend`: host/container `8081`.
- `frontend` / container `crm_frontend`: host `5173`, container `80`.
- `mailpit` / container `crm_mailpit`: UI `8025`, SMTP `1025`.
- Persistent database volume: `crm_mysql_data`.

Flyway status:

- V1 roles/users.
- V2 customers/contacts.
- V3 leads.
- V4 tasks/notes.
- V5 audit logs.
- V6 default admin seed.
- V7 refresh tokens.
- V8 password-change requirement.
- V9 password-reset tokens.
- V10 notifications.
- V11 permissions and role-permission mappings.
- V11 was applied successfully.

Inspect schema interactively using the username from `.env`:

```powershell
docker exec -it crm_mysql mysql -u<MYSQL_USER> -p crm_db
```

Then run `SHOW TABLES;`, or check Flyway directly:

```powershell
docker exec -it crm_mysql mysql -u<MYSQL_USER> -p crm_db -e "SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;"
```

Known database issue: none active. Avoid editing applied migration files; add a new migration for every schema change.

## 7. Exact Run Commands

Start in the project root:

```powershell
cd "C:\Users\hp\Documents\Codex\2026-06-01\you-are-a-principal-software-architect\crm-system"
```

Docker:

```powershell
docker compose up -d
docker compose ps
docker ps
```

Rebuild application containers:

```powershell
docker compose up -d --build backend frontend
```

Logs and health:

```powershell
docker logs crm_backend --tail 150
docker logs crm_frontend --tail 100
Invoke-RestMethod "http://localhost:8081/actuator/health"
Invoke-RestMethod "http://localhost:8081/actuator/info"
```

Restart/stop:

```powershell
docker compose restart backend
docker compose restart frontend
docker compose down
```

Do not add `-v` to `docker compose down` unless the database volume should be deleted.

Backend build/tests:

```powershell
cd "C:\Users\hp\Documents\Codex\2026-06-01\you-are-a-principal-software-architect\crm-system\backend"
mvn clean package
mvn test
```

Run backend outside Docker only when port 8081 is free:

```powershell
mvn spring-boot:run
```

Frontend development/build:

```powershell
cd "C:\Users\hp\Documents\Codex\2026-06-01\you-are-a-principal-software-architect\crm-system\frontend"
npm install
npm run dev
npm run build
npm run lint
```

Git checkpoint:

```powershell
cd "C:\Users\hp\Documents\Codex\2026-06-01\you-are-a-principal-software-architect\crm-system"
git status --short
git diff --check
git log --oneline -5
```

## 8. Phase 55 Continuation Plan

### Phase 55 - Branded HTML Email Templates

- Goal: Replace plain password-reset email with an accessible Tadamun HTML template plus plain-text fallback.
- Likely files: `backend/pom.xml`, `backend/src/main/java/com/crm/backend/email`, `backend/src/main/resources/templates/email`, `backend/src/test/java/com/crm/backend/email`.
- Test: Maven tests/package; request password reset; inspect HTML and text parts in Mailpit; verify no token appears in logs.
- Done: Responsive branded email renders correctly, includes fallback text, expires clearly, and keeps secrets out of logs.

### Phase 56 - Notification Preferences

- Goal: Let users control optional email and in-app notifications while mandatory security emails remain enabled.
- Likely files: new Flyway V12 migration; backend notification entity/repository/service/controller/DTOs; frontend settings page and notification service.
- Test: Verify defaults, update preferences, restart containers, and confirm preferences affect optional delivery.
- Done: Preferences persist per user and are enforced safely.

### Phase 57 - Attachment Backend

- Goal: Securely upload, list, download, and remove customer/lead attachments.
- Likely files: new Flyway migration; new `attachment` backend package; storage configuration; permission enum/mappings; tests.
- Test: Allowed/disallowed type, size limit, path traversal, authorization, missing file, and audit events.
- Done: Files and metadata are protected, validated, audited, and never exposed by raw filesystem paths.

### Phase 58 - Attachment Frontend

- Goal: Add attachment controls to customer/lead detail drawers.
- Likely files: `frontend/src/services/attachmentService.ts`, customer/lead pages, shared upload/list components.
- Test: Upload, download, remove, loading/error/empty states, permission hiding, and mobile layout.
- Done: Authorized users can manage attachments from the correct record context.

### Phase 59 - Calendar Backend Foundation

- Goal: Provide date-range APIs for tasks and calendar events with correct timezone handling.
- Likely files: task repository/service/controller/DTOs; optional migration/event package; permission rules and tests.
- Test: Date boundaries, timezone, assigned-user filtering, permissions, and pagination/limits.
- Done: A scoped calendar API returns reliable events for a requested range.

### Phase 60 - Calendar Frontend

- Goal: Add a usable calendar view for tasks/follow-ups.
- Likely files: calendar page/components/service, `frontend/src/App.tsx`, `AppLayout.tsx`, permission helpers.
- Test: Month/week navigation, filters, record links, loading/empty/error states, responsive behavior.
- Done: Users can understand and open scheduled CRM work from the calendar.

### Phase 61 - Advanced Reporting Backend

- Goal: Add date-filtered business reports for pipeline, conversion, task completion, and customer activity.
- Likely files: report repositories/queries, service/controller/DTOs, indexes through a new Flyway migration, tests.
- Test: Date ranges, totals, empty data, permissions, query performance, and no full-table in-memory aggregation.
- Done: APIs answer defined business questions accurately and efficiently.

### Phase 62 - Advanced Reporting Frontend

- Goal: Turn Reports into real filters, charts, comparisons, and drill-down summaries.
- Likely files: `ReportsPage.tsx`, report service/types, shared chart/filter/export components.
- Test: Filters, loading, empty/error states, permissions, mobile, and values matching API responses.
- Done: Reports are useful to managers and do not display fabricated trends.

### Phase 63 - Excel and PDF Exports

- Goal: Export authorized report data to real Excel/PDF files.
- Likely files: backend export service/controllers/dependencies/tests; frontend report download actions.
- Test: Content type, filename, authorization, empty/large datasets, spreadsheet/PDF opening correctly.
- Done: Export buttons download valid files generated from scoped server data.

### Phase 64 - Team Dashboard and Data Scope

- Goal: Introduce explicit own/team/all data visibility and team-aware dashboards.
- Likely files: user/team/ownership model, Flyway migration, security/data-scope service, dashboard/report queries, frontend filters, tests.
- Test: Admin, manager, sales, and support isolation; cross-team access denial; dashboard totals per scope.
- Done: Authorized users see only the records allowed by both permission and ownership scope.

Remaining Version 2 direction:

- Phase 65: Unified activity timeline.
- Phase 66: Global search across CRM modules.
- Phase 67: Testing upgrade: Testcontainers, frontend tests, permission/data-scope regression tests.
- Phase 68: GitHub Actions CI/CD.
- Phase 69: Production hardening: HTTPS, secrets, monitoring/alerts, scheduled backups/restore drill, production email, performance tests.
- Phase 70: Version 2 final audit, documentation, release, and `v2.0.0` tag.

Possible Version 3 direction:

- Multi-tenant organizations and strict tenant isolation.
- SaaS onboarding and Stripe billing.
- Public API, API keys, rate limits, and webhooks.
- WhatsApp and third-party integrations.
- Workflow automation, approvals, queues, and transactional outbox.
- Organization/team administration and tenant-aware reporting.

Possible Version 4 direction:

- AI lead scoring and record summaries.
- Predictive analytics and sales forecasting.
- Semantic search and a permission-aware CRM assistant.
- Mobile application, offline support, and push notifications.
- Advanced rules and automation recommendations.

Do not begin AI or multi-tenancy until data quality, ownership scope, testing, security, and production operations are strong.

## 9. MVP Finish Mode Rules

- Act as mentor and technical lead. Guide first; edit files only when the user explicitly says to do it.
- Fix or implement one logical issue at a time.
- Do not perform broad rewrites.
- Do not add large features outside the active phase.
- Keep responses short and use simple English.
- Show only changed snippets unless a complete small file is necessary.
- Before code, state the file path, purpose, and where the snippet belongs.
- Explain why before how.
- Protect existing authentication, permissions, data, and working UI.
- Read the actual current files before advising changes; never guess old code.
- Add schema changes only through a new Flyway migration; never edit an applied migration.
- Run the smallest relevant test first, then full backend/frontend validation before completing a phase.
- Report files changed, commands run, and results.
- Review user-written code before continuing.
- Check `git status --short` before and after work.
- Commit/checkpoint after each stable phase when practical; never commit `.env`, secrets, or database dumps.
- Do not continue automatically to the next phase without user approval.

## 10. First Message for the New Chat

Copy and send this:

> We are continuing the Tadamun CRM from Phase 55. The complete checkpoint is at `C:\Users\hp\Documents\Codex\2026-06-01\you-are-a-principal-software-architect\crm-system\docs\CODEX_HANDOFF_PHASE_54.md`. Read that file first, then inspect only the files needed for Phase 55. Do not rewrite the project or start future phases. Confirm `git status --short`, `docker compose ps`, backend tests, and the current email files. Act as my senior mentor: use simple English, explain why before how, guide me to make changes myself unless I explicitly ask you to edit, show small snippets only, and wait for my confirmation after each step. Start Phase 55: branded HTML password-reset email with plain-text fallback, Mailpit verification, tests, and no token leakage in logs.
