-- verify_audit_log: 实名认证审计日志
CREATE TABLE IF NOT EXISTS `verify_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `record_id` BIGINT NOT NULL,
  `action` VARCHAR(32) NOT NULL,
  `detail` TEXT,
  `operator_id` BIGINT,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_audit_record` (`record_id`)
);

-- deposit_record: 保证金充值/冻结/退还记录
CREATE TABLE IF NOT EXISTS `deposit_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `deposit_type` VARCHAR(32) NOT NULL DEFAULT 'INITIAL',
  `out_trade_no` VARCHAR(64),
  `transaction_id` VARCHAR(64),
  `pay_time` DATETIME,
  `refund_time` DATETIME,
  `remark` VARCHAR(255),
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_deposit_user` (`user_id`),
  KEY `idx_deposit_out_trade_no` (`out_trade_no`)
);

-- deposit_deduction: 保证金扣除记录
CREATE TABLE IF NOT EXISTS `deposit_deduction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `operator_id` BIGINT,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_deduction_user` (`user_id`)
);

-- account_deletion: 账号注销记录
CREATE TABLE IF NOT EXISTS `account_deletion` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 0,
  `request_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `cooling_until` DATETIME NOT NULL,
  `completed_time` DATETIME,
  `cancelled_time` DATETIME,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_deletion_user` (`user_id`),
  KEY `idx_deletion_status` (`status`)
);

-- permission: 权限定义
CREATE TABLE IF NOT EXISTS `permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(64) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `category` VARCHAR(32) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_code` (`code`)
);

-- role_permission: 角色-权限关联
CREATE TABLE IF NOT EXISTS `role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `role_id` BIGINT NOT NULL,
  `permission_code` VARCHAR(64) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`, `permission_code`)
);

-- user_verify_record 新增字段
ALTER TABLE `user_verify_record` ADD COLUMN `real_name_hash` VARCHAR(64);
ALTER TABLE `user_verify_record` ADD COLUMN `cert_no_hash` VARCHAR(64);
ALTER TABLE `user_verify_record` ADD COLUMN `cert_image_key` VARCHAR(256);
ALTER TABLE `user_verify_record` ADD COLUMN `ocr_request_id` VARCHAR(128);
ALTER TABLE `user_verify_record` ADD COLUMN `face_image_url` VARCHAR(512);

-- role 新增 level 字段 (支持权限继承)
ALTER TABLE `role` ADD COLUMN `level` INT NOT NULL DEFAULT 0;

-- user 新增 deletion_status 字段
ALTER TABLE `user` ADD COLUMN `deletion_status` TINYINT NOT NULL DEFAULT 0;

-- seller_account 新增 deposit_amount 字段
ALTER TABLE `seller_account` ADD COLUMN `deposit_amount` DECIMAL(10,2) NOT NULL DEFAULT 0;

-- 修改 deposit_paid 默认值为 0 (未支付保证金)
ALTER TABLE `user` ALTER COLUMN `deposit_paid` SET DEFAULT 0;
