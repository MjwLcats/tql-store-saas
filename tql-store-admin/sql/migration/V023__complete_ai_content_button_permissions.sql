USE tql_store_saas;

-- Keep every actionable control in AI content center as a BUTTON menu.
-- Existing records are reused by permission_code so this migration is safe to rerun.
INSERT INTO sys_menu
    (tenant_id, parent_id, menu_name, menu_type, route_name, route_path, component_key,
     icon, permission_code, client_type, sort_order, visible, status, system_builtin, deleted)
SELECT page.tenant_id, page.id, permission_item.menu_name, 'BUTTON', NULL, NULL, NULL,
       NULL, permission_item.permission_code, 'MERCHANT', permission_item.sort_order,
       0, 1, 0, 0
FROM sys_menu page
JOIN (
    SELECT 'MerchantContentPlans' page_route, '查询发布计划' menu_name,
           'merchant:content:plan:query' permission_code, 10 sort_order
    UNION ALL SELECT 'MerchantContentPlans', '新建发布计划',
           'merchant:content:plan:create', 20
    UNION ALL SELECT 'MerchantContentPlans', '保存计划草稿',
           'merchant:content:plan:save', 30
    UNION ALL SELECT 'MerchantContentPlans', '导入人员/下载模板',
           'merchant:content:plan:employee:import', 31
    UNION ALL SELECT 'MerchantContentPlans', '校验导入人员',
           'merchant:content:plan:employee:validate', 32
    UNION ALL SELECT 'MerchantContentPlans', '发布计划',
           'merchant:content:plan:publish', 40
    UNION ALL SELECT 'MerchantContentPlans', '查看人员',
           'merchant:content:plan:delivery:view', 50
    UNION ALL SELECT 'MerchantContentPlans', '取消发布计划',
           'merchant:content:plan:cancel', 60
    UNION ALL SELECT 'MerchantContentPlans', '编辑发布计划',
           'merchant:content:plan:update', 65
    UNION ALL SELECT 'MerchantContentPlans', '导出人员',
           'merchant:content:plan:delivery:export', 70
    UNION ALL SELECT 'MerchantContentPlans', '导出素材收集',
           'merchant:content:plan:material:export', 80
    UNION ALL SELECT 'MerchantContentPlans', '选择组织/人员',
           'merchant:content:plan:employee:select', 90
    UNION ALL SELECT 'MerchantContentPlans', 'AI生成分镜脚本',
           'merchant:content:plan:script:generate', 100

    UNION ALL SELECT 'MerchantContentCalendar', '新建日历计划',
           'merchant:content:calendar:create', 10

    UNION ALL SELECT 'MerchantContentAnalytics', '查询内容数据',
           'merchant:content:analytics:query', 10

    UNION ALL SELECT 'MerchantContentAccounts', '查询账号',
           'merchant:content:account:query', 10
    UNION ALL SELECT 'MerchantContentAccounts', '查看账号',
           'merchant:content:account:detail:view', 11
    UNION ALL SELECT 'MerchantContentAccounts', '新增账号',
           'merchant:content:account:create', 15
    UNION ALL SELECT 'MerchantContentAccounts', '编辑账号',
           'merchant:content:account:update', 16
    UNION ALL SELECT 'MerchantContentAccounts', '删除账号',
           'merchant:content:account:delete', 17
    UNION ALL SELECT 'MerchantContentAccounts', '导入账号',
           'merchant:content:account:import', 20
    UNION ALL SELECT 'MerchantContentAccounts', '下载账号',
           'merchant:content:account:export', 30
) permission_item ON permission_item.page_route = page.route_name
WHERE page.client_type = 'MERCHANT'
  AND page.menu_type = 'MENU'
  AND page.deleted = 0
  AND NOT EXISTS (
      SELECT 1
      FROM sys_menu existing
      WHERE existing.tenant_id = page.tenant_id
        AND existing.client_type = 'MERCHANT'
        AND existing.permission_code = permission_item.permission_code
        AND existing.deleted = 0
  );

