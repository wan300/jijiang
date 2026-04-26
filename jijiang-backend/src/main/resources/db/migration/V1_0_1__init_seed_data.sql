INSERT INTO `campus` (`id`, `name`, `short_name`, `email_suffix`, `status`)
SELECT 1, '示例大学', '示大', '@example.edu.cn', 1
WHERE NOT EXISTS (SELECT 1 FROM `campus` WHERE `id` = 1);

INSERT INTO `role` (`id`, `code`, `name`)
SELECT 1, 'ROLE_USER', '普通用户'
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `code` = 'ROLE_USER');

INSERT INTO `role` (`id`, `code`, `name`)
SELECT 2, 'ROLE_SELLER', '讲师'
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `code` = 'ROLE_SELLER');

INSERT INTO `role` (`id`, `code`, `name`)
SELECT 3, 'ROLE_ADMIN', '管理员'
WHERE NOT EXISTS (SELECT 1 FROM `role` WHERE `code` = 'ROLE_ADMIN');

INSERT INTO `category` (`id`, `name`, `sort_order`, `status`)
SELECT 1, '考研保研', 10, 1
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `id` = 1);

INSERT INTO `category` (`id`, `name`, `sort_order`, `status`)
SELECT 2, '期末辅导', 20, 1
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `id` = 2);

INSERT INTO `category` (`id`, `name`, `sort_order`, `status`)
SELECT 3, '软件技能', 30, 1
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `id` = 3);

INSERT INTO `category` (`id`, `name`, `sort_order`, `status`)
SELECT 4, '求职指导', 40, 1
WHERE NOT EXISTS (SELECT 1 FROM `category` WHERE `id` = 4);

INSERT INTO `sensitive_word` (`word`, `level`)
SELECT '微信', 2
WHERE NOT EXISTS (SELECT 1 FROM `sensitive_word` WHERE `word` = '微信');

INSERT INTO `sensitive_word` (`word`, `level`)
SELECT 'QQ', 2
WHERE NOT EXISTS (SELECT 1 FROM `sensitive_word` WHERE `word` = 'QQ');

INSERT INTO `sensitive_word` (`word`, `level`)
SELECT '代写', 3
WHERE NOT EXISTS (SELECT 1 FROM `sensitive_word` WHERE `word` = '代写');

INSERT INTO `sensitive_word` (`word`, `level`)
SELECT '加我', 2
WHERE NOT EXISTS (SELECT 1 FROM `sensitive_word` WHERE `word` = '加我');
