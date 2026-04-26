# 05 - API 接口规范

> 规范版本：v1  
> 基准路径：`https://api.jijiang.com`（生产） / `https://dev-api.jijiang.com`（测试）  
> 协议：HTTPS + JSON  
> 文档工具：Knife4j (OpenAPI 3) —— 开发环境 `/doc.html` 可访问

---

## 1. 通用规范

### 1.1 URL 规范

- 全部使用小写，单词间用 `-` 连接
- 业务前缀：`/api/<module>/<action>`，例如 `/api/order/create`
- 管理后台：`/admin/<module>/<action>`
- 版本化：V2.0 起引入 `/api/v2/...`；V1 阶段**不写版本号**以保持 URL 简洁

### 1.2 HTTP Method 使用

| Method | 用途 |
| :--- | :--- |
| `GET` | 查询（分页、详情） |
| `POST` | 创建 / 复杂查询 / 动作类（如 `/refund`、`/confirm`） |
| `PUT` | 全量更新（少用） |
| `DELETE` | 删除（少用，业务多用逻辑删除） |

> 本项目主要使用 `GET` / `POST`；避免 REST 教条，以可读性为先。

### 1.3 请求头

| Header | 必填 | 说明 |
| :--- | :--- | :--- |
| `Authorization` | 登录后必填 | `Bearer <accessToken>` |
| `Content-Type` | POST/PUT 必填 | `application/json;charset=UTF-8` |
| `X-Client-Version` | 推荐 | 客户端版本，如 `miniapp/1.0.0` |
| `X-Trace-Id` | 可选 | 链路追踪 ID，客户端生成 |
| `X-Idempotency-Key` | 写操作推荐 | 幂等键（UUID） |

