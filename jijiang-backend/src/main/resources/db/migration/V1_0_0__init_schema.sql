CREATE TABLE IF NOT EXISTS `campus` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(128) NOT NULL,
  `short_name` VARCHAR(32) NOT NULL,
  `email_suffix` VARCHAR(64),
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_campus_name` (`name`)
);

CREATE TABLE IF NOT EXISTS `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `openid` VARCHAR(64) NOT NULL,
  `unionid` VARCHAR(64),
  `nickname` VARCHAR(64),
  `avatar_url` VARCHAR(512),
  `real_name_encrypted` VARCHAR(128),
  `student_no_encrypted` VARCHAR(128),
  `campus_id` BIGINT,
  `major` VARCHAR(128),
  `grade` VARCHAR(16),
  `verify_status` TINYINT NOT NULL DEFAULT 0,
  `is_seller_verified` TINYINT NOT NULL DEFAULT 0,
  `current_role` TINYINT NOT NULL DEFAULT 1,
  `credit_score` INT NOT NULL DEFAULT 100,
  `deposit_paid` TINYINT NOT NULL DEFAULT 1,
  `status` TINYINT NOT NULL DEFAULT 1,
  `last_login_time` DATETIME,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_openid` (`openid`),
  KEY `idx_user_campus` (`campus_id`),
  KEY `idx_user_verify` (`verify_status`)
);

CREATE TABLE IF NOT EXISTS `user_verify_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `campus_id` BIGINT NOT NULL,
  `cert_type` TINYINT NOT NULL,
  `cert_image_url` VARCHAR(512) NOT NULL,
  `real_name` VARCHAR(64),
  `student_no` VARCHAR(64),
  `ocr_result` TEXT,
  `ocr_confidence` DECIMAL(5,4),
  `status` TINYINT NOT NULL DEFAULT 1,
  `review_mode` TINYINT NOT NULL DEFAULT 2,
  `reviewer_id` BIGINT,
  `review_time` DATETIME,
  `reject_reason` VARCHAR(255),
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_verify_user` (`user_id`),
  KEY `idx_verify_status` (`status`)
);

CREATE TABLE IF NOT EXISTS `role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `code` VARCHAR(32) NOT NULL,
  `name` VARCHAR(64) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
);

CREATE TABLE IF NOT EXISTS `user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `role_id` BIGINT NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
);

CREATE TABLE IF NOT EXISTS `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(64) NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
);

CREATE TABLE IF NOT EXISTS `service_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `seller_id` BIGINT NOT NULL,
  `campus_id` BIGINT NOT NULL,
  `category_id` BIGINT NOT NULL,
  `title` VARCHAR(128) NOT NULL,
  `description` TEXT,
  `price` DECIMAL(10,2) NOT NULL,
  `price_config` TEXT,
  `cover_url` VARCHAR(512),
  `stock` INT NOT NULL DEFAULT 1,
  `used_stock` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `score_avg` DECIMAL(3,2) NOT NULL DEFAULT 5.00,
  `sales_count` INT NOT NULL DEFAULT 0,
  `publish_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_service_campus_status` (`campus_id`, `status`),
  KEY `idx_service_seller` (`seller_id`)
);

CREATE TABLE IF NOT EXISTS `order_main` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(32) NOT NULL,
  `buyer_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `service_id` BIGINT NOT NULL,
  `campus_id` BIGINT NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 10,
  `remark` VARCHAR(255),
  `deliver_text` TEXT,
  `expire_time` DATETIME,
  `pay_time` DATETIME,
  `accept_time` DATETIME,
  `deliver_time` DATETIME,
  `confirm_time` DATETIME,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_buyer_status` (`buyer_id`, `status`),
  KEY `idx_order_seller_status` (`seller_id`, `status`)
);

CREATE TABLE IF NOT EXISTS `order_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `from_status` TINYINT,
  `to_status` TINYINT NOT NULL,
  `operator_id` BIGINT,
  `remark` VARCHAR(255),
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_order_log_order` (`order_id`)
);

CREATE TABLE IF NOT EXISTS `payment_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `out_trade_no` VARCHAR(64) NOT NULL,
  `transaction_id` VARCHAR(64),
  `amount` DECIMAL(10,2) NOT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `pay_channel` VARCHAR(32) NOT NULL DEFAULT 'XUNHUPAY',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_out_trade_no` (`out_trade_no`),
  KEY `idx_payment_order` (`order_id`)
);

CREATE TABLE IF NOT EXISTS `seller_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `seller_id` BIGINT NOT NULL,
  `balance` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `frozen_balance` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `total_income` DECIMAL(10,2) NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_seller` (`seller_id`)
);

CREATE TABLE IF NOT EXISTS `account_flow` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `seller_id` BIGINT NOT NULL,
  `order_id` BIGINT,
  `flow_type` VARCHAR(32) NOT NULL,
  `amount` DECIMAL(10,2) NOT NULL,
  `balance_after` DECIMAL(10,2) NOT NULL,
  `remark` VARCHAR(255),
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_flow_seller` (`seller_id`)
);

CREATE TABLE IF NOT EXISTS `message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `sender_id` BIGINT NOT NULL,
  `receiver_id` BIGINT NOT NULL,
  `content` VARCHAR(1000) NOT NULL,
  `is_read` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_message_order` (`order_id`)
);

CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL,
  `service_id` BIGINT NOT NULL,
  `reviewer_id` BIGINT NOT NULL,
  `seller_id` BIGINT NOT NULL,
  `score` TINYINT NOT NULL,
  `content` VARCHAR(1000),
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_order_reviewer` (`order_id`, `reviewer_id`)
);

CREATE TABLE IF NOT EXISTS `credit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `order_id` BIGINT,
  `delta` INT NOT NULL,
  `score_after` INT NOT NULL,
  `reason` VARCHAR(255) NOT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_credit_user` (`user_id`)
);

CREATE TABLE IF NOT EXISTS `sensitive_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `word` VARCHAR(64) NOT NULL,
  `level` TINYINT NOT NULL DEFAULT 1,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sensitive_word` (`word`)
);

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT,
  `operation` VARCHAR(64) NOT NULL,
  `target_type` VARCHAR(64),
  `target_id` BIGINT,
  `detail` VARCHAR(1000),
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_operation_target` (`target_type`, `target_id`)
);
