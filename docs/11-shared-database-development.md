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

Run the main backend with the shared database profile from its own directory:

```bash
cd 12group-backend
cp .env.example .env
```

Then edit `12group-backend/.env`:

```env
SPRING_PROFILES_ACTIVE=shared-db
DB_URL=jdbc:mysql://39.102.114.72:3307/jijiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
DB_USERNAME=jijiang
DB_PASSWORD=your-db-password
REDIS_HOST=39.102.114.72
REDIS_PORT=6380
REDIS_PASSWORD=your-redis-password
```

Start it:

```bash
mvn spring-boot:run
```

Run the payment server from its own directory:

```bash
cd jijiang-payment-server
cp .env.example .env
```

Then edit `jijiang-payment-server/.env`:

```env
SPRING_PROFILES_ACTIVE=shared-db
PAYMENT_DB_URL=jdbc:mysql://39.102.114.72:3307/jijiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
PAYMENT_DB_USERNAME=jijiang
PAYMENT_DB_PASSWORD=your-db-password
REDIS_HOST=39.102.114.72
REDIS_PORT=6380
REDIS_PASSWORD=your-redis-password
```

Start it:

```bash
mvn spring-boot:run
```

The frontend points at whichever local backend is being run from its own env file:

```env
# 12group-frontend/.env
VITE_API_BASE=
VITE_API_PROXY_TARGET=http://localhost:8080
```

## Migration rule

The shared database runs Flyway migrations from the application services. When
schema files change, let one developer or CI run migrations first. Everyone else
can set `FLYWAY_ENABLED=false` temporarily after the shared database is current.
