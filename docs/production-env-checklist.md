# Production Environment Checklist

## Required Values

Before production deployment, set:

- MYSQL_DATABASE
- MYSQL_USER
- MYSQL_PASSWORD
- MYSQL_ROOT_PASSWORD
- SPRING_DATASOURCE_URL
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- JWT_SECRET
- JWT_EXPIRATION_MS
- SERVER_PORT
- FRONTEND_PORT
- BACKEND_PORT
- VITE_API_BASE_URL
- CORS_ALLOWED_ORIGINS

## Rules

- Do not reuse local development passwords.
- Do not commit `.env`.
- Use long random secrets.
- Use different secrets per environment.
- Restrict CORS to the real frontend domain.
- Keep MySQL private when possible.

## Validation

Run:

```powershell
docker compose config
docker compose up -d
docker ps
Invoke-RestMethod -Uri "http://localhost:8081/actuator/health" -Method Get