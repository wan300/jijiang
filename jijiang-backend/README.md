# jijiang-backend

V1.0 可运行后端基线，覆盖 mock 登录、实名认证审核、服务发布/检索、下单、模拟支付、履约、评价与后台审核主链路。

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
- `POST /api/payment/mock-pay`
- `POST /api/order/accept`
- `POST /api/order/deliver`
- `POST /api/order/confirm`
- `POST /api/review/submit`
- `GET /admin/verify/pending`
- `POST /admin/verify/review`
- `GET /admin/service/pending`
- `POST /admin/service/review`
