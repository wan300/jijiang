# jijiang-payment-server

独立 B 支付服务，负责虎皮椒真实支付下单、虎皮椒回调验签、真实支付流水保存，以及向 A 后端 `/internal/payment/callback` 回推支付成功结果。

## 主要接口

- `POST /internal/payment/orders`：A 后端创建支付单，使用 `X-JJ-*` HMAC 签名。
- `POST /api/payment/xunhu-notify`：虎皮椒异步回调入口。

## 必要配置

- `XUNHU_APP_ID`
- `XUNHU_APP_SECRET`
- `XUNHU_NOTIFY_URL`
- `APP_SERVER_CALLBACK_URL`
- `APP_SERVER_CLIENT_ID`
- `APP_SERVER_SHARED_SECRET`
