USE tql_store_saas;

-- 页面访问权限与“查看账号”按钮权限分离，便于角色按按钮分配。
INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT parent.tenant_id, parent.id, '查看账号', 'BUTTON', NULL, NULL, NULL,
       NULL, 'merchant:content:account:detail:view', 'MERCHANT', 14, 0, 1, 0, 0
FROM sys_menu parent
WHERE parent.client_type = 'MERCHANT'
  AND parent.route_name = 'MerchantContentAccounts'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = 'merchant:content:account:detail:view'
        AND existing.deleted = 0
  );

-- 商家管理员拥有本商户全部有效目录、菜单和按钮权限。
-- 后续通过菜单管理新增的权限会由 MenuManagementService 自动关联。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.tenant_id = role.tenant_id
 AND menu.client_type = 'MERCHANT'
 AND menu.deleted = 0
WHERE role.client_type = 'MERCHANT'
  AND role.role_code = 'MERCHANT_ADMIN'
  AND role.status = 1;