-- Normalize names, ownership and visibility for permissions created by older migrations.
UPDATE sys_menu button_menu
JOIN sys_menu page
  ON page.tenant_id = button_menu.tenant_id
 AND page.client_type = 'MERCHANT'
 AND page.menu_type = 'MENU'
 AND page.deleted = 0
JOIN (
    SELECT 'MerchantContentPlans' page_route, '查询发布计划' menu_name,
           'merchant:content:plan:query' permission_code, 10 sort_order
    UNION ALL SELECT 'MerchantContentPlans', '新建发布计划', 'merchant:content:plan:create', 20
    UNION ALL SELECT 'MerchantContentPlans', '保存计划草稿', 'merchant:content:plan:save', 30
    UNION ALL SELECT 'MerchantContentPlans', '导入人员/下载模板', 'merchant:content:plan:employee:import', 31
    UNION ALL SELECT 'MerchantContentPlans', '校验导入人员', 'merchant:content:plan:employee:validate', 32
    UNION ALL SELECT 'MerchantContentPlans', '发布计划', 'merchant:content:plan:publish', 40
    UNION ALL SELECT 'MerchantContentPlans', '查看人员', 'merchant:content:plan:delivery:view', 50
    UNION ALL SELECT 'MerchantContentPlans', '取消发布计划', 'merchant:content:plan:cancel', 60
    UNION ALL SELECT 'MerchantContentPlans', '编辑发布计划', 'merchant:content:plan:update', 65
    UNION ALL SELECT 'MerchantContentPlans', '导出人员', 'merchant:content:plan:delivery:export', 70
    UNION ALL SELECT 'MerchantContentPlans', '导出素材收集', 'merchant:content:plan:material:export', 80
    UNION ALL SELECT 'MerchantContentPlans', '选择组织/人员', 'merchant:content:plan:employee:select', 90
    UNION ALL SELECT 'MerchantContentPlans', 'AI生成分镜脚本', 'merchant:content:plan:script:generate', 100
    UNION ALL SELECT 'MerchantContentCalendar', '新建日历计划', 'merchant:content:calendar:create', 10
    UNION ALL SELECT 'MerchantContentAnalytics', '查询内容数据', 'merchant:content:analytics:query', 10
    UNION ALL SELECT 'MerchantContentAccounts', '查询账号', 'merchant:content:account:query', 10
    UNION ALL SELECT 'MerchantContentAccounts', '查看账号', 'merchant:content:account:detail:view', 11
    UNION ALL SELECT 'MerchantContentAccounts', '新增账号', 'merchant:content:account:create', 15
    UNION ALL SELECT 'MerchantContentAccounts', '编辑账号', 'merchant:content:account:update', 16
    UNION ALL SELECT 'MerchantContentAccounts', '删除账号', 'merchant:content:account:delete', 17
    UNION ALL SELECT 'MerchantContentAccounts', '导入账号', 'merchant:content:account:import', 20
    UNION ALL SELECT 'MerchantContentAccounts', '下载账号', 'merchant:content:account:export', 30
) permission_item
  ON permission_item.page_route = page.route_name
 AND permission_item.permission_code = button_menu.permission_code
SET button_menu.parent_id = page.id,
    button_menu.menu_name = permission_item.menu_name,
    button_menu.menu_type = 'BUTTON',
    button_menu.route_name = NULL,
    button_menu.route_path = NULL,
    button_menu.component_key = NULL,
    button_menu.icon = NULL,
    button_menu.sort_order = permission_item.sort_order,
    button_menu.visible = 0,
    button_menu.status = 1,
    button_menu.update_time = CURRENT_TIMESTAMP
WHERE button_menu.client_type = 'MERCHANT'
  AND button_menu.deleted = 0;

-- Administrators receive all current and future AI content permissions by default.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role.id, menu.id
FROM sys_role role
JOIN sys_menu menu
  ON menu.tenant_id = role.tenant_id
 AND menu.client_type = 'MERCHANT'
 AND menu.permission_code LIKE 'merchant:content:%'
 AND menu.deleted = 0
 AND menu.status = 1
WHERE role.client_type = 'MERCHANT'
  AND UPPER(role.role_code) IN ('MERCHANT_ADMIN', 'ADMIN', 'SUPER_ADMIN')
  AND role.status = 1;
