# CRM Production Checklist

## 1. Environment Secrets

Before production, change all default secrets.

Required variables:

```env
MYSQL_DATABASE=crm_db
MYSQL_USER=crm_user
MYSQL_PASSWORD=replace_with_strong_database_password
MYSQL_ROOT_PASSWORD=replace_with_strong_root_password

SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/crm_db
SPRING_DATASOURCE_USERNAME=crm_user
SPRING_DATASOURCE_PASSWORD=replace_with_strong_database_password

JWT_SECRET=replace_with_long_random_secret_at_least_64_characters
JWT_EXPIRATION_MS=3600000

SERVER_PORT=8081
BACKEND_PORT=8081
FRONTEND_PORT=5173

VITE_API_BASE_URL=https://api.your-domain.com/api/v1
CORS_ALLOWED_ORIGINS=https://your-domain.com