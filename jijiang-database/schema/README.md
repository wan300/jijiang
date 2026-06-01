# Flyway Schema Migrations

此目录存放 Flyway 迁移 SQL 文件的同步副本，与 `12group-backend/src/main/resources/db/migration/` 保持一致。

Flyway 会自动按版本号顺序执行迁移，迁移文件命名规范：`V{主版本}_{次版本}_{修订号}__{描述}.sql`

## 迁移列表

| 文件 | 对应版本 | 说明 |
|---|---|---|
| `V1_0_14__report_system.sql` | V1.0.14 | 举报与风控工单系统 — report 表 |
