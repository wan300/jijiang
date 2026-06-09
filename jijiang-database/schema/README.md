# 核心配置 Schema

此目录仅存放数据库核心配置（如初始化 DDL、字符集、时区等基础设置），不存放业务功能迁移。

业务功能的 Flyway 迁移请直接提交到 `12group-backend/src/main/resources/db/migration/`，由后端 Flyway 统一管理版本执行。
