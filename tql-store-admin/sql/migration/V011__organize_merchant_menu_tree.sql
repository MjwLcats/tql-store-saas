INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path,
     component_key, icon, permission_code, client_type, sort_order,
     visible, status, system_builtin, deleted)
SELECT tenant.id, 0, '系统管理', 'DIRECTORY', NULL, '/system',
       NULL, 'IconSettings', NULL, 'MERCHANT', 30,
       1, 1, 0, 0
FROM sys_tenant tenant
WHERE tenant.status = 1
  AND NOT EXISTS (
      SELECT 1
      FROM sys_menu existing
      WHERE existing.tenant_id = tenant.id
        AND existing.client_type = 'MERCHANT'
        AND existing.parent_id = 0
        AND existing.menu_type = 'DIRECTORY'
        AND existing.menu_name = '系统管理'
        AND existing.deleted = 0
  );

UPDATE sys_menu child
JOIN sys_menu directory
  ON directory.tenant_id = child.tenant_id
 AND directory.client_type = 'MERCHANT'
 AND directory.parent_id = 0
 AND directory.menu_type = 'DIRECTORY'
 AND directory.menu_name = '系统管理'
 AND directory.deleted = 0
SET child.parent_id = directory.id,
    child.sort_order = CASE child.component_key
        WHEN 'users' THEN 10
        WHEN 'roles' THEN 20
        ELSE child.sort_order
    END
WHERE child.client_type = 'MERCHANT'
  AND child.component_key IN ('users', 'roles')
  AND child.deleted = 0;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT DISTINCT role_menu.role_id, directory.id
FROM sys_role_menu role_menu
JOIN sys_menu child
  ON child.id = role_menu.menu_id
 AND child.client_type = 'MERCHANT'
 AND child.component_key IN ('users', 'roles')
 AND child.deleted = 0
JOIN sys_menu directory
  ON directory.id = child.parent_id
 AND directory.menu_type = 'DIRECTORY'
 AND directory.deleted = 0;