### 1.4 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {},
  "timestamp": 1714032000000
}
```

- `code`：业务码，`0` 成功；非 0 为错误码
- `message`：成功统一 `success`；失败为用户可读错误描述
- `data`：业务数据；失败时通常为 `null`
- `timestamp`：服务器毫秒时间戳

### 1.5 分页规范

请求参数：

| 字段 | 类型 | 默认 | 说明 |
| :--- | :--- | :--- | :--- |
| `page` | int | 1 | 页码，从 1 起 |
| `size` | int | 20 | 每页条数，最大 100 |
| `orderBy` | string | `create_time` | 排序字段白名单 |
| `orderType` | string | `desc` | `asc`/`desc` |

响应体：

```json
{
  "code": 0,
  "data": {
    "records": [...],
    "total": 100,
    "current": 1,
    "size": 20,
    "pages": 5
  }
}
```

### 1.6 错误码分段

| 区间 | 模块 | 典型错误 |
| :--- | :--- | :--- |
| 0 | 成功 | - |
| 10001~19999 | 用户/鉴权 | `10001` 未登录；`10010` 未实名 |
| 20001~29999 | 订单 | `20005` 库存不足；`20008` 状态非法 |
| 30001~39999 | 支付 | `30001` 验签失败；`30010` 退款超限 |
| 40001~49999 | 服务/内容 | `40001` 命中敏感词 |
| 50001~59999 | 消息/通知 | - |
| 60001~69999 | 评价/信誉 | - |
| 70001~79999 | 风控 | `70001` 操作频繁 |
| 80001~89999 | 管理 | - |
| 90001~99999 | 系统 | `90001` 系统繁忙 |

### 1.7 参数校验错误

参数不合法时，`code = 400001`，`message` 返回第一个错误字段描述：

```json
{ "code": 400001, "message": "标题不能为空", "data": null }
```

### 1.8 限流与防刷

| 接口 | 限流策略 |
| :--- | :--- |
| `POST /api/auth/wx-login` | 单 IP 60 次 / 分钟 |
| `POST /api/user/verify` | 单用户 1 次 / 60s |
| `POST /api/order/create` | 单用户 10 次 / 分钟 |
| `POST /api/message/send` | 单用户 30 条 / 分钟 |
| `GET /api/service/search` | 单用户 100 次 / 分钟 |

> 超限返回 `code=70001`，`message="操作过于频繁，请稍后再试"`。

---

## 2. 鉴权接口

### 2.1 微信小程序登录

```
POST /api/auth/wx-login
```

**Request**：

```json
{ "code": "0c3xxxxxxxx" }
```

**Response**：

```json
{
  "code": 0,
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "userInfo": {
      "id": 1001,
      "nickname": "小陈",
      "avatarUrl": "https://...",
      "verifyStatus": 0,
      "currentRole": 1,
      "campusId": 1,
      "campusName": "XX大学",
      "creditScore": 100,
      "isSellerVerified": 0,
      "depositPaid": 0
    }
  }
}
```

### 2.2 刷新 Token

```
POST /api/auth/refresh
```

**Request**：

```json
{ "refreshToken": "eyJhbGci..." }
```

**Response**：同登录返回。

### 2.3 退出登录

```
POST /api/auth/logout
```

**Response**：

```json
{ "code": 0, "message": "success", "data": null }
```

### 2.4 切换身份

```
POST /api/auth/switch-role
```

**Request**：

```json
{ "targetRole": 2 }
```

**Response**：返回新 Token（含新角色声明），客户端需替换。

---

## 3. 用户与认证

### 3.1 获取当前用户信息

```
GET /api/user/me
```

**Response**：同登录返回 `userInfo`。

### 3.2 提交实名认证

```
POST /api/user/verify/submit
```

**Request**：

```json
{
  "campusId": 1,
  "certType": 1,
  "certImageUrl": "https://oss.../private/verify/xxx.jpg",
  "realName": "张三",
  "studentNo": "2024123456"
}
```

**Response**：

```json
{
  "code": 0,
  "data": {
    "recordId": 123,
    "status": 1,
    "reviewMode": 2,
    "message": "已提交审核，预计 1 小时内完成"
  }
}
```

**错误**：

| code | message |
| :--- | :--- |
| 10011 | 请先选择学校 |
| 70001 | 操作过于频繁，请 60 秒后重试 |

### 3.3 查询认证状态

```
GET /api/user/verify/status
```

**Response**：

```json
{
  "code": 0,
  "data": {
    "status": 2,
    "rejectReason": null,
    "approvedTime": "2026-04-24 12:00:00"
  }
}
```

### 3.4 缴纳讲师保证金

```
POST /api/user/seller/pay-deposit
```

**Response**：返回微信支付参数（同 3.11）。

### 3.5 申请注销账号

```
POST /api/user/logout/apply
```

**Response**：`{ "code": 0, "data": { "effectiveTime": "2026-05-01" } }`

### 3.6 撤销注销

```
POST /api/user/logout/cancel
```

---

## 4. 服务（商品）

### 4.1 获取分类

```
GET /api/service/categories
```

**Response**：

```json
{
  "code": 0,
  "data": [
    { "id": 1, "name": "考研保研", "icon": "...", "sort": 0 },
    { "id": 2, "name": "期末辅导", "icon": "...", "sort": 1 }
  ]
}
```

### 4.2 服务检索

```
GET /api/service/search
```

**Params**：

| 字段 | 类型 | 必填 | 说明 |
| :--- | :--- | :--- | :--- |
| `campusId` | Long | 是 | 校区 |
| `keyword` | String | 否 | 搜索词 |
| `categoryId` | Long | 否 | 分类 |
| `minPrice` | Decimal | 否 | 最低价 |
| `maxPrice` | Decimal | 否 | 最高价 |
| `sortBy` | String | 否 | `smart`(默认) / `sales` / `price_asc` / `price_desc` |
| `page` | int | 否 | 页码 |
| `size` | int | 否 | 每页 |

**Response**：

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 101,
        "title": "Python 数据分析 1v1 辅导",
        "coverUrl": "...",
        "price": 50.00,
        "salesCount": 18,
        "scoreAvg": 4.9,
        "seller": {
          "id": 10,
          "nickname": "林学姐",
          "avatarUrl": "...",
          "creditLevel": "A+",
          "badges": ["考研81分", "绩点3.9"]
        }
      }
    ],
    "total": 35, "current": 1, "size": 20, "pages": 2
  }
}
```

### 4.3 服务详情

```
GET /api/service/detail
```

**Params**：`id=101`

**Response**：

```json
{
  "code": 0,
  "data": {
    "id": 101,
    "title": "...",
    "description": "<p>HTML 富文本</p>",
    "coverUrl": "...",
    "images": ["...", "..."],
    "price": 50.00,
    "priceConfig": [
      { "key": "single", "name": "单次体验", "price": 50, "unit": "小时", "qty": 1 },
      { "key": "pack3",  "name": "3次套餐", "price": 135, "unit": "次", "qty": 3 }
    ],
    "serviceMode": 3,
    "tags": ["Python", "数据分析", "1v1"],
    "stock": 5,
    "remainingStock": 3,
    "salesCount": 18,
    "scoreAvg": 4.9,
    "reviewCount": 16,
    "seller": { ... },
    "recentReviews": [ ... ]
  }
}
```

