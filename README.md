# Tadamun CRM

Tadamun CRM is a full-stack customer relationship management system built with Java 21, Spring Boot, React, TypeScript, MySQL, and Docker.

The project is designed as an enterprise-style modular monolith for managing customers, leads, contacts, tasks, notes, users, reports, notifications, and audit logs.

## Project Status

Version 1 is working and demo-ready.

Completed:

- Authentication with JWT access tokens
- Refresh tokens and logout
- Password change and password reset
- Login rate limiting
- Role-based access control
- User management
- Customer management
- Lead management
- Contact management
- Task management
- Notes
- Notifications
- Dashboard
- Reports
- Audit logs
- Search, filters, pagination, detail drawers, and quick create
- Light and dark frontend themes
- Dockerized frontend, backend, and MySQL
- Health checks
- Security and handover documentation

## Tech Stack

### Backend

- Java 21
- Spring Boot
- Spring Security
- JWT
- Hibernate / JPA
- Maven
- MySQL
- Flyway
- Spring Boot Actuator

### Frontend

- React
- TypeScript
- Vite
- Tailwind CSS
- Axios
- Framer Motion
- Recharts
- Lucide icons

### DevOps

- Docker
- Docker Compose
- Nginx frontend container
- MySQL container
- Environment-based configuration
- Git

## Architecture

The system uses a modular monolith architecture.

Backend layers:

- Controller layer
- Service layer
- Repository layer
- Entity layer
- DTO layer
- Security layer
- Exception handling layer
- Configuration layer

Frontend structure:

- Pages
- Layouts
- Components
- Shared UI components
- Services
- Utility libraries

## Environment Setup

Copy the example environment file:

```powershell
copy .env.example .env
```

Update `.env` with your local values.

Do not commit `.env`.

## Run With Docker

From the project root:

```powershell
docker compose up -d
```

Rebuild after code changes:

```powershell
docker compose up -d --build
```

If you only need to rebuild the frontend:

```powershell
docker compose up -d --no-deps --build frontend
```

Check containers:

```powershell
docker ps
```

Expected containers:

- `crm_frontend`
- `crm_backend`
- `crm_mysql`

## Local URLs

Frontend:

```text
http://localhost:5173
```

Backend health:

```text
http://localhost:8081/actuator/health
```

Backend info:

```text
http://localhost:8081/actuator/info
```

## Backend Checks

```powershell
cd backend
mvn clean test
```

## Frontend Checks

```powershell
cd frontend
npm run build
npm run lint
```

## Security Notes

- Never commit `.env`
- Use a long random `JWT_SECRET`
- Use strong database passwords
- Rotate any password shared in chat, screenshots, or demos
- Keep production CORS restricted to the real domain
- Do not expose MySQL publicly in production
- Use HTTPS in production
- Store production secrets in a secret manager or secure deployment environment

## Documentation

Important documents are stored in `docs/`.

Recommended reading:

- `docs/phase-41-handover-summary.md`
- `docs/phase-42-security-review.md`
- `docs/phase-43-demo-script.md`
- `docs/phase-44-company-handover-package.md`

## Future Roadmap

Recommended next improvements:

- Advanced permissions
- Advanced reporting
- File attachments
- Email delivery configuration
- Calendar integration
- Workflow automation
- Multi-tenant SaaS support
- Billing and subscriptions
- Public API
- Mobile application
- AI insights
- Team performance dashboards

## Summary

Tadamun CRM Version 1 demonstrates backend engineering, frontend engineering, database design, authentication, authorization, audit logging, Docker deployment, and professional CRM UI/UX design.
