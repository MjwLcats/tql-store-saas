CREATE DATABASE IF NOT EXISTS tql_store_saas
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE tql_store_saas;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS sys_tenant (
    id BIGINT PRIMARY KEY COMMENT '租户ID',
    tenant_code VARCHAR(64) NOT NULL UNIQUE COMMENT '租户编码',
    tenant_name VARCHAR(120) NOT NULL COMMENT '租户名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '租户状态：0停用，1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB COMMENT='系统租户表';

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台用户',
    primary_store_id BIGINT COMMENT '用户主要所属门店ID',
    username VARCHAR(64) NOT NULL COMMENT '登录账号',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希值',
    display_name VARCHAR(64) NOT NULL COMMENT '用户显示名称',
    email VARCHAR(128) COMMENT '电子邮箱',
    phone VARCHAR(32) COMMENT '手机号码',
    client_type VARCHAR(16) NOT NULL COMMENT '客户端类型：PLATFORM平台端，MERCHANT商家端',
    data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态：0停用，1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_tenant_client (tenant_id, username, client_type),
    KEY idx_user_tenant (tenant_id),
    KEY idx_user_primary_store (primary_store_id)
) ENGINE=InnoDB COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '门店ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级组织或门店ID，0表示根节点',
    store_code VARCHAR(64) NOT NULL COMMENT '门店编码',
    store_name VARCHAR(120) NOT NULL COMMENT '门店名称',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '门店状态：0停用，1启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '门店排序号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_store_tenant_code (tenant_id, store_code),
    KEY idx_store_tenant_parent (tenant_id, parent_id)
) ENGINE=InnoDB COMMENT='系统门店表';

CREATE TABLE IF NOT EXISTS sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '角色ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台角色',
    role_code VARCHAR(64) NOT NULL COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    client_type VARCHAR(16) NOT NULL COMMENT '客户端类型：PLATFORM平台端，MERCHANT商家端',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '角色状态：0停用，1启用',
    remark VARCHAR(255) COMMENT '角色备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_role_tenant_client_code (tenant_id, client_type, role_code),
    KEY idx_role_tenant (tenant_id)
) ENGINE=InnoDB COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_role_menu_menu (menu_id)
) ENGINE=InnoDB COMMENT='角色菜单关联表';

CREATE TABLE IF NOT EXISTS sys_user_store (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    store_id BIGINT NOT NULL COMMENT '授权门店ID',
    PRIMARY KEY (user_id, store_id),
    KEY idx_user_store_store (store_id)
) ENGINE=InnoDB COMMENT='用户授权门店关联表';

CREATE TABLE IF NOT EXISTS sys_menu (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '菜单ID',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户ID，0表示平台公共菜单',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '父级菜单ID，0表示顶级菜单',
    menu_name VARCHAR(64) NOT NULL COMMENT '菜单名称',
    menu_type VARCHAR(16) NOT NULL DEFAULT 'MENU' COMMENT '节点类型：DIRECTORY目录，MENU菜单，BUTTON按钮',
    route_name VARCHAR(64) DEFAULT NULL COMMENT '前端路由名称',
    route_path VARCHAR(128) DEFAULT NULL COMMENT '前端路由路径',
    component_key VARCHAR(64) DEFAULT NULL COMMENT '前端组件标识',
    icon VARCHAR(64) COMMENT '菜单图标名称',
    permission_code VARCHAR(128) COMMENT '权限编码',
    client_type VARCHAR(16) NOT NULL COMMENT '客户端类型：PLATFORM平台端，MERCHANT商家端',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '菜单排序号',
    visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否显示：0隐藏，1显示',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    system_builtin TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统内置：0否，1是',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_menu_tenant_route_name (tenant_id, client_type, route_name),
    KEY idx_menu_tenant (tenant_id),
    KEY idx_menu_parent (tenant_id, client_type, parent_id, deleted),
    KEY idx_menu_permission (tenant_id, client_type, permission_code, deleted)
) ENGINE=InnoDB COMMENT='系统菜单表';

CREATE TABLE IF NOT EXISTS ops_content (
    id BIGINT PRIMARY KEY COMMENT '内容ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    store_id BIGINT COMMENT '所属门店ID',
    title VARCHAR(160) NOT NULL COMMENT '内容标题',
    category VARCHAR(32) NOT NULL COMMENT '内容分类编码',
    status VARCHAR(16) NOT NULL COMMENT '内容状态编码',
    owner VARCHAR(64) NOT NULL COMMENT '内容负责人姓名',
    publish_time DATETIME COMMENT '发布时间',
    create_by BIGINT COMMENT '创建人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_by BIGINT COMMENT '更新人用户ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    KEY idx_content_tenant (tenant_id),
    KEY idx_content_store (tenant_id, store_id),
    KEY idx_content_query (category, status, publish_time)
) ENGINE=InnoDB COMMENT='门店运营内容表';

