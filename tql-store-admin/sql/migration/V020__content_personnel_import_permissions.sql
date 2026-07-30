USE tql_store_saas;

INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT parent.tenant_id, parent.id, permission_item.menu_name, 'BUTTON', NULL, NULL, NULL,
       NULL, permission_item.permission_code, 'MERCHANT', permission_item.sort_order, 0, 1, 0, 0
FROM sys_menu parent
JOIN (
    SELECT
        '导入人员/下载模板' AS menu_name,
        'merchant:content:plan:employee:import' AS permission_code,
        31 AS sort_order
    UNION ALL
    SELECT
        '校验导入人员',
        'merchant:content:plan:employee:validate',
        32
) permission_item
WHERE parent.client_type = 'MERCHANT'
  AND parent.permission_code = 'merchant:content:plan:create'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = permission_item.permission_code
        AND existing.deleted = 0
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.tenant_id = role.tenant_id
 AND menu.permission_code IN (
     'merchant:content:plan:employee:import',
     'merchant:content:plan:employee:validate'
 )
 AND menu.deleted = 0
WHERE role.client_type = 'MERCHANT'
  AND role.role_code = 'MERCHANT_ADMIN'
  AND role.status = 1;
