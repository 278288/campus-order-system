-- =============================================================================
-- 校园点餐系统 数据库建表脚本
-- 项目：校园点餐系统（campus-order-system）
-- 学号：2520145325  姓名：郑志雄
-- 数据库：campus_order_db   引擎：InnoDB   字符集：utf8mb4   排序规则：utf8mb4_general_ci
-- 命名规范：全小写加下划线、单词用单数、主键以 id 结尾、外键用 <表名>_id
-- 说明：本脚本可在 MySQL 8.0 及以上版本的命令行或 Navicat 中直接执行。
-- =============================================================================

DROP DATABASE IF EXISTS `campus_order_db`;
CREATE DATABASE `campus_order_db`
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE `campus_order_db`;

-- -----------------------------------------------------------------------------
-- 1. 用户表 user
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    `username`        VARCHAR(50)  NOT NULL COMMENT '登录账号，全局唯一',
    `password`        VARCHAR(100) NOT NULL COMMENT '密码的 BCrypt 带盐哈希，禁止明文存储',
    `real_name`       VARCHAR(50)  NOT NULL COMMENT '真实姓名或昵称',
    `phone`           VARCHAR(20)  NOT NULL COMMENT '手机号，全局唯一，用于找回账号',
    `email`           VARCHAR(100) DEFAULT NULL COMMENT '邮箱，可为空',
    `role`            TINYINT      NOT NULL DEFAULT 0 COMMENT '角色：0师生用户，1食堂商家，2管理员',
    `status`          TINYINT      NOT NULL DEFAULT 1 COMMENT '账号状态：1正常，0停用',
    `last_login_time` DATETIME     DEFAULT NULL COMMENT '最近一次登录成功时间',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_username` (`username`),
    UNIQUE KEY `uk_user_phone` (`phone`),
    KEY `idx_user_role_status` (`role`, `status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='用户表，保存三类角色的账号与权限信息';

