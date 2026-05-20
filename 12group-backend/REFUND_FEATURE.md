# 退款功能

## 概述

为技匠平台新增订单退款完整链路。买家可在订单状态 20/30/40 时发起退款申请，状态 20（已支付/卖家未接单）自动退款，状态 30/40（卖家已接单/已交付）进入管理员人工审核。

---

## 涉及文件

### 后端（12group-backend）

| 操作 | 文件 | 说明 |
|---|---|---|
| **新建** | `src/main/resources/db/migration/V1_0_8__refund_feature.sql` | 退款申请表 |
| **新建** | `modules/RefundAppService.java` | 退款核心业务逻辑 |
| **新建** | `modules/RefundController.java` | 用户端退款 API |
| **修改** | `infra/PaymentServerClient.java` | 新增 `refundPayment` 接口 + `RefundRequest`/`RefundResponse` |
| **修改** | `infra/HttpPaymentServerClient.java` | 实现退款 HTTP 调用，POST `/internal/payment/orders/{tradeOrderId}/refund` |
| **修改** | `modules/AdminController.java` | 新增 `GET /admin/refund/list`、`/admin/refund/detail`、`POST /admin/refund/review` |
| **修改** | `modules/AdminAppService.java` | 新增 `refundList()`、`refundDetail()`、`reviewRefund()` |
| **修改** | `.../PaymentAppServiceTest.java` | Stub 补充 `refundPayment` 实现 |

### 前端（12group-frontend）

| 操作 | 文件 | 说明 |
|---|---|---|
| **新建** | `src/api/refund.ts` | `submitRefund()`、`listMyRefunds()`、`getRefundDetail()` |
| **修改** | `src/types/domain.ts` | 新增 `RefundSubmitRequest`、`RefundRequestItem` 类型 |
| **修改** | `src/pages/order/refund.vue` | `submit()` 从占位 toast 改为调用真实 API |

---

## 数据库

### 新表 `refund_request`

```sql
CREATE TABLE refund_request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL COMMENT '申请人(买家)',
    seller_id BIGINT NOT NULL COMMENT '卖家ID',
    reason VARCHAR(500) NOT NULL COMMENT '退款原因',
    evidence_urls TEXT COMMENT '证据图片URL，JSON数组',
    amount DECIMAL(10,2) COMMENT '申请退款金额',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0待审核 1已退款 2已驳回 3卖家拒绝(预留)',
    reviewer_id BIGINT COMMENT '审核管理员ID',
    review_remark VARCHAR(500) COMMENT '审核备注',
    review_time DATETIME,
    deduct_deposit DECIMAL(10,2) DEFAULT 0 COMMENT '扣除保证金',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_refund_order (order_id),
    INDEX idx_refund_user (user_id),
    INDEX idx_refund_seller (seller_id),
    INDEX idx_refund_status (status)
);
```

### `order_main` 状态码

| 状态码 | 含义 | 退款相关 |
|---|---|---|
| 10 | 待支付 | 不可退款 |
| 20 | 已支付/待接单 | 自动退款 |
| 30 | 已接单/服务中 | 人工审核 |
| 40 | 已交付/待确认 | 人工审核 |
| 50 | 已完成 | 暂不支持退款 |
| 70 | 退款中 | 预留 |
| 80 | 已退款 | 退款成功后 |

---

## 退款流程

```
买家申请 → ──┬── 状态 20（卖家未接单）→ 调支付服务退还 → order.status = 80
             └── 状态 30/40（卖家已接单）→ 管理员审核 ──┬→ 通过 → 退款 + status=80
                                                       └→ 驳回 → refund.status=2
```

- 状态 20 自动退款：卖家尚未接单投入成本，无需人工审核
- 状态 30/40 人工审核：管理员可查看订单详情、支付记录、证据后判定，可选扣除卖方保证金

---

## API 端点

### 用户端

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/refund/submit` | 买家发起退款。body: `{orderId, reason, evidenceUrls?}` |
| GET | `/api/refund/list` | 买家退款列表 |
| GET | `/api/refund/detail?refundId=` | 退款详情 |

### 管理端

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/admin/refund/list` | 退款列表（?status=&keyword=&page=&pageSize=） |
| GET | `/admin/refund/detail?refundId=` | 退款详情 + 订单 + 支付记录 |
| POST | `/admin/refund/review` | 审核退款。body: `{refundId, passed, reason, deductDeposit?}` |

---

## 支付服务对接要求

退款最终通过支付服务器原路退回，需支付服务器新增接口：

```
POST /internal/payment/orders/{tradeOrderId}/refund
```

详见 [支付服务退款接口说明](#)（已单独提供给对接方）。支付服务器未实现该接口时，自动退款会失败并记录到 `refund_request.review_remark`。

---

## 资金说明

- 退款范围（状态 20/30/40）的订单均未确认完成，`frozen_balance` 中无此订单款项
- 退款资金全部走支付服务原路退回，不操作 `seller_account`
- 管理员可选择扣除卖方保证金（`deposit_amount`）作为处罚
