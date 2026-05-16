# Shared Database Development

This project now supports a standalone database module for team development.

## Server side

Use `jijiang-database/` on the server:

```bash
cd jijiang-database
cp .env.example .env
docker compose up -d
```

The shared development deployment listens publicly:

- MySQL: `39.102.114.72:3307`
- Redis: `39.102.114.72:6380`

Open inbound TCP `3307` and `6380` in the cloud security group. Public database
ports are only for shared development; production should keep MySQL and Redis
private.

## Local development

Run the main backend with the shared database profile:

```bash
cd jijiang-backend
SPRING_PROFILES_ACTIVE=shared-db \
DB_URL='jdbc:mysql://39.102.114.72:3307/jijiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
DB_USERNAME=jijiang \
DB_PASSWORD='your-db-password' \
REDIS_HOST=39.102.114.72 \
REDIS_PORT=6380 \
REDIS_PASSWORD='your-redis-password' \
mvn spring-boot:run
```

Run the payment server:

```bash
cd jijiang-payment-server
SPRING_PROFILES_ACTIVE=shared-db \
PAYMENT_DB_URL='jdbc:mysql://39.102.114.72:3307/jijiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
PAYMENT_DB_USERNAME=jijiang \
PAYMENT_DB_PASSWORD='your-db-password' \
REDIS_HOST=39.102.114.72 \
REDIS_PORT=6380 \
REDIS_PASSWORD='your-redis-password' \
mvn spring-boot:run
```

The frontend still points at whichever local backend is being run:

```env
VITE_API_BASE=http://localhost:8080
```

## Migration rule

The shared database runs Flyway migrations from the application services. When
schema files change, let one developer or CI run migrations first. Everyone else
can set `FLYWAY_ENABLED=false` temporarily after the shared database is current.
