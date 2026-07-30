CREATE TABLE sys_icon (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '图标ID',
    icon_name VARCHAR(64) NOT NULL COMMENT '图标名称',
    icon_code VARCHAR(64) NOT NULL COMMENT '图标唯一编码',
    category VARCHAR(32) NOT NULL DEFAULT '通用' COMMENT '图标分类',
    source_type VARCHAR(16) NOT NULL COMMENT '来源：SYSTEM/CUSTOM',
    svg_content TEXT DEFAULT NULL COMMENT '清洗后的SVG内容',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0停用，1启用',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序',
    created_by BIGINT DEFAULT NULL COMMENT '创建人',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_icon_code (icon_code),
    KEY idx_icon_status (status, sort_order)
) COMMENT='统一图标库';

INSERT INTO sys_icon (id, icon_name, icon_code, category, source_type, status, sort_order)
VALUES
    (1, '工作台', 'IconDashboard', '导航', 'SYSTEM', 1, 10),
    (2, '文件', 'IconFile', '导航', 'SYSTEM', 1, 20),
    (3, '用户', 'IconUser', '导航', 'SYSTEM', 1, 30),
    (4, '设置', 'IconSettings', '导航', 'SYSTEM', 1, 40),
    (5, '同步', 'IconSync', '导航', 'SYSTEM', 1, 50),
    (6, '菜单', 'IconMenu', '导航', 'SYSTEM', 1, 60),
    (7, '应用', 'IconApps', '导航', 'SYSTEM', 1, 70)
ON DUPLICATE KEY UPDATE icon_name = VALUES(icon_name), status = 1;

ALTER TABLE sys_menu
    ADD COLUMN icon_id BIGINT DEFAULT NULL COMMENT '图标库ID' AFTER icon,
    ADD KEY idx_menu_icon (icon_id);

UPDATE sys_menu m JOIN sys_icon i ON i.icon_code = m.icon
SET m.icon_id = i.id
WHERE m.icon IS NOT NULL;

INSERT INTO sys_menu
    (id, tenant_id, parent_id, menu_name, menu_type, route_name, route_path,
     component_key, icon, icon_id, permission_code, client_type, sort_order,
     visible, status, system_builtin, deleted)
VALUES
    (110, 0, 105, '图标管理', 'MENU', 'PlatformIcons', '/system/icons',
     'icon-management', 'IconApps', 7, 'platform:system:icon:view',
     'PLATFORM', 40, 1, 1, 1, 0),
    (111, 0, 110, '上传图标', 'BUTTON', NULL, NULL, NULL, NULL, NULL,
     'platform:system:icon:create', 'PLATFORM', 10, 0, 1, 1, 0),
    (112, 0, 110, '修改图标', 'BUTTON', NULL, NULL, NULL, NULL, NULL,
     'platform:system:icon:update', 'PLATFORM', 20, 0, 1, 1, 0),
    (113, 0, 110, '删除图标', 'BUTTON', NULL, NULL, NULL, NULL, NULL,
     'platform:system:icon:delete', 'PLATFORM', 30, 0, 1, 1, 0)
ON DUPLICATE KEY UPDATE parent_id=VALUES(parent_id), menu_name=VALUES(menu_name),
 component_key=VALUES(component_key), icon=VALUES(icon), icon_id=VALUES(icon_id),
 permission_code=VALUES(permission_code), status=1, deleted=0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT r.id, m.id FROM sys_role r JOIN sys_menu m ON m.id IN (110,111,112,113)
WHERE r.tenant_id=0 AND r.client_type='PLATFORM' AND r.role_code='PLATFORM_SUPER_ADMIN';
