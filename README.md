# Tadamun CRM

Tadamun CRM is a CRM application built to help businesses manage customers, leads, contacts, tasks, notes, and users in one place.

This project was built with Java 21, Spring Boot, React, TypeScript, MySQL, and Docker. The goal was to practice building a production-style full-stack application using a modular architecture.

---

## Features

Current version includes:

- JWT authentication

- Refresh tokens and logout

- Password change and reset

- Role-based access control

- Login rate limiting

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

- Search, filtering, pagination

- Quick create actions

- Detail drawers

- Light and dark mode

- Docker support

- Health checks

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

---

## Documentation

Project documentation can be found in the `docs` folder, including deployment notes, security review, demo guide, and handover documents.

---

## Next Steps

Some features I'd like to add in future versions:

- File uploads

- Calendar integration

- Email support

- Workflow automation

- Team dashboards

- AI-assisted insights

- Public API

- Mobile app

- Multi-tenant SaaS support

---

## About

This project was built as a personal full-stack learning project to gain experience with backend development, frontend development, authentication, Docker, database design, and building larger applications with a clean architecture.