CREATE TABLE IF NOT EXISTS integration_sync_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '同步任务ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    provider VARCHAR(32) NOT NULL COMMENT '第三方数据来源编码',
    data_type VARCHAR(32) NOT NULL COMMENT '同步数据类型编码',
    sync_mode VARCHAR(16) NOT NULL COMMENT '同步方式：INCREMENTAL增量，FULL全量',
    trigger_type VARCHAR(16) NOT NULL COMMENT '触发方式：MANUAL手动，SCHEDULED定时，RETRY重试',
    retry_of BIGINT COMMENT '原失败任务ID',
    range_start DATE COMMENT '同步业务日期起始值',
    range_end DATE COMMENT '同步业务日期结束值',
    status VARCHAR(16) NOT NULL COMMENT '任务状态：PENDING待执行，RUNNING执行中，SUCCESS成功，FAILED失败',
    total_count INT NOT NULL DEFAULT 0 COMMENT '待处理数据总数',
    success_count INT NOT NULL DEFAULT 0 COMMENT '处理成功数量',
    failed_count INT NOT NULL DEFAULT 0 COMMENT '处理失败数量',
    error_message VARCHAR(1000) COMMENT '任务失败原因',
    active_lock_key VARCHAR(160) COMMENT '运行中任务防重复锁键',
    created_by BIGINT NOT NULL COMMENT '任务创建人用户ID',
    started_at DATETIME COMMENT '任务开始执行时间',
    finished_at DATETIME COMMENT '任务执行结束时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '任务创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '任务更新时间',
    UNIQUE KEY uk_integration_sync_active (active_lock_key),
    KEY idx_integration_sync_tenant_time (tenant_id, create_time),
    KEY idx_integration_sync_query (tenant_id, provider, data_type, status),
    KEY idx_integration_sync_retry (retry_of)
) ENGINE=InnoDB COMMENT='第三方数据同步任务表';

CREATE TABLE IF NOT EXISTS integration_sync_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '同步日志ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    task_id BIGINT NOT NULL COMMENT '同步任务ID',
    log_level VARCHAR(16) NOT NULL COMMENT '日志级别',
    stage VARCHAR(32) NOT NULL COMMENT '任务执行阶段',
    message VARCHAR(500) NOT NULL COMMENT '日志消息',
    detail TEXT COMMENT '日志详细信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '日志创建时间',
    KEY idx_integration_log_task (tenant_id, task_id, id)
) ENGINE=InnoDB COMMENT='第三方数据同步日志表';

CREATE TABLE IF NOT EXISTS integration_hll_shop (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_shop_id VARCHAR(64) NOT NULL COMMENT '哗啦啦门店ID',
    external_group_id VARCHAR(64) DEFAULT NULL COMMENT '哗啦啦集团ID',
    external_shop_code VARCHAR(64) DEFAULT NULL COMMENT '哗啦啦组织编码',
    shop_name VARCHAR(160) NOT NULL COMMENT '门店名称',
    brand_id VARCHAR(64) DEFAULT NULL COMMENT '哗啦啦品牌ID',
    brand_name VARCHAR(160) DEFAULT NULL COMMENT '品牌名称',
    business_model VARCHAR(32) DEFAULT NULL COMMENT '业务模式',
    operation_mode VARCHAR(32) DEFAULT NULL COMMENT '运营模式',
    business_status VARCHAR(32) DEFAULT NULL COMMENT '门店营业状态',
    city_code VARCHAR(32) DEFAULT NULL COMMENT '城市编码',
    city_name VARCHAR(64) DEFAULT NULL COMMENT '城市名称',
    address VARCHAR(255) DEFAULT NULL COMMENT '门店详细地址',
    shop_phone VARCHAR(32) DEFAULT NULL COMMENT '门店联系电话',
    shop_open_time VARCHAR(64) DEFAULT NULL COMMENT '门店营业时间',
    image_path VARCHAR(500) DEFAULT NULL COMMENT '门店图片路径',
    longitude DECIMAL(10, 6) DEFAULT NULL COMMENT '百度地图经度',
    latitude DECIMAL(10, 6) DEFAULT NULL COMMENT '百度地图纬度',
    record_action VARCHAR(32) DEFAULT NULL COMMENT '第三方记录状态',
    source_create_time DATETIME DEFAULT NULL COMMENT '第三方记录创建时间',
    source_update_time DATETIME DEFAULT NULL COMMENT '第三方记录修改时间',
    raw_json JSON NOT NULL COMMENT '第三方门店原始JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_hll_shop (tenant_id, external_shop_id),
    KEY idx_integration_hll_shop_group (tenant_id, external_group_id),
    KEY idx_integration_hll_shop_code (tenant_id, external_shop_code),
    KEY idx_integration_hll_shop_brand (tenant_id, brand_id),
    KEY idx_integration_hll_shop_status (tenant_id, business_status, deleted),
    KEY idx_integration_hll_shop_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='哗啦啦门店数据镜像表';

INSERT INTO sys_tenant (id, tenant_code, tenant_name, status)
VALUES (10001, 'TQL-DEMO', '同庆楼示范商家', 1)
ON DUPLICATE KEY UPDATE tenant_name = VALUES(tenant_name), status = VALUES(status);

INSERT INTO sys_user
    (id, tenant_id, primary_store_id, username, password_hash, display_name, email, phone, client_type, data_scope, status)
VALUES
    (1, 0, NULL, 'platform_admin',
     '120000:4f1a9c77b0d24a3e:bcf9a7840a376df2847f170bf665f5688ea24751e699c7060445c17b402ae90d',
     '平台管理员', 'platform@tql.local', '13800000001', 'PLATFORM', 'ALL', 1),
    (2, 10001, 11001, 'merchant_admin',
     '120000:9bd38fe1c2204d91:cba31f0ed99c1746e8cee560adb04c98b5c27ec3e13a6d3fa3c6213a26c676ae',
     '商家管理员', 'merchant@tql.local', '13800000002', 'MERCHANT', 'ALL', 1)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    display_name = VALUES(display_name),
    email = VALUES(email),
    phone = VALUES(phone),
    tenant_id = VALUES(tenant_id),
    primary_store_id = VALUES(primary_store_id),
    data_scope = VALUES(data_scope),
    status = VALUES(status);

INSERT INTO sys_store (id, tenant_id, parent_id, store_code, store_name, status, sort_order)
VALUES
    (11001, 10001, 0, 'TQL-HQ', '同庆楼示范商家总部', 1, 10),
    (11002, 10001, 11001, 'TQL-BH', '同庆楼包河门店', 1, 20),
    (11003, 10001, 11001, 'TQL-BH-LAKE', '同庆楼滨湖门店', 1, 30),
    (11004, 10001, 11001, 'TQL-XZ', '同庆楼新站门店', 1, 40)
ON DUPLICATE KEY UPDATE
    store_name = VALUES(store_name), parent_id = VALUES(parent_id),
    status = VALUES(status), sort_order = VALUES(sort_order);

INSERT INTO sys_role (id, tenant_id, role_code, role_name, client_type, status, remark)
VALUES
    (1001, 0, 'PLATFORM_SUPER_ADMIN', '平台超级管理员', 'PLATFORM', 1, '平台端全部权限'),
    (2001, 10001, 'MERCHANT_ADMIN', '商家管理员', 'MERCHANT', 1, '商家端全部权限'),
    (2002, 10001, 'STORE_MANAGER', '门店店长', 'MERCHANT', 1, '门店日常管理权限'),
    (2003, 10001, 'STORE_STAFF', '门店员工', 'MERCHANT', 1, '门店基础操作权限')
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name), status = VALUES(status), remark = VALUES(remark);