-- -----------------------------------------------------------------------------
-- 2. 食堂窗口表 canteen_window
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `canteen_window`;
CREATE TABLE `canteen_window`
(
    `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '窗口主键',
    `window_name`     VARCHAR(50) NOT NULL COMMENT '窗口名称，如“一楼三号窗口”',
    `location`        VARCHAR(100) NOT NULL COMMENT '窗口位置，如“第一食堂一楼东侧”',
    `business_status` TINYINT     NOT NULL DEFAULT 1 COMMENT '营业状态：1营业中，0已打烊',
    `status`          TINYINT     NOT NULL DEFAULT 1 COMMENT '记录状态：1有效，0停用（保留历史订单关联）',
    `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_window_name` (`window_name`),
    KEY `idx_window_status` (`status`, `business_status`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='食堂窗口表，保存窗口位置与营业状态';

-- -----------------------------------------------------------------------------
-- 3. 窗口管理员表 window_manager（用户与窗口的多对多中间表）
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `window_manager`;
CREATE TABLE `window_manager`
(
    `id`          BIGINT   NOT NULL AUTO_INCREMENT COMMENT '中间表主键',
    `window_id`   BIGINT   NOT NULL COMMENT '窗口编号，引用 canteen_window.id',
    `user_id`     BIGINT   NOT NULL COMMENT '商家账号编号，引用 user.id',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_window_manager` (`window_id`, `user_id`),
    KEY `idx_window_manager_user` (`user_id`),
    CONSTRAINT `fk_window_manager_window` FOREIGN KEY (`window_id`) REFERENCES `canteen_window` (`id`),
    CONSTRAINT `fk_window_manager_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='窗口管理员表，维护商家账号与食堂窗口的多对多授权关系';

-- -----------------------------------------------------------------------------
-- 4. 菜品表 dish
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `dish`;
CREATE TABLE `dish`
(
    `id`          BIGINT         NOT NULL AUTO_INCREMENT COMMENT '菜品主键',
    `window_id`   BIGINT         NOT NULL COMMENT '所属窗口编号，引用 canteen_window.id',
    `dish_name`   VARCHAR(80)    NOT NULL COMMENT '菜品名称',
    `category`    VARCHAR(30)    NOT NULL DEFAULT '其他' COMMENT '菜品分类，如主食、荤菜、素菜、汤品、饮品',
    `price`       DECIMAL(10, 2) NOT NULL COMMENT '单价，单位元，必须大于 0',
    `stock`       INT            NOT NULL DEFAULT 0 COMMENT '可售库存，非负整数',
    `image_url`   VARCHAR(255)   DEFAULT NULL COMMENT '菜品图片地址',
    `description` VARCHAR(200)   DEFAULT NULL COMMENT '菜品简介',
    `status`      TINYINT        NOT NULL DEFAULT 1 COMMENT '上下架状态：1在售，0已下架',
    `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_dish_window_status` (`window_id`, `status`),
    KEY `idx_dish_category` (`category`),
    KEY `idx_dish_name` (`dish_name`),
    CONSTRAINT `fk_dish_window` FOREIGN KEY (`window_id`) REFERENCES `canteen_window` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='菜品表，保存各窗口菜品、价格与库存';

-- -----------------------------------------------------------------------------
-- 5. 订单主表 orders
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `orders`;
CREATE TABLE `orders`
(
    `id`            BIGINT         NOT NULL AUTO_INCREMENT COMMENT '订单主键',
    `order_no`      VARCHAR(32)    NOT NULL COMMENT '订单号，对外唯一标识',
    `user_id`       BIGINT         NOT NULL COMMENT '下单用户编号，引用 user.id',
    `window_id`     BIGINT         NOT NULL COMMENT '所属窗口编号，引用 canteen_window.id',
    `status`        TINYINT        NOT NULL DEFAULT 0 COMMENT '订单状态：0待接单，1备餐中，2待取餐，3已完成，4已取消，5已拒绝',
    `total_amount`  DECIMAL(10, 2) NOT NULL COMMENT '成交总金额，单位元，由服务端按菜品单价重新计算',
    `request_no`    VARCHAR(64)    NOT NULL COMMENT '客户端唯一请求号，与 user_id 组成联合唯一约束用于幂等',
    `pickup_code`   VARCHAR(6)     NOT NULL COMMENT '取餐码，6 位数字，与订单绑定，完成后失效',
    `reject_reason` VARCHAR(200)   DEFAULT NULL COMMENT '商家拒单原因',
    `cancel_reason` VARCHAR(200)   DEFAULT NULL COMMENT '用户取消原因',
    `rating`        TINYINT        DEFAULT NULL COMMENT '用户评价星级 1~5，为空表示未评价',
    `rating_comment` VARCHAR(200)  DEFAULT NULL COMMENT '评价内容',
    `accept_time`   DATETIME       DEFAULT NULL COMMENT '商家接单时间',
    `ready_time`    DATETIME       DEFAULT NULL COMMENT '备餐完成、进入待取餐时间',
    `finish_time`   DATETIME       DEFAULT NULL COMMENT '取餐完成时间',
    `create_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `update_time`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_no` (`order_no`),
    UNIQUE KEY `uk_order_user_request` (`user_id`, `request_no`),
    KEY `idx_order_window_status_time` (`window_id`, `status`, `create_time`),
    KEY `idx_order_user_status_time` (`user_id`, `status`, `create_time`),
    KEY `idx_order_create_time` (`create_time`),
    CONSTRAINT `fk_order_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`),
    CONSTRAINT `fk_order_window` FOREIGN KEY (`window_id`) REFERENCES `canteen_window` (`id`),
    CONSTRAINT `ck_order_status` CHECK (`status` BETWEEN 0 AND 5),
    CONSTRAINT `ck_order_amount` CHECK (`total_amount` >= 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='订单主表，保存订单状态、金额与取餐码';

-- -----------------------------------------------------------------------------
-- 6. 订单明细表 order_item
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `order_item`;
CREATE TABLE `order_item`
(
    `id`         BIGINT         NOT NULL AUTO_INCREMENT COMMENT '明细主键',
    `order_id`   BIGINT         NOT NULL COMMENT '所属订单编号，引用 orders.id',
    `dish_id`    BIGINT         NOT NULL COMMENT '菜品编号，引用 dish.id',
    `dish_name`  VARCHAR(80)    NOT NULL COMMENT '下单时的菜品名称快照',
    `unit_price` DECIMAL(10, 2) NOT NULL COMMENT '下单时的菜品单价快照',
    `quantity`   INT            NOT NULL COMMENT '购买数量，正整数',
    `amount`     DECIMAL(10, 2) NOT NULL COMMENT '本条明细小计金额 = unit_price × quantity',
    `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_item_order` (`order_id`),
    KEY `idx_item_dish` (`dish_id`),
    CONSTRAINT `fk_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`),
    CONSTRAINT `fk_item_dish` FOREIGN KEY (`dish_id`) REFERENCES `dish` (`id`),
    CONSTRAINT `ck_item_quantity` CHECK (`quantity` > 0)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='订单明细表，保存菜品名称与单价的成交快照';

-- -----------------------------------------------------------------------------
-- 7. 菜单表 sys_menu（实验4：授权）。
--    用一张表承载目录、菜单、按钮三类数据，通过 menu_type 区分：
--      1 目录（M）——只用于分组，不直接对应接口
--      2 菜单（C）——对应一个页面
--      3 按钮（F）——对应一个具体操作，是权限标识的挂载点
--    权限标识 perms 统一遵循「模块:操作」命名规范，如 user:list、order:accept。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`
(
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '菜单主键',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父菜单编号，0 表示顶级',
    `menu_name`   VARCHAR(50)  NOT NULL COMMENT '菜单名称',
    `menu_type`   TINYINT      NOT NULL COMMENT '类型：1 目录，2 菜单，3 按钮',
    `perms`       VARCHAR(64)  DEFAULT NULL COMMENT '权限标识，模块:操作，如 user:list',
    `path`        VARCHAR(100) DEFAULT NULL COMMENT '前端路由地址',
    `component`   VARCHAR(100) DEFAULT NULL COMMENT '前端组件路径',
    `icon`        VARCHAR(50)  DEFAULT NULL COMMENT '菜单图标',
    `order_num`   INT          NOT NULL DEFAULT 0 COMMENT '同级显示顺序',
    `visible`     TINYINT      NOT NULL DEFAULT 1 COMMENT '是否显示：1 显示，0 隐藏',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：1 正常，0 停用',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_menu_parent` (`parent_id`),
    KEY `idx_menu_perms` (`perms`),
    CONSTRAINT `ck_menu_type` CHECK (`menu_type` BETWEEN 1 AND 3)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='菜单表，一张表承载目录、菜单、按钮三类数据';

-- -----------------------------------------------------------------------------
-- 8. 角色菜单关联表 sys_role_menu。
--    角色用角色编码（ROLE_STUDENT / ROLE_MERCHANT / ROLE_ADMIN）标识，
--    与 user.role 数值一一对应；这样授权关系可以直接写 SQL 维护，无需额外的角色表。
-- -----------------------------------------------------------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`
(
    `id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '关联主键',
    `role_code`   VARCHAR(32) NOT NULL COMMENT '角色编码，如 ROLE_ADMIN',
    `menu_id`     BIGINT      NOT NULL COMMENT '菜单编号，引用 sys_menu.id',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_role_menu` (`role_code`, `menu_id`),
    KEY `idx_role_menu_menu` (`menu_id`),
    CONSTRAINT `fk_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `sys_menu` (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_general_ci COMMENT ='角色菜单关联表，保存角色与菜单、按钮权限的授权关系';

-- =============================================================================
-- 菜单与授权数据（实验4：授权）
-- 权限标识统一为「模块:操作」。目录与菜单本身不参与接口鉴权，
-- 鉴权使用挂在按钮上的权限标识；角色菜单关联表决定每个角色可见的菜单与可用操作。
-- =============================================================================

-- 目录：0 顶级
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `icon`, `order_num`)
VALUES (100, 0, '平台管理', 1, NULL, NULL, 'setting', 1),
       (200, 0, '窗口经营', 1, NULL, NULL, 'shop', 2),
       (300, 0, '我的点餐', 1, NULL, NULL, 'food', 3);

-- 平台管理下的菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `order_num`)
VALUES (101, 100, '账号与角色', 2, NULL, '/admin/users', 'admin/AdminUserView', 1),
       (102, 100, '食堂窗口', 2, NULL, '/admin/windows', 'admin/AdminWindowView', 2),
       (103, 100, '全平台订单', 2, NULL, '/admin/orders', 'admin/AdminOrderView', 3),
       (104, 100, '平台统计', 2, NULL, '/admin/stats', 'admin/AdminStatsView', 4);

-- 平台管理下的按钮权限（模块:操作）
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `order_num`)
VALUES (105, 101, '查询账号列表', 3, 'user:list', 1),
       (106, 101, '查询账号详情', 3, 'user:query', 2),
       (107, 101, '维护账号角色', 3, 'user:edit', 3),
       (108, 101, '启停账号', 3, 'user:status', 4),
       (109, 102, '查询窗口列表', 3, 'window:list', 1),
       (110, 102, '查询窗口详情', 3, 'window:query', 2),
       (111, 102, '新增窗口', 3, 'window:add', 3),
       (112, 102, '修改窗口', 3, 'window:edit', 4),
       (113, 102, '启停窗口', 3, 'window:status', 5),
       (117, 102, '维护窗口营业状态', 3, 'window:business', 6),
       (114, 103, '查询订单列表', 3, 'order:list', 1),
       (115, 103, '查询订单详情', 3, 'order:query', 2),
       (116, 104, '查看统计报表', 3, 'stats:list', 1);

