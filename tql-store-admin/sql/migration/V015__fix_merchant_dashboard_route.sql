USE tql_store_saas;

-- 商家工作台必须加载仪表盘组件，避免误命中角色管理页面。
UPDATE sys_menu
SET menu_name = '商家工作台',
    menu_type = 'MENU',
    route_name = 'MerchantDashboard',
    route_path = '/dashboard',
    component_key = 'dashboard',
    icon = 'IconDashboard',
    visible = 1,
    status = 1,
    update_time = CURRENT_TIMESTAMP
WHERE client_type = 'MERCHANT'
  AND permission_code = 'merchant:dashboard:view'
  AND deleted = 0;