INSERT INTO sys_menu
    (id, tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
VALUES
    (100, 0, 0, '平台工作台', 'MENU', 'PlatformDashboard', '/dashboard', 'dashboard', 'IconDashboard', 'platform:dashboard:view', 'PLATFORM', 10, 1, 1, 1, 0),
    (101, 0, 0, '内容审查', 'MENU', 'PlatformContent', '/content', 'content', 'IconFile', 'platform:content:view', 'PLATFORM', 20, 1, 1, 1, 0),
    (105, 0, 0, '系统管理', 'DIRECTORY', NULL, '/system', NULL, 'IconSettings', NULL, 'PLATFORM', 30, 1, 1, 1, 0),
    (103, 0, 105, '平台用户', 'MENU', 'PlatformUsers', '/users', 'users', 'IconUser', 'platform:system:user:view', 'PLATFORM', 10, 1, 1, 1, 0),
    (104, 0, 105, '平台角色', 'MENU', 'PlatformRoles', '/roles', 'roles', 'IconSettings', 'platform:system:role:view', 'PLATFORM', 20, 1, 1, 1, 0),
    (106, 0, 105, '菜单管理', 'MENU', 'PlatformMenus', '/system/menus', 'menu-management', 'IconMenu', 'platform:system:menu:view', 'PLATFORM', 30, 1, 1, 1, 0),
    (107, 0, 106, '新增菜单', 'BUTTON', NULL, NULL, NULL, NULL, 'platform:system:menu:create', 'PLATFORM', 10, 0, 1, 1, 0),
    (108, 0, 106, '修改菜单', 'BUTTON', NULL, NULL, NULL, NULL, 'platform:system:menu:update', 'PLATFORM', 20, 0, 1, 1, 0),
    (109, 0, 106, '删除菜单', 'BUTTON', NULL, NULL, NULL, NULL, 'platform:system:menu:delete', 'PLATFORM', 30, 0, 1, 1, 0),
    (102, 0, 0, '个人中心', 'MENU', 'PlatformProfile', '/profile', 'profile', 'IconUser', 'profile:view', 'PLATFORM', 90, 1, 1, 1, 0),
    (200, 10001, 0, '商家工作台', 'MENU', 'MerchantDashboard', '/dashboard', 'dashboard', 'IconDashboard', 'merchant:dashboard:view', 'MERCHANT', 10, 1, 1, 0, 0),
    (201, 10001, 0, '内容管理', 'MENU', 'MerchantContent', '/content', 'content', 'IconFile', 'merchant:content:view', 'MERCHANT', 20, 1, 1, 0, 0),
    (206, 10001, 0, '系统管理', 'DIRECTORY', NULL, '/system', NULL, 'IconSettings', NULL, 'MERCHANT', 30, 1, 1, 0, 0),
    (203, 10001, 206, '用户管理', 'MENU', 'MerchantUsers', '/users', 'users', 'IconUser', 'merchant:system:user:view', 'MERCHANT', 10, 1, 1, 0, 0),
    (204, 10001, 206, '角色管理', 'MENU', 'MerchantRoles', '/roles', 'roles', 'IconSettings', 'merchant:system:role:view', 'MERCHANT', 20, 1, 1, 0, 0),
    (205, 10001, 0, '第三方数据同步', 'MENU', 'MerchantIntegrationSync', '/integration/sync', 'integration-sync', 'IconSync', 'merchant:integration:sync:view', 'MERCHANT', 50, 1, 1, 0, 0),
    (202, 10001, 0, '个人中心', 'MENU', 'MerchantProfile', '/profile', 'profile', 'IconUser', 'profile:view', 'MERCHANT', 90, 1, 1, 0, 0)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    menu_name = VALUES(menu_name),
    menu_type = VALUES(menu_type),
    route_path = VALUES(route_path),
    component_key = VALUES(component_key),
    icon = VALUES(icon),
    permission_code = VALUES(permission_code),
    sort_order = VALUES(sort_order),
    visible = VALUES(visible),
    status = VALUES(status),
    system_builtin = VALUES(system_builtin),
    deleted = 0;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
VALUES (1, 1001), (2, 2001);

INSERT IGNORE INTO sys_user_store (user_id, store_id)
VALUES (2, 11001), (2, 11002), (2, 11003), (2, 11004);

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 1001, id FROM sys_menu WHERE tenant_id = 0 AND client_type = 'PLATFORM';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2001, id FROM sys_menu WHERE tenant_id = 10001 AND client_type = 'MERCHANT';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2002, id FROM sys_menu
WHERE tenant_id = 10001 AND client_type = 'MERCHANT'
  AND route_path IN ('/dashboard', '/content', '/profile');

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT 2003, id FROM sys_menu
WHERE tenant_id = 10001 AND client_type = 'MERCHANT'
  AND route_path IN ('/dashboard', '/profile');

INSERT INTO ops_content
    (id, tenant_id, store_id, title, category, status, owner, publish_time, create_by)
VALUES
    (1001, 10001, 11002, '七月门店食品安全巡检通知', '运营通知', 'PUBLISHED', '张敏', '2026-07-15 09:30:00', 2),
    (1002, 10001, 11003, '夏季新品菜品培训资料', '培训资料', 'PUBLISHED', '李伟', '2026-07-14 15:10:00', 2),
    (1003, 10001, 11001, '华东区域周度经营复盘', '经营分析', 'DRAFT', '王芳', NULL, 2),
    (1004, 10001, 11004, '门店服务标准升级说明', '运营通知', 'PUBLISHED', '陈晨', '2026-07-12 11:20:00', 2),
    (1005, 10001, 11001, '采购预测使用手册', '培训资料', 'OFFLINE', '赵磊', '2026-07-10 14:00:00', 2),
    (1006, 10001, 11002, '六月顾客满意度分析', '经营分析', 'PUBLISHED', '孙悦', '2026-07-08 16:45:00', 2),
    (1007, 10001, 11003, '门店整改任务处理规范', '制度规范', 'DRAFT', '张敏', NULL, 2),
    (1008, 10001, 11004, '高温天气门店运营提醒', '运营通知', 'PUBLISHED', '李伟', '2026-07-06 10:15:00', 2)
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    category = VALUES(category),
    status = VALUES(status),
    owner = VALUES(owner),
    store_id = VALUES(store_id),
    publish_time = VALUES(publish_time),
    deleted = 0;

-- 人力管家镜像表及平台端、商家端独立组织用户主数据表。
-- 本节与 V007__hr_butler_master_data.sql 保持一致，确保全新环境可一次初始化。
USE tql_store_saas;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS integration_rlgj_organization (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_org_id VARCHAR(64) NOT NULL COMMENT '人力管家组织ID',
    external_parent_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家上级组织ID',
    org_code VARCHAR(64) DEFAULT NULL COMMENT '人力管家组织编码',
    org_name VARCHAR(160) NOT NULL COMMENT '组织名称',
    org_type VARCHAR(32) NOT NULL COMMENT '组织类型：company公司、dept部门、store门店、area区域',
    store_code VARCHAR(64) DEFAULT NULL COMMENT '门店编码，仅门店类型组织有值',
    lead_user_id VARCHAR(64) DEFAULT NULL COMMENT '组织负责人在人力管家的用户ID',
    lead_name VARCHAR(64) DEFAULT NULL COMMENT '组织负责人姓名',
    valid_status TINYINT NOT NULL DEFAULT 1 COMMENT '人力管家组织状态：0删除，1正常',
    brand_id VARCHAR(64) DEFAULT NULL COMMENT '品牌ID，仅门店类型组织可能有值',
    brand_name VARCHAR(160) DEFAULT NULL COMMENT '品牌名称',
    province VARCHAR(64) DEFAULT NULL COMMENT '所在省份名称',
    city VARCHAR(64) DEFAULT NULL COMMENT '所在城市名称',
    district VARCHAR(64) DEFAULT NULL COMMENT '所在区县名称',
    org_address VARCHAR(255) DEFAULT NULL COMMENT '组织详细地址',
    cost_org_code VARCHAR(64) DEFAULT NULL COMMENT '成本组织编码',
    corporation_code VARCHAR(64) DEFAULT NULL COMMENT '法人公司编码',
    corporation_name VARCHAR(160) DEFAULT NULL COMMENT '法人公司名称',
    super_lead_user_id VARCHAR(64) DEFAULT NULL COMMENT '分管领导在人力管家的用户ID',
    source_create_time DATETIME DEFAULT NULL COMMENT '人力管家记录创建时间',
    source_update_time DATETIME DEFAULT NULL COMMENT '人力管家记录更新时间',
    raw_json JSON NOT NULL COMMENT '经过字段筛选后的第三方组织JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_rlgj_org (tenant_id, external_org_id),
    KEY idx_integration_rlgj_org_parent (tenant_id, external_parent_id),
    KEY idx_integration_rlgj_org_type (tenant_id, org_type, valid_status, deleted),
    KEY idx_integration_rlgj_org_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力管家组织数据镜像表';

CREATE TABLE IF NOT EXISTS integration_rlgj_position (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_position_id VARCHAR(64) NOT NULL COMMENT '人力管家职位ID',
    position_name VARCHAR(160) NOT NULL COMMENT '职位名称',
    raw_json JSON NOT NULL COMMENT '经过字段筛选后的第三方职位JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_rlgj_position (tenant_id, external_position_id),
    KEY idx_integration_rlgj_position_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力管家职位数据镜像表';

CREATE TABLE IF NOT EXISTS integration_rlgj_post (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_org_id VARCHAR(64) NOT NULL COMMENT '岗位所属的人力管家组织ID',
    external_post_id VARCHAR(64) NOT NULL COMMENT '人力管家岗位ID',
    external_parent_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家上级岗位ID',
    external_position_id VARCHAR(64) DEFAULT NULL COMMENT '岗位关联的人力管家职位ID',
    post_name VARCHAR(160) NOT NULL COMMENT '岗位名称',
    post_type VARCHAR(64) DEFAULT NULL COMMENT '岗位分类ID',
    post_type_name VARCHAR(160) DEFAULT NULL COMMENT '岗位分类名称',
    raw_json JSON NOT NULL COMMENT '经过字段筛选后的第三方岗位JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_rlgj_post (tenant_id, external_org_id, external_post_id),
    KEY idx_integration_rlgj_post_position (tenant_id, external_position_id),
    KEY idx_integration_rlgj_post_parent (tenant_id, external_org_id, external_parent_id),
    KEY idx_integration_rlgj_post_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力管家岗位数据镜像表';

CREATE TABLE IF NOT EXISTS integration_rlgj_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    external_user_id VARCHAR(64) NOT NULL COMMENT '人力管家用户ID',
    external_org_id VARCHAR(64) DEFAULT NULL COMMENT '用户所属的人力管家组织ID',
    user_number VARCHAR(64) DEFAULT NULL COMMENT '员工工号',
    user_name VARCHAR(64) NOT NULL COMMENT '员工姓名',
    user_name_pinyin VARCHAR(160) DEFAULT NULL COMMENT '员工姓名拼音',
    gender_code VARCHAR(32) DEFAULT NULL COMMENT '性别编码：118001男，118002女',
    mobile VARCHAR(32) DEFAULT NULL COMMENT '手机号码',
    email VARCHAR(128) DEFAULT NULL COMMENT '电子邮箱',
    user_type VARCHAR(32) DEFAULT NULL COMMENT '人力管家用工类型编码',
    user_status VARCHAR(32) DEFAULT NULL COMMENT '人力管家员工状态编码',
    external_position_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家职位ID',
    position_name VARCHAR(160) DEFAULT NULL COMMENT '职位名称',
    external_post_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家岗位ID',
    post_name VARCHAR(160) DEFAULT NULL COMMENT '岗位名称',
    post_type_name VARCHAR(160) DEFAULT NULL COMMENT '岗位分类名称',
    external_rank_id VARCHAR(64) DEFAULT NULL COMMENT '人力管家职级ID',
    rank_name VARCHAR(160) DEFAULT NULL COMMENT '职级名称',
    leader_user_ids VARCHAR(500) DEFAULT NULL COMMENT '直接上级人力管家用户ID列表，多个ID使用逗号分隔',
    offer_date DATE DEFAULT NULL COMMENT '实际转正日期',
    start_date DATE DEFAULT NULL COMMENT '入职日期',
    raw_json JSON NOT NULL COMMENT '已移除证件号、银行卡号等敏感字段的第三方用户JSON数据',
    last_sync_task_id BIGINT NOT NULL COMMENT '最后同步任务ID',
    last_sync_time DATETIME NOT NULL COMMENT '最后同步时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_integration_rlgj_user (tenant_id, external_user_id),
    KEY idx_integration_rlgj_user_org (tenant_id, external_org_id),
    KEY idx_integration_rlgj_user_post (tenant_id, external_post_id),
    KEY idx_integration_rlgj_user_status (tenant_id, user_status, deleted),
    KEY idx_integration_rlgj_user_task (tenant_id, last_sync_task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人力管家用户数据镜像表';

CREATE TABLE IF NOT EXISTS sys_platform_organization (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '平台组织ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级平台组织ID，0表示根组织',
    org_code VARCHAR(64) NOT NULL COMMENT '平台组织编码',
    org_name VARCHAR(160) NOT NULL COMMENT '平台组织名称',
    org_type VARCHAR(32) NOT NULL DEFAULT 'DEPARTMENT' COMMENT '平台组织类型编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '平台组织状态：0停用，1启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '平台组织排序号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人平台用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人平台用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_platform_org_code (org_code),
    KEY idx_sys_platform_org_parent (parent_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台端组织表';

CREATE TABLE IF NOT EXISTS sys_platform_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '平台用户ID',
    organization_id BIGINT DEFAULT NULL COMMENT '所属平台组织ID',
    username VARCHAR(64) NOT NULL COMMENT '平台登录账号',
    password_hash VARCHAR(255) NOT NULL COMMENT '平台登录密码哈希值',
    display_name VARCHAR(64) NOT NULL COMMENT '平台用户显示名称',
    email VARCHAR(128) DEFAULT NULL COMMENT '电子邮箱',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号码',
    data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '平台用户状态：0停用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人平台用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人平台用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_platform_user_name (username),
    KEY idx_sys_platform_user_org (organization_id, status, deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台端用户表';

CREATE TABLE IF NOT EXISTS sys_merchant_organization (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商家组织ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    parent_id BIGINT NOT NULL DEFAULT 0 COMMENT '上级商家组织ID，0表示根组织',
    org_code VARCHAR(64) NOT NULL COMMENT '商家组织编码',
    org_name VARCHAR(160) NOT NULL COMMENT '商家组织名称',
    org_type VARCHAR(32) NOT NULL COMMENT '商家组织类型编码',
    store_code VARCHAR(64) DEFAULT NULL COMMENT '门店编码，仅门店组织有值',
    source_type VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '数据来源：LOCAL本地、HR_BUTLER人力管家',
    source_id VARCHAR(64) DEFAULT NULL COMMENT '第三方来源记录ID，本地数据为空',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '商家组织状态：0停用，1启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '商家组织排序号',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人商家用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人商家用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_merchant_org_code (tenant_id, org_code),
    UNIQUE KEY uk_sys_merchant_org_source (tenant_id, source_type, source_id),
    KEY idx_sys_merchant_org_parent (tenant_id, parent_id, status, deleted),
    KEY idx_sys_merchant_org_store (tenant_id, store_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家端组织表';

CREATE TABLE IF NOT EXISTS sys_merchant_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商家用户ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    organization_id BIGINT DEFAULT NULL COMMENT '所属商家组织ID',
    username VARCHAR(64) DEFAULT NULL COMMENT '商家登录账号，未开通登录时为空',
    password_hash VARCHAR(255) DEFAULT NULL COMMENT '商家登录密码哈希值，未开通登录时为空',
    employee_number VARCHAR(64) DEFAULT NULL COMMENT '员工工号',
    display_name VARCHAR(64) NOT NULL COMMENT '商家用户显示名称',
    name_pinyin VARCHAR(160) DEFAULT NULL COMMENT '姓名拼音',
    gender_code VARCHAR(32) DEFAULT NULL COMMENT '性别编码：118001男，118002女',
    email VARCHAR(128) DEFAULT NULL COMMENT '电子邮箱',
    phone VARCHAR(32) DEFAULT NULL COMMENT '手机号码',
    position_id VARCHAR(64) DEFAULT NULL COMMENT '第三方职位ID',
    position_name VARCHAR(160) DEFAULT NULL COMMENT '职位名称',
    post_id VARCHAR(64) DEFAULT NULL COMMENT '第三方岗位ID',
    post_name VARCHAR(160) DEFAULT NULL COMMENT '岗位名称',
    source_type VARCHAR(32) NOT NULL DEFAULT 'LOCAL' COMMENT '数据来源：LOCAL本地、HR_BUTLER人力管家',
    source_id VARCHAR(64) DEFAULT NULL COMMENT '第三方来源记录ID，本地数据为空',
    login_enabled TINYINT NOT NULL DEFAULT 0 COMMENT '是否允许登录：0不允许，1允许',
    directory_visible TINYINT NOT NULL DEFAULT 1 COMMENT '是否在用户目录显示：0隐藏，1显示',
    data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围编码',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '商家用户状态：0停用，1启用',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标志：0未删除，1已删除',
    create_by BIGINT DEFAULT NULL COMMENT '创建人商家用户ID',
    update_by BIGINT DEFAULT NULL COMMENT '更新人商家用户ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_merchant_user_name (tenant_id, username),
    UNIQUE KEY uk_sys_merchant_user_source (tenant_id, source_type, source_id),
    KEY idx_sys_merchant_user_org (tenant_id, organization_id, status, deleted),
    KEY idx_sys_merchant_user_directory (tenant_id, directory_visible, status, deleted),
    KEY idx_sys_merchant_user_employee (tenant_id, employee_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家端用户表';

INSERT INTO sys_platform_organization
    (id, parent_id, org_code, org_name, org_type, status, sort_order)
VALUES (1, 0, 'TQL_PLATFORM', '同庆楼SaaS平台', 'COMPANY', 1, 10)
ON DUPLICATE KEY UPDATE org_name = VALUES(org_name), status = VALUES(status), deleted = 0;

INSERT INTO sys_platform_user
    (id, organization_id, username, password_hash, display_name, email, phone, data_scope, status)
SELECT id, 1, username, password_hash, display_name, email, phone, data_scope, status
FROM sys_user
WHERE id = 1 AND client_type = 'PLATFORM'
ON DUPLICATE KEY UPDATE
    organization_id = VALUES(organization_id), password_hash = VALUES(password_hash),
    display_name = VALUES(display_name), email = VALUES(email), phone = VALUES(phone),
    data_scope = VALUES(data_scope), status = VALUES(status), deleted = 0;

INSERT INTO sys_merchant_organization
    (id, tenant_id, parent_id, org_code, org_name, org_type, store_code, source_type, source_id, status, sort_order)
SELECT id, tenant_id, parent_id, store_code, store_name, 'STORE', store_code, 'LOCAL', NULL, status, sort_order
FROM sys_store
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), org_name = VALUES(org_name), store_code = VALUES(store_code),
    status = VALUES(status), sort_order = VALUES(sort_order), deleted = 0;

INSERT INTO sys_merchant_user
    (id, tenant_id, organization_id, username, password_hash, display_name, email, phone,
     source_type, source_id, login_enabled, data_scope, status)
SELECT id, tenant_id, primary_store_id, username, password_hash, display_name, email, phone,
       'LOCAL', NULL, 1, data_scope, status
FROM sys_user
WHERE client_type = 'MERCHANT'
ON DUPLICATE KEY UPDATE
    organization_id = VALUES(organization_id), password_hash = VALUES(password_hash),
    display_name = VALUES(display_name), email = VALUES(email), phone = VALUES(phone),
    login_enabled = VALUES(login_enabled), data_scope = VALUES(data_scope),
    status = VALUES(status), deleted = 0;

-- 平台端与商家端用户、角色、门店权限完整拆分。
-- 本节与 V008__split_platform_merchant_user_rbac.sql 保持一致。
USE tql_store_saas;
SET NAMES utf8mb4;

SET @primary_store_column_exists = (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = 'tql_store_saas'
      AND TABLE_NAME = 'sys_merchant_user'
      AND COLUMN_NAME = 'primary_store_id'
);
SET @add_primary_store_sql = IF(
    @primary_store_column_exists = 0,
    'ALTER TABLE sys_merchant_user ADD COLUMN primary_store_id BIGINT DEFAULT NULL COMMENT ''用户主要所属门店ID'' AFTER organization_id, ADD KEY idx_sys_merchant_user_primary_store (tenant_id, primary_store_id)',
    'SELECT 1'
);
PREPARE add_primary_store_statement FROM @add_primary_store_sql;
EXECUTE add_primary_store_statement;
DEALLOCATE PREPARE add_primary_store_statement;

CREATE TABLE IF NOT EXISTS sys_platform_user_role (
    platform_user_id BIGINT NOT NULL COMMENT '平台用户ID',
    role_id BIGINT NOT NULL COMMENT '平台角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (platform_user_id, role_id),
    KEY idx_platform_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_merchant_user_role (
    merchant_user_id BIGINT NOT NULL COMMENT '商家用户ID',
    role_id BIGINT NOT NULL COMMENT '商家角色ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (merchant_user_id, role_id),
    KEY idx_merchant_user_role_role (role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家用户角色关联表';

CREATE TABLE IF NOT EXISTS sys_merchant_user_store (
    merchant_user_id BIGINT NOT NULL COMMENT '商家用户ID',
    store_id BIGINT NOT NULL COMMENT '授权门店ID',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (merchant_user_id, store_id),
    KEY idx_merchant_user_store_store (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商家用户授权门店关联表';

SET @user_source_table = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user'),
    'sys_user', 'sys_user_legacy'
);
SET @user_role_source_table = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_role'),
    'sys_user_role', 'sys_user_role_legacy'
);
SET @user_store_source_table = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_store'),
    'sys_user_store', 'sys_user_store_legacy'
);
SET @create_user_source_view_sql = CONCAT(
    'CREATE OR REPLACE VIEW migration_v008_user_source AS SELECT * FROM ', @user_source_table
);
PREPARE create_user_source_view_statement FROM @create_user_source_view_sql;
EXECUTE create_user_source_view_statement;
DEALLOCATE PREPARE create_user_source_view_statement;
SET @create_user_role_source_view_sql = CONCAT(
    'CREATE OR REPLACE VIEW migration_v008_user_role_source AS SELECT * FROM ', @user_role_source_table
);
PREPARE create_user_role_source_view_statement FROM @create_user_role_source_view_sql;
EXECUTE create_user_role_source_view_statement;
DEALLOCATE PREPARE create_user_role_source_view_statement;
SET @create_user_store_source_view_sql = CONCAT(
    'CREATE OR REPLACE VIEW migration_v008_user_store_source AS SELECT * FROM ', @user_store_source_table
);
PREPARE create_user_store_source_view_statement FROM @create_user_store_source_view_sql;
EXECUTE create_user_store_source_view_statement;
DEALLOCATE PREPARE create_user_store_source_view_statement;

UPDATE sys_platform_user target
JOIN migration_v008_user_source source
  ON source.id = target.id AND source.client_type = 'PLATFORM'
SET target.username = source.username,
    target.password_hash = source.password_hash,
    target.display_name = source.display_name,
    target.email = source.email,
    target.phone = source.phone,
    target.data_scope = source.data_scope,
    target.status = source.status,
    target.deleted = 0;

UPDATE sys_merchant_user target
JOIN migration_v008_user_source source
  ON source.tenant_id = target.tenant_id
 AND source.username = target.username
 AND source.client_type = 'MERCHANT'
SET target.primary_store_id = source.primary_store_id,
    target.password_hash = source.password_hash,
    target.display_name = source.display_name,
    target.email = source.email,
    target.phone = source.phone,
    target.login_enabled = 1,
    target.data_scope = source.data_scope,
    target.status = source.status,
    target.deleted = 0;

INSERT IGNORE INTO sys_platform_user_role (platform_user_id, role_id)
SELECT platform_user.id, relation.role_id
FROM sys_platform_user platform_user
JOIN migration_v008_user_source source_user
  ON source_user.id = platform_user.id AND source_user.client_type = 'PLATFORM'
JOIN migration_v008_user_role_source relation ON relation.user_id = source_user.id
JOIN sys_role role
  ON role.id = relation.role_id AND role.client_type = 'PLATFORM' AND role.tenant_id = 0;

INSERT IGNORE INTO sys_merchant_user_role (merchant_user_id, role_id)
SELECT merchant_user.id, relation.role_id
FROM sys_merchant_user merchant_user
JOIN migration_v008_user_source source_user
  ON source_user.tenant_id = merchant_user.tenant_id
 AND source_user.username = merchant_user.username
 AND source_user.client_type = 'MERCHANT'
JOIN migration_v008_user_role_source relation ON relation.user_id = source_user.id
JOIN sys_role role
  ON role.id = relation.role_id
 AND role.client_type = 'MERCHANT'
 AND role.tenant_id = merchant_user.tenant_id;

INSERT IGNORE INTO sys_merchant_user_store (merchant_user_id, store_id)
SELECT merchant_user.id, relation.store_id
FROM sys_merchant_user merchant_user
JOIN migration_v008_user_source source_user
  ON source_user.tenant_id = merchant_user.tenant_id
 AND source_user.username = merchant_user.username
 AND source_user.client_type = 'MERCHANT'
JOIN migration_v008_user_store_source relation ON relation.user_id = source_user.id
JOIN sys_store store_record
  ON store_record.id = relation.store_id AND store_record.tenant_id = merchant_user.tenant_id;

DROP VIEW migration_v008_user_source;
DROP VIEW migration_v008_user_role_source;
DROP VIEW migration_v008_user_store_source;

SET @rename_user_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user'),
    IF(
        EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_legacy'),
        'DROP TABLE sys_user',
        'RENAME TABLE sys_user TO sys_user_legacy'
    ), 'SELECT 1'
);
PREPARE rename_user_statement FROM @rename_user_sql;
EXECUTE rename_user_statement;
DEALLOCATE PREPARE rename_user_statement;

SET @rename_user_role_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_role'),
    IF(
        EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_role_legacy'),
        'DROP TABLE sys_user_role',
        'RENAME TABLE sys_user_role TO sys_user_role_legacy'
    ), 'SELECT 1'
);
PREPARE rename_user_role_statement FROM @rename_user_role_sql;
EXECUTE rename_user_role_statement;
DEALLOCATE PREPARE rename_user_role_statement;

SET @rename_user_store_sql = IF(
    EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_store'),
    IF(
        EXISTS(SELECT 1 FROM information_schema.TABLES WHERE TABLE_SCHEMA = 'tql_store_saas' AND TABLE_NAME = 'sys_user_store_legacy'),
        'DROP TABLE sys_user_store',
        'RENAME TABLE sys_user_store TO sys_user_store_legacy'
    ), 'SELECT 1'
);
PREPARE rename_user_store_statement FROM @rename_user_store_sql;
EXECUTE rename_user_store_statement;
DEALLOCATE PREPARE rename_user_store_statement;

ALTER TABLE sys_user_legacy COMMENT = '历史统一用户表，仅用于迁移审计，运行时代码禁止访问';
ALTER TABLE sys_user_role_legacy COMMENT = '历史用户角色关联表，仅用于迁移审计，运行时代码禁止访问';
ALTER TABLE sys_user_store_legacy COMMENT = '历史用户门店关联表，仅用于迁移审计，运行时代码禁止访问';

-- 本地引导账号保留认证能力，但不进入真实业务目录；示例组织、门店和内容统一停用。
UPDATE sys_merchant_user
SET organization_id = NULL, primary_store_id = NULL, email = NULL, phone = NULL, directory_visible = 0
WHERE tenant_id = 10001 AND username = 'merchant_admin' AND source_type = 'LOCAL';
UPDATE sys_tenant SET tenant_name = '同庆楼'
WHERE id = 10001 AND tenant_code = 'TQL-DEMO' AND tenant_name = '同庆楼示范商家';
UPDATE sys_merchant_organization SET status = 0, deleted = 1
WHERE tenant_id = 10001 AND source_type = 'LOCAL'
  AND (id = 11001 OR org_code = 'TQL-HQ' OR org_name = '同庆楼示范商家总部');
UPDATE sys_store SET status = 0
WHERE tenant_id = 10001 AND id IN (11001, 11002, 11003, 11004)
  AND store_code IN ('TQL-HQ', 'TQL-BH', 'TQL-BH-LAKE', 'TQL-XZ');
DELETE FROM sys_merchant_user_store WHERE merchant_user_id = 2 AND store_id IN (11001, 11002, 11003, 11004);
UPDATE ops_content SET deleted = 1
WHERE tenant_id = 10001 AND id BETWEEN 1001 AND 1008 AND create_by = 2;
