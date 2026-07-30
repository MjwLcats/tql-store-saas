USE tql_store_saas;

-- AI内容中心升级为目录，原有查看权限继续挂在目录上，兼容历史角色授权。
UPDATE sys_menu
SET menu_name = 'AI内容中心',
    menu_type = 'DIRECTORY',
    route_name = NULL,
    route_path = '/content',
    component_key = NULL,
    icon = 'IconVideoCamera',
    visible = 1,
    status = 1,
    update_time = CURRENT_TIMESTAMP
WHERE client_type = 'MERCHANT'
  AND parent_id = 0
  AND permission_code = 'merchant:content:view'
  AND deleted = 0;

-- 四个业务菜单。
INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT parent.tenant_id, parent.id, item.menu_name, 'MENU', item.route_name, item.route_path,
       'ai-content', item.icon, item.permission_code, 'MERCHANT', item.sort_order, 1, 1, 0, 0
FROM sys_menu parent
JOIN (
    SELECT '发布计划' menu_name, 'MerchantContentPlans' route_name, '/content/plans' route_path,
           'IconApps' icon, 'merchant:content:view' permission_code, 10 sort_order
    UNION ALL SELECT '运营日历', 'MerchantContentCalendar', '/content/calendar',
           'IconCalendar', 'merchant:content:calendar:view', 20
    UNION ALL SELECT '数据中心', 'MerchantContentAnalytics', '/content/analytics',
           'IconBarChart', 'merchant:content:analytics:view', 30
    UNION ALL SELECT '账号管理', 'MerchantContentAccounts', '/content/accounts',
           'IconStorage', 'merchant:content:account:view', 40
) item
WHERE parent.client_type = 'MERCHANT'
  AND parent.menu_type = 'DIRECTORY'
  AND parent.parent_id = 0
  AND parent.permission_code = 'merchant:content:view'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.route_name = item.route_name
        AND existing.deleted = 0
  );

-- 发布计划按钮权限。
INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT parent.tenant_id, parent.id, item.menu_name, 'BUTTON', NULL, NULL, NULL,
       NULL, item.permission_code, 'MERCHANT', item.sort_order, 0, 1, 0, 0
FROM sys_menu parent
JOIN (
    SELECT '查询发布计划' menu_name, 'merchant:content:plan:query' permission_code, 10 sort_order
    UNION ALL SELECT '新建发布计划', 'merchant:content:plan:create', 20
    UNION ALL SELECT '保存计划草稿', 'merchant:content:plan:save', 30
    UNION ALL SELECT '发布计划', 'merchant:content:plan:publish', 40
    UNION ALL SELECT '查看发送明细', 'merchant:content:plan:delivery:view', 50
    UNION ALL SELECT '取消发布计划', 'merchant:content:plan:cancel', 60
    UNION ALL SELECT '导出发送明细', 'merchant:content:plan:delivery:export', 70
    UNION ALL SELECT '导出素材收集', 'merchant:content:plan:material:export', 80
    UNION ALL SELECT '选择员工', 'merchant:content:plan:employee:select', 90
    UNION ALL SELECT 'AI生成分镜脚本', 'merchant:content:plan:script:generate', 100
) item
WHERE parent.client_type = 'MERCHANT'
  AND parent.route_name = 'MerchantContentPlans'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = item.permission_code
        AND existing.deleted = 0
  );

-- 修复V013中已经创建的创建/发布按钮，使其归属发布计划菜单。
UPDATE sys_menu button_menu
JOIN sys_menu plan_menu
  ON plan_menu.tenant_id = button_menu.tenant_id
 AND plan_menu.client_type = 'MERCHANT'
 AND plan_menu.route_name = 'MerchantContentPlans'
 AND plan_menu.deleted = 0
SET button_menu.parent_id = plan_menu.id
WHERE button_menu.client_type = 'MERCHANT'
  AND button_menu.menu_type = 'BUTTON'
  AND button_menu.permission_code IN (
      'merchant:content:plan:create',
      'merchant:content:plan:publish'
  )
  AND button_menu.deleted = 0;

-- 运营日历、数据中心、账号管理按钮权限。
INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT parent.tenant_id, parent.id, item.menu_name, 'BUTTON', NULL, NULL, NULL,
       NULL, item.permission_code, 'MERCHANT', item.sort_order, 0, 1, 0, 0
FROM sys_menu parent
JOIN (
    SELECT 'MerchantContentCalendar' route_name, '新建日历计划' menu_name,
           'merchant:content:calendar:create' permission_code, 10 sort_order
    UNION ALL SELECT 'MerchantContentAnalytics', '查询内容数据',
           'merchant:content:analytics:query', 10
    UNION ALL SELECT 'MerchantContentAccounts', '查询账号',
           'merchant:content:account:query', 10
    UNION ALL SELECT 'MerchantContentAccounts', '批量导入账号',
           'merchant:content:account:import', 20
    UNION ALL SELECT 'MerchantContentAccounts', '导出账号列表',
           'merchant:content:account:export', 30
) item ON item.route_name = parent.route_name
WHERE parent.client_type = 'MERCHANT'
  AND parent.deleted = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_menu existing
      WHERE existing.tenant_id = parent.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = item.permission_code
        AND existing.deleted = 0
  );

-- 历史已获得AI内容中心权限的角色，默认保留“发布计划”菜单访问。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_menu.role_id, child.id
FROM sys_role_menu role_menu
JOIN sys_menu directory
  ON directory.id = role_menu.menu_id
 AND directory.client_type = 'MERCHANT'
 AND directory.menu_type = 'DIRECTORY'
 AND directory.permission_code = 'merchant:content:view'
 AND directory.deleted = 0
JOIN sys_menu child
  ON child.tenant_id = directory.tenant_id
 AND child.parent_id = directory.id
 AND child.route_name = 'MerchantContentPlans'
 AND child.deleted = 0;

-- 商户管理员默认拥有目录、四个菜单及全部按钮；其他角色可在角色管理中按需授权。
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu directory
  ON directory.tenant_id = role.tenant_id
 AND directory.client_type = 'MERCHANT'
 AND directory.menu_type = 'DIRECTORY'
 AND directory.permission_code = 'merchant:content:view'
 AND directory.deleted = 0
JOIN sys_menu menu
  ON menu.tenant_id = directory.tenant_id
 AND (menu.id = directory.id OR menu.parent_id = directory.id
      OR menu.parent_id IN (
          SELECT child.id FROM sys_menu child
          WHERE child.tenant_id = directory.tenant_id
            AND child.parent_id = directory.id
            AND child.deleted = 0
      ))
 AND menu.deleted = 0
WHERE role.client_type = 'MERCHANT'
  AND role.role_code = 'MERCHANT_ADMIN'
  AND role.status = 1;
