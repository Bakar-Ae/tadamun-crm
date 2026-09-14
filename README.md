# Tadamun CRM

Tadamun CRM is a multi-tenant SaaS CRM for managing customers, leads,
contacts, tasks, notes, users, subscriptions, integrations, and automation in
one place.

Version 3 is built with Java 21, Spring Boot, React, TypeScript, MySQL, and
Docker using a modular architecture with tenant isolation and operational
release controls.

---

## Features

Current version includes:

- JWT authentication
- Refresh tokens and logout
- Password change and reset
- Multi-organization workspaces and memberships
- Tenant-scoped roles and permissions
- Organization invitations and onboarding
- Tenant-isolated customers, leads, contacts, tasks, notes, and attachments
- Login rate limiting
- User management
- Customer management
- Lead management
- Contact management
- Task management
- Notes
- Notifications
- Dashboard
- Reports and exports
- Tenant-aware audit logs and platform administration
- Stripe subscription billing foundation
- Subscription plans, usage metering, and feature limits
- Public API keys with scoped access and rate limits
- Signed outbound webhooks with retries and delivery history
- Event-driven workflow automation with durable retries and execution history
- Encrypted WhatsApp Cloud and SMTP integrations with durable delivery history
- Search, filtering, pagination
- Quick create actions
- Detail drawers
- Light and dark mode
- Docker health checks, monitoring, load tests, and verified backups

---

## Tech Stack

### Backend

- Java 21

- Spring Boot

- Spring Security

- JWT

- Hibernate / JPA

- Flyway

- MySQL

- Maven

- Spring Boot Actuator

### Frontend

- React

- TypeScript

- Vite

- Tailwind CSS

- Axios

- Framer Motion

- Recharts

- Lucide Icons

### DevOps

- Docker

- Docker Compose

- Nginx

- Git

---

## Project Structure

### Backend

```

controller

service

repository

entity

dto

security

config

exception

```

### Frontend

```

pages

layouts

components

services

shared

utils

```

---

## Getting Started

Clone the repository and copy the example environment file.

```powershell

copy .env.example .env

```

Update the values in `.env` before running the project.

---

## Running with Docker

Start everything:

```powershell

docker compose up -d

```

Rebuild containers:

```powershell

docker compose up -d --build

```

Rebuild only the frontend:

```powershell

docker compose up -d --no-deps --build frontend

```

Check running containers:

```powershell

docker ps

```

Expected containers:

```

crm_frontend

crm_backend

crm_mysql

```

---

## Local Development

Frontend

```

http://localhost:5173

```

Backend Health

```

http://localhost:8081/actuator/health

```

Backend Info

```

http://localhost:8081/actuator/info

```

---

## Running Tests

Backend

```powershell

cd backend

mvn clean test

```

Frontend

```powershell

cd frontend

npm run build

npm run lint

npm test

```

---

## Security

A few things worth keeping in mind:

- Don't commit `.env`

- Use a strong JWT secret

- Use secure database credentials

- Restrict CORS in production

- Keep MySQL private

- Use HTTPS when deploying

- Store tenant and billing secrets only in the deployment secret manager

- Run the migration rehearsal and verified-backup scripts before releases

---

## Documentation

Project documentation can be found in the `docs` folder, including deployment notes, security review, demo guide, and handover documents.

---

## Next Steps

Version 4 planning is documented in `docs/version-4-roadmap.md` and includes
PWA/mobile improvements, AI-assisted CRM features, richer analytics, and
expanded integration capabilities.

---

## About

This project was built as a personal full-stack learning project to gain experience with backend development, frontend development, authentication, Docker, database design, and building larger applications with a clean architecture.
