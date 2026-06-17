# Production Secret Review

## Required Environment Variables

The backend requires these values from the environment:

- SPRING_DATASOURCE_URL
- SPRING_DATASOURCE_USERNAME
- SPRING_DATASOURCE_PASSWORD
- JWT_SECRET
- CORS_ALLOWED_ORIGINS

Docker also requires:

- MYSQL_DATABASE
- MYSQL_USER
- MYSQL_PASSWORD
- MYSQL_ROOT_PASSWORD

Frontend build requires:

- VITE_API_BASE_URL

## Rules

- Never commit `.env`.
- Never commit real database passwords.
- Never commit real JWT secrets.
- Use `.env.example` only for placeholder values.
- Rotate secrets if they are exposed.
- Use strong unique secrets per environment.
- Production CORS should allow only real frontend domains.
- Production JWT secret should be long, random, and private.

## Validation

Before deployment:

1. Confirm `.env` is ignored by Git.
2. Confirm `.env.example` has placeholders only.
3. Confirm backend fails if required secrets are missing.
4. Confirm Docker Compose starts with the production `.env`.
5. Confirm CORS only allows the deployed frontend URL.