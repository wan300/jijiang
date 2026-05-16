# jijiang-payment-server

独立 B 支付服务，负责虎皮椒真实支付下单、虎皮椒回调验签和真实支付流水保存。支付成功后可以选择回推 A 后端，也可以关闭回推，由任意 A 后端通过 `/api/payment/sync` 主动查询支付状态。

## 主要接口

- `POST /internal/payment/orders`：A 后端创建支付单，使用 `X-JJ-*` HMAC 签名。
- `GET /internal/payment/orders/{tradeOrderId}`：A 后端主动查询支付状态，使用 `X-JJ-*` HMAC 签名。
- `POST /api/payment/xunhu-notify`：虎皮椒异步回调入口。

## 本地启动

```bash
cd jijiang-payment-server
cp .env.example .env
mvn spring-boot:run
```

`application.yml` 会自动读取当前目录下的 `.env`，不依赖根目录 `.env`。开发环境默认使用 H2 内存库；如需连接共享 MySQL/Redis，可在本目录 `.env` 中设置 `SPRING_PROFILES_ACTIVE=shared-db` 并填写 `PAYMENT_DB_*`、`REDIS_*`。

## 中心支付服务器模式

推荐用于多台电脑联调：只开放支付服务器的 `6670` 端口，让外部电脑主动访问支付服务器，支付服务器不主动回调外部电脑。

支付服务器配置：

- `XUNHU_NOTIFY_URL=http://jijiangzhifu.0721ciallo.com:6670/api/payment/xunhu-notify`
- `APP_SERVER_CALLBACK_ENABLED=false`
- `APP_SERVER_CALLBACK_URL=` 留空
- `APP_SERVER_CLIENT_ID` 与 A 后端 `PAYMENT_SERVER_CLIENT_ID` 一致
- `APP_SERVER_SHARED_SECRET` 与 A 后端 `PAYMENT_SERVER_SHARED_SECRET` 一致

任意电脑上的 A 后端配置：

- `PAYMENT_SERVER_BASE_URL=http://jijiangzhifu.0721ciallo.com:6670`
- `PAYMENT_SERVER_CLIENT_ID=jijiang-app`
- `PAYMENT_SERVER_SHARED_SECRET` 与支付服务器一致

前端配置：

- `VITE_API_BASE` 指向当前电脑实际运行的 A 后端，例如 `http://localhost:8080`。

## 必要配置

- `XUNHU_APP_ID`
- `XUNHU_APP_SECRET`
- `XUNHU_NOTIFY_URL`
- `APP_SERVER_CALLBACK_ENABLED`
- `APP_SERVER_CLIENT_ID`
- `APP_SERVER_SHARED_SECRET`
