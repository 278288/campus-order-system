-- 数据库自检脚本：验证建表结果、约束、索引与数据一致性
USE `campus_order_db`;

SELECT '===== 1. 表清单与存储引擎/字符集 =====' AS section;
SELECT TABLE_NAME, ENGINE, TABLE_COLLATION, TABLE_COMMENT
FROM information_schema.TABLES
WHERE TABLE_SCHEMA = 'campus_order_db'
ORDER BY TABLE_NAME;

SELECT '===== 2. 各表记录数 =====' AS section;
SELECT 'user' AS tbl, COUNT(*) AS cnt FROM `user`
UNION ALL SELECT 'canteen_window', COUNT(*) FROM `canteen_window`
UNION ALL SELECT 'window_manager', COUNT(*) FROM `window_manager`
UNION ALL SELECT 'dish', COUNT(*) FROM `dish`
UNION ALL SELECT 'orders', COUNT(*) FROM `orders`
UNION ALL SELECT 'order_item', COUNT(*) FROM `order_item`;

SELECT '===== 3. 索引清单 =====' AS section;
SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS cols, NON_UNIQUE
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = 'campus_order_db'
GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE
ORDER BY TABLE_NAME, INDEX_NAME;

SELECT '===== 4. 外键约束 =====' AS section;
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE TABLE_SCHEMA = 'campus_order_db' AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, CONSTRAINT_NAME;

SELECT '===== 5. 一致性校验：明细小计合计与订单总额（应返回空集）=====' AS section;
SELECT o.order_no, o.total_amount, SUM(i.amount) AS item_sum
FROM orders o JOIN order_item i ON i.order_id = o.id
GROUP BY o.id, o.order_no, o.total_amount
HAVING ABS(o.total_amount - SUM(i.amount)) > 0.001;

SELECT '===== 6. 一致性校验：负库存（应返回空集）=====' AS section;
SELECT id, dish_name, stock FROM dish WHERE stock < 0;

SELECT '===== 7. 一致性校验：孤立明细（应返回空集）=====' AS section;
SELECT i.id FROM order_item i LEFT JOIN orders o ON o.id = i.order_id WHERE o.id IS NULL;

SELECT '===== 8. 幂等唯一约束验证：重复插入同一 user_id + request_no 应报 1062 =====' AS section;
INSERT INTO orders (order_no, user_id, window_id, status, total_amount, request_no, pickup_code)
VALUES ('DUP-TEST-0001', 4, 1, 0, 1.00, 'req-seed-0001', '000001');

SELECT '===== 9. 索引命中验证：窗口 + 状态 + 时间联合索引 =====' AS section;
EXPLAIN SELECT * FROM orders WHERE window_id = 1 AND status = 0 ORDER BY create_time DESC;

SELECT '===== 10. 索引命中验证：按用户名等值查询走唯一索引 =====' AS section;
EXPLAIN SELECT * FROM user WHERE username = 'admin';

SELECT '===== 11. 账号与角色分布 =====' AS section;
SELECT role, COUNT(*) AS cnt, SUM(status = 1) AS enabled FROM `user` GROUP BY role;

SELECT '===== 12. 外键约束验证：插入不存在的 window_id 应报 1452 =====' AS section;
INSERT INTO orders (order_no, user_id, window_id, status, total_amount, request_no, pickup_code)
VALUES ('FK-TEST-0001', 4, 9999, 0, 1.00, 'req-fk-test', '000002');

SELECT '===== 13. CHECK 约束验证：插入负数金额应报 3819 =====' AS section;
INSERT INTO orders (order_no, user_id, window_id, status, total_amount, request_no, pickup_code)
VALUES ('CK-TEST-0001', 4, 1, 0, -1.00, 'req-ck-test', '000003');

SELECT '===== 14. 菜单表结构验证：一张表承载目录/菜单/按钮三类数据 =====' AS section;
SELECT CASE menu_type WHEN 1 THEN '1 目录' WHEN 2 THEN '2 菜单' WHEN 3 THEN '3 按钮' END AS 类型,
       COUNT(*) AS 数量
FROM sys_menu GROUP BY menu_type ORDER BY menu_type;

SELECT '===== 15. 权限标识规范验证：全部符合「模块:操作」，且无重复挂载导致的歧义 =====' AS section;
SELECT SUBSTRING_INDEX(perms, ':', 1) AS 模块, COUNT(DISTINCT perms) AS 权限数,
       GROUP_CONCAT(DISTINCT perms ORDER BY perms) AS 权限标识
FROM sys_menu WHERE perms IS NOT NULL GROUP BY 模块 ORDER BY 模块;

SELECT '===== 16. 角色授权结果：各角色持有的去重权限标识 =====' AS section;
SELECT rm.role_code AS 角色, COUNT(DISTINCT m.perms) AS 权限数,
       GROUP_CONCAT(DISTINCT m.perms ORDER BY m.perms) AS 权限标识
FROM sys_role_menu rm JOIN sys_menu m ON m.id = rm.menu_id
WHERE m.perms IS NOT NULL GROUP BY rm.role_code ORDER BY rm.role_code;

SELECT '===== 17. 授权完整性验证：每个角色至少持有一个权限（应返回空集）=====' AS section;
SELECT rc.role_code
FROM (SELECT 'ROLE_STUDENT' AS role_code UNION SELECT 'ROLE_MERCHANT' UNION SELECT 'ROLE_ADMIN') rc
LEFT JOIN sys_role_menu rm ON rm.role_code = rc.role_code
LEFT JOIN sys_menu m ON m.id = rm.menu_id AND m.perms IS NOT NULL
GROUP BY rc.role_code
HAVING COUNT(m.perms) = 0;

SELECT '===== 18. 菜单树完整性验证：除顶级外每个菜单的父节点都存在（应返回空集）=====' AS section;
SELECT c.id, c.menu_name, c.parent_id
FROM sys_menu c
LEFT JOIN sys_menu p ON p.id = c.parent_id
WHERE c.parent_id <> 0 AND p.id IS NULL;

SELECT '===== 19. 角色菜单唯一键验证：重复授权同一角色同一菜单应报 1062 =====' AS section;
INSERT INTO sys_role_menu (role_code, menu_id) VALUES ('ROLE_ADMIN', 100);

SELECT '===== 20. 授权层级完整性验证：已授权的子节点其父节点也必须被授权 =====' AS section;
-- 应返回空集。若不为空，说明菜单树的父级缺失，
-- 前端渲染时这些节点会被当成根节点裸露在导航顶层，出现无名条目。
SELECT rm.role_code AS 角色, c.id AS 菜单ID, c.menu_name AS 菜单, c.parent_id AS 缺失的父节点
FROM sys_role_menu rm
JOIN sys_menu c ON c.id = rm.menu_id
LEFT JOIN sys_role_menu rp ON rp.role_code = rm.role_code AND rp.menu_id = c.parent_id
WHERE c.parent_id <> 0
  AND c.menu_type <> 1
  AND rp.menu_id IS NULL;
