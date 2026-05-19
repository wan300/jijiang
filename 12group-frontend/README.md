# 12group-frontend

Uni-app 客户端，可在当前目录独立运行。当前主开发方式是本地启动前端和 A 后端，MySQL、Redis 与真实支付服务在服务器运行；前端不直接调用支付服务器，只调用本地后端。

## 当前完成情况

- 已完成：H5/小程序端登录、首页/发现、服务检索、服务发布、实名认证提交流程、下单、支付跳转/同步、订单履约、评价、消息入口、投诉举报入口和用户中心。
- 已包含轻量管理端页面：小程序内的管理员登录、审核列表、服务审核、订单列表/详情。
- 仍是流程占位或后续扩展：提现、退款/仲裁等资金后续流程。

## 本地 H5 启动

```bash
cd 12group-frontend
cp .env.example .env
npm install
npm run dev:h5
```

启动前请先运行本地后端 `http://localhost:8080`。本地 H5 开发保持 `VITE_API_BASE=` 为空，让请求走 Vite 代理；代理目标由 `VITE_API_PROXY_TARGET` 指向本地后端，默认是 `http://localhost:8080`。

## 微信小程序启动

```bash
cd 12group-frontend
cp .env.example .env
npm install
npm run dev:mp-weixin
```

开发期请在微信开发者工具中打开 `12group-frontend` 根目录，不要直接打开旧的 `dist/build/mp-weixin`。`npm run dev:mp-weixin` 会把根目录 `project.config.json` 的 `miniprogramRoot` 固定为 `dist/dev/mp-weixin/`；如果切换过 build/dev 产物，请在微信开发者工具里清缓存并重新编译，必要时重启工具。

微信小程序不能使用浏览器 Vite 代理，请把 `VITE_MP_API_BASE` 配成当前设备能访问的后端地址。模拟器本机调试可用 `http://localhost:8080`；真机联调必须改成电脑局域网 IP 或可访问的后端域名，例如 `http://192.168.x.x:8080`。

本地 HTTP 调试时，还需要在微信开发者工具的“详情 - 本地设置”中勾选“不校验合法域名、web-view（业务域名）、TLS 版本以及 HTTPS 证书”。否则请求 URL 已经正确时，仍可能被域名或协议校验拦截。

生产构建时可把 `VITE_API_BASE` 设置为真实后端地址；本地 Docker 运行建议保持为空，让 Nginx 代理到本机后端。

## Docker Compose

当前目录可以独立构建并运行 H5 静态站点，不依赖仓库根目录配置：

```bash
cd 12group-frontend
cp .env.example .env
docker compose up -d --build
```

默认访问地址为 `http://localhost:8081`。容器内 Nginx 会把 `/api`、`/admin` 和 `/actuator` 代理到宿主机 `localhost:8080`，因此需要先启动后端或把 `VITE_API_BASE` 改成真实后端地址后重新构建。
