# 12group-backend

技匠 A 后端，当前是 V1.0 可运行基线。已覆盖登录、实名认证提交与审核、服务发布/检索、下单、真实支付摘要入账、支付状态同步、履约、评价与管理后台审核主链路。

当前开发拓扑为“本地开发前后端，服务器承载共享基础设施”：

- 本地只启动 `12group-backend`、`12group-frontend` 和可选的 `jijiang-admin`。
- MySQL、Redis 已转移到共享服务器运行，后端通过 `shared-db` profile 连接。
- 真实支付模块已转移到服务器上的 `jijiang-payment-server` 运行，本地后端通过 `PAYMENT_SERVER_BASE_URL` 调用它，不需要本地启动支付服务。
- 微信登录、COS、OCR 在未配置真实密钥时会降级为 mock，方便当前阶段联调。

## 当前完成情况

- 已完成：用户登录、实名认证提交/审核、服务发布和审核、服务检索、订单创建、支付创建/同步、支付回调镜像、接单、交付、确认完成、评价、站内信、投诉举报和管理后台核心接口。
- 已拆分：虎皮椒真实支付下单、虎皮椒异步回调验签和支付流水保存由服务器支付服务负责。
- 仍是流程占位或后续扩展：退款、提现、生产级微信/COS/OCR 配置和更完整的资金清结算。

## 本地启动（主开发方式）

```bash
cd 12group-backend
cp .env.example .env
mvn spring-boot:run
```

`.env.example` 默认使用 `SPRING_PROFILES_ACTIVE=shared-db`，即本地后端监听 `http://localhost:8080`，数据库、Redis 和支付服务连接服务器。`application.yml` 会自动读取当前目录下的 `.env`，不依赖仓库根目录 `.env`。

本地前端联调时保持后端端口为 `8080`，前端和管理后台的 Vite 代理会转发到这个地址。

如需临时脱离共享数据库，用 H2 快速启动，可以把 `.env` 改为：

```env
SPRING_PROFILES_ACTIVE=dev
PAYMENT_SERVER_BASE_URL=http://jijiangzhifu.0721ciallo.com:6670
```

## 管理后台本地启动

```bash
cd 12group-backend/jijiang-admin
cp .env.example .env
npm install
npm run dev
```

管理后台开发服务固定监听 `http://localhost:5175`。保持 `VITE_API_BASE=` 为空时，请求会通过 Vite 代理转发到 `VITE_ADMIN_PROXY_TARGET`，默认是 `http://localhost:8080`。

## Docker Compose

当前目录也包含一个可选的 Docker 栈，不依赖仓库根目录 `.env` 或 `docker-compose.yml`。默认拓扑与主开发方式一致：只启动本地后端 API 和管理后台，MySQL、Redis、支付服务仍连接 `.env` 中配置的服务器地址。

```bash
cd 12group-backend
cp .env.example .env
docker compose up -d --build
```

默认会启动：

- 后端 API: `http://localhost:8080`
- 管理后台: `http://localhost:8088`

如需脱离服务器做隔离验证，可以启用本地 MySQL/Redis profile，并把 `.env` 里的 `DB_URL`、`REDIS_HOST` 等改为容器内地址后启动：

```bash
docker compose --profile local-infra up -d --build
```

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

## 默认账号

开发环境启动后会自动创建 `.env` 中配置的管理员账号；示例默认是 `admin / change-me-admin-password`。如果开发库里已存在同名管理员，启动时会同步为该默认密码。
