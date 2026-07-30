USE tql_store_saas;

UPDATE sys_menu
SET menu_name = '人员', update_time = CURRENT_TIMESTAMP
WHERE client_type = 'MERCHANT'
  AND permission_code = 'merchant:content:plan:delivery:view'
  AND deleted = 0;

UPDATE sys_menu
SET menu_name = '导出人员', update_time = CURRENT_TIMESTAMP
WHERE client_type = 'MERCHANT'
  AND permission_code = 'merchant:content:plan:delivery:export'
  AND deleted = 0;

INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT parent.tenant_id, parent.id, '编辑发布计划', 'BUTTON', NULL, NULL, NULL,
       NULL, 'merchant:content:plan:update', 'MERCHANT', 65, 0, 1, 0, 0
FROM sys_menu parent
WHERE parent.client_type = 'MERCHANT'
  AND parent.route_name = 'MerchantContentPlans'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = 'merchant:content:plan:update'
        AND existing.deleted = 0
  );

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.tenant_id = role.tenant_id
 AND menu.client_type = 'MERCHANT'
 AND menu.permission_code = 'merchant:content:plan:update'
 AND menu.deleted = 0
WHERE role.client_type = 'MERCHANT'
  AND role.role_code IN ('MERCHANT_ADMIN', 'ADMIN');