-- 窗口经营下的菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `order_num`)
VALUES (201, 200, '接单与备餐', 2, NULL, '/merchant/orders', 'merchant/MerchantOrderView', 1),
       (202, 200, '核码取餐', 2, NULL, '/merchant/pickup', 'merchant/MerchantPickupView', 2),
       (203, 200, '菜品维护', 2, NULL, '/merchant/dishes', 'merchant/MerchantDishView', 3),
       (204, 200, '窗口统计', 2, NULL, '/merchant/stats', 'merchant/MerchantStatsView', 4);

-- 窗口经营下的按钮权限
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `order_num`)
VALUES (205, 203, '查询本窗口菜品', 3, 'dish:list', 1),
       (206, 203, '查询菜品详情', 3, 'dish:query', 2),
       (207, 203, '新增菜品', 3, 'dish:add', 3),
       (208, 203, '修改菜品', 3, 'dish:edit', 4),
       (209, 203, '菜品上下架', 3, 'dish:status', 5),
       (210, 203, '调整菜品库存', 3, 'dish:stock', 6),
       (211, 201, '接单与流转状态', 3, 'order:accept', 1),
       (212, 201, '拒单', 3, 'order:reject', 2),
       (213, 202, '核验取餐码', 3, 'order:pickup', 1),
       (214, 204, '查看窗口统计', 3, 'stats:list', 1),
       -- 商家需要给自己所属窗口打烊，因此授予 window:business。
       -- 注意父节点必须是商家已被授权的菜单（204 窗口统计），
       -- 否则该按钮不会出现在商家的菜单树里，前端就拿不到这个权限。
       -- window:add / window:edit / window:status 仍仅管理员持有。
       (215, 204, '维护本人窗口营业状态', 3, 'window:business', 2);

