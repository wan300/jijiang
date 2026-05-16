# JiJiang shared database module

This module runs the shared development data services only:

- MySQL 8.0
- Redis 7.2

It is intended for a server used by the team while everyone runs frontend,
backend, and payment services locally.

## Start on the server

```bash
cd jijiang-database
cp .env.example .env
# edit .env and replace all change-me values
docker compose up -d
docker compose ps
```

The shared development deployment binds services publicly so team members can
connect without SSH tunnels:

- MySQL: `0.0.0.0:3307`
- Redis: `0.0.0.0:6380`

Restrict `3307` and `6380` with cloud security group rules and use strong
passwords. Public database ports are convenient for development but should not
be used for production.

## Local backend connection

Run the main backend locally:

```bash
cd 12group-backend
SPRING_PROFILES_ACTIVE=shared-db \
DB_URL='jdbc:mysql://39.102.114.72:3307/jijiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
DB_USERNAME=jijiang \
DB_PASSWORD='change-me-db-password' \
REDIS_HOST=39.102.114.72 \
REDIS_PORT=6380 \
REDIS_PASSWORD='change-me-redis-password' \
mvn spring-boot:run
```

Run the payment server locally:

```bash
cd jijiang-payment-server
SPRING_PROFILES_ACTIVE=shared-db \
PAYMENT_DB_URL='jdbc:mysql://39.102.114.72:3307/jijiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
PAYMENT_DB_USERNAME=jijiang \
PAYMENT_DB_PASSWORD='change-me-db-password' \
REDIS_HOST=39.102.114.72 \
REDIS_PORT=6380 \
REDIS_PASSWORD='change-me-redis-password' \
mvn spring-boot:run
```

On Windows PowerShell:

```powershell
$env:SPRING_PROFILES_ACTIVE='shared-db'
$env:DB_URL='jdbc:mysql://39.102.114.72:3307/jijiang?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true'
$env:DB_USERNAME='jijiang'
$env:DB_PASSWORD='change-me-db-password'
$env:REDIS_HOST='39.102.114.72'
$env:REDIS_PORT='6380'
$env:REDIS_PASSWORD='change-me-redis-password'
mvn spring-boot:run
```

## Migration ownership

Both application services use Flyway. For a shared database, designate one
person or one CI job as the migration runner when schema files change.

Other developers can temporarily disable local migration execution with:

```env
FLYWAY_ENABLED=false
```

Only do this after the shared database has already been migrated.