### 4.4 发布服务

```
POST /api/service/publish
```

需要角色 `ROLE_SELLER` + 已认证 + 已缴保证金。

**Request**：

```json
{
  "title": "...",
  "description": "<p>...</p>",
  "coverUrl": "...",
  "categoryId": 1,
  "price": 50.00,
  "priceConfig": [...],
  "serviceMode": 3,
  "tags": ["Python", "1v1"],
  "stock": 5
}
```

**Response**：`{ "code": 0, "data": { "id": 101 } }`

### 4.5 编辑 / 下架 / 上架

```
POST /api/service/update        // 编辑
POST /api/service/offline       // 下架 body: { id }
POST /api/service/online        // 上架
```

### 4.6 讲师服务列表（自己的）

```
GET /api/service/my-list
```

**Params**：`status=1&page=1&size=20`

### 4.7 讲师获取评价列表

```
GET /api/service/reviews
```

**Params**：`serviceId=101&page=1&size=20`

---

## 5. 订单

### 5.1 预览订单（计算价格）

```
POST /api/order/preview
```

**Request**：

```json
{ "serviceId": 101, "pricePlanKey": "pack3", "quantity": 1, "couponId": null }
```

**Response**：

```json
{
  "code": 0,
  "data": {
    "amount": 135.00,
    "couponAmount": 0,
    "payAmount": 135.00,
    "commission": 13.50,
    "settleAmount": 121.50
  }
}
```

### 5.2 创建订单

```
POST /api/order/create
```

**Request**：

```json
{
  "serviceId": 101,
  "pricePlanKey": "pack3",
  "quantity": 1,
  "remark": "希望周末上课",
  "couponId": null
}
```

**Response**：

```json
{
  "code": 0,
  "data": {
    "id": 5001,
    "orderNo": "20260424000100000001",
    "status": 10,
    "amount": 135.00,
    "payAmount": 135.00,
    "expireTime": "2026-04-24 12:15:00"
  }
}
```

**错误**：

| code | message |
| :--- | :--- |
| 20001 | 服务已下架 |
| 20005 | 库存不足，已被其他同学锁定 |
| 20006 | 不能购买自己的服务 |

### 5.3 取消订单（未支付）

```
POST /api/order/cancel
```

**Request**：`{ "orderId": 5001 }`

### 5.4 订单列表

```
GET /api/order/list
```

**Params**：

| 字段 | 说明 |
| :--- | :--- |
| `role` | `buyer`（默认）/ `seller` |
| `status` | 多值用逗号分隔，如 `10,20,30` |
| `page` / `size` | 分页 |

**Response**：

```json
{
  "code": 0,
  "data": {
    "records": [
      {
        "id": 5001,
        "orderNo": "...",
        "status": 30,
        "statusName": "进行中",
        "service": { "id": 101, "title": "...", "coverUrl": "..." },
        "counterpart": { "id": 10, "nickname": "林学姐", "avatarUrl": "..." },
        "amount": 135.00,
        "createTime": "2026-04-24 12:00:00"
      }
    ],
    "total": 5, "current": 1, "size": 20
  }
}
```

### 5.5 订单详情

```
GET /api/order/detail
```

**Params**：`orderNo=...` 或 `id=5001`

**Response**（关键字段）：

```json
{
  "code": 0,
  "data": {
    "id": 5001,
    "orderNo": "...",
    "status": 30,
    "statusTrack": [
      { "status": 10, "name": "创建", "time": "2026-04-24 12:00:00" },
      { "status": 20, "name": "已支付，等待接单", "time": "2026-04-24 12:01:00" },
      { "status": 30, "name": "讲师已接单", "time": "2026-04-24 12:30:00" }
    ],
    "service": { ... },
    "buyer": { ... },
    "seller": { ... },
    "amount": 135.00,
    "payAmount": 135.00,
    "commission": 13.50,
    "settleAmount": 121.50,
    "buyerRemark": "希望周末上课",
    "evidence": [
      { "id": 1, "fileUrl": "...", "uploaderId": 10, "createTime": "..." }
    ]
  }
}
```

### 5.6 讲师接单

```
POST /api/order/accept
```

**Request**：`{ "orderId": 5001 }`

### 5.7 讲师提交交付凭证

```
POST /api/order/deliver
```

**Request**：

```json
{
  "orderId": 5001,
  "evidence": [{ "fileUrl": "...", "fileType": "image", "description": "已完成辅导" }]
}
```

