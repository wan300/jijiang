CREATE TABLE IF NOT EXISTS `report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reporter_id` BIGINT NOT NULL,
  `target_type` VARCHAR(16) NOT NULL,
  `target_id` BIGINT NOT NULL,
  `reason` VARCHAR(2000) NOT NULL,
  `evidence_urls` VARCHAR(2048),
  `status` TINYINT NOT NULL DEFAULT 1,
  `handler_id` BIGINT,
  `handle_remark` VARCHAR(1000),
  `handle_time` DATETIME,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_report_reporter` (`reporter_id`),
  KEY `idx_report_status` (`status`),
  KEY `idx_report_target` (`target_type`, `target_id`)
);
