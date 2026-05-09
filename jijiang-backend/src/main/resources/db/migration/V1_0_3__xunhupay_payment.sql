ALTER TABLE `payment_record` ADD COLUMN `pay_url` VARCHAR(512) DEFAULT NULL;
ALTER TABLE `payment_record` ADD COLUMN `notify_body` TEXT DEFAULT NULL;
ALTER TABLE `payment_record` ADD COLUMN `pay_time` DATETIME DEFAULT NULL;

CREATE INDEX `idx_payment_transaction_id` ON `payment_record` (`transaction_id`);
