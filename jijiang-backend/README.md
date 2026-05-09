# jijiang-backend

V1.0 可运行后端基线，覆盖 mock 登录、实名认证审核、服务发布/检索、下单、真实支付摘要入账、履约、评价与后台审核主链路。真实支付由独立 `jijiang-payment-server` 调用虎皮椒并回推本服务。

## 本地启动

```bash
cd ..
cp .env.example .env
docker compose up -d mysql redis
cd jijiang-backend
mvn spring-boot:run
```

开发环境默认使用 H2 内存库，方便无 MySQL 时快速运行。使用 Docker Compose 的 `backend` 服务时会连接 MySQL 与 Redis。

## 主要接口

- `POST /api/auth/wx-login`
- `POST /api/user/verify/submit`
- `GET /api/service/search`
- `POST /api/service/publish`
- `POST /api/order/create`
- `POST /api/payment/create`
- `POST /internal/payment/callback`（B 支付服务回推）
- `POST /api/order/accept`
- `POST /api/order/deliver`
- `POST /api/order/confirm`
- `POST /api/review/submit`
- `POST /admin/auth/login`
- `GET /admin/dashboard/overview`
- `GET /admin/verify/pending`
- `POST /admin/verify/review`
- `GET /admin/service/list`
- `POST /admin/service/review`
- `POST /admin/service/offline`
- `GET /admin/order/list`
- `GET /admin/order/detail`

## 管理后台默认账号

开发环境启动后会自动创建 `admin / Admin@123456`。如果开发库里已存在同名管理员，启动时会同步为该默认密码；生产环境必须通过 `ADMIN_USERNAME` 和 `ADMIN_PASSWORD` 配置初始管理员。

管理后台 Vite 开发代理默认转发到 `http://localhost:8081`，可通过 `VITE_ADMIN_PROXY_TARGET` 覆盖。
