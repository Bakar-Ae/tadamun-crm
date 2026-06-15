# Deployment Checklist

## Pre-Deployment Checks

- Backend tests pass
- Frontend build passes
- Docker Compose runs locally
- `.env.example` exists
- Real `.env` exists only on server
- `.env` is ignored by Git
- MySQL is not exposed publicly in production
- JWT secret is strong and random
- SQL logging is disabled
- Database backups are planned

## Backend Checks

- Application starts successfully
- Flyway migrations run successfully
- JWT authentication works
- Protected endpoints require token
- Role-based access works
- Logs show no startup errors

## Frontend Checks

- Production build succeeds
- Login page works
- Dashboard loads
- Sidebar navigation works
- Logout works
- API base URL points to production backend

## Docker Checks

- `docker compose config` works
- Containers start cleanly
- Backend can connect to MySQL
- Frontend is reachable
- Ports are correct
- Volumes are configured

## Security Checks

- HTTPS enabled
- Firewall allows only required ports
- Database port is not public
- Strong database passwords
- Strong JWT secret
- No secrets committed to Git

## Backup Checks

- Backup command tested
- Restore command tested
- Backup storage location selected
- Backup schedule defined

## Post-Deployment Checks

- Login as admin
- Create test customer
- View dashboard
- View audit logs
- Check backend logs
- Check database volume