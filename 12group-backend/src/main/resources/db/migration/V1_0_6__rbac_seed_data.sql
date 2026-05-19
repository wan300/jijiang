-- 更新角色级别 (高级继承低级权限)
UPDATE `role` SET `level` = 0 WHERE `code` = 'ROLE_USER';
UPDATE `role` SET `level` = 10 WHERE `code` = 'ROLE_SELLER';
UPDATE `role` SET `level` = 30 WHERE `code` = 'ROLE_ADMIN';

-- ===== 权限定义 =====
INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'SERVICE_PUBLISH', '发布服务', 'SERVICE'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'SERVICE_PUBLISH');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'SERVICE_REVIEW', '审核服务', 'SERVICE'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'SERVICE_REVIEW');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'SERVICE_OFFLINE', '下架服务', 'SERVICE'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'SERVICE_OFFLINE');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'ORDER_CREATE', '创建订单', 'ORDER'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'ORDER_CREATE');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'ORDER_ACCEPT', '接单', 'ORDER'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'ORDER_ACCEPT');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'ORDER_DELIVER', '交付订单', 'ORDER'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'ORDER_DELIVER');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'ORDER_CONFIRM', '确认完成', 'ORDER'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'ORDER_CONFIRM');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'PAYMENT_CREATE', '发起支付', 'PAYMENT'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'PAYMENT_CREATE');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'PAYMENT_SYNC', '同步支付状态', 'PAYMENT'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'PAYMENT_SYNC');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'REVIEW_SUBMIT', '提交评价', 'REVIEW'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'REVIEW_SUBMIT');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'MESSAGE_SEND', '发送消息', 'MESSAGE'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'MESSAGE_SEND');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'USER_VERIFY', '提交实名认证', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'USER_VERIFY');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'USER_VERIFY_REVIEW', '审核实名认证', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'USER_VERIFY_REVIEW');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'USER_DELETE', '注销账号', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'USER_DELETE');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'DEPOSIT_PAY', '支付保证金', 'DEPOSIT'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'DEPOSIT_PAY');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'DEPOSIT_MANAGE', '管理保证金', 'DEPOSIT'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'DEPOSIT_MANAGE');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'ADMIN_DASHBOARD', '查看仪表盘', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'ADMIN_DASHBOARD');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'ADMIN_ORDER_MANAGE', '管理订单', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'ADMIN_ORDER_MANAGE');

INSERT INTO `permission` (`code`, `name`, `category`)
SELECT 'ADMIN_MANAGE', '系统管理', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM `permission` WHERE `code` = 'ADMIN_MANAGE');

-- ===== 角色-权限关联 =====
-- ROLE_USER (id=1): 基础用户权限
INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'ORDER_CREATE' FROM `role` r WHERE r.code = 'ROLE_USER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'ORDER_CREATE');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'ORDER_CONFIRM' FROM `role` r WHERE r.code = 'ROLE_USER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'ORDER_CONFIRM');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'PAYMENT_CREATE' FROM `role` r WHERE r.code = 'ROLE_USER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'PAYMENT_CREATE');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'PAYMENT_SYNC' FROM `role` r WHERE r.code = 'ROLE_USER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'PAYMENT_SYNC');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'REVIEW_SUBMIT' FROM `role` r WHERE r.code = 'ROLE_USER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'REVIEW_SUBMIT');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'MESSAGE_SEND' FROM `role` r WHERE r.code = 'ROLE_USER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'MESSAGE_SEND');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'USER_VERIFY' FROM `role` r WHERE r.code = 'ROLE_USER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'USER_VERIFY');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'USER_DELETE' FROM `role` r WHERE r.code = 'ROLE_USER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'USER_DELETE');

-- ROLE_SELLER (id=2): 讲师权限 (继承用户权限 + 讲师特有)
INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'SERVICE_PUBLISH' FROM `role` r WHERE r.code = 'ROLE_SELLER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'SERVICE_PUBLISH');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'ORDER_ACCEPT' FROM `role` r WHERE r.code = 'ROLE_SELLER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'ORDER_ACCEPT');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'ORDER_DELIVER' FROM `role` r WHERE r.code = 'ROLE_SELLER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'ORDER_DELIVER');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'DEPOSIT_PAY' FROM `role` r WHERE r.code = 'ROLE_SELLER' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'DEPOSIT_PAY');

-- ROLE_ADMIN (id=3): 管理员权限 (所有权限)
INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'SERVICE_REVIEW' FROM `role` r WHERE r.code = 'ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'SERVICE_REVIEW');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'SERVICE_OFFLINE' FROM `role` r WHERE r.code = 'ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'SERVICE_OFFLINE');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'USER_VERIFY_REVIEW' FROM `role` r WHERE r.code = 'ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'USER_VERIFY_REVIEW');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'DEPOSIT_MANAGE' FROM `role` r WHERE r.code = 'ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'DEPOSIT_MANAGE');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'ADMIN_DASHBOARD' FROM `role` r WHERE r.code = 'ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'ADMIN_DASHBOARD');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'ADMIN_ORDER_MANAGE' FROM `role` r WHERE r.code = 'ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'ADMIN_ORDER_MANAGE');

INSERT INTO `role_permission` (`role_id`, `permission_code`)
SELECT r.id, 'ADMIN_MANAGE' FROM `role` r WHERE r.code = 'ROLE_ADMIN' AND NOT EXISTS (SELECT 1 FROM `role_permission` rp WHERE rp.role_id = r.id AND rp.permission_code = 'ADMIN_MANAGE');