### 5.8 买家确认完成

```
POST /api/order/confirm
```

### 5.9 申请退款/仲裁

```
POST /api/order/refund/apply
```

**Request**：

```json
{
  "orderId": 5001,
  "expectAmount": 67.50,
  "reason": "讲师未按约定完成辅导",
  "images": ["..."]
}
```

### 5.10 协商退款（双方确认）

```
POST /api/order/refund/negotiate
```

**Request**：`{ "orderId": 5001, "refundAmount": 67.50, "action": "agree" | "reject" }`

### 5.11 支付下单（微信 JSAPI）

```
POST /api/payment/wx-pay
```

**Request**：`{ "orderId": 5001 }`

**Response**：

```json
{
  "code": 0,
  "data": {
    "timeStamp": "1714032000",
    "nonceStr": "...",
    "package": "prepay_id=wx...",
    "signType": "RSA",
    "paySign": "..."
  }
}
```

### 5.12 微信支付回调（服务端→服务端）

```
POST /api/payment/wx-notify
```

> 由微信支付平台调用；应用层需验签、幂等处理。详见 [03-后端开发指南](./03-后端开发指南.md) §3.4.2。

---

## 6. 站内信

### 6.1 发送消息

```
POST /api/message/send
```

**Request**：`{ "orderId": 5001, "content": "您好，什么时候方便开始？" }`

### 6.2 订单消息列表

```
GET /api/message/list
```

**Params**：`orderId=5001&page=1&size=50`

### 6.3 消息列表（订单维度）

```
GET /api/message/conversations
```

返回用户参与的所有订单聊天概览（未读数、最新消息预览）。

### 6.4 标记已读

```
POST /api/message/read
```

**Request**：`{ "orderId": 5001 }`

---

## 7. 通知

### 7.1 通知列表

```
GET /api/notification/list
```

### 7.2 未读数

```
GET /api/notification/unread-count
```

---

## 8. 评价

### 8.1 提交评价

```
POST /api/review/submit
```

**Request**：

```json
{
  "orderId": 5001,
  "score": 5,
  "tags": ["耐心", "讲得清楚"],
  "content": "学姐非常耐心，知识点讲得特别清楚",
  "images": [],
  "isAnonymous": 1
}
```

### 8.2 讲师回复评价

```
POST /api/review/reply
```

### 8.3 评价列表（服务维度）

```
GET /api/review/list
```

**Params**：`serviceId=101&score=5&page=1&size=10`

---

## 9. 资产中心（讲师）

### 9.1 账户概览

```
GET /api/account/overview
```

**Response**：

```json
{
  "code": 0,
  "data": {
    "balance": 320.50,
    "frozenBalance": 135.00,
    "totalIncome": 1234.00,
    "totalWithdraw": 800.00,
    "deposit": 100.00
  }
}
```

### 9.2 资金流水

```
GET /api/account/flow
```

**Params**：`bizType=1&page=1&size=20`

### 9.3 申请提现

```
POST /api/account/withdraw/apply
```

**Request**：

```json
{
  "amount": 200.00,
  "channel": 1,
  "accountInfo": "微信实名：张*"
}
```

### 9.4 提现记录

```
GET /api/account/withdraw/list
```

---

## 10. 上传

### 10.1 获取 OSS 上传凭证

```
GET /api/upload/oss-token
```

**Params**：`scene=verify|cover|evidence|chat`

**Response**：

```json
{
  "code": 0,
  "data": {
    "bucket": "jijiang-public",
    "region": "oss-cn-hangzhou",
    "policy": "...",
    "signature": "...",
    "accessKeyId": "...",
    "dir": "public/cover/2026/04/",
    "host": "https://jijiang-public.oss-cn-hangzhou.aliyuncs.com",
    "expire": 1714032000
  }
}
```

> **私有场景**（如实名认证）使用私有 Bucket + 临时 STS。

### 10.2 上传后确认（可选，用于记录访问控制）

```
POST /api/upload/confirm
```

---

## 11. 举报与风控

### 11.1 提交举报

```
POST /api/report/submit
```

**Request**：

```json
{
  "targetType": "user",
  "targetId": 10,
  "reason": "涉嫌代写",
  "images": ["..."]
}
```

### 11.2 站内信文本预检（客户端调用，提升体验）

```
POST /api/risk/text-check
```

**Request**：`{ "content": "加我微信..." }`

**Response**：`{ "code": 0, "data": { "pass": false, "reason": "不得交换联系方式" } }`