-- 我的点餐下的菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `path`, `component`, `order_num`)
VALUES (301, 300, '浏览菜品', 2, NULL, '/dishes', 'DishBrowseView', 1),
       (302, 300, '购物车', 2, NULL, '/cart', 'CartView', 2),
       (303, 300, '我的订单', 2, NULL, '/orders', 'OrderListView', 3);

-- 我的点餐下的按钮权限
-- 注意：order:list 与 order:query 在学生端与商家端都要用，因此分别挂在两个菜单下；
-- 权限标识在接口鉴权时只看值不看来源，重复挂载不会产生问题，
-- 但角色授权时会按 DISTINCT 去重，避免唯一键冲突。
INSERT INTO `sys_menu` (`id`, `parent_id`, `menu_name`, `menu_type`, `perms`, `order_num`)
VALUES (304, 301, '浏览在售菜品', 3, 'dish:list', 1),
       (305, 301, '查询菜品详情', 3, 'dish:query', 2),
       (306, 303, '查询本人订单', 3, 'order:list', 1),
       (307, 303, '查询订单详情', 3, 'order:query', 2),
       (308, 303, '提交订单', 3, 'order:create', 3),
       (309, 303, '取消订单', 3, 'order:cancel', 4),
       (310, 303, '评价订单', 3, 'order:rate', 5);

-- 角色授权：管理员拥有全部菜单与按钮
INSERT INTO `sys_role_menu` (`role_code`, `menu_id`)
SELECT 'ROLE_ADMIN', `id` FROM `sys_menu`;

