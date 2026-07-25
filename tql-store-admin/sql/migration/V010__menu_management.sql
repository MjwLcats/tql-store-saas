ALTER TABLE sys_menu
    DROP INDEX uk_menu_client_route,
    MODIFY COLUMN id BIGINT NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    MODIFY COLUMN route_name VARCHAR(64) DEFAULT NULL COMMENT '前端路由名称',
    MODIFY COLUMN route_path VARCHAR(128) DEFAULT NULL COMMENT '前端路由路径',
    MODIFY COLUMN component_key VARCHAR(64) DEFAULT NULL COMMENT '前端组件标识',
    ADD COLUMN menu_type VARCHAR(16) NOT NULL DEFAULT 'MENU' COMMENT '节点类型：DIRECTORY目录，MENU菜单，BUTTON按钮' AFTER menu_name,
    ADD COLUMN status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用' AFTER visible,
    ADD COLUMN system_builtin TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统内置：0否，1是' AFTER status,
    ADD COLUMN deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常，1删除' AFTER system_builtin,
    ADD COLUMN create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间' AFTER deleted,
    ADD COLUMN update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间' AFTER create_time,
    ADD UNIQUE KEY uk_menu_tenant_route_name (tenant_id, client_type, route_name),
    ADD KEY idx_menu_parent (tenant_id, client_type, parent_id, deleted),
    ADD KEY idx_menu_permission (tenant_id, client_type, permission_code, deleted);

UPDATE sys_menu
SET menu_type = 'MENU',
    status = 1,
    system_builtin = IF(client_type = 'PLATFORM', 1, 0),
    deleted = 0;

INSERT INTO sys_menu
    (id, tenant_id, parent_id, menu_name, menu_type, route_name, route_path,
     component_key, icon, permission_code, client_type, sort_order, visible,
     status, system_builtin, deleted)
VALUES
    (105, 0, 0, '系统管理', 'DIRECTORY', NULL, '/system',
     NULL, 'IconSettings', NULL, 'PLATFORM', 30, 1, 1, 1, 0),
    (106, 0, 105, '菜单管理', 'MENU', 'PlatformMenus', '/system/menus',
     'menu-management', 'IconMenu', 'platform:system:menu:view',
     'PLATFORM', 30, 1, 1, 1, 0),
    (107, 0, 106, '新增菜单', 'BUTTON', NULL, NULL,
     NULL, NULL, 'platform:system:menu:create',
     'PLATFORM', 10, 0, 1, 1, 0),
    (108, 0, 106, '修改菜单', 'BUTTON', NULL, NULL,
     NULL, NULL, 'platform:system:menu:update',
     'PLATFORM', 20, 0, 1, 1, 0),
    (109, 0, 106, '删除菜单', 'BUTTON', NULL, NULL,
     NULL, NULL, 'platform:system:menu:delete',
     'PLATFORM', 30, 0, 1, 1, 0)
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

UPDATE sys_menu
SET parent_id = 105, sort_order = 10
WHERE id = 103 AND tenant_id = 0 AND client_type = 'PLATFORM';

UPDATE sys_menu
SET parent_id = 105, sort_order = 20
WHERE id = 104 AND tenant_id = 0 AND client_type = 'PLATFORM';

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id
FROM sys_role r
JOIN sys_menu m ON m.id IN (105, 106, 107, 108, 109)
WHERE r.tenant_id = 0
  AND r.client_type = 'PLATFORM'
  AND r.role_code = 'PLATFORM_SUPER_ADMIN';