---

## 12. 管理后台接口（`/admin/`）

> 仅 `ROLE_ADMIN` 可访问；所有接口路径以 `/admin/` 开头。未授权统一返回 `404 Not Found`（隐藏接口存在性）。

| 路径 | 说明 |
| :--- | :--- |
| `POST /admin/auth/login` | 管理员账号密码登录 |
| `GET /admin/dashboard/overview` | 数据看板 |
| `GET /admin/user/list` / `POST /admin/user/ban` | 用户管理 |
| `GET /admin/verify/pending` / `POST /admin/verify/approve` | 实名审核 |
| `GET /admin/service/list` / `POST /admin/service/offline` | 服务管理 |
| `GET /admin/order/list` / `GET /admin/order/detail` | 订单监控 |
| `GET /admin/dispute/list` / `POST /admin/dispute/resolve` | 仲裁处理 |
| `GET /admin/finance/withdraw` / `POST /admin/finance/withdraw/approve` | 提现审核 |
| `GET /admin/finance/commission` | 佣金账目 |
| `GET /admin/risk/sensitive-word` / `POST /admin/risk/sensitive-word/add` | 敏感词 |
| `GET /admin/risk/report` / `POST /admin/risk/report/handle` | 举报处理 |
| `GET /admin/setting/platform` / `POST /admin/setting/platform/update` | 平台配置 |

详细 Schema 在 Knife4j 自动生成，此处不重复列出。

---

## 13. WebHook 与第三方集成

### 13.1 微信支付回调

- 路径：`POST /api/payment/wx-notify`
- 鉴权：微信支付 V3 签名验证
- 幂等：见后端开发指南 §3.4.2

### 13.2 微信订阅消息

场景：订单状态变更、审核结果、提现结果。

```text
order_paid            —— 您的订单已支付成功
order_accept          —— 讲师已接单
order_deliver         —— 讲师已提交交付凭证
order_refund          —— 退款成功
verify_result         —— 实名认证结果
withdraw_result       —— 提现结果
```

---

## 14. 示例：完整下单支付流程

```text
1. GET  /api/service/detail?id=101
2. POST /api/order/preview           → 确认价格
3. POST /api/order/create            → orderNo=..., status=10
4. POST /api/payment/wx-pay          → 微信支付参数
5. [客户端] uni.requestPayment
6. [微信服务器] → POST /api/payment/wx-notify → 订单 status=20
7. GET  /api/order/detail?id=5001    → 客户端轮询或被动通知
8. [讲师] POST /api/order/accept     → status=30
9. [讲师] POST /api/order/deliver    → status=40
10. [买家] POST /api/order/confirm   → status=50
11. [买家] POST /api/review/submit
12. T+1: 讲师账户 frozen → balance
```

---

## 15. 错误码完整映射

| Code | Message | HTTP |
| :--- | :--- | :-: |
| 0 | success | 200 |
| 10001 | 未登录或登录已过期 | 200 |
| 10002 | Token 解析失败 | 200 |
| 10003 | 账户被封禁 | 200 |
| 10010 | 请先完成实名认证 | 200 |
| 10011 | 请选择学校 | 200 |
| 10012 | 实名信息与证件不符 | 200 |
| 10020 | 请先成为讲师 | 200 |
| 10021 | 请先缴纳保证金 | 200 |
| 10030 | 无权限访问 | 200 |
| 20001 | 服务已下架 | 200 |
| 20002 | 服务不存在 | 200 |
| 20005 | 库存不足 | 200 |
| 20006 | 不能购买自己的服务 | 200 |
| 20007 | 订单不存在 | 200 |
| 20008 | 订单状态不允许该操作 | 200 |
| 30001 | 支付验签失败 | 200 |
| 30002 | 支付失败 | 200 |
| 30010 | 退款金额超过可退金额 | 200 |
| 30020 | 余额不足 | 200 |
| 40001 | 内容命中敏感词 | 200 |
| 40002 | 内容审核未通过 | 200 |
| 60001 | 订单未完成，不可评价 | 200 |
| 60002 | 已评价过 | 200 |
| 70001 | 操作过于频繁 | 200 |
| 70002 | 请勿交换联系方式 | 200 |
| 90001 | 系统繁忙 | 200 |
| 400001 | 参数校验失败 | 200 |

---

## 下一步

- 阅读 [06-核心业务流程](./06-核心业务流程.md) 理解接口背后的状态机
- 阅读 [07-安全与风控设计](./07-安全与风控设计.md) 了解鉴权细节
