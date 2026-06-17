# Enterprise CRM System

A full-stack CRM system built with Spring Boot, React, MySQL, and Docker.

## Features

- JWT authentication
- Refresh tokens
- Role-based access control
- Forced password change
- User management
- Customer management
- Lead management
- Contact management
- Task management
- Notes
- Audit logs
- Dashboard analytics
- Basic reports
- Dockerized frontend, backend, and database
- Health checks
- Backup and restore documentation
- Production hardening documentation

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

### Frontend

- React
- TypeScript
- Vite
- Tailwind CSS
- Axios

### DevOps

- Docker
- Docker Compose
- Spring Boot Actuator
- Git

## Environment Setup

Copy the example environment file:

```powershell
copy .env.example .env
```

Update `.env` with local or production values.

Do not commit `.env`.

## Run With Docker

```powershell
docker compose up -d --build
```

Check containers:

```powershell
docker ps
```

## Backend Tests

```powershell
cd backend
mvn test
```

## Frontend Build

```powershell
cd frontend
npm run build
```

## Health Checks

Backend:

```powershell
Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method Get
```

Frontend:

```powershell
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:5173" -Method Get
```

## Security Notes

- Never commit `.env`
- Use strong JWT secrets
- Use strong database passwords
- Change default or temporary passwords
- Keep production CORS restricted
- Do not expose MySQL publicly in production