-- 角色授权：商家 = 「窗口经营」整个目录 + 「全平台订单」这条订单查询子树的完整层级
-- （100 平台管理 → 103 全平台订单 → 114/115 两个按钮）。
-- 必须连父级一起授予：若子节点的父菜单未被授权，该节点会因为找不到父节点
-- 而被当成根节点裸露在菜单树顶层，前端导航会出现无名条目。
-- 数据库自检脚本第 20 项专门校验这一约束。
-- 用 DISTINCT 去重，避免 order:list / order:query 在两处菜单下重复挂载导致唯一键冲突。
INSERT INTO `sys_role_menu` (`role_code`, `menu_id`)
SELECT DISTINCT 'ROLE_MERCHANT', `id`
FROM `sys_menu`
WHERE `id` = 200
   OR `parent_id` = 200
   OR `parent_id` IN (SELECT `id` FROM (SELECT `id` FROM `sys_menu` WHERE `parent_id` = 200) AS t)
   OR `id` IN (100, 103, 114, 115);

-- 角色授权：师生用户 = 「我的点餐」整个目录（含其下菜单与按钮）+ 浏览菜品所需的查询权限。
-- 注意这里要排除 205/206：它们与 304/305 权限标识相同，但挂在商家的「菜品维护」菜单下，
-- 若一并授予，学生菜单树会多出一条上级为「窗口经营」的商家菜单。
INSERT INTO `sys_role_menu` (`role_code`, `menu_id`)
SELECT 'ROLE_STUDENT', `id`
FROM `sys_menu`
WHERE `id` = 300
   OR `parent_id` = 300
   OR `parent_id` IN (SELECT `id` FROM (SELECT `id` FROM `sys_menu` WHERE `parent_id` = 300) AS t)
   OR `id` IN (304, 305);

-- =============================================================================
-- 业务测试数据
-- 密码约定：所有测试账号的初始密码均为 123456，
-- 数据库中保存的是 BCrypt 带盐哈希，禁止保存明文。
-- =============================================================================

-- 用户：1 个管理员、2 个商家、2 个师生用户
INSERT INTO `user` (`username`, `password`, `real_name`, `phone`, `email`, `role`, `status`)
VALUES ('admin', '$2a$10$MxIMDmLxvGpg.ntkNC9KZOnQIUeLlreqK63Thf/4kVZLgm7bln.Ze', '系统管理员', '13900000000', 'admin@campus.edu.cn', 2, 1),
       ('canteen1', '$2a$10$MxIMDmLxvGpg.ntkNC9KZOnQIUeLlreqK63Thf/4kVZLgm7bln.Ze', '张师傅', '13900000001', 'chef1@campus.edu.cn', 1, 1),
       ('canteen2', '$2a$10$MxIMDmLxvGpg.ntkNC9KZOnQIUeLlreqK63Thf/4kVZLgm7bln.Ze', '李师傅', '13900000002', 'chef2@campus.edu.cn', 1, 1),
       ('student1', '$2a$10$MxIMDmLxvGpg.ntkNC9KZOnQIUeLlreqK63Thf/4kVZLgm7bln.Ze', '郑志雄', '13900000003', 'stu1@campus.edu.cn', 0, 1),
       ('student2', '$2a$10$MxIMDmLxvGpg.ntkNC9KZOnQIUeLlreqK63Thf/4kVZLgm7bln.Ze', '王小明', '13900000004', 'stu2@campus.edu.cn', 0, 1);

-- 食堂窗口
INSERT INTO `canteen_window` (`window_name`, `location`, `business_status`, `status`)
VALUES ('一楼一号窗口', '第一食堂一楼东侧 01 号', 1, 1),
       ('一楼二号窗口', '第一食堂一楼东侧 02 号', 1, 1),
       ('二楼风味窗口', '第二食堂二楼南侧 05 号', 0, 1);

-- 窗口管理员授权：商家 canteen1 负责一楼一号窗口，商家 canteen2 负责一楼二号窗口与二楼风味窗口
INSERT INTO `window_manager` (`window_id`, `user_id`)
VALUES (1, 2),
       (2, 3),
       (3, 3);

