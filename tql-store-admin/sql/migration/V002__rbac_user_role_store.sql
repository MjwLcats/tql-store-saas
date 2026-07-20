USE tql_store_saas;
SET NAMES utf8mb4;

ALTER TABLE sys_user
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    ADD COLUMN primary_store_id BIGINT NULL COMMENT '用户主要所属门店ID' AFTER tenant_id,
    ADD COLUMN data_scope VARCHAR(32) NOT NULL DEFAULT 'SELF' COMMENT '数据权限范围编码' AFTER client_type,
    ADD KEY idx_user_primary_store (primary_store_id);

ALTER TABLE sys_user
    DROP INDEX uk_user_client,
    ADD UNIQUE KEY uk_user_tenant_client (tenant_id, username, client_type);

CREATE TABLE sys_store (
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

CREATE TABLE sys_role (
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

CREATE TABLE sys_user_role (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    PRIMARY KEY (user_id, role_id),
    KEY idx_user_role_role (role_id)
) ENGINE=InnoDB COMMENT='用户角色关联表';

CREATE TABLE sys_role_menu (
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    PRIMARY KEY (role_id, menu_id),
    KEY idx_role_menu_menu (menu_id)
) ENGINE=InnoDB COMMENT='角色菜单关联表';

CREATE TABLE sys_user_store (
    user_id BIGINT NOT NULL COMMENT '用户ID',
    store_id BIGINT NOT NULL COMMENT '授权门店ID',
    PRIMARY KEY (user_id, store_id),
    KEY idx_user_store_store (store_id)
) ENGINE=InnoDB COMMENT='用户授权门店关联表';

INSERT INTO sys_store (id, tenant_id, parent_id, store_code, store_name, status, sort_order)
VALUES
    (11001, 10001, 0, 'TQL-HQ', '同庆楼示范商家总部', 1, 10),
    (11002, 10001, 11001, 'TQL-BH', '同庆楼包河门店', 1, 20),
    (11003, 10001, 11001, 'TQL-BH-LAKE', '同庆楼滨湖门店', 1, 30),
    (11004, 10001, 11001, 'TQL-XZ', '同庆楼新站门店', 1, 40);

INSERT INTO sys_role (id, tenant_id, role_code, role_name, client_type, status, remark)
VALUES
    (1001, 0, 'PLATFORM_SUPER_ADMIN', '平台超级管理员', 'PLATFORM', 1, '平台端全部权限'),
    (2001, 10001, 'MERCHANT_ADMIN', '商家管理员', 'MERCHANT', 1, '商家端全部权限'),
    (2002, 10001, 'STORE_MANAGER', '门店店长', 'MERCHANT', 1, '门店日常管理权限'),
    (2003, 10001, 'STORE_STAFF', '门店员工', 'MERCHANT', 1, '门店基础操作权限');

INSERT INTO sys_menu
    (id, tenant_id, parent_id, menu_name, route_name, route_path, component_key, icon, permission_code, client_type, sort_order, visible)
VALUES
    (103, 0, 0, '平台用户', 'PlatformUsers', '/users', 'users', 'IconUser', 'platform:system:user:view', 'PLATFORM', 30, 1),
    (104, 0, 0, '平台角色', 'PlatformRoles', '/roles', 'roles', 'IconSettings', 'platform:system:role:view', 'PLATFORM', 40, 1),
    (203, 10001, 0, '用户管理', 'MerchantUsers', '/users', 'users', 'IconUser', 'merchant:system:user:view', 'MERCHANT', 30, 1),
    (204, 10001, 0, '角色管理', 'MerchantRoles', '/roles', 'roles', 'IconSettings', 'merchant:system:role:view', 'MERCHANT', 40, 1);

UPDATE sys_user SET data_scope = 'ALL' WHERE id IN (1, 2);
UPDATE sys_user SET primary_store_id = 11001 WHERE id = 2;

INSERT INTO sys_user_role (user_id, role_id) VALUES (1, 1001), (2, 2001);
INSERT INTO sys_user_store (user_id, store_id)
VALUES (2, 11001), (2, 11002), (2, 11003), (2, 11004);

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1001, id FROM sys_menu WHERE tenant_id = 0 AND client_type = 'PLATFORM';
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2001, id FROM sys_menu WHERE tenant_id = 10001 AND client_type = 'MERCHANT';
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2002, id FROM sys_menu
WHERE tenant_id = 10001 AND client_type = 'MERCHANT'
  AND route_path IN ('/dashboard', '/content', '/profile');
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 2003, id FROM sys_menu
WHERE tenant_id = 10001 AND client_type = 'MERCHANT'
  AND route_path IN ('/dashboard', '/profile');