-- 菜品
INSERT INTO `dish` (`window_id`, `dish_name`, `category`, `price`, `stock`, `description`, `status`)
VALUES (1, '红烧肉盖饭', '主食', 15.00, 60, '五花肉红烧配米饭', 1),
       (1, '番茄炒蛋盖饭', '主食', 12.00, 80, '番茄鸡蛋配米饭', 1),
       (1, '紫菜蛋花汤', '汤品', 3.00, 120, '紫菜蛋花清汤', 1),
       (1, '可乐', '饮品', 3.50, 0, '罐装可乐 330ml，当前售罄', 1),
       (1, '清炒时蔬', '素菜', 8.00, 40, '当季时令蔬菜', 0),
       (2, '宫保鸡丁盖饭', '主食', 14.00, 70, '鸡丁花生配米饭', 1),
       (2, '麻婆豆腐盖饭', '主食', 11.00, 90, '川味麻婆豆腐配米饭', 1),
       (2, '酸辣土豆丝', '素菜', 7.00, 50, '手切土豆丝', 1),
       (2, '豆浆', '饮品', 2.50, 100, '现磨黄豆浆', 1),
       (3, '兰州牛肉面', '主食', 13.00, 40, '手工拉面配牛肉', 1);

-- 订单与明细（覆盖待接单、备餐中、待取餐、已完成、已取消、已拒绝六种状态）
INSERT INTO `orders` (`order_no`, `user_id`, `window_id`, `status`, `total_amount`, `request_no`, `pickup_code`,
                      `reject_reason`, `cancel_reason`, `rating`, `accept_time`, `ready_time`, `finish_time`, `create_time`)
VALUES ('202610100001', 4, 1, 0, 30.00, 'req-seed-0001', '135790', NULL, NULL, NULL, NULL, NULL, NULL, '2026-10-10 11:20:00'),
       ('202610100002', 4, 1, 1, 15.00, 'req-seed-0002', '246810', NULL, NULL, NULL, '2026-10-10 11:35:00', NULL, NULL, '2026-10-10 11:30:00'),
       ('202610100003', 5, 2, 2, 26.00, 'req-seed-0003', '112233', NULL, NULL, NULL, '2026-10-10 12:00:00', '2026-10-10 12:12:00', NULL, '2026-10-10 11:55:00'),
       ('202610090004', 4, 2, 3, 25.00, 'req-seed-0004', '445566', NULL, NULL, 5, '2026-10-09 12:01:00', '2026-10-09 12:10:00', '2026-10-09 12:20:00', '2026-10-09 11:58:00'),
       ('202610090005', 5, 1, 4, 27.00, 'req-seed-0005', '778899', NULL, '临时有事，无法按时取餐', NULL, NULL, NULL, NULL, '2026-10-09 17:00:00'),
       ('202610090006', 4, 3, 5, 13.00, 'req-seed-0006', '990011', '牛肉面原料缺货', NULL, NULL, NULL, NULL, NULL, '2026-10-09 17:30:00');

INSERT INTO `order_item` (`order_id`, `dish_id`, `dish_name`, `unit_price`, `quantity`, `amount`)
VALUES (1, 1, '红烧肉盖饭', 15.00, 2, 30.00),
       (2, 1, '红烧肉盖饭', 15.00, 1, 15.00),
       (3, 6, '宫保鸡丁盖饭', 14.00, 1, 14.00),
       (3, 8, '酸辣土豆丝', 7.00, 1, 7.00),
       (3, 9, '豆浆', 2.50, 2, 5.00),
       (4, 6, '宫保鸡丁盖饭', 14.00, 1, 14.00),
       (4, 7, '麻婆豆腐盖饭', 11.00, 1, 11.00),
       (5, 1, '红烧肉盖饭', 15.00, 1, 15.00),
       (5, 2, '番茄炒蛋盖饭', 12.00, 1, 12.00),
       (6, 10, '兰州牛肉面', 13.00, 1, 13.00);

-- =============================================================================
-- 提交前自检查询
-- =============================================================================
-- 1) 查看全部表及注释
-- SHOW TABLE STATUS FROM campus_order_db;
-- 2) 校验订单明细小计与订单总额是否一致（应返回空集）
-- SELECT o.order_no, o.total_amount, SUM(i.amount) AS item_sum
-- FROM orders o JOIN order_item i ON i.order_id = o.id
-- GROUP BY o.id HAVING ABS(o.total_amount - SUM(i.amount)) > 0.001;
-- 3) 校验是否存在负库存（应返回空集）
-- SELECT id, dish_name, stock FROM dish WHERE stock < 0;
-- 4) 校验索引是否被使用
-- EXPLAIN SELECT * FROM orders WHERE window_id = 1 AND status = 0 ORDER BY create_time DESC